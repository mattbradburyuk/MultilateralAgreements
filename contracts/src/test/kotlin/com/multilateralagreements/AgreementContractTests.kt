package com.multilateralagreements.contracts

import com.multilateralagreements.contracts.AgreementContract
import com.multilateralagreements.contracts.AgreementState
import net.corda.core.identity.CordaX500Name
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


    private val mockAgreementState = AgreementState(agreementDetails = "This is a mock agreement",
            party1 = party1.party,
            party2 = party2.party)


    private val mockAgreementState_2 = AgreementState(agreementDetails = "This is a mock agreement but with bad agreement details",
            party1 = party1.party,
            party2 = party2.party)

    @Test
    fun `dummy test`() {

        ledgerServices.ledger {

            transaction{
                output(AgreementContract.ID, mockAgreementState)
                command(party1.publicKey, AgreementContract.Commands.Agree())
                this.verifies()

            }

            transaction{
                output(AgreementContract.ID, mockAgreementState_2)
                command(party1.publicKey, AgreementContract.Commands.Agree())
                this.`fails with`("contains the mock agreement")

            }


        }
        println(ledgerServices.myInfo)

    }
}
