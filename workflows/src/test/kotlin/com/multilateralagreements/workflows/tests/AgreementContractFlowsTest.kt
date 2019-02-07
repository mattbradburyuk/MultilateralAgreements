package com.multilateralagreements.workflows.tests

import com.multilateralagreements.workflows.CreateAgreementFlow
import com.multilateralagreements.workflows.CreateAgreementResponderFlow
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.workflows.AgreeAgreementFlow
import com.multilateralagreements.workflows.AgreeAgreementResponderFlow
import net.corda.core.node.services.queryBy
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
            it.registerInitiatedFlow(AgreeAgreementResponderFlow::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `create agreement flow test`() {

        val flow = CreateAgreementFlow("This is a mock agreement", partyb)

        val future = a.startFlow(flow)

        network.runNetwork()

        val returnedTx = future.getOrThrow()

        assert(returnedTx.toLedgerTransaction(a.services).outputs.single().data is AgreementState)


        val result = b.services.vaultService.queryBy<AgreementState>()

        assert(result.states[0].ref.txhash == returnedTx.id)

    }

    @Test
    fun `agree agreement flow test`() {

        // need to setup create transaction

        val flow1 = CreateAgreementFlow("This is a mock agreement", partyb)
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        val returnedTx1 = future1.getOrThrow()

        val state = returnedTx1.coreTransaction.outputStates.first() as AgreementState
        val linearId = state.linearId

        // agree

        val flow2 = AgreeAgreementFlow(linearId, partyb)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()


        assert(returnedTx2.toLedgerTransaction(a.services).outputs.single().data is AgreementState)

        // todo: add test to check second transaction has been successful

//        val result = b.services.vaultService.queryBy<AgreementState>()
//
//        assert(result.states[0].ref.txhash == returnedTx.id)

    }
}