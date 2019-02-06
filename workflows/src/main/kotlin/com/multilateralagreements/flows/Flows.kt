package com.multilateralagreements.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********


    @InitiatingFlow
    @StartableByRPC
    class Initiator : FlowLogic<Unit>() {
        override val progressTracker = ProgressTracker()
//        init {
//            logger.info("MB: Initiator called")
//        }
        @Suspendable
        override fun call() {
            // Initiator flow logic goes here.

            logger.info("MB: Initiator.call called")
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            // Responder flow logic goes here.
        }
    }
