package com.multilateralagreements.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

// ************
// * Contract *
// ************
class AgreementContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.multilateralagreements.contracts.AgreementContract"

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create: Commands
        class Agree : Commands
    }


    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        val commands = tx.commandsOfType<AgreementContract.Commands>()

        if( commands.size != 1){
            throw IllegalArgumentException("Transaction must contain exactly one Agreement Contract Command")
        }

        val command = commands.first()

        when (command.value){

            is Commands.Create -> verifyCreateTransaction(tx, command)
            is Commands.Agree -> verifyAgreeTransaction(tx, command)
            else -> throw IllegalArgumentException("Unsupported command ${command.value}")
        }

        // dummy checks:

//        requireThat {
//            val outputState: AgreementState = tx.outputStates.single() as AgreementState
//            "contains the mock agreement" using (outputState.agreementDetails == "This is a mock agreement" )
//        }



    }


    private fun verifyCreateTransaction(tx: LedgerTransaction, command: Command<Commands>){

        requireThat{

            "There should be no inputs" using (tx.inputStates.isEmpty())
            "There should be a single output of type AgreementState" using (tx.outputStates.size == 1 && tx.outputStates.first() is AgreementState)

            val output = tx.outputStates.single() as AgreementState
            "Either party1 or party2 should be signer" using ((command.signers union listOf(output.party1.owningKey, output.party2.owningKey)).isNotEmpty())
            "AgreementState Status should be DRAFT" using (output.status == AgreementStateStatus.DRAFT)
        }
    }

    private fun verifyAgreeTransaction(tx: LedgerTransaction, command: Command<Commands>){


        // remember - there will be other input states not just AgreementStates.

        requireThat {

//            "forced fail" using (false)

            // AgreementState inputs

            val agreementStateInputs = tx.inputsOfType<AgreementState>()
            "There should be one input state of type AgreementState" using (agreementStateInputs.size == 1)

            val agreementStateInput = agreementStateInputs.single()
            "Input AgreementState should have status DRAFT" using(agreementStateInput.status == AgreementStateStatus.DRAFT)

            // AgreementState outputs

            val agreementStateOutputs= tx.outputsOfType<AgreementState>()
            "There should be one output state of type AgreementState" using (agreementStateOutputs.size ==1)
            val agreementStateOutput = agreementStateOutputs.single()
            "Output AgreementState should have status AGREE" using (agreementStateOutput.status == AgreementStateStatus.AGREED)


            // Signatures

            "Both party1 and party2 must sign the transaction" using (setOf<PublicKey>(agreementStateInput.party1.owningKey, agreementStateInput.party2.owningKey) == command.signers.toSet())



            // Requires Proposal State

            val agreementStateInputStateAndRef = tx.inputs.filterStatesOfType<AgreementState>().single()
            val proposalStateStateAndRef = tx.inputs.filterStatesOfType<ProposalState>().single()



            "There must be a ProposalState who's currentStateRef matches the AgreementState StateRef" using (proposalStateStateAndRef.state.data.currentStateRef == agreementStateInputStateAndRef.ref)

            val proposalState = proposalStateStateAndRef.state.data

            "Transaction must contain a ProposalState whose candidateState is the same as the output AgreementState" using (proposalState.candidateState == agreementStateOutput)


            // require a signed ready state for each responder

            val readyStateStateAndRefs = tx.inputs.filterStatesOfType<ReadyState>()
            val readyStateStates = readyStateStateAndRefs.map { it.state.data }



        }



    }

}