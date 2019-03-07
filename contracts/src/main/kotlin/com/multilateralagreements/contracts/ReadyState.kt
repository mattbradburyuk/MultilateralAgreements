package com.multilateralagreements.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
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
data class ReadyState(
        val owner: Party,
        val proposalStateRef: StateRef,
        val currentStateRef: StateRef,
        val expiryTime: Instant,
        val proposer: Party,
        val responders: List<Party>

): ContractState, QueryableState {

    override fun generateMappedObject(schema : MappedSchema) : PersistentState {
        return when (schema) {
            is ReadyStateSchemaV1 -> ReadyStateSchemaV1.PersistentReadyState(owner, proposalStateRef, currentStateRef, proposer)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas() = listOf(ReadyStateSchemaV1)









    override val participants: List<AbstractParty> = (responders.union(listOf(proposer)).toList())


}