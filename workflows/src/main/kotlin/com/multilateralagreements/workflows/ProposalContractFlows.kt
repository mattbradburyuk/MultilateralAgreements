package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.ProposalContract
import com.multilateralagreements.contracts.ProposalState
import com.multilateralagreements.contracts.ReadyState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant


// *********
// * Flows *
// *********

// Create Proposal flows

// Corda shell commands: todo: corda shell commands


//@InitiatingFlow
//@StartableByRPC
//class CreateProposalFlow(val linearId: UniqueIdentifier,
//                         val candidateState: ContractState,
//                         val expiryTime: Instant,
//                         val responders: List<Party>
//                         ): FlowLogic<SignedTransaction>(){
//
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//
//        // find ref state
//
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        val result = serviceHub.vaultService.queryBy<AgreementState>(criteria)
//        val currentStateAndRef = result.states.first()
//        val currentTxState = currentStateAndRef.state
//        val currentState = currentTxState.data
//
//
//        // create output state
//
//        val me = serviceHub.myInfo.legalIdentities.first()
//        val outputState = ProposalState(currentState, candidateState, expiryTime, me, responders)
//
//        // create command and signers
//
//        val command = ProposalContract.Commands.Propose()
//        val signers = me
//
//        // build transaction
//
//        val txBuilder = TransactionBuilder()
//
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        txBuilder.notary = notary
//
//        txBuilder.addReferenceState(ReferencedStateAndRef(currentStateAndRef))
//        txBuilder.addOutputState(outputState)
//        txBuilder.addCommand(command, me.owningKey)
//
//        // verify
//
//        txBuilder.verify(serviceHub)
//
//        // sign
//
//        val stx = serviceHub.signInitialTransaction(txBuilder)
//
//        val sessions = mutableListOf<FlowSession>()
//
//        responders.forEach { sessions.add(initiateFlow(it)) }
//
//        val ftx = subFlow(FinalityFlow(stx,sessions))
//
//        return ftx
//
//    }
//}
//
//@InitiatedBy(CreateProposalFlow::class)
//class CreateProposalResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){
//
//    @Suspendable
//    override fun call(): SignedTransaction{
//
//        return subFlow(ReceiveFinalityFlow(otherPartySession))
//    }
//}

// todo: FinaliseProposalFlow -> goes to AgreeementFLow - or is it a subflow

// todo: CancelProposalFlow
// todo: CancelProposalResponderFlow

// todo: ConsentFlow
// todo: ConsentFlowResponder

// Consent Flows

//@InitiatingFlow
//@StartableByRPC
//class ConsentFlow(val proposalState: ProposalState, val consentExpiryTime: Instant = Instant.MAX ): FlowLogic<SignedTransaction>(){
//
//
//    @Suspendable
//    override fun call(): SignedTransaction{
//
//
//        // find reference state
//
////        val proposalStateLinearId = proposalState.
////
////        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
////        val result = serviceHub.vaultService.queryBy<AgreementState>(criteria)
////        val currentStateAndRef = result.states.first()
////        val currentTxState = currentStateAndRef.state
////        val currentState = currentTxState.data
//
//        // create output ReadyState
//
//        val me = serviceHub.myInfo.legalIdentities.first()
//        val readyState = ReadyState(me,
//                proposalState,
//                consentExpiryTime,
//                proposalState.proposer,
//                proposalState.responders)
//
//
//
//
//
//
//    }
//}



// todo: RevokeConsentFlow
// todo: RevokeConsentResponderFlow

// these could be written by the Services layer, but doing as Flows here

// todo: Get Proposals for AgreementState linearId
/// todo: Get ReadyStates for Agreement LinearId
// todo Get Readystates for proposal




//
//class PropsalStatesfromCurrentStateFlow(val currentState: LinearState): FlowLogic<ProposalState>(){
//
//
//    @Suspendable
//    override fun call(): ProposalState {
//
//
////        todo: workout how to query the vault for properties of states
//
////        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(currentState.linearId))
////        val result = serviceHub.vaultService.queryBy<ProposalState>(criteria)
////        val currentStateAndRef = result.states.first()
////        val currentTxState = currentStateAndRef.state
////        val currentState = currentTxState.data
//
//    }
//
//
//}



