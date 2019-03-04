package com.multilateralagreements

import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.ProposalState
import com.multilateralagreements.contracts.ReadyState
import com.multilateralagreements.workflows.CreateAgreementFlow
import com.multilateralagreements.workflows.CreateConsentFlow
import com.multilateralagreements.workflows.CreateProposalFlow
import com.multilateralagreements.workflows.GetProposalStatesFromAgreementStateRefFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.Test
import rx.Observable
import java.time.Instant
import java.util.concurrent.Future
import kotlin.test.assertEquals

// TODO: understand driver tests

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    private val party1Identity = TestIdentity(CordaX500Name("Party1", "", "GB"))
    private val party2Identity = TestIdentity(CordaX500Name("Party2", "", "US"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting workflows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(bankB.name, partyAHandle.resolveName(bankB.name))
        assertEquals(bankA.name, partyBHandle.resolveName(bankA.name))
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true, networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()


    @Test
    fun `Agreement test`() = withDriver {

        val party1User = User("party1User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val party2User = User("party2User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val (party1, party2) = listOf(
                startNode(providedName = party1Identity.name,rpcUsers = listOf(party1User) ),
                startNode(providedName = party2Identity.name,rpcUsers = listOf(party2User) )
        ).map { it.getOrThrow() }

        val party1Client = CordaRPCClient(party1.rpcAddress)
        val party1Proxy = party1Client.start("party1User", "").proxy

        val party2Client = CordaRPCClient(party2.rpcAddress)
        val party2Proxy = party2Client.start("party2User", "").proxy

        val party1VaultUpdates: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates
        val party2VaultUpdates: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        party1Proxy.startFlow(::CreateAgreementFlow, "This is a mock agreement", party2.nodeInfo.singleIdentity()). returnValue.getOrThrow()

        party2VaultUpdates.expectEvents {
            expect{ update ->
                println("MB: Party2 got a vault update of $update")
                val state = update.produced.first().state.data as AgreementState
                assert(state.agreementDetails == "This is a mock agreement")
            }
        }


    }

    @Test
    fun `Create Proposal and Ready State test`() = withDriver {

        // Set up

        val party1User = User("party1User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val party2User = User("party2User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val (party1, party2) = listOf(
                startNode(providedName = party1Identity.name,rpcUsers = listOf(party1User) ),
                startNode(providedName = party2Identity.name,rpcUsers = listOf(party2User) )
        ).map { it.getOrThrow() }

        val party1Client = CordaRPCClient(party1.rpcAddress)
        val party1Proxy = party1Client.start("party1User", "").proxy

        val party2Client = CordaRPCClient(party2.rpcAddress)
        val party2Proxy = party2Client.start("party2User", "").proxy


        // Party1 Creates agreement State

        val party1VaultUpdates_1: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates
        val party2VaultUpdates_1: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        party1Proxy.startFlow(::CreateAgreementFlow, "This is a mock agreement", party2.nodeInfo.singleIdentity()). returnValue.getOrThrow()

        val agreementStateRefs = mutableListOf<StateRef>()

        party2VaultUpdates_1.expectEvents {
            expect{ update ->
                println("MB: event 2a")
                println("MB: Party2 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as AgreementState
                assert(state.agreementDetails == "This is a mock agreement")
                agreementStateRefs.add(stateAndRef.ref)
            }
        }
        party1VaultUpdates_1.expectEvents {
            expect{ update ->
                println("MB: event 2b")
                println("MB: Party1 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as AgreementState
                assert(state.agreementDetails == "This is a mock agreement")
            }
        }


        // Create proposal state (party 2 proposer, party 1 responder)

        val agreementCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = agreementStateRefs)
        val agreementState = party2Proxy.vaultQueryBy<AgreementState>(agreementCriteria).states.single().state.data
        val candidateState = agreementState.copy(agreementDetails = "This is a modified agreement")

        val party1VaultUpdates_2: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates

        party2Proxy.startFlow(::CreateProposalFlow, agreementStateRefs.single(), candidateState, Instant.MAX, listOf(party1.nodeInfo.singleIdentity() ))

        val proposalStateRefs = mutableListOf<StateRef>()

        party1VaultUpdates_2.expectEvents {
            expect{ update ->
                println("MB: Party1 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ProposalState
                val cs = state.candidateState as AgreementState
                assert(cs.agreementDetails == "This is a modified agreement")
                proposalStateRefs.add(stateAndRef.ref)
            }
        }

        // party 1 creates ready state

        val party2VaultUpdates_2: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        party1Proxy.startFlow(::CreateConsentFlow, proposalStateRefs.single(), Instant.MAX)

        val readyStateRefs = mutableListOf<StateRef>()

        party2VaultUpdates_2.expectEvents {
            expect{ update ->
                println("MB: Party2 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ReadyState
                assert(state.proposalStateRef == proposalStateRefs.single())
                readyStateRefs.add(stateAndRef.ref)
            }
        }

        val vaultSnapShot = party1Proxy.vaultQueryBy<ContractState>()

        println("MB: vaultSnapShot: $vaultSnapShot")

    }

    @Test
    fun `requires Proposal and ready to update Agreement State Test`() = withDriver {

        // Set up

        val party1User = User("party1User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val party2User = User("party2User", "", permissions = setOf(
                startFlow<CreateAgreementFlow>(),
                startFlow<CreateProposalFlow>(),
                startFlow<CreateConsentFlow>(),
                startFlow<GetProposalStatesFromAgreementStateRefFlow>(),
                invokeRpc("vaultTrackBy")
        ))

        val (party1, party2) = listOf(
                startNode(providedName = party1Identity.name,rpcUsers = listOf(party1User) ),
                startNode(providedName = party2Identity.name,rpcUsers = listOf(party2User) )
        ).map { it.getOrThrow() }

        val party1Client = CordaRPCClient(party1.rpcAddress)
        val party1Proxy = party1Client.start("party1User", "").proxy

        val party2Client = CordaRPCClient(party2.rpcAddress)
        val party2Proxy = party2Client.start("party2User", "").proxy


        // Party1 Creates agreement State

        val party1VaultUpdates_1: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates
        val party2VaultUpdates_1: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        party1Proxy.startFlow(::CreateAgreementFlow, "This is a mock agreement", party2.nodeInfo.singleIdentity()). returnValue.getOrThrow()

        val agreementStateRefs = mutableListOf<StateRef>()

        party2VaultUpdates_1.expectEvents {
            expect{ update ->
                println("MB: event 2a")
                println("MB: Party2 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as AgreementState
                assert(state.agreementDetails == "This is a mock agreement")
                agreementStateRefs.add(stateAndRef.ref)
            }
        }
        party1VaultUpdates_1.expectEvents {
            expect{ update ->
                println("MB: event 2b")
                println("MB: Party1 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as AgreementState
                assert(state.agreementDetails == "This is a mock agreement")
            }
        }


        // Create proposal state (party 2 proposer, party 1 responder)

        val agreementCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = agreementStateRefs)
        val agreementState = party2Proxy.vaultQueryBy<AgreementState>(agreementCriteria).states.single().state.data
        val candidateState = agreementState.copy(agreementDetails = "This is a modified agreement")

        val party1VaultUpdates_2: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates

        party2Proxy.startFlow(::CreateProposalFlow, agreementStateRefs.single(), candidateState, Instant.MAX, listOf(party1.nodeInfo.singleIdentity() ))

        val proposalStateRefs = mutableListOf<StateRef>()

        party1VaultUpdates_2.expectEvents {
            expect{ update ->
                println("MB: Party1 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ProposalState
                val cs = state.candidateState as AgreementState
                assert(cs.agreementDetails == "This is a modified agreement")
                proposalStateRefs.add(stateAndRef.ref)
            }
        }

        // party 1 creates ready state

        val party2VaultUpdates_2: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        party1Proxy.startFlow(::CreateConsentFlow, proposalStateRefs.single(), Instant.MAX)

        val readyStateRefs = mutableListOf<StateRef>()

        party2VaultUpdates_2.expectEvents {
            expect{ update ->
                println("MB: Party2 got a vault update of $update")
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ReadyState
                assert(state.proposalStateRef == proposalStateRefs.single())
                readyStateRefs.add(stateAndRef.ref)
            }
        }

        val vaultSnapShot = party1Proxy.vaultQueryBy<ContractState>()

        println("MB: vaultSnapShot: $vaultSnapShot")

    }


}