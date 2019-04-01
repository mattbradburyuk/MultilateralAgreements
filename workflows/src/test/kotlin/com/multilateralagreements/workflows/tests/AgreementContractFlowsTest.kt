package com.multilateralagreements.workflows.tests

import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.AgreementStateStatus
import com.multilateralagreements.contracts.ReadyState
import com.multilateralagreements.workflows.*
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assert


// to configure JUnit tests:
// VM modules: -ea -javaagent:lib/quasar.jar
// working directory: .


class AgreementContractFlowsTests {

    val mnp = MockNetworkParameters(listOf(TestCordapp.findCordapp("com.multilateralagreements.contracts"), TestCordapp.findCordapp("com.multilateralagreements.workflows")
    ))

    val mockNetworkParameters = mnp.withNetworkParameters(testNetworkParameters(minimumPlatformVersion = 4))

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

    // todo: fix test - need to update for requirement to have porposal and ready state

    @Test
    fun `agree agreement flow test`() {

        // set up Draft AgreementState on the Ledger

        val flow = CreateAgreementFlow("This is a the current AgreementState", partyb)
        val future = a.startFlow(flow)
        network.runNetwork()
        val returnedTx = future.getOrThrow()

        // get the currentState and pointer from the transaction and create the ProposalState

        val currentStateRef = StateRef(returnedTx.id, 0)

        val currentState = a.services.toStateAndRef<AgreementState>(currentStateRef).state.data

        val candidateState = AgreementState("This is the proposed AgreementState",
                partya,
                partyb,
                status = AgreementStateStatus.AGREED,
                linearId = currentState.linearId)

        // CreateProposalFlow

        val flow2 = CreateProposalFlow(currentStateRef, candidateState, Instant.MAX, listOf(partyb))
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()

        val proposalStateRef = StateRef(returnedTx2.id, 0)

        // createConsentFlow

        val flow3 = CreateConsentFlow(proposalStateRef, Instant.MAX)
        val future3 = b.startFlow(flow3)
        network.runNetwork()
        val returnedTx3 = future3.getOrThrow()

        val readyStateRef = StateRef(returnedTx3.id, 0)

        val readyState = a.services.toStateAndRef<ReadyState>(readyStateRef).state.data

        assert(readyState.currentStateRef == currentStateRef)
        assert(readyState.proposalStateRef == proposalStateRef)

        // End set up

        val flow4 = AgreeAgreementFlow(currentStateRef, proposalStateRef, partyb)
        val future4 = a.startFlow(flow4)
        network.runNetwork()
        val returnedTx4 = future4.getOrThrow()

        println("MB: returnedTx4: $returnedTx4")

        val finalStateRef = StateRef(returnedTx2.id, 0)

        val finalStateAndRef = a.services.toStateAndRef<AgreementState>(finalStateRef)

        println("Pause")
    }






        // agree transaction
//
//        val flow2 = AgreeAgreementFlow(linearId, partyb)
//        val future2 = a.startFlow(flow2)
//        network.runNetwork()
//        val returnedTx2 = future2.getOrThrow()
//
//        assert(returnedTx2.toLedgerTransaction(a.services).outputs.single().data is AgreementState)
//
//        // get first transaction output states from b's vault
//
//        val tx1OutputStateRef = returnedTx1.coreTransaction.outRef<AgreementState>(0).ref
//        val criteriaTx1 = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(tx1OutputStateRef), status = Vault.StateStatus.ALL)
//        val resultTx1 = b.services.vaultService.queryBy<AgreementState>(criteriaTx1)
//
//        // get second transactions output states from b's vault
//
//        val tx2OutputStateRef = returnedTx2.coreTransaction.outRef<AgreementState>(0).ref
//        val criteriaTx2 = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(tx2OutputStateRef))
//        val resultTx2 = b.services.vaultService.queryBy<AgreementState>(criteriaTx2)
//
//        // check that second tx output state is the same as first tx but with updated status
//        assert(resultTx1.states.first().state.data.copy(status = AgreementStateStatus.AGREED) ==  resultTx2.states.first().state.data)


}