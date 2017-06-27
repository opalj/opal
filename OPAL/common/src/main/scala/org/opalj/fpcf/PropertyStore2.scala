/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf

/*
import java.util.concurrent.{ConcurrentHashMap ⇒ JCHMap}

/**
 *
 * @param data The core array which contains - for each property key - the map of entities to the
 *             derived property. The map is lazily initialized.
 */
class PropertyStore2 private (
        val entities:        Set[Entity],
        private val data:    Array[JCHMap[Entity, EntityCell]] = new Array(1024 /* TODO MAKE IT CONFIGURABLE .. A NUMBER MUCH LARGER THAN THE LARGEST PROPERTY_KEY*/ ),
        @volatile var debug: Boolean
) {

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     * @note The returned collection can be used to create an [[IntermediateResult]].
     */
    def apply[P <: Property](e: Entity, pk: PropertyKey[P]): EOptionP[e.type, P] = {
        data(pk).get(e) match {
            case null                 ⇒ EPK(e, pk)
            case ec: EntityCell[_, _] ⇒ EP(e, ec.p)
        }
    }

    def set(e: Entity, p: Property): Unit = {

    }

    def run(f: Entity ⇒ PropertyComputationResult): Unit = {

    }

}

private[FPCF] case class EntityCell[+E <: Entity, +P <: Property]() {

    def p: P = ???
}
*/
/*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicStampedReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import scala.concurrent.Future
import scala.util.Try

 class PropertyStore2(
                         val entities:        Set[Entity],
                     @volatile var debug : Boolean
                      )(
implicit
val logContext: LogContext
) { store ⇒

     @volatile private[this] val scheduled = new AtomicInteger(0)

     @volatile private[this] val onCompletionHandler  = new AtomicReference[() ⇒ Unit](null)

     /**
      * The given function is called back when all currently running and subsequently scheduled
      * computations have finished. This method should only be called from the thread that
      * also calls `schedule`s the initial computations.
      *
      * @param f The function that is called.
      */
     def onCompletion(f : ⇒ Unit) : Unit = {
         if(scheduled.get == 0) {
             f // immediately execute it... we are already finished
         }
         if (! onCompletionHandler.compareAndSet(null,f _)) {
             OPALLogger.error("property store - internal","only one completion handler can be used")
         }
     }

     private[this] def handleInitialPCR(pcr: Try[PropertyComputationResult])     : Unit = {
         case scala.util.Success(pcr) ⇒
             if (pcr != null) {
                 ???
             }

         case scala.util.Failure(e) ⇒
             scheduled
             // TODO Log error!
             ???

     }

     /**
      * Concurrently executes the given property computation function for all entities which
      * pass the given filter.
      *
      * Consider using `schedule(Traversable[() => PropertyComputationResult])`!
      *
      * @example
      *        {{{
      *        schedule{case m : Method if m.body.isDefined => Result(...)  }
      *        }}}
      */
     def schedule(f: PartialFunction[Entity,PropertyComputationResult]) : Unit = {
        entities foreach {e ⇒
            scheduled.incrementAndGet()
            Future {                f.applyOrElse(e,null)            } onComplete { handleInitialPCR            }
        }
     }

     /**
      * Executes the given (initialized) property computations in parallel. In this case,
      * the client is responsible for associating each (relevant) entity with a relevant
      * computation function. Doing so - in particular if the function only operates on
      * an easily identifiable subset of all entities - is generally more efficient than
      * using `schedule(PartialFunction[Entity, PropertyComputationResult]` as that function
      * iterates over '''all''' entities and schedules a computation.
      *
      * @param pcs
      */
     def schedule(pcs: Traversable[() ⇒ PropertyComputationResult]) : Unit = {
         pcs foreach { pc ⇒
             scheduled.incrementAndGet()
             Future {                 pc()             } onComplete {handleInitialPCR          }
         }
     }





     // Provides global unique ids to order the updates
     val seqIdFactory = new AtomicInteger(Int.MinValue)

     /**
      * A cell represents the current property of an entity w.r.t. a specific kind.
      */
     class EntityCell(e: Entity) {

         /** The cell's current value and the global update id.
          *
          * WE ENSURE THAT ALL UPDATES THAT HAPPENED BEFORE THE VALUE'S STAMP HAS BEEN
          * INCORPORATED.
          */
         val value: AtomicStampedReference[AnyRef]

         /** The computation that will be triggered, when a value has changed on which it depends. */
         private[this] val continuation: AtomicReference[Continuation[Property]] = new AtomicReference(null)

         /** Called by a dependee, when the dependee's value changes. */
         def run(e: Entity, p: Property, seqId : Int ): Unit = {
             // check if we are still interested in this update...; i.e., we have a
             // a dependency and the value that we used the last time was older..

             if(seqId <= value.getStamp) return; // we have already seen the value...



             // Let's create a new stamp; we will only use it if we know
             // that the potentially next value is newer than the values of the dependees; i.e.
             // we effectively prevent the propagation of intermediate values where
             // we effectively prevent the propagation of intermediate values where
             // we know that a dependee has already changed again.
             val newStamp = store.seqIdFactory.getAndIncrement()

             // We ensure that the same computation is never triggered concurrently!
             //
             // However, we have to ensure that we never loose an update that occurs concurrently.
             // The latter is done by continuously checking all new dependers
             val continuation = this.continuation.getAndSet(null)
             if (continuation ne null) {
                 val (newP,newC,newDependees) = continuation(e, p)
                 //  analyze result....

                 var runContinuation : Boolean = false
                 // 1) remove obsolete dependees
                 // 2) add new dependees
                 for (dependee ← newDependees) {
                     runContinuation ||= (dependee wasUpdated)
                 }
                 if (runContinuation) ...

                 // update value using the new stamp...
                 val newValue = new Object()
                 value.set(newValue,newStamp)
                 // ... the new value is now immediately available

                 // inform all current dependers about the new value
                 for (d ← dependers.keys()) {
                         d.run
                 }
             }
         }

         /**
          * Those computations that depend on the value of this cell.
          *
          * Whenever this cell's value changes, the dependers are invoked. A depender is responsible
          * for de-registering itself.
          */
         def dependers: ConcurrentHashMap[Cell, AnyRef] // we use it as a set

     }

 }
 */

