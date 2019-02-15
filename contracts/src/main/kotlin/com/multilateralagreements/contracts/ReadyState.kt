package com.multilateralagreements.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StaticPointer
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

// *********
// * State *
// *********
@BelongsToContract(ProposalContract::class)
data class ReadyState(
        val owner: Party,
        val proposalStatePointer: StaticPointer<ProposalState>,
        val currentStatePointer: StaticPointer<AgreementState>,
        val expiryTime: Instant,
        val proposer: Party,
        val responders: List<Party>

): ContractState {

    override val participants: List<AbstractParty> = (responders.union(listOf(proposer)).toList())


}