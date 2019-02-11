package com.multilateralagreements.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************

class ProposalContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.multilateralagreements.contracts.ProposalContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        val commands = tx.commandsOfType<Commands>()

        requireThat {
            "There should be exactly one Propose Contract command" using (commands.size == 1)
        }





    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
    }
}