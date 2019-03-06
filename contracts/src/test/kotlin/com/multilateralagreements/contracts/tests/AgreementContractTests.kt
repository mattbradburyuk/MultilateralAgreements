package com.multilateralagreements.contracts.tests



import com.multilateralagreements.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import java.time.Instant

class AgreementContractTests {
    private val ledgerServices = MockServices(
            cordappPackages = listOf("com.multilateralagreements.contracts"),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")),
            identityService = makeTestIdentityService(),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))

    private val party1 = TestIdentity(CordaX500Name.parse("O=party1,L=London,C=GB"))
    private val party2 = TestIdentity(CordaX500Name.parse("O=party2,L=NewYork,C=US"))
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

    private val mockAgreementState_4 = AgreementState(agreementDetails = "This is an unrelated mock agreement",
            party1 = party1.party,
            party2 = party2.party,
            status = AgreementStateStatus.DRAFT)


    data class DummyState(val party: Party) : ContractState {
        override val participants: List<AbstractParty> = listOf(party)
    }

    interface TestCommands : CommandData {
        class dummyCommand: TestCommands
    }


    @Test
    fun `valid commands`(){

        ledgerServices.ledger {

            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            // include non Agreement command
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                command(party1.publicKey, TestCommands.dummyCommand())
                this.verifies()
            }
            //  no Agreement command
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, TestCommands.dummyCommand())
                this.failsWith("Transaction must contain exactly one Agreement Contract Command")
            }
            // include 2 Agreement command
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                command(party2.publicKey, AgreementContract.Commands.Create())
                this.failsWith("Transaction must contain exactly one Agreement Contract Command")
            }
            // include 2 Agreement command
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                command(party2.publicKey, AgreementContract.Commands.Agree())
                this.failsWith("Transaction must contain exactly one Agreement Contract Command")
            }
        }
    }

    // create contract Tests

    @Test
    fun `create transaction - no inputs test`() {

        ledgerServices.ledger {

            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                input(AgreementContract.ID, mockAgreementState)
                output(AgreementContract.ID, mockAgreementState_2)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.`fails with`("There should be no inputs")
            }
        }
    }

    @Test
    fun `create transaction - single AgreementState output`() {

        ledgerServices.ledger {

            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                output(AgreementContract.ID, mockAgreementState_2)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.failsWith("There should be a single output of type AgreementState")
            }
            transaction {
                output(AgreementContract.ID, DummyState(party1.party))
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.failsWith("There should be a single output of type AgreementState")
            }
        }
    }

    @Test
    fun `create transaction - party1 or party 2 are signer`() {

        ledgerServices.ledger {
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(otherIdentity.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
        }
    }

    @Test
    fun `create transaction - status is DRAFT`() {

        ledgerServices.ledger {
            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                output(AgreementContract.ID, mockAgreementState_3)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.failsWith("AgreementState Status should be DRAFT")
            }
        }
    }

    // Agree Contract Tests

//    @Test
//    fun `agree - input states`(){
//
//        ledgerServices.ledger {
//            transaction{
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.verifies()
//            }
//            // no input
//            transaction{
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one input state of type AgreementState")
//            }
//            // two inputs
//            transaction{
//                input(AgreementContract.ID, mockAgreementState)
//                input(AgreementContract.ID, mockAgreementState_2)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one input state of type AgreementState")
//            }
//            // wrong state
//            transaction{
//                input(AgreementContract.ID, DummyState(party1.party))
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one input state of type AgreementState")
//            }
//            // wrong status
//            transaction{
//                input(AgreementContract.ID, mockAgreementState_3)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("Input AgreementState should have status DRAFT")
//            }
//        }
//    }

//    @Test
//    fun `agree - output states`(){
//
//        ledgerServices.ledger {
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.verifies()
//            }
//            // no output
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one output state of type AgreementState")
//            }
//            // two outputs
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one output state of type AgreementState")
//            }
//            // wrong state type
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, DummyState(party1.party))
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("There should be one output state of type AgreementState")
//            }
//            // wrong status
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_2)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("Output AgreementState should have status AGREE")
//            }
//        }
//    }

