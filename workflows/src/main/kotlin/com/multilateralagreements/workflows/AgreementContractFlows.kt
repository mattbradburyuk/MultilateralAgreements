package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.AgreementContract
import com.multilateralagreements.contracts.AgreementState
import com.multilateralagreements.contracts.AgreementStateStatus
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********

// Create Agreement flows

// todo: work out how to run this from corda shell

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
class AgreeAgreementFlow(val linearId: UniqueIdentifier, val otherParty: Party): FlowLogic<SignedTransaction>(){

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

        txBuilder.verify(serviceHub)

        // Sign and finalise

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