package com.multilateralagreements.contracts


import com.multilateralagreements.contracts.ProposalContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

// *********
// * State *
// *********
@BelongsToContract(ProposalContract::class)
data class ProposalState(
        val priorState: ContractState,
        val proposedState: ContractState,
        val expiryTime: Instant,
        val proposer: Party,
        val responders: List<Party>

): ContractState {

    override val participants: List<AbstractParty> = (responders.union(listOf(proposer)).toList())

    val hashpriorState = getHashForState(priorState)
    val hashProposedState = getHashForState(proposedState)




}


// Is this the right place for this utility
fun getHashForState(state: ContractState): SecureHash.SHA256{
    return SecureHash.sha256( state.toString())
}
