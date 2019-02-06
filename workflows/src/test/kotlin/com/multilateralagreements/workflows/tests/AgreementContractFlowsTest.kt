package com.multilateralagreements.workflows.tests

import com.multilateralagreements.workflows.CreateAgreementFlow
import com.multilateralagreements.workflows.CreateAgreementResponderFlow
import com.multilateralagreements.contracts.AgreementState
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assert


class AgreementContractFlowsTest {

    val mockNetworkParameters = MockNetworkParameters(listOf(TestCordapp.findCordapp("com.multilateralagreements.contracts")))

    private val network = MockNetwork(mockNetworkParameters)

    private val a = network.createNode()
    private val b = network.createNode()

    private val partya = a.info.legalIdentities.first()
    private val partyb = b.info.legalIdentities.first()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(CreateAgreementResponderFlow::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `dummy test`() {

        val flow = CreateAgreementFlow("This is a mock agreement", partyb)

        val future = a.startFlow(flow)

        network.runNetwork()

        val returnedTx = future.getOrThrow()

        assert(returnedTx.toLedgerTransaction(a.services).outputs.single().data is AgreementState)



    }
}