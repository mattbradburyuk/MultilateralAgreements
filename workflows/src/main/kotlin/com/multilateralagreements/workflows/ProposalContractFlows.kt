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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant


// *********
// * Flows *
// *********

// Create Proposal flows

// Corda shell commands: todo: corda shell commands


@InitiatingFlow
@StartableByRPC
class CreateProposalFlow(val currentStateRef: StateRef,
                         val candidateState: ContractState,
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
        val signers = me

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

// todo: ConsentFlow
// todo: ConsentFlowResponder

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
        val signers = me

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
 * Use case is: I have an AgreementState, I want to see all proposals and ready's against it
 *
 * Will need to work out how to query the vault for states which have currentStateStaticPointer poointing to the State in question
 *
 * Likely need to make Proposal and Ready States Queryable, set up a scheme and write a VaultCustomQueryCriteria. See Ivan's example in billing app
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



