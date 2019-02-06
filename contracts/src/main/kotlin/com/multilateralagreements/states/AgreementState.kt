package com.multilateralagreements.states

import com.multilateralagreements.contracts.AgreementContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(AgreementContract::class)
data class AgreementState(val agreementDetails: String,
                          val party1: Party,
                          val party2: Party,
                          override val linearId : UniqueIdentifier = UniqueIdentifier()
                          ): LinearState {

    override val participants: List<AbstractParty> = listOf(party1, party2)

}


