package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.*
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********

// Create Agreement flows

// Corda shell commands:

// >>> flow start CreateAgreementFlow agreement: This is an agreement between party A and B, otherParty: "O=PartyB,L=New York,C=US"
// >>> run vaultQuery contractStateType: com.multilateralagreements.contracts.AgreementState
// >>> flow start AgreeAgreementFlow linearId: <linearId>, otherParty: "O=PartyB,L=New York,C=US"




@InitiatingFlow
@StartableByRPC
class CreateAgreementFlow(val agreement: String, val otherParty: Party): FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {


        // TODO where does logging go to

        logger.info("MB: test log message")

        val txBuilder = TransactionBuilder()

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.notary = notary

        val state = AgreementState(agreement, serviceHub.myInfo.legalIdentities.first(), otherParty)
        val txState = TransactionState<AgreementState>(state, AgreementContract.ID, notary )
        txBuilder.addOutputState(txState)

        val me = serviceHub.myInfo.legalIdentities.first()
        val command = AgreementContract.Commands.Create()
        txBuilder.addCommand(command, me.owningKey, otherParty.owningKey )

        txBuilder.verify(serviceHub)

        val pstx = serviceHub.signInitialTransaction(txBuilder)

        val session = initiateFlow(otherParty)

        val stx: SignedTransaction = subFlow(CollectSignaturesFlow(pstx,listOf(session)))

        return subFlow(FinalityFlow(stx, listOf(session)))

   }
}


@InitiatedBy(CreateAgreementFlow::class)
class CreateAgreementResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{


        val signTransactionFlow = object: SignTransactionFlow(otherPartySession){

            override fun checkTransaction(stx: SignedTransaction){
                val output = stx.tx.outputs.single().data
                requireThat { "output is an AgreementState" using  (output is AgreementState) }
            }
        }

        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))


    }

}


// Agree agreement Flows


@InitiatingFlow
@StartableByRPC
class AgreeAgreementFlowOld(val linearId: UniqueIdentifier, val otherParty: Party): FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // find input state

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val result = serviceHub.vaultService.queryBy<AgreementState>(criteria)
        val inputStateAndRef = result.states.first()
        val inputTxState = inputStateAndRef.state
        val inputState = inputTxState.data

       // create output state

        val outputState = inputState.copy(status = AgreementStateStatus.AGREED)
        val outputTxState = inputTxState.copy(data = outputState)

        //create command and signers

        val command = AgreementContract.Commands.Agree()

        val signers = inputState.participants.map{it.owningKey}

        // get notary from input state

        val notary = inputTxState.notary

        // build transaction

        val txBuilder = TransactionBuilder()
        txBuilder.notary = notary
        txBuilder.addInputState(inputStateAndRef)
        txBuilder.addCommand(command, signers)
        txBuilder.addOutputState(outputTxState)


        try {
            txBuilder.verify(serviceHub)
        }catch(e:Exception){

            println("MB: e on verify: $e")

        }


        // Sign and finalise

        val pstx = serviceHub.signInitialTransaction(txBuilder)
        val session = initiateFlow(otherParty)
        val stx: SignedTransaction = subFlow(CollectSignaturesFlow(pstx,listOf(session)))

        return subFlow(FinalityFlow(stx, listOf(session)))

    }
}

@InitiatingFlow
@StartableByRPC
class AgreeAgreementFlow(val currentStateRef: StateRef, val proposalStateRef: StateRef, val otherParty: Party): FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // find current State

        val currentStateCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(currentStateRef))
        val currentStateStateAndRef = serviceHub.vaultService.queryBy<AgreementState>(currentStateCriteria).states.single()
        val currentState = currentStateStateAndRef.state.data

        println("MB: agreementState: $currentState")

        // find proposal State

        val proposalStateCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(proposalStateRef))
        val proposalStateStateAndRef = serviceHub.vaultService.queryBy<ProposalState>(proposalStateCriteria).states.single()
        val proposalState = proposalStateStateAndRef.state.data


        println("MB: proposalState: $proposalState")


        // get candidate State and cast to AgreementState

        val candidateState = proposalState.candidateState as AgreementState


        // find the readyStates

        val readyStateCriteria = ReadyStateSchemaV1.PersistentReadyState::proposalStateRef.equal(proposalStateStateAndRef.ref)

        val readyStateStateAndRefs =  serviceHub.vaultService.queryBy<ReadyState>(QueryCriteria.VaultCustomQueryCriteria(readyStateCriteria)).states

        println("MB: readyStateStateAndRefs: $readyStateStateAndRefs")


        //create commands and signers

        val agreeCommand = AgreementContract.Commands.Agree()

        val finaliseCommand = ProposalContract.Commands.Finalise()

        val signers = listOf(currentState.party1, currentState.party2)

        val signersKeys = signers.map {it.owningKey}

        // get notary from input state

        val notary = currentStateStateAndRef.state.notary

        // build transaction

        val txBuilder = TransactionBuilder()
        txBuilder.notary = notary
        txBuilder.addInputState(currentStateStateAndRef)
        txBuilder.addInputState(proposalStateStateAndRef)

        readyStateStateAndRefs.forEach { txBuilder.addInputState(it) }

        txBuilder.addCommand(agreeCommand, signersKeys)
        txBuilder.addCommand(finaliseCommand, signersKeys)

        txBuilder.addOutputState(candidateState)

//
//
        try {
            txBuilder.verify(serviceHub)
        }catch(e:Exception){

            println("MB: e on verify: $e")

        }

        println("MB: txBuilder: $txBuilder")

//        // Sign and finalise

        val pstx = serviceHub.signInitialTransaction(txBuilder)
        val session = initiateFlow(otherParty)
        val stx: SignedTransaction = subFlow(CollectSignaturesFlow(pstx,listOf(session)))



        return subFlow(FinalityFlow(stx, listOf(session)))

    }
}



@InitiatedBy(AgreeAgreementFlow::class)
class AgreeAgreementResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{

        val signTransactionFlow = object: SignTransactionFlow(otherPartySession){

            override fun checkTransaction(stx: SignedTransaction){
                val output = stx.tx.outputs.single().data
                requireThat { "output is an AgreementState" using  (output is AgreementState) }
            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
    }
}