package com.multilateralagreements.contracts.tests

import com.multilateralagreements.contracts.*
import net.corda.core.contracts.*
import net.corda.core.identity.CordaX500Name
import net.corda.serialization.internal.model.PropertyName
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import java.time.Instant


class ProposalContractTests(){
    private val ledgerServices = MockServices(
            cordappPackages = listOf("com.multilateralagreements.contracts"),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")),
            identityService = makeTestIdentityService(),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))

    private val party1 = TestIdentity(CordaX500Name.parse("O=party1,L=London,C=GB"))
    private val party2 = TestIdentity(CordaX500Name.parse("O=party2,L=NewYork,C=US"))
    private val party3 = TestIdentity(CordaX500Name.parse("O=party3,L=Zurich,C=CH"))
    private val otherIdentity = TestIdentity(CordaX500Name.parse("O=otherIdentity,L=Paris,C=FR"))


    private val mockAgreementState = AgreementState(agreementDetails = "This is a mock agreement",
            party1 = party1.party,
            party2 = party2.party)


    private val mockAgreementState_2 = AgreementState(agreementDetails = "This is a mock agreement but with different details",
            party1 = party1.party,
            party2 = party2.party)

    private val mockAgreementState_3 = AgreementState(agreementDetails = "This is a mock agreement but with different details",
            party1 = party1.party,
            party2 = party2.party,
            status = AgreementStateStatus.AGREED)



    // todo: add extra dummy states as outputs input to test filtering out of proposal states


    @Test
    fun `one proposalContract command only`(){

// todo: work out how to do unit tests involving reference states

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // two commands - same
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                command(party2.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be exactly one Propose Contract command")
            }
            // two commands, but different
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                command(party2.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should be exactly one Propose Contract command")
            }
        }
    }

    @Test
    fun `propose - no inputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // one input
            transaction {
                reference("AgreementState Label")
                input(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be no ProposalState inputs")
            }
        }
    }

    @Test
    fun `propose - outputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // two outputs
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be a single output of type ProposalState")
            }
        }
    }

    @Test
    fun `propose - reference states`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // no reference state
            transaction {
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be one existing AgreementState as a reference state")
            }
            // two reference state
            transaction {
                reference("AgreementState Label")
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be one existing AgreementState as a reference state")
            }
            // reference state doesn't match currentStatePointer
            transaction {
                reference(AgreementContract.ID, mockAgreementState_2)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.fails()
            }
        }
    }

    @Test
    fun `propose - signers`() {

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // proposer and responder sign
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(listOf(party1.publicKey,party2.publicKey), ProposalContract.Commands.Propose())
                this.verifies()
            }
            // responder signs but not proposer
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(party2.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("Proposer should sign the transaction")
            }
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, proposalState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("Proposer should sign the transaction")
            }
        }
    }

    @Test
    fun `finalise - inputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // two inputs
            transaction {
                input(ProposalContract.ID, proposalState)
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should be a single ProposalState inputs")
            }
            // non ProposalState input + proposal state
            transaction {
                input(ProposalContract.ID, proposalState)
                input(MockContract.ID, MockState(party1.party))
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
        }
    }

    @Test
    fun `finalise - outputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // non ProposalState output state
            transaction {
                input(ProposalContract.ID, proposalState)
                output(MockContract.ID,MockState(party1.party) )
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // ProposalState output
            transaction {
                input(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should no output of type ProposalState")
            }
        }
    }



    @Test
    fun `finalise - signatures`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // responder signs
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party2.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // signer not in proposer/responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, proposalState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("The proposer or any responder should sign the transaction")
            }
        }
    }

    @Test
    fun `cancel - inputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // two inputs
            transaction {
                input(ProposalContract.ID, proposalState)
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("There should be a single ProposalState inputs")
            }
            // non ProposalState input + proposal state
            transaction {
                input(ProposalContract.ID, proposalState)
                input(MockContract.ID, MockState(party1.party))
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
        }
    }

    @Test
    fun `cancel - outputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // non ProposalState output state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, proposalState)
                output(MockContract.ID,MockState(party1.party) )
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
            // ProposalState output
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("There should no output of type ProposalState")
            }
        }
    }

    @Test
    fun `cancel - signatures`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // check verifies
            transaction {
                input(ProposalContract.ID, proposalState)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // signer is a responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, proposalState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("The proposer should sign the transaction")
            }
            // signer is not proposer or responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, proposalState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("The proposer should sign the transaction")
            }
        }
    }


    @Test
    fun `consent - no inputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }

            // one input
            transaction {
                reference("ProposalState Label")
                input(ProposalContract.ID,readyState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be no ReadyState inputs")
            }
        }
    }

    @Test
    fun `consent - outputs`(){

        ledgerServices.ledger {
            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }

            // two outputs
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be a single output of type ReadyState")
            }
        }
    }

    @Test
    fun `consent - reference states`(){

//        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
//        val proposalState_2 = ProposalState(mockAgreementState_2, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
//        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
            val proposalState_2 = ProposalState(agreementStateStaticPointer, mockAgreementState_2, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }
            // no reference state
            transaction {
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be one existing ProposalState as a reference state")
            }

            // two Proposal reference states
            transaction {
                reference("ProposalState Label")
                reference(ProposalContract.ID, proposalState_2)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be one existing ProposalState as a reference state")

            }
            // two different reference states
            transaction {
                reference("ProposalState Label")
                reference("AgreementState Label")
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }
             // referenced ProposalState doesn't match hash in ReadyState
            transaction {
                reference(ProposalContract.ID, proposalState_2)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.fails()
            }
        }
    }

    @Test
    fun `consent - signers`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
            val proposalState_2 = ProposalState(agreementStateStaticPointer, mockAgreementState_2, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }

            // party1 signs
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(party1.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("Owner should sign the transaction")
            }
            //  only party who is no proposer or in responders signs
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("Owner should sign the transaction")
            }
            //  owner signs with another party
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID, readyState)
                command(listOf(party2.publicKey, otherIdentity.publicKey), ProposalContract.Commands.Consent())
                this.verifies()
            }

        }
    }

    @Test
    fun `revokeConsent - inputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
            val proposalState_2 = ProposalState(agreementStateStaticPointer, mockAgreementState_2, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                input(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.verifies()
            }
            // two inputs
            transaction {
                input(ProposalContract.ID, readyState)
                input(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.failsWith("There should be a single ReadyState input")
            }
            // non ProposalState input + proposal state
            transaction {
                input(ProposalContract.ID, readyState)
                input(MockContract.ID, MockState(party1.party))
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.verifies()
            }
        }
    }

    @Test
    fun `revokeConsent - outputs`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
            val proposalState_2 = ProposalState(agreementStateStaticPointer, mockAgreementState_2, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                input(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.verifies()
            }
            // output state
            transaction {
                input(ProposalContract.ID, readyState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.failsWith("There should no output of type ReadyState")
            }
        }
    }

    @Test
    fun `revokeConsent - signers`() {

        ledgerServices.ledger {
            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val agreementStateStaticPointer = StaticPointer<AgreementState>(agreementStateStateAndRef.ref, AgreementState::class.java)
            val proposalState = ProposalState(agreementStateStaticPointer, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
            val proposalState_2 = ProposalState(agreementStateStaticPointer, mockAgreementState_2, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val proposalStateStaticPointer = StaticPointer<ProposalState>(proposalStateStateAndRef.ref, ProposalState::class.java)
            val readyState = ReadyState(party2.party, proposalStateStaticPointer, agreementStateStaticPointer, Instant.MAX, proposalState.proposer, proposalState.responders)
            // check verifies
            transaction {
                input(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.RevokeConsent())
                this.verifies()
            }
            // signed by somebody else
            transaction {
                input(ProposalContract.ID, readyState)
                command(otherIdentity.publicKey, ProposalContract.Commands.RevokeConsent())
                this.failsWith("The owner should sign the transaction")
            }
            transaction {
                input(ProposalContract.ID, readyState)
                command(listOf(party2.publicKey, otherIdentity.publicKey), ProposalContract.Commands.RevokeConsent())
                this.verifies()
            }
        }
    }

}
































