package com.multilateralagreements.contracts

import com.multilateralagreements.contracts.AgreementContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********

@BelongsToContract(AgreementContract::class)
data class AgreementState(val agreementDetails: String,
                          val party1: Party,
                          val party2: Party,
                          val status: AgreementStateStatus = AgreementStateStatus.DRAFT,
                          override val linearId : UniqueIdentifier = UniqueIdentifier()
                          ): LinearState {

    override val participants: List<AbstractParty> = listOf(party1, party2)

}
// :todo work out problems with upgrading enums and find an alternative (use objects like in progress tracker??)

@CordaSerializable
enum class AgreementStateStatus{ DRAFT, AGREED}

