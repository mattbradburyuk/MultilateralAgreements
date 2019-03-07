package com.multilateralagreements.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************

class ProposalContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.multilateralagreements.contracts.ProposalContract"
    }

// todo: Make the contract and state current state agnostic

// todo: current can only have one Propose state in a transcation - make it so you can have multiple

// todo add constraints around expiry time

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Finalise: Commands
        class Cancel: Commands
        class Consent: Commands
        class RevokeConsent: Commands
    }

    override fun verify(tx: LedgerTransaction) {

        val commands = tx.commandsOfType<ProposalContract.Commands>()

        requireThat {
            "There should be exactly one Propose Contract command" using (commands.size == 1)
        }

        val command = commands.single()

        when (command.value) {
            // Proposal State Commands
            is Commands.Propose -> verifyProposeTransaction(tx, command)
            is Commands.Finalise -> verifyFinaliseTransaction(tx, command)
            is Commands.Cancel -> verifyCancelTransaction(tx, command)
            // DraftState Commands
            is Commands.Consent -> verifyConsentTransaction(tx, command)
            is Commands.RevokeConsent -> verifyRevokeConsentTransaction(tx, command)
            else -> throw  IllegalArgumentException("Unsupported command ${command.value}")
        }
    }


    private fun verifyProposeTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ProposalState Inputs
            val proposalStateInputs = tx.inputsOfType<ProposalState>()
            "There should be no ProposalState inputs" using (proposalStateInputs.isEmpty())

            // ProposalState Outputs
            val proposalStateOutputs = tx.outputsOfType<ProposalState>()
            "There should be a single output of type ProposalState" using (proposalStateOutputs.size == 1)

            // Reference states
            val referenceAgreementStates = tx.referenceInputsOfType<AgreementState>()
            "There should be one existing AgreementState as a reference state" using (referenceAgreementStates.size == 1)

            val proposalStateOutput = proposalStateOutputs.single()

            /**
             *     todo: check this logic now changed to stateref
             *     This error message will never be called because if there is only one AgreementState referenced and the
             *     CurrentStatePointer points to different state, then when the currentStatePointer.resolve(tx) is called,
             *     it will error because the state that currentStatePointer is pointing to cannot be in the transaction.
             *     Hence throwing a "Contract verification failed: Collection contains no element matching the predicate"
             *     error rather than this error message.
             *
             *     For unit tests use this.fails(), rather than this.failsWith("Resolved currentStatePointer must match Reference State StateAndRef")
             */

            "Resolved currentStatePointer must match Reference State StateRef" using (proposalStateOutput.currentStateRef ==
                    tx.referenceInputRefsOfType<AgreementState>().single().ref   )

            // todo: add check that LinearId of Proposal State is same as Current State

            // Signatures
            "Proposer should sign the transaction" using (proposalStateOutputs.first().proposer.owningKey in command.signers)
        }
    }


    private fun verifyFinaliseTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ProposalState Inputs
//            val proposalStateInputs = tx.inputsOfType<ProposalState>()
            val proposalStateInputsStateAndRefs= tx.inputs.filterStatesOfType<ProposalState>()
            "There should be a single ProposalState inputs" using (proposalStateInputsStateAndRefs.size ==1 )

            // ProposalState Outputs
            val proposalStateOutputs = tx.outputsOfType<ProposalState>()
            "There should no output of type ProposalState" using (proposalStateOutputs.isEmpty())

            // Signatures
            val proposalStateStateAndRef = proposalStateInputsStateAndRefs.single()
            val proposalState = proposalStateStateAndRef.state.data
            val validSigners = listOf(proposalState.proposer).union(proposalState.responders)
            val validSignersKeys = validSigners.map{it.owningKey}

            "The proposer or any responder should sign the transaction" using (command.signers.intersect(validSignersKeys).isNotEmpty())

            // require a ReadyState for each responder

            val readyStateStateAndRefs = tx.inputs.filterStatesOfType<ReadyState>()
            val readyStates = readyStateStateAndRefs.map { it.state.data }

            //identify proposer

            proposalState.responders.forEach { responder ->

                val matchedReadyState = readyStates.filter {it.owner == responder}

                "Each responder must have a readyState which they own" using (matchedReadyState.size == 1)

                "Each ReadyState must refer to the correct ProposalState" using (matchedReadyState.single().proposalStateRef == proposalStateStateAndRef.ref)


            }




        }
    }



    private fun verifyCancelTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ProposalState Inputs
            val proposalStateInputs = tx.inputsOfType<ProposalState>()
            "There should be a single ProposalState inputs" using (proposalStateInputs.size ==1 )

            // ProposalState Outputs
            val proposalStateOutputs = tx.outputsOfType<ProposalState>()
            "There should no output of type ProposalState" using (proposalStateOutputs.isEmpty())

            // Signatures
            val proposalStateInput = proposalStateInputs.single()
            "The proposer should sign the transaction" using (proposalStateInput.proposer.owningKey in command.signers)
        }
    }

    private fun verifyConsentTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ProposalState Inputs
            val readyStateInputs = tx.inputsOfType<ReadyState>()
            "There should be no ReadyState inputs" using (readyStateInputs.isEmpty())

            // ProposalState Outputs
            val readyStateOutputs = tx.outputsOfType<ReadyState>()
            "There should be a single output of type ReadyState" using (readyStateOutputs.size == 1)
            val readyStateOutput = readyStateOutputs.single()


            // Reference states
            val referenceProposalStates = tx.referenceInputsOfType<ProposalState>()
            "There should be one existing ProposalState as a reference state" using (referenceProposalStates.size == 1)

            val referenceProposalStateAndRef = tx.referenceInputRefsOfType<ProposalState>().single()
            val referenceProposalStateRef = referenceProposalStateAndRef.ref

            // pointer checks

            /**
             *     todo: check this logic now changed to stateref
             *     This error message will never be called because if there is only one ProposalState referenced and the
             *     proposalStatePointer points to different state, then when the proposalStatePointer.resolve(tx) is called,
             *     it will error because the state that proposalStatePointer is pointing to cannot be in the transaction.
             *     Hence throwing a "Contract verification failed: Collection contains no element matching the predicate"
             *     error rather than this error message.
             *
             *     For unit tests use this.fails(), rather than this.failsWith("Resolved currentStatePointer must match Reference State StateAndRef")
             */

            "Resolved proposalStatePointer must match Reference State StateAndRef" using ( readyStateOutput.proposalStateRef ==
                    referenceProposalStateRef )

            "The currentStatePointer in the ReadyState must match the currentStatePointer in the referenced ProposalState" using (referenceProposalStateAndRef.state.data.currentStateRef == readyStateOutput.currentStateRef)


            // Signatures
            "Owner should sign the transaction" using (readyStateOutputs.first().owner.owningKey in command.signers)
        }
    }

    private fun verifyRevokeConsentTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ReadyState Inputs
            val readyStateInputs = tx.inputsOfType<ReadyState>()
            "There should be a single ReadyState input" using (readyStateInputs.size ==1 )

            // ReadyState Outputs
            val readyStateOutputs = tx.outputsOfType<ReadyState>()
            "There should no output of type ReadyState" using (readyStateOutputs.isEmpty())

            // Signatures
            val readyStateInput = readyStateInputs.single()
            "The owner should sign the transaction" using (readyStateInput.owner.owningKey in command.signers)
        }
    }
}




























































