package com.multilateralagreements.contracts.tests

import com.multilateralagreements.contracts.*
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.contracts.DummyContract
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



//    interface TestCommands : CommandData {
//        class dummyCommand: TestCommands
//    }

//    @Test
//    fun `dummy test`() {
//
//        val refState = mockAgreementState
//        val hashRefState = getHashForState(refState)
//        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
//
//        ledgerServices.ledger {
//
//            transaction {
//                output(AgreementContract.ID, mockAgreementState)
//                command(party1.publicKey, AgreementContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                reference(AgreementContract.ID, mockAgreementState)
//                output(ProposalContract.ID, state)
//                command(party1.publicKey, ProposalContract.Commands.Propose())
//
//                this.verifies()
//            }
//
//        }
//    }

    // todo: add extra dummy states as outputs input to test filtering out of proposal states

    @Test
    fun `one proposalContract command only`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // two commands - same
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                command(party2.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be exactly one Propose Contract command")
            }
            // two commands, but different
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                command(party2.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should be exactly one Propose Contract command")
            }
        }
    }

    @Test
    fun `propose - no inputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // one input
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be no ProposalState inputs")
            }
        }
    }

    @Test
    fun `propose - outputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // two outputs
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be a single output of type ProposalState")
            }
        }
    }

    @Test
    fun `propose - reference states`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // no reference state
            transaction {
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be one existing AgreementState as a reference state")
            }
            // two reference state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be one existing AgreementState as a reference state")
            }
            // reference state doesn't match hash in ProposalState
            transaction {
                reference(AgreementContract.ID, mockAgreementState_2)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("hashCurrentState must equal the sha256 hash of the referenced AgreementState")
            }
        }
    }

    @Test
    fun `propose - signers`() {

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            // proposer and responder sign
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(listOf(party1.publicKey,party2.publicKey), ProposalContract.Commands.Propose())
                this.verifies()
            }
            // responder signs but not proposer
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party2.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("Proposer should sign the transaction")
            }
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(otherIdentity.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("Proposer should sign the transaction")
            }
        }
    }

    @Test
    fun `finalise - inputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // two inputs
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should be a single ProposalState inputs")
            }
            // non ProposalState input + proposal state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                input(MockContract.ID, MockState(party1.party))
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
        }
    }

    @Test
    fun `finalise - outputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // non ProposalState output state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                output(MockContract.ID,MockState(party1.party) )
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // ProposalState output
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("There should no output of type ProposalState")
            }
        }
    }



    @Test
    fun `finalise - signatures`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party, party3.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Finalise())
                this.verifies()
            }
            // signer not in proposer/responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(otherIdentity.publicKey, ProposalContract.Commands.Finalise())
                this.failsWith("The proposer or any responder should sign the transaction")
            }
        }
    }

    @Test
    fun `cancel - inputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
            // two inputs
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("There should be a single ProposalState inputs")
            }
            // non ProposalState input + proposal state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                input(MockContract.ID, MockState(party1.party))
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
        }
    }

    @Test
    fun `cancel - outputs`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
            // non ProposalState output state
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                output(MockContract.ID,MockState(party1.party) )
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
            // ProposalState output
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("There should no output of type ProposalState")
            }
        }
    }

    @Test
    fun `cancel - signatures`(){

        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party, party3.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Cancel())
                this.verifies()
            }
            // signer is a responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(otherIdentity.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("The proposer should sign the transaction")
            }
            // signer is not proposer or responder
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                input(ProposalContract.ID, state)
                command(otherIdentity.publicKey, ProposalContract.Commands.Cancel())
                this.failsWith("The proposer should sign the transaction")
            }
        }
    }


    @Test
    fun `consent - no inputs`(){

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }
            // one input
            transaction {
                reference(ProposalContract.ID, proposalState)
                input(ProposalContract.ID,readyState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be no ReadyState inputs")
            }
        }
    }

    @Test
    fun `consent - outputs`(){

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }
            // two outputs
            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be a single output of type ReadyState")
            }
        }
    }

    @Test
    fun `consent - reference states`(){

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val proposalState_2 = ProposalState(mockAgreementState_2, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

            transaction {
                reference(ProposalContract.ID, proposalState)
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
            // two reference state
            transaction {
                reference(ProposalContract.ID, proposalState)
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("There should be one existing ProposalState as a reference state")
            }
            // // referenced ProposalState doesn't match hash in ReadyState
            transaction {
                reference(ProposalContract.ID, proposalState_2)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("hashProposalState must equal the sha256 hash of the referenced ProposalState")
            }
        }
    }

    @Test
    fun `consent - signers`(){

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }
            // party1 signs
            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(party1.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("Owner should sign the transaction")
            }
            //  only party who is no proposer or in responders signs
            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(otherIdentity.publicKey, ProposalContract.Commands.Consent())
                this.failsWith("Owner should sign the transaction")
            }
            //  owner signs with another party
            transaction {
                reference(ProposalContract.ID, proposalState)
                output(ProposalContract.ID, readyState)
                command(listOf(party2.publicKey, otherIdentity.publicKey), ProposalContract.Commands.Consent())
                this.verifies()
            }

        }
    }

    @Test
    fun `revokeConsent - inputs`(){

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

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

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders )

        ledgerServices.ledger {

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

        val proposalState = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))
        val readyState = ReadyState(party2.party, proposalState, Instant.now(), proposalState.proposer, proposalState.responders)

        ledgerServices.ledger {

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
































