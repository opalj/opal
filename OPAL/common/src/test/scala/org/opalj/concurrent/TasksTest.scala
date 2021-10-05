/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package concurrent

import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
//import java.util.concurrent.ConcurrentLinkedQueue
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

/**
 * Tests [[Tasks]].
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TasksTest extends AnyFunSpec with Matchers {

    final val ThreadPool = BoundedThreadPool("tasks test", 128)
    implicit final val TestExecutionContext = ExecutionContext.fromExecutorService(ThreadPool)

    describe("the Tasks control abstraction") {

        it("it should be possible to process an empty set of tasks") {
            val counter = new AtomicInteger(0)
            Tasks { (tasks: Tasks[Int], i: Int) => counter.incrementAndGet() }.join()
            counter.get should be(0)
        }

        it("it should be possible process a single task") {
            val counter = new AtomicInteger(0)
            val tasks = Tasks { (tasks: Tasks[Int], i: Int) =>
                counter.incrementAndGet()
                Thread.sleep(50)
            }
            tasks.submit(1)
            tasks.join()
            counter.get should be(1)
        }

        it("it should be possible to reuse tasks after join") {
            val counter = new AtomicInteger(0)
            val tasks = Tasks { (tasks: Tasks[Int], i: Int) =>
                counter.incrementAndGet()
            }
            tasks.submit(1)
            while (tasks.currentTasksCount > 0) {
                Thread.sleep(50);
            }
            tasks.submit(1)
            while (tasks.currentTasksCount > 0) {
                Thread.sleep(50);
            }
            tasks.submit(1)
            tasks.join()
            counter.get should be(3)

            tasks.submit(1)
            tasks.join()
            counter.get should be(4)
        }

        it("it should be possible to add a task while processing a task") {
            val counter = new AtomicInteger(0)
            val tasks: Tasks[Int] = Tasks { (tasks: Tasks[Int], i: Int) =>
                counter.incrementAndGet()
                if (i == 1) { tasks.submit(2) }
                Thread.sleep(50)
            }
            tasks.submit(1)
            tasks.join()
            counter.get should be(2)
        }

        it("it should be possible to create thousands of tasks while processing a task") {
            val processedValues = new AtomicIntegerArray(100000)
            val tasks: Tasks[Int] = Tasks { (tasks: Tasks[Int], i: Int) =>
                if (processedValues.addAndGet(i, 1) != 1) {
                    fail(s"the value $i was already processed")
                }
                if (i == 0) {
                    for (i <- 1 until 100000) tasks.submit(i)
                } else {
                    Thread.sleep(1)
                }
            }
            tasks.submit(0)
            tasks.join()
            for (i <- 0 until 100000) processedValues.get(i) should be(1)
        }

        it("it should be possible to submit tasks with a significant delay") {
            val processedValues = new AtomicInteger(0)
            val tasks: Tasks[Int] = Tasks { (tasks: Tasks[Int], i: Int) =>
                Thread.sleep(100)
                processedValues.incrementAndGet()
            }
            tasks.submit(1)
            Thread.sleep(250) // ttask 1 is probably already long finished...
            tasks.submit(2)
            tasks.join() // task 2 is probably still running..
            processedValues.get() should be(2)
        }

        it("it should be possible to create thousands of tasks in multiple steps multiple times") {
            for { r <- 1 to 3 } {
                val processedValues = new AtomicInteger(0)
                val subsequentlyScheduled = new AtomicInteger(0)
                val nextValue = new AtomicInteger(100000)
                val tasks: Tasks[Int] = Tasks { (tasks: Tasks[Int], i: Int) =>
                    processedValues.incrementAndGet()
                    if ((i % 1000) == 0) {
                        for (t <- 1 until 10) {
                            subsequentlyScheduled.incrementAndGet()
                            tasks.submit(nextValue.incrementAndGet())
                        }
                    } else {
                        Thread.sleep(1)
                    }
                }
                for (i <- 0 until 100000) tasks.submit(i)
                tasks.join()
                processedValues.get() should be(100000 + subsequentlyScheduled.get)
                info(s"run $r succeeded")
            }
        }

        it("it should be possible to create thousands of tasks in multiple steps even if some exceptions are thrown") {
            val processedValues = new AtomicInteger(0)
            val subsequentlyScheduled = new AtomicInteger(0)
            val aborted = new AtomicInteger(0)

            val nextValue = new AtomicInteger(100000)
            val tasks: Tasks[Int] = Tasks { (tasks: Tasks[Int], i: Int) =>
                if (i % 1000 == 0) {
                    for (i <- 1 until 10) {
                        subsequentlyScheduled.incrementAndGet()
                        tasks.submit(nextValue.incrementAndGet())
                    }
                } else if (i % 1333 == 0) {
                    aborted.incrementAndGet()
                    throw new Exception();
                } else {
                    Thread.sleep(1)
                }
                processedValues.incrementAndGet()
            }
            for (i <- 0 until 100000) tasks.submit(i)

            var exceptions: Array[Throwable] = null
            try {
                tasks.join()
            } catch {
                case ce: ConcurrentExceptions =>
                    exceptions = ce.getSuppressed
            }

            info("subsequently scheduled: "+subsequentlyScheduled.get)
            info("number of caught exceptions: "+exceptions.size)

            exceptions.size should be(aborted.get)
            processedValues.get() should be(100000 + subsequentlyScheduled.get - aborted.get)
        }
    }
}
