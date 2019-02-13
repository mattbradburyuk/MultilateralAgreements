package com.multilateralagreements.contracts

import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.sha256
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

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Finalise: Commands
        class Cancel: Commands
        class Consent: Commands
        class RevokeConsent: Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
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


            // allows other type of States as may need to Bill, for example
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
            val referenceAgreementState = referenceAgreementStates.single()
            "hashCurrentState must equal the sha256 hash of the referenced AgreementState" using (proposalStateOutput.hashCurrentState == getHashForState(referenceAgreementState))

            // Signatures
            "Proposer should sign the transaction" using (proposalStateOutputs.first().proposer.owningKey in command.signers)
        }
    }


    private fun verifyFinaliseTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ProposalState Inputs
            val proposalStateInputs = tx.inputsOfType<ProposalState>()
            "There should be a single ProposalState inputs" using (proposalStateInputs.size ==1 )

            // ProposalState Outputs
            val proposalStateOutputs = tx.outputsOfType<ProposalState>()
            "There should no output of type ProposalState" using (proposalStateOutputs.isEmpty())

            // Signatures
            val proposalStateInput = proposalStateInputs.single()
            val validSigners = listOf(proposalStateInput.proposer).union(proposalStateInput.responders)
            val validSignersKeys = validSigners.map{it.owningKey}

            "The proposer or any responder should sign the transaction" using (command.signers.intersect(validSignersKeys).isNotEmpty())

            // todo: add must have ReadyState in finalise - if this is the right place to have it - might be better in AgreementContract
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

            // allows other type of States as may need to Bill, for example
            // ProposalState Inputs
            val readyStateInputs = tx.inputsOfType<ReadyState>()
            "There should be no ReadyState inputs" using (readyStateInputs.isEmpty())

            // ProposalState Outputs
            val readyStateOutputs = tx.outputsOfType<ReadyState>()
            "There should be a single output of type ReadyState" using (readyStateOutputs.size == 1)

            // Reference states
            val referenceProposalStates = tx.referenceInputsOfType<ProposalState>()
            "There should be one existing ProposalState as a reference state" using (referenceProposalStates.size == 1)

            val readyStateOutput = readyStateOutputs.single()
            val proposalStateOutput = referenceProposalStates.single()
            "hashProposalState must equal the sha256 hash of the referenced ProposalState" using (readyStateOutput.hashProposalState == getHashForState(proposalStateOutput))

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




























































