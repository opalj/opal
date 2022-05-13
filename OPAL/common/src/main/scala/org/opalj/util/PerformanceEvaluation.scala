/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

import java.lang.management.ManagementFactory

import scala.collection.mutable.Map

import org.opalj.concurrent.Locking
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext

/**
 * Measures the execution time of some code.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Eichberg
 */
class PerformanceEvaluation extends Locking {

    private[this] val timeSpans: Map[Symbol, Nanoseconds] = Map.empty

    /**
     * Times the execution of the given method / function literal / code block and
     * adds it to the execution time of previous methods / function literals / code blocks
     * that were measured and for which the same symbol was used. <br/>
     * E.g., <code>time('base_analysis){ ... do something ... }</code>
     *
     * @param s Symbol used to put multiple measurements into relation.
     * @param f The function that will be evaluated and for which the execution
     *      time will be measured.
     */
    final def time[T](s: Symbol)(f: => T): T = {
        val startTime = System.nanoTime
        try {
            f
        } finally {
            val endTime = System.nanoTime
            val timeSpan = new Nanoseconds(endTime - startTime)
            withWriteLock { doUpdateTimes(s, timeSpan) }
        }
    }

    /**
     * Called by the `time` method.
     *
     * ==Thread Safety==
     * The `time` method takes care of the synchronization.
     */
    protected[this] def doUpdateTimes(s: Symbol, timeSpan: Nanoseconds): Unit = {
        val oldTimeSpan = timeSpans.getOrElseUpdate(s, Nanoseconds.None)
        timeSpans.update(s, oldTimeSpan + timeSpan)
    }

    /**
     * Returns the overall time spent by computations with the given symbol.
     */
    final def getTime(s: Symbol): Nanoseconds = withReadLock { doGetTime(s) }

    final def getNanoseconds(s: Symbol): Nanoseconds = getTime(s)

    final def getMilliseconds(s: Symbol): Milliseconds = getTime(s).toMilliseconds

    final def getSeconds(s: Symbol): Seconds = getTime(s).toSeconds

    /**
     * Called by the `getTime(Symbol)` method.
     *
     * ==Thread Safety==
     * The `getTime` method takes care of the synchronization.
     */
    protected[this] def doGetTime(s: Symbol): Nanoseconds = {
        timeSpans.getOrElse(s, Nanoseconds.None)
    }

    /**
     * Resets the overall time spent by computations with the given symbol.
     */
    def reset(s: Symbol): Unit = withWriteLock { doReset(s) }

    /**
     * Called by the `reset(Symbol)` method.
     *
     * ==Thread Safety==
     * The `reset` method takes care of the synchronization.
     */
    private[this] def doReset(s: Symbol): Unit = timeSpans.remove(s)

    /**
     * Resets everything. The effect is comparable to creating a new
     * `PerformanceEvaluation` object, but is a bit more efficient.
     */
    def resetAll(): Unit = withWriteLock { doResetAll() }

    /**
     * Called by the `resetAll` method.
     *
     * ==Thread Safety==
     * The `resetAll` method takes care of the synchronization.
     */
    private[this] def doResetAll(): Unit = timeSpans.clear()

}

object GlobalPerformanceEvaluation extends PerformanceEvaluation

/**
 * Collection of helper functions useful when evaluating the performance of some
 * code.
 *
 * @example
 * Measuring the time and memory used by some piece of code:
 * {{{
 * import org.opalj.util.PerformanceEvaluation.{memory,time}
 * var store : Array[Object] = null
 * implicit val logContext = Some(org.opalj.log.GlobalLogContext)
 * for(i <- 1 to 5){
 *   memory{store = null}(l => println("empty: "+l))
 *   memory{
 *     time{
 *       store = Array.fill(1000000){val l : Object = List(i); l}
 *    }(t => println("time:"+t.toSeconds))
 *   }(l => println("non-empty:"+l))
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
object PerformanceEvaluation {

    /**
     * Measures the amount of memory that is used as a side-effect
     * of executing the given function `f`. I.e., the amount of memory is measured that is
     * used before and after executing `f`; i.e., the permanent data structures that are created
     * by `f` are measured.
     *
     * @note    If large data structures are used by `f` that are not used anymore afterwards
     *          then it may happen that the used amount of memory is negative.
     */
    def memory[T](
        f: => T
    )(
        mu: Long => Unit
    )(
        implicit
        logContext: Option[LogContext] = None
    ): T = {
        val memoryMXBean = ManagementFactory.getMemoryMXBean
        gc(memoryMXBean)
        val usedBefore = memoryMXBean.getHeapMemoryUsage.getUsed
        val r = f
        gc(memoryMXBean)
        val usedAfter = memoryMXBean.getHeapMemoryUsage.getUsed
        mu(usedAfter - usedBefore)
        r
    }

    /**
     * Times the execution of a given function `f`. If the timing may be affected by
     * (required) garbage collection runs it is recommended to first run the garbage collector.
     *
     * @param   r A function that is passed the time (in nanoseconds) that it
     *          took to evaluate `f`. `r` is called even if `f` fails with an exception.
     */
    def time[T](f: => T)(r: Nanoseconds => Unit): T = {
        val startTime: Long = System.nanoTime
        val result =
            try {
                f
            } finally {
                val endTime: Long = System.nanoTime
                r(Nanoseconds.TimeSpan(startTime, endTime))
            }
        result
    }

    def timed[T](f: => T): (Nanoseconds, T) = {
        val startTime: Long = System.nanoTime
        val result = f
        (Nanoseconds.TimeSpan(startTime, System.nanoTime), result)
    }

    /**
     * Times the execution of a given function `f` until the execution time has
     * stabilized and the average time for evaluating `f` is only changing in a
     * well-understood manner.
     *
     * In general, `time` repeats the execution of `f` as long as the average changes
     * significantly. Furthermore, `f` is executed at least `minimalNumberOfRelevantRuns`
     * times and only those runs are taken into consideration for the calculation of the
     * average that are `consideredRunsEpsilon`% worse than the best run. However, if we
     * have more than `10*minimalNumberOfRelevantRuns` runs that did not contribute
     * to the calculation of the average, the last run is added anyway. This way, we
     * ensure that the evaluation will more likely terminate in reasonable time without
     * affecting the average too much. Nevertheless, if the behavior of `f` is
     * extremely erratic, the evaluation may not terminate.
     *
     * ==Example Usage==
     * {{{
     * import org.opalj.util.PerformanceEvaluation._
     * import org.opalj.util._
     * time[String](2,4,3,{Thread.sleep(300).toString}){ (t, ts) =>
     *     val sTs = ts.map(t => f"\${t.toSeconds.timeSpan}%1.4f").mkString(", ")
     *     println(f"Avg: \${avg(ts).timeSpan}%1.4f; T: \${t.toSeconds.timeSpan}%1.4f; Ts: \$sTs")
     * }
     * }}}
     * {{{
     * import org.opalj.util.PerformanceEvaluation.{gc,memory,time,avg}
     * var store : Array[Object] = null
     * implicit val logContext = Some(org.opalj.log.GlobalLogContext)
     * time{
     *   for(i <- 1 to 5){
     *     memory{store = null}(l => println("empty: "+l))
     *     memory{
     *       time(2,4,3,
     *            {store = Array.fill(1000000){val l : Object = List(i); l}},
     *            runGC=true
     *       ){ (t, ts) =>
     *          val sTs = ts.map(t => f"\${t.toSeconds.timeSpan}%1.4f").mkString(", ")
     *          println(f"Avg: \${avg(ts).timeSpan}%1.4f; T:\${t.toSeconds.timeSpan}%1.4f; Ts:\$sTs")
     *        }
     *     }(l => println("non-empty:"+l))
     *   }
     * }{t => println("overall-time:"+t.toSeconds)}
     * }}}
     *
     * @note    **If `f` has side effects it may not be possible to use this method.**
     *
     * @note    If epsilon is too small we can get an endless loop as the termination
     *          condition is never met. However, in practice often a value such as "1 or 2"
     *          is still useable.
     *
     * @note    This method can generally only be used to measure the time of some process
     *          that does not require user interaction or disk/network access. In the latter
     *          case the variation between two runs will be too coarse grained to get
     *          meaningful results.
     *
     * @param   epsilon The maximum percentage that *the final run* is allowed to affect
     *          the average. In other words, if the effect of the last execution on the
     *          average is less than `epsilon` percent then the evaluation halts and the
     *          result of the last run is returned.
     * @param   consideredRunsEpsilon Controls which runs are taken into consideration
     *          when calculating the average. Only those runs are used that are at most
     *          `consideredRunsEpsilon%` slower than the last run. Additionally,
     *          the last run is only considered if it is at most `consideredRunsEpsilon%`
     *          slower than the average. Hence, it is even possible that the average may rise
     *          during the evaluation of `f`.
     * @param   f The side-effect free function that will be measured.
     * @param   r A function that is called back whenever `f` was successfully evaluated.
     *          The signature is:
     *          {{{
     *          def r(
     *              lastExecutionTime:Nanoseconds,
     *              consideredExecutionTimes : Seq[Nanoseconds]
     *          ) : Unit
     *          }}}
     *           1. The first parameter is the last execution time of `f`.
     *           1. The last parameter is the list of times required to evaluate `f` that are taken
     *          into consideration when calculating the average.
     * @param   runGC If `true` the garbage collector is run using `org.opalj.util.gc()`
     *          before each run. This may be necessary to get reasonable stable behavior between
     *          multiple runs. However, if each run takes very long and the VM has to perform
     *          garbage collection as part of executing f (and also has to increase the JVM's heap)
     *          getting stable measurements is unlikely.
     */
    def time[T](
        epsilon:                     Int,
        consideredRunsEpsilon:       Int,
        minimalNumberOfRelevantRuns: Int,
        f:                           => T,
        runGC:                       Boolean = false
    )(
        r: (Nanoseconds, Seq[Nanoseconds]) => Unit
    ): T = {

        try {
            require(minimalNumberOfRelevantRuns >= 3)

            require(
                consideredRunsEpsilon > epsilon,
                s"epsilon ($epsilon) < consideredRunsEpsilon ($consideredRunsEpsilon)"
            )

            var result: T = 0.asInstanceOf[T]

            val e = epsilon.toDouble / 100.0d
            val filterE = (consideredRunsEpsilon + 100).toDouble / 100.0d

            var runsSinceLastUpdate = 0
            var times = List.empty[Nanoseconds]
            if (runGC) gc()
            time { result = f } { t =>
                times = t :: times
                if (t.timeSpan <= 999 /*ns*/ ) {
                    r(t, times)
                    OPALLogger.warn(
                        "common",
                        s"the time required by the function (${t.toString}) "+
                            "is too small to get meaningful measurements."
                    )(GlobalLogContext)

                    // Non local-returns will be deprecated in Scala 3
                    // Replace this by scala.util.control.NonLocalReturns in Scala 3
                    throw Return[T](result)
                }
            }
            var avg: Double = times.head.timeSpan.toDouble
            do {
                if (runGC) gc()
                time {
                    result = f
                } { t =>
                    if (t.timeSpan <= avg * filterE) {
                        // let's throw away all runs that are significantly slower than the last run
                        times = t :: times.filter(_.timeSpan <= t.timeSpan * filterE)
                        avg = times.map(_.timeSpan).sum.toDouble / times.size.toDouble
                        runsSinceLastUpdate = 0
                    } else {
                        runsSinceLastUpdate += 1
                        if (runsSinceLastUpdate > minimalNumberOfRelevantRuns * 2) {
                            // for whatever reason the current average seems to be "too" slow
                            // let's add the last run to rise the average
                            times = t :: times
                            avg = times.map(_.timeSpan).sum.toDouble / times.size.toDouble
                            runsSinceLastUpdate = 0
                        }
                    }
                    r(t, times)
                }
            } while (times.size < minimalNumberOfRelevantRuns ||
                Math.abs(avg - times.head.timeSpan) > avg * e)

            result

        } catch {
            case Return(result) => result.asInstanceOf[T]
        }
    }

    /**
     * Times the execution of a given function `f`.
     *
     * @param    r A function that is passed the time that it took to evaluate `f` and the result
     *             produced by `f`; `r` is only called if `f` succeeds.
     */
    def run[T, X](f: => T)(r: (Nanoseconds, T) => X): X = {
        val startTime: Long = System.nanoTime
        val result = f
        val endTime: Long = System.nanoTime
        r(Nanoseconds.TimeSpan(startTime, endTime), result)
    }
}
