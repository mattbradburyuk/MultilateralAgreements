package com.multilateralagreements.workflows.tests

import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.AgreementStateStatus
import com.multilateralagreements.contracts.ProposalState
import com.multilateralagreements.contracts.ReadyState
import com.multilateralagreements.workflows.*
import net.corda.core.contracts.StateRef
//import com.multilateralagreements.workflows.CreateProposalFlow
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


class ProposalContractFlowsTests {
//
//    val mockNetworkParameters = MockNetworkParameters(listOf(TestCordapp.findCordapp("com.multilateralagreements.contracts"), TestCordapp.findCordapp("com.multilateralagreements.workflows")
//    ))

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
//            it.registerInitiatedFlow(CreateConsentResponderFlow::class.java)
//            it.registerInitiatedFlow(CreateProposalResponderFlow::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    // todo: split this out
    @Test
    fun `createProposalFlow test`() {

        // set up Draft AgreementState on the Ledger

        val flow = CreateAgreementFlow("This is a mock agreement", partyb)
        val future = a.startFlow(flow)
        network.runNetwork()
        val returnedTx = future.getOrThrow()

        // get the currentState and pointer from the transaction and create the ProposalState

        val currentStateRef = StateRef(returnedTx.id, 0)

        val currentState = a.services.toStateAndRef<AgreementState>(currentStateRef).state.data

        val candidateState = AgreementState("This is a modified mock Agreement",
                partya,
                partyb,
                status = AgreementStateStatus.AGREED,
                linearId = currentState.linearId)

        // CreateProposalFlow

        val flow2 = CreateProposalFlow(currentStateRef, candidateState, Instant.MAX, listOf(partyb))
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()

        // check state return from the flow is the correct type



        assert(returnedTx2.toLedgerTransaction(a.services).outputs.single().data is ProposalState)

        val proposalState = returnedTx2.toLedgerTransaction(a.services).outputs.single().data as ProposalState

        // get propose transactions output states from b's vault

        val tx2OutputStateRef = returnedTx2.coreTransaction.outRef<ProposalState>(0).ref
        val criteriaTx2 = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(tx2OutputStateRef))
        val resultTx2 = b.services.vaultService.queryBy<ProposalState>(criteriaTx2)
        val returnedProposalStateFromB = resultTx2.states.single().state.data

        // check candidateState is the same in b's vault as was put into a's flow

        assert(returnedProposalStateFromB == proposalState)


        // check

        val flow3 = GetProposalStatesFromAgreementStateRefFlow(currentStateRef)
        val future3 = a.startFlow(flow3)
        network.runNetwork()
        val returnedList = future3.getOrThrow()

        val proposalStateFromAVault = returnedList.single().state.data

        assert(proposalStateFromAVault == proposalState)

    }


    @Test
    fun `createConsentFlow test`() {

        // set up Draft AgreementState on the Ledger

        val flow = CreateAgreementFlow("This is a mock agreement", partyb)
        val future = a.startFlow(flow)
        network.runNetwork()
        val returnedTx = future.getOrThrow()

        // get the currentState and pointer from the transaction and create the ProposalState

        val currentStateRef = StateRef(returnedTx.id, 0)

        val currentState = a.services.toStateAndRef<AgreementState>(currentStateRef).state.data

        val candidateState = AgreementState("This is a modified mock Agreement",
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

        assert(readyState.currentStateRef == currentStateRef )
        assert(readyState.proposalStateRef == proposalStateRef)
    }
}