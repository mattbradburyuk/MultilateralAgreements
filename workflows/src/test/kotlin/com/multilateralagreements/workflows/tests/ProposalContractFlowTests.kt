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

// to configure JUnit tests:
// VM modules: -ea -javaagent:lib/quasar.jar
// working directory: .

class ProposalContractFlowsTests {


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

        val proposalStateRef = StateRef(returnedTx2.id, 0)
        val proposalStateFromA = a.services.toStateAndRef<ProposalState>(proposalStateRef)
        val proposalStateFromB = b.services.toStateAndRef<ProposalState>(proposalStateRef)


        // check that party a and party b have the same state

        assert(proposalStateFromA == proposalStateFromB)

        val proposalState = proposalStateFromA.state.data

        // check proposalState is correctly formed

        assert(proposalState.currentStateRef == currentStateRef)
        assert(proposalState.candidateState == candidateState)
        assert(proposalState.proposer == a.info.legalIdentities.first())
        assert(proposalState.responders.toSet() == setOf(currentState.party2))

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

        assert(readyState.currentStateRef == currentStateRef)
        assert(readyState.proposalStateRef == proposalStateRef)
    }


    @Test
    fun `GetProposalStatesFromAgreementStateRefFlow test`() {

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

        val proposalStateRef = StateRef(returnedTx2.id, 0)
        val proposalState = a.services.toStateAndRef<ProposalState>(proposalStateRef).state.data

        // add another AgreementState

        val flow3 = CreateAgreementFlow("This is a unrelated mock agreement", partyb)
        val future3 = a.startFlow(flow3)
        network.runNetwork()
        val returnedTx3 = future3.getOrThrow()



        // check retrieving ProposalState using GetProposalStatesFromAgreementStateRefFlow

        val flow4 = GetProposalStatesFromAgreementStateRefFlow(currentStateRef)
        val future4 = a.startFlow(flow4)
        network.runNetwork()
        val returnedList = future4.getOrThrow()

        val proposalStateFromAVault = returnedList.single().state.data

        assert(proposalStateFromAVault == proposalState)

    }
}