//    @Test
//    fun `agree - signatures`(){
//
//        ledgerServices.ledger {
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.verifies()
//            }
//            // swap signature order
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party2.publicKey, party1.publicKey), AgreementContract.Commands.Agree())
//                this.verifies()
//            }
//            // missing signature
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("Both party1 and party2 must sign the transaction")
//            }
//            // double signature
//            transaction {
//                input(AgreementContract.ID, mockAgreementState)
//                output(AgreementContract.ID, mockAgreementState_3)
//                command(listOf(party2.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
//                this.failsWith("Both party1 and party2 must sign the transaction")
//            }
//        }
//    }

    @Test
    fun `agree - requires ProposalState`(){

        ledgerServices.ledger {

            // Set up Agreement State on Ledger
            transaction {
                output(AgreementContract.ID, "AgreementState Label", mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            // Get StaticPointer to ref state and create the ProposalState
            val agreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "AgreementState Label")
            val proposalState = ProposalState(agreementStateStateAndRef.ref, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            // set up ProposalState on Ledger
            transaction {
                reference("AgreementState Label")
                output(ProposalContract.ID, "ProposalState Label",  proposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }

            val proposalStateStateAndRef = retrieveOutputStateAndRef(ProposalState::class.java, "ProposalState Label")
            val readyState = ReadyState(party2.party, proposalStateStateAndRef.ref, agreementStateStateAndRef.ref, Instant.MAX, proposalState.proposer, proposalState.responders)


            // Set up ReadyState on Ledger
            transaction {
                reference("ProposalState Label")
                output(ProposalContract.ID,"ReadyState Label", readyState)
                command(party2.publicKey, ProposalContract.Commands.Consent())
                this.verifies()
            }

            // verifies
            transaction {
                input("AgreementState Label")
                input("ProposalState Label")
                input("ReadyState Label")
                output(AgreementContract.ID, mockAgreementState_3)
                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
                command(listOf(party1.publicKey), ProposalContract.Commands.Finalise())
                this.verifies()
            }

            // No Proposal state
            transaction {
                input("AgreementState Label")
//                input("ProposalState Label")
                input("ReadyState Label")
                output(AgreementContract.ID, mockAgreementState_3)
                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
                command(listOf(party1.publicKey), ProposalContract.Commands.Finalise())
                this.`fails with`("There must be one and only one ProposalState whose currentStateRef equals the input AgreementState ")
            }


            // set up extra unrelated agreement state and unrelated proposal

            transaction {
                output(AgreementContract.ID, "Unrelated AgreementState Label", mockAgreementState_4)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }

            val unrelatedAgreementStateStateAndRef = retrieveOutputStateAndRef(AgreementState::class.java, "Unrelated AgreementState Label")
            val unrelatedProposalState = ProposalState(unrelatedAgreementStateStateAndRef.ref, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

            transaction {
                reference("Unrelated AgreementState Label")
                output(ProposalContract.ID, "Unrelated ProposalState Label",  unrelatedProposalState)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }


            // todo: extra AgreementContract tests

            // Extra different Proposal State  - verifies
            // verifies
            transaction {
                input("AgreementState Label")
                input("ProposalState Label")
                input("ReadyState Label")
                output(AgreementContract.ID, mockAgreementState_3)
                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
                command(listOf(party1.publicKey), ProposalContract.Commands.Finalise())
                this.verifies()
            }

            transaction {
                input("AgreementState Label")
                input("ProposalState Label")
                input("Unrelated ProposalState Label")
                input("ReadyState Label")
                output(AgreementContract.ID, mockAgreementState_3)
                command(listOf(party1.publicKey), ProposalContract.Commands.Finalise())
                command(listOf(party1.publicKey, party2.publicKey), AgreementContract.Commands.Agree())
                this.`fails with`("There should be a single ProposalState inputs")
            }




            // Two proposals for one agreement state - fails

            // Proposal doesn't match Agreement - fails

            println("Pause")


        }




    }






}
