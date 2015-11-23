/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package concurrent

import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.ConcurrentLinkedQueue
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import scala.concurrent.ExecutionContext

/**
 * Tests the workqueue algorithm.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class WhileNonEmptyTest extends FunSpec with Matchers with ParallelTestExecution {

    final val ThreadPool = ThreadPoolN(128)
    implicit final val TestExecutionContext = ExecutionContext.fromExecutorService(ThreadPool)

    describe("the whileNonEmpty control abstraction") {

        it("it should be possible process an empty workqueue containing no elements") {
            val counter = new AtomicInteger(0)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            whileNonEmpty(workQueue) { i ⇒ counter.incrementAndGet() }
            counter.get should be(0)
        }

        it("it should be possible process a workqueue containing one element") {
            val counter = new AtomicInteger(0)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            workQueue.add(1)
            whileNonEmpty(workQueue) { i ⇒ counter.incrementAndGet() }
            counter.get should be(1)
        }

        it("it should be possible process a workqueue that initially contains only one element but adds another one") {
            val counter = new AtomicInteger(0)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            workQueue.add(1)
            val exceptions =
                whileNonEmpty(workQueue) { i ⇒
                    counter.incrementAndGet()
                    if (i == 1) { workQueue.add(2) }
                }
            exceptions should be(empty)
            counter.get should be(2)
        }

        it("it should be possible process a workqueue that initially contains only one element but adds thousands") {
            val processedValues = new AtomicIntegerArray(100000)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            workQueue.add(0)
            val exceptions =
                whileNonEmpty(workQueue) { i ⇒
                    if (processedValues.addAndGet(i, 1) != 1)
                        fail(s"the value $i was already processed")

                    if (i == 0) {
                        for (i ← 1 until 100000)
                            workQueue.add(i)
                    } else {
                        Thread.sleep(1)
                    }
                }
            exceptions should be(empty)
            for (i ← 0 until 100000) processedValues.get(i) should be(1)
        }

        it("it should be possible process a workqueue that initially contains only one element but adds thousands in multiple steps") {
            val processedValues = new AtomicInteger(0)
            val subsequentlyScheduled = new AtomicInteger(0)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            for (i ← 0 until 100000) workQueue.add(i)
            val nextValue = new AtomicInteger(100000)
            val exceptions =
                whileNonEmpty(workQueue) { i ⇒
                    processedValues.incrementAndGet()

                    if ((i % 1000) == 0) {
                        for (i ← 1 until 10) {
                            subsequentlyScheduled.incrementAndGet()
                            workQueue.add(nextValue.incrementAndGet())
                        }
                    } else {
                        Thread.sleep(1)
                    }
                }
            exceptions should be(empty)
            processedValues.get() should be(100000 + subsequentlyScheduled.get)
        }

        it("it should be possible process a workqueue that initially contains only one element but adds thousands in multiple steps and occasionally throws an exception") {
            val processedValues = new AtomicInteger(0)
            val subsequentlyScheduled = new AtomicInteger(0)
            val aborted = new AtomicInteger(0)
            val workQueue = new ConcurrentLinkedQueue[Integer]()
            for (i ← 0 until 100000) workQueue.add(i)
            val nextValue = new AtomicInteger(100000)
            val exceptions =
                whileNonEmpty(workQueue) { i ⇒
                    if ((i % 1000) == 0) {
                        for (i ← 1 until 10) {
                            subsequentlyScheduled.incrementAndGet()
                            workQueue.add(nextValue.incrementAndGet())
                        }
                    } else if ((i % 1333 == 0)) {
                        aborted.incrementAndGet()
                        throw new Exception();
                    } else {
                        Thread.sleep(1)
                    }
                    processedValues.incrementAndGet()
                }
            info("subsequently scheduled: "+subsequentlyScheduled.get)
            info("number of caught exceptions: "+exceptions.size)
            exceptions.isEmpty should be(false)
            exceptions.size should be(aborted.get)
            processedValues.get() should be(100000 + subsequentlyScheduled.get - aborted.get)
        }
    }
}
