package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.ProposalContract
import com.multilateralagreements.contracts.ProposalState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant


// *********
// * Flows *
// *********

// Create Agreement flows

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
//        // todo: got to here - sort out refernce state reference so it can be added
//
////        txBuilder.addReferenceState(currentStateAndRef)
//
//
//
//
//    }
//
//
//
//}


// todo: CreateProposalResponderFlow


// todo: FinaliseProposalFlow -> goes to AgreeementFLow - or is it a subflow

// todo: CancelProposalFlow
// todo: CancelProposalResponderFlow


// todo: ConsentFlow
// todo: ConsentFlowResponder

// todo: RevokeConsentFlow
// todo: RevokeConsentResponderFlow