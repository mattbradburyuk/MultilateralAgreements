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
        val proposedState: ContractState,
        val expiryTime: Instant,
        val proposer: Party,
        val responders: List<Party>

): ContractState {

    override val participants: List<AbstractParty> = listOf()


    // Is this the right way to do this??
    val hashProposedState = SecureHash.SHA256(proposedState.toString().toByteArray())


    // Is this the right place for this utility
    fun getHashForState(state: ContractState): SecureHash.SHA256{

        return SecureHash.SHA256( state.toString().toByteArray())

    }

}



