package com.multilateralagreements.contracts.tests

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

@BelongsToContract(MockContract::class)
data class MockState(val party: Party) : ContractState {
    override val participants: List<AbstractParty> = listOf(party)
}

class MockContract: Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.multilateralagreements.contracts.tests.MockContract"
    }

    interface MockCommands : CommandData {
        class mockCommand: MockCommands
    }

    override fun verify(tx: LedgerTransaction) {}

}