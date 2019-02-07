package com.multilateralagreements.contracts.tests



import com.multilateralagreements.contracts.AgreementContract
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.AgreementStateStatus
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test

class AgreementContractTests {
    private val ledgerServices = MockServices(
            cordappPackages = listOf("com.multilateralagreements.contracts"),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")),
            identityService = makeTestIdentityService())

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

    // todo Agree contract tests


}
