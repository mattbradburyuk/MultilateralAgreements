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
            @Column(name = "current_pointer")
            var currentStatePointer: StaticPointer<AgreementState>,
            @Column(name = "proposer")
            var proposer : Party) : PersistentState()
}



// todo: Something wrong here, get error: org.hibernate.MappingException: Could not determine type for: net.corda.core.contracts.StaticPointer, at table: proposal_states, for columns: [org.hibernate.mapping.Column(current_pointer)]

// todo: write a simple cordapp with a queryable state without StaticPointer, get it working, then add StaticPointer to see if it breaks

// can also try compiling it against corda v3.3 before adding Static pointer