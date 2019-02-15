package com.multilateralagreements.workflows.tests

import com.multilateralagreements.workflows.CreateAgreementFlow
import com.multilateralagreements.workflows.CreateAgreementResponderFlow
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.AgreementStateStatus
import com.multilateralagreements.workflows.AgreeAgreementFlow
import com.multilateralagreements.workflows.AgreeAgreementResponderFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assert


class AgreementContractFlowsTests {

    val mockNetworkParameters = MockNetworkParameters(listOf(TestCordapp.findCordapp("com.multilateralagreements.contracts"), TestCordapp.findCordapp("com.multilateralagreements.workflows")
            ))

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

        // check b has the transaction in its vault
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

        // agree transaction

        val flow2 = AgreeAgreementFlow(linearId, partyb)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()

        assert(returnedTx2.toLedgerTransaction(a.services).outputs.single().data is AgreementState)

        // get first transaction output states from b's vault

        val tx1OutputStateRef = returnedTx1.coreTransaction.outRef<AgreementState>(0).ref
        val criteriaTx1 = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(tx1OutputStateRef), status = Vault.StateStatus.ALL)
        val resultTx1 = b.services.vaultService.queryBy<AgreementState>(criteriaTx1)

        // get second transactions output states from b's vault

        val tx2OutputStateRef = returnedTx2.coreTransaction.outRef<AgreementState>(0).ref
        val criteriaTx2 = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(tx2OutputStateRef))
        val resultTx2 = b.services.vaultService.queryBy<AgreementState>(criteriaTx2)

        // check that second tx output state is the same as first tx but with updated status
        assert(resultTx1.states.first().state.data.copy(status = AgreementStateStatus.AGREED) ==  resultTx2.states.first().state.data)

    }
}