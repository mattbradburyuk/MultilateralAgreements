package com.multilateralagreements.contracts.tests

import com.multilateralagreements.contracts.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
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

    data class DummyState(val party: Party) : ContractState {
        override val participants: List<AbstractParty> = listOf(party)
    }

    interface TestCommands : CommandData {
        class dummyCommand: TestCommands
    }

    @Test
    fun `dummy test`() {

        val refState = mockAgreementState
        val hashRefState = getHashForState(refState)
        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Create())
                this.verifies()
            }
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())

                this.verifies()
            }
//todo: got to here - next step write the propose verify()
        }
    }

    @Test
    fun `propose - one propose command only`(){

        val refState = mockAgreementState
        val hashRefState = getHashForState(refState)
        val state = ProposalState(mockAgreementState, mockAgreementState_3, Instant.now(), party1.party, listOf(party2.party))

        ledgerServices.ledger {

            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                this.verifies()
            }
            transaction {
                reference(AgreementContract.ID, mockAgreementState)
                output(ProposalContract.ID, state)
                command(party1.publicKey, ProposalContract.Commands.Propose())
                command(party2.publicKey, ProposalContract.Commands.Propose())
                this.failsWith("There should be exactly one Propose Contract command")
            }
        }


    }

}