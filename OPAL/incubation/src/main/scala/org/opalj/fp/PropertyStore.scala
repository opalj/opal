/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.fp

import java.util.concurrent.atomic.AtomicInteger
import java.util.{ IdentityHashMap ⇒ JIDMap }
import scala.collection.mutable.{ HashMap ⇒ HMap }
import scala.collection.mutable.{ ListBuffer ⇒ Buffer }
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.opalj.concurrent.Locking.{ withReadLock, withWriteLock }
import org.opalj.concurrent.ThreadPoolN
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import java.util.concurrent.ExecutorCompletionService
import org.opalj.log.OPALLogger
import org.opalj.concurrent.UncaughtExceptionHandler.uncaughtException
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

/**
 * The central store which manages the execution of all
 * analyses that provide information about the elements of the store.
 *
 * ==Core Requirements==
 *  - A specific kind of property is always computed by only one PropertyComputation
 *      function.
 *  - Two property computation functions that operate on two different elements
 *      never computed properties related to the same elements.
 *
 * ==Thread Safety==
 * The PropertyStore is thread-safe.
 */
class PropertyStore private (
        private[this] val data: JIDMap[AnyRef, PropertyStoreValue]) { store ⇒

    /**
     * The set of all stored elements.
     *
     * This set is immutable.
     */
    private[this] val keys = data.keySet()

    /**
     * Returns the property currently associated with the given element.
     *
     * @param e A value in the store.
     * @param key The kind of property.
     * @return `None` if no information about the respective property is available.
     *      `Some(Property)` otherwise.
     *      Note that the returned value may change over time but only such that it
     *      is strictly more precise.
     */
    def apply(e: AnyRef, key: PropertyKey): Option[Property] = {
        val (lock, (properties)) = data.get(e)
        withReadLock(lock) {
            properties.get(key) match {
                case None | Some((null, _))             ⇒ None
                case Some((property, _ /*observers*/ )) ⇒ Some(property)
            }
        }
    }

    /**
     * Returns all elements which have a property of the respective kind.
     *
     * '''This method is only to be called after all properties are computed!'''
     */
    def apply(key: PropertyKey): Traversable[(AnyRef, Property)] = {

        assert(Tasks.scheduled == 0, "all tasks have completed")

        import scala.collection.JavaConversions._
        val valuesWithProperty =
            data.entrySet() filter { v ⇒
                val (_ /*lock*/ , properties) = v.getValue()
                properties.get(key) match {
                    case Some((null, _)) | None ⇒ false
                    case _                      ⇒ true
                }
            }
        valuesWithProperty map { v ⇒ (v.getKey, v.getValue._2(key)._1) }
    }

    /**
     * Registers a function that calculates a property for all or some elements
     * of the store.
     *
     * This store ensures that f is never invoked more than once for the
     * same element at the same time. If f is invoked again for a specific element
     * then only because a dependee has changed!
     */
    def <<=(f: PropertyComputation): Unit = {
        val it = keys.iterator()
        while (it.hasNext()) {
            scheduleComputation(it.next(), f)
        }
    }

    /**
     * Called by a client to await the completion of the computation of all
     * properties of all previously registered property computation functions.
     *
     * This function is only guaranteed to wait on the completion of the computation
     * of those properties that were registered by this thread.
     */
    def waitOnPropertyComputationCompletion(): Unit = Tasks.waitOnCompletion()

    override def toString: String = {
        s"PropertyStore(elementsCount= ${data.size()}, executedComputations= ${Tasks.executed}%n"
    }

    //
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    private[this] final val threadPool = ThreadPoolN(NumberOfThreadsForCPUBoundTasks)

    /**
     * General information about the tasks that are executed.
     */
    private[this] object Tasks {

        @volatile var executed = 0

        /**
         * The number of scheduled tasks. I.e., the number of tasks that are running or
         * that will run in the future.
         */
        @volatile var scheduled = 0

        def taskStarted() = synchronized { executed += 1; scheduled += 1 }

        def taskCompleted() = synchronized {
            assert(scheduled > 0)
            scheduled -= 1
            if (scheduled == 0) {
                // TODO Let's check if we have a cycle.
                breakUpCycles()

                // TODO Terminate the remaining observers...

                // Well... it seems as if we are done
                println("The last task completed...")
                notifyAll()
            }
        }

        def waitOnCompletion() = synchronized { while (scheduled > 0) { wait } }

    }

    private[this] def breakUpCycles(): Unit = {
        import scala.collection.JavaConversions._
        for {
            entry ← data.entrySet()
            e = entry.getKey()
            properties = entry.getValue()._2
            (propertyKey, (property, observers)) ← properties
            if observers.nonEmpty
        } {
            println("finding observers..."+propertyKey+
                " "+property + observers)
        }
    }

    /**
     * Associates / Updates the property with element e. If observers are registered
     * with the respective property then those observers will be informed about the
     * property change.
     */
    // Invariant: always only at most one function exists that will compute/update
    // the property p of the element e.
    private[this] def update(e: AnyRef, p: Property): Unit = {
        val (lock, properties) = data.get(e)
        withWriteLock(lock) {
            properties.put(p.key, (p, Buffer.empty)) match {

                case Some((oldP, observers)) ⇒
                    assert(oldP != p, "the old and the new property are identical")
                    observers.foreach { o ⇒ o(e, p) }

                case None ⇒
                // Nothing to do ...
            }
        }
    }

    //    /**
    //     * Registers the observer, if the property is not yet known.
    //     *
    //     * @return The current property. None if the property is not yet known.
    //     */
    //    private[this] def registerObserver(
    //        e: AnyRef,
    //        pk: PropertyKey,
    //        o: PropertyObserver): Option[Property] = {
    //        // always only at most one function exists that will update the property p
    //        // of the element e
    //        val (lock, properties) = data.get(e)
    //        withWriteLock(lock) {
    //            val propertyAndObservers = properties.get(pk)
    //            if (propertyAndObservers == None) {
    //                properties.put(pk, (null, Buffer(o)))
    //                None
    //            } else {
    //                val Some((property, observers)) = propertyAndObservers
    //                if (property eq null) {
    //                    observers += o
    //                    None
    //                } else {
    //                    Some(property)
    //                }
    //            }
    //        }
    //    }

    private[this] def handleResult(
        e: AnyRef,
        r: PropertyComputationResult): Unit = {
        r match {
            case NoResult         ⇒ // Nothing to do..

            case Result(property) ⇒ update(e, property)

            case IntermediateResult(property, dependingElements) ⇒
                update(e, property)
            // TODO register handler for updates on depending elements

            case suspended @ Suspended((dependingElement, propertyKey)) ⇒

                // IDEA
                // First, let's get the property, then...
                //  - If we now have a property, let's immediately continue
                //    the computation.
                //  - If the property is still not computed, register an
                //    observer that will schedule the computation when the
                //    property was computed.

                val (lock, (properties)) = data.get(dependingElement)
                withWriteLock(lock) {
                    properties.get(propertyKey) match {
                        case Some((property, observers)) ⇒
                            if (property eq null) {
                                // we have other analysis that are also waiting...
                                observers += new PropertyObserver {
                                    def apply(dependingElement: AnyRef, property: Property): Unit = {
                                        scheduleComputation(
                                            e,
                                            (AnyRef) ⇒ suspended.continue(dependingElement, property)
                                        )
                                    }
                                }
                            } else {
                                // the property was computed in the meantime
                                scheduleComputation(
                                    e,
                                    (AnyRef) ⇒ suspended.continue(dependingElement, property)
                                )
                            }
                        case _ ⇒
                            // this computation is the first who is interested in the property
                            properties.put(propertyKey, (null, Buffer(new PropertyObserver {
                                def apply(dependingElement: AnyRef, property: Property): Unit = {
                                    scheduleComputation(
                                        e,
                                        (AnyRef) ⇒ suspended.continue(dependingElement, property)
                                    )
                                }
                            })))
                    }
                }
        }
    }

    /**
     * Schedules the computation of a property w.r.t. the element e.
     */
    private[this] def scheduleComputation(
        e: AnyRef,
        f: PropertyComputation): Unit = {

        Tasks.taskStarted()
        threadPool.submit(new Runnable {
            def run(): Unit = {
                try {
                    handleResult(e, f(e))
                } catch {
                    case t: Throwable ⇒ uncaughtException(Thread.currentThread(), t)
                } finally {
                    Tasks.taskCompleted()
                }
            }
        })
    }

}
/**
 * Factory to create [[PropertyStore]]s.
 */
object PropertyStore {

    def apply(elements: Traversable[AnyRef]): PropertyStore = {
        val elementsCount = elements.size
        val map = new JIDMap[AnyRef, PropertyStoreValue](elementsCount)

        elements.foreach { e ⇒ map.put(e, (new ReentrantReadWriteLock, HMap.empty)) }

        new PropertyStore(map)
    }

}

sealed trait PropertyComputationResult

/**
 * Computing a property for the respective element is not possible.
 */
case object NoResult extends PropertyComputationResult

/**
 * Encapsulates the final result of the computation of the property.
 *
 * Result is only to be used if no further refinement is possible or may happen.
 */
case class Result(property: Property) extends PropertyComputationResult

/**
 * Encapsulates the result of the computation of a property.
 *
 * If the property of any depending element is changed, the function is called again.
 * In general, the framework tries to wait on the completion of the computation of
 * the relevant properties of as many dependees as possible before the
 * [[PropertyComputation]] is scheduled again.
 */
case class IntermediateResult(
    property: Property,
    dependingElements: Traversable[(AnyRef, PropertyKey, PropertyComputation)])
        extends PropertyComputationResult {

    private[fp] var unsatisfiedDependencies = new AtomicInteger(dependingElements.size)

}

/**
 * @param dependingElements The elements and properties required by this computation before the computation
 * can be continued.
 */
abstract class Suspended(
    val e: AnyRef,
    val dependingElement: AnyRef,
    val dependingProperty: PropertyKey)
        extends PropertyComputationResult {

    /**
     * Called by the framework if properties for all given elements are available.
     */
    def continue(dependingElement: AnyRef, dependingProperty: Property): PropertyComputationResult

    /**
     * Terminates this computation.
     *
     * This method is called by the framework if this computation is waiting on the results
     * of computations of properties for elements for which no further computations
     * are running.
     */
    def terminate(): Unit

    /**
     * The fallback [[Property]] associated with the computation. This method is
     * called by the framework if it identifies a cycle and tries to continue the computation
     * by using default properties for one or more elements of the cycle.
     */
    def fallback: Property
}

object Suspended {

    def unapply(computation: Suspended): Some[(AnyRef, PropertyKey)] =
        Some((computation.dependingElement, computation.dependingProperty))

}

/**
 * A PropertyObserver is a function that is called if the property associated
 * with the respective becomes available or changes.
 *
 * The parameters of the function are the observed element and its property.
 */
abstract class PropertyObserver extends ((AnyRef, Property) ⇒ Unit) {

}
