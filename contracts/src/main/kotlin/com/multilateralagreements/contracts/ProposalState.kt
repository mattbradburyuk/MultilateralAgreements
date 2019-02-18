package com.multilateralagreements.contracts


import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StatePointer
import net.corda.core.contracts.StaticPointer
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

// *********
// * State *
// *********

@BelongsToContract(ProposalContract::class)
data class ProposalState(

        val currentStatePointer: StaticPointer<AgreementState>,
        val candidateState: ContractState,
        val expiryTime: Instant,
        val proposer: Party,
        val responders: List<Party>

): ContractState, QueryableState {

    override fun generateMappedObject(schema : MappedSchema) : PersistentState {
        return when (schema) {
            is ProposalStateSchemaV1 -> ProposalStateSchemaV1.PersistentProposalState(currentStatePointer, proposer)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas() = listOf(ProposalStateSchemaV1)




    override val participants: List<AbstractParty> = (responders.union(listOf(proposer)).toList())

//    val hashCurrentState = getHashForState(currentState)
//    val hashCandidateState = getHashForState(candidateState)

}


// Is this the right place for this utility
fun getHashForState(state: ContractState): SecureHash.SHA256{
    return SecureHash.sha256( state.toString())
}
