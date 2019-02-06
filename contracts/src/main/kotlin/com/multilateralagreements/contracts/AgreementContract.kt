package com.multilateralagreements.contracts

import com.multilateralagreements.states.AgreementState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class AgreementContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.multilateralagreements.contracts.AgreementContract"

    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {



        requireThat {

            val outputState: AgreementState = tx.outputStates.single() as AgreementState

            "contains the mock agreement" using (outputState.agreementDetails == "This is a mock agreement" ) }

        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create: Commands
        class Agree : Commands
    }
}