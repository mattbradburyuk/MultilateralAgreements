package com.multilateralagreements.flows

import co.paralleluniverse.fibers.Suspendable
import com.multilateralagreements.contracts.AgreementContract
import com.multilateralagreements.states.AgreementState
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********


@InitiatingFlow
@StartableByRPC
class CreateAgreementFlow(val agreement: String, val otherParty: Party): FlowLogic<SignedTransaction>(){


    override val progressTracker = ProgressTracker()



    @Suspendable
    override fun call(): SignedTransaction {

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

        return subFlow(FinalityFlow(stx, listOf(session), progressTracker))


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
