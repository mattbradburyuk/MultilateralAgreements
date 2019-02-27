package com.multilateralagreements.contracts

import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.hexToByteArray
import net.corda.core.utilities.toHex
import java.security.PublicKey
import javax.persistence.*

@CordaSerializable
object ProposalStateSchemaV1 : MappedSchema(schemaFamily = ProposalState::class.java, version = 1, mappedTypes = listOf(PersistentProposalState::class.java)) {
    @Entity
    @Table(name = "proposal_states")
    class PersistentProposalState (
            @Column(name = "current_pointer")
            @Convert(converter = StateRefToTextConverter::class)
            var currentStateRef: StateRef,
            @Column(name = "proposer")
            var proposer : Party) : PersistentState()
}



/**
 * Converts to and from a StateRef into a  string.
 * Used by JPA to automatically map a [StateRef] to a text column
 */
@Converter(autoApply = true)
class StateRefToTextConverter : AttributeConverter<StateRef, String> {

    val SEPERATOR: String = "|"

    override fun convertToDatabaseColumn(stateRef: StateRef): String = stateRef.txhash.toString().plus(SEPERATOR).plus(stateRef.index.toString())

    override fun convertToEntityAttribute(text: String): StateRef {

        val parts = text.split(SEPERATOR)
        return StateRef(SecureHash.parse(parts[0]), parts[1].toInt())
    }
}



// todo: Something wrong here, get error: org.hibernate.MappingException: Could not determine type for: net.corda.core.contracts.StaticPointer, at table: proposal_states, for columns: [org.hibernate.mapping.Column(current_pointer)]

// todo: write a simple cordapp with a queryable state without StaticPointer, get it working, then add StaticPointer to see if it breaks

// can also try compiling it against corda v3.3 before adding Static pointer