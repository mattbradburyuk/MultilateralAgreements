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
            @Convert(converter = StateRefToTextConverter::class)    // when explicitly stating which converter to use don't annotate the converter with
            var currentStateRef: StateRef,

            @Column(name = "proposer")
            var proposer : Party) : PersistentState()
}

@CordaSerializable
object ReadyStateSchemaV1 : MappedSchema(schemaFamily = ReadyState::class.java, version = 1, mappedTypes = listOf(PersistentReadyState::class.java)) {
    @Entity
    @Table(name = "ready_states")
    class PersistentReadyState (

            @Column(name = "owner")
            var owner: Party,

            @Column(name = "proposal_pointer")
            @Convert(converter = StateRefToTextConverter::class)    // when explicitly stating which converter to use don't annotate the converter with
            var proposalStateRef: StateRef,

            @Column(name = "current_pointer")
            @Convert(converter = StateRefToTextConverter::class)    // when explicitly stating which converter to use don't annotate the converter with
            var currentStateRef: StateRef,

            @Column(name = "proposer")
            var proposer : Party) : PersistentState()
}









/**
 * Converts to and from a StateRef into a string.
 * Used by JPA to automatically map a StateRef to a text column
 *
 * see https://stackoverflow.com/questions/45475265/corda-error-org-hibernate-instantiationexception-no-default-constructor-for-en for the gradle changes required to use this
 */

@Converter
class StateRefToTextConverter : AttributeConverter<StateRef, String> {

    val SEPERATOR: String = "|"

    override fun convertToDatabaseColumn(stateRef: StateRef?): String? = stateRef?.txhash.toString().plus(SEPERATOR).plus(stateRef?.index.toString())

    override fun convertToEntityAttribute(text: String?): StateRef? {

        val parts = text?.split(SEPERATOR)
        return parts?.let { StateRef(SecureHash.parse(it[0]), it[1].toInt())}

    }
}

