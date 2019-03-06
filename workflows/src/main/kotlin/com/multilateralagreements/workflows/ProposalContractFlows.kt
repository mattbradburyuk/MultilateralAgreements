package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.*
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant


// *********
// * Flows *
// *********

// Create Proposal flows

// Corda shell commands: todo: corda shell commands

/**
 * start CreateProposalFlow currentStateRef: newStateRef: { txhash: 2FD63295957D1922C55D41C113EA891B7D69FFB3B749AC87EB2AF10F5A64E82B, index: 0 }, candidateState: newAgreementState: { agreementDetails: "This is a modified agreement between party A and B", party1: "O=PartyA, L=London, C=GB", party2: "O=PartyB, L=New York, C=US", status: "AGREED", linearId: newUniqueIdentifier {externalId: null, id: "4526a320-2d9f-4182-b130-766994c7569e" }}, expiryTime: "2019-12-22T00:00:00Z", responders: ["O=PartyB, L=New York, C=US"]
 *
 * start CreateProposalFlow currentStateRef: { txhash: 2FD63295957D1922C55D41C113EA891B7D69FFB3B749AC87EB2AF10F5A64E82B, index: 0 }, candidateState: { agreementDetails: "This is a modified agreement between party A and B", party1: "O=PartyA, L=London, C=GB", party2: "O=PartyB, L=New York, C=US", status: "AGREED", expiryTime: "2019-12-22T00:00:00Z", responders: ["O=PartyB, L=New York, C=US"]
 *
  */



//
//
//
//
//
// This is an agreement between party A and B, otherParty: "O=PartyB,L=New York,C=US"

@CordaSerializable
data class TestClass(val str1: String, val str2: String)


@InitiatingFlow
@StartableByRPC
class YAMLTest(val str: String): FlowLogic<String>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

        return str
    }

}

@InitiatingFlow
@StartableByRPC
class YAMLTest2(val testClass: TestClass): FlowLogic<String>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

        return testClass.toString()
    }

}




@InitiatingFlow
@StartableByRPC
class CreateProposalFlow(val currentStateRef: StateRef,
                         val candidateState: AgreementState,
                         val expiryTime: Instant,
                         val responders: List<Party>
                         ): FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val currentStateAndRef = serviceHub.toStateAndRef<AgreementState>(currentStateRef)

        // create output state

        val me = serviceHub.myInfo.legalIdentities.first()
        val outputState = ProposalState(currentStateRef, candidateState, expiryTime, me, responders)

        // create command and signers

        val command = ProposalContract.Commands.Propose()

        // build transaction

        val txBuilder = TransactionBuilder()

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.notary = notary

        txBuilder.addReferenceState(ReferencedStateAndRef(currentStateAndRef))
        txBuilder.addOutputState(outputState)
        txBuilder.addCommand(command, me.owningKey)

        // verify

        txBuilder.verify(serviceHub)

        // sign

        val stx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = mutableListOf<FlowSession>()

        responders.forEach { sessions.add(initiateFlow(it)) }

        val ftx = subFlow(FinalityFlow(stx,sessions))

        return ftx

    }
}

@InitiatedBy(CreateProposalFlow::class)
class CreateProposalResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{

        return subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}

// todo: CancelProposalFlow
// todo: CancelProposalResponderFlow



@InitiatingFlow
@StartableByRPC
class CreateConsentFlow(val proposalStateRef: StateRef,
                         val expiryTime: Instant
): FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val proposalStateAndRef = serviceHub.toStateAndRef<ProposalState>(proposalStateRef)
        val proposalState = proposalStateAndRef.state.data

        // create output state

        val me = serviceHub.myInfo.legalIdentities.first()
        val outputState = ReadyState(me, proposalStateRef, proposalState.currentStateRef, expiryTime, proposalState.proposer, proposalState.responders)

        // create command and signers

        val command = ProposalContract.Commands.Consent()

        // build transaction

        val txBuilder = TransactionBuilder()

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.notary = notary

        txBuilder.addReferenceState(ReferencedStateAndRef(proposalStateAndRef))
        txBuilder.addOutputState(outputState)
        txBuilder.addCommand(command, me.owningKey)

        // verify

        txBuilder.verify(serviceHub)

        // sign

        val stx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = mutableListOf<FlowSession>()
        val parties = outputState.responders.union (listOf(outputState.proposer))
        parties.filter { it != me }.forEach { sessions.add(initiateFlow(it)) }
        val ftx = subFlow(FinalityFlow(stx,sessions))

        return ftx
    }
}

@InitiatedBy(CreateConsentFlow::class)
class CreateConsentResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{

        return subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}


// todo: RevokeConsentFlow
// todo: RevokeConsentResponderFlow



// todo: write flows which return Proposals and readystates against a currentState - likely via a StateRef or StaticPointer ot the current State

/**
 * Use case is: I have an AgreementState, I want to see all proposals
 *
 */

@InitiatingFlow
@StartableByRPC
class GetProposalStatesFromAgreementStateRefFlow(val currentStateRef: StateRef): FlowLogic<List<StateAndRef<ProposalState>>>(){

    @Suspendable
    override fun call(): List<StateAndRef<ProposalState>>{

        val criteria = ProposalStateSchemaV1.PersistentProposalState::currentStateRef.equal(currentStateRef)

            return serviceHub.vaultService.queryBy<ProposalState>(QueryCriteria.VaultCustomQueryCriteria(criteria)).states


    }
}



