package com.multilateralagreements.contracts

import net.corda.core.contracts.StaticPointer
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@CordaSerializable
object ProposalStateSchemaV1 : MappedSchema(schemaFamily = ProposalState::class.java, version = 1, mappedTypes = listOf(PersistentProposalState::class.java)) {
    @Entity
    @Table(name = "proposal_states")
    class PersistentProposalState (

           // todo: Something wrong here, get error: org.hibernate.MappingException: Could not determine type for: net.corda.core.contracts.StaticPointer, at table: proposal_states, for columns: [org.hibernate.mapping.Column(current_pointer)]
            @Column(name = "current_pointer")
            var currentStatePointer: StaticPointer<AgreementState>,
            @Column(name = "proposer")
            var proposer : Party) : PersistentState()
}