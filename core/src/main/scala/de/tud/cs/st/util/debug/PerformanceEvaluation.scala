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
package de.tud.cs.st
package util
package debug

/**
 * Measures the execution time of some code.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Eichberg
 */
class PerformanceEvaluation extends Locking {

    import scala.collection.mutable.Map

    private[this] val times: Map[Symbol, Long] = Map.empty

    /**
     * Times the execution of the given method / function literal / code block and
     * adds it to the execution time of previous methods / function literals/ code blocks
     * that were measured and for which the same symbol was used. <br/>
     * E.g. <code>time('base_analysis){ ... do something ... }</code>
     *
     * @param s Symbol used to put multiple measurements into relation.
     * @param f The function that will be evaluated and for which the execution
     *      time will be measured.
     */
    final def time[T](s: Symbol)(f: ⇒ T): T = {
        val startTime = System.nanoTime
        try {
            f
        } finally {
            val endTime = System.nanoTime
            withWriteLock { doUpdateTimes(s, endTime - startTime) }
        }
    }

    /**
     *
     * Called by the `time` method.
     *
     * ==Thread Safety==
     * The `time` method takes care of the synchronization.
     */
    protected[this] def doUpdateTimes(s: Symbol, duration: Long): Unit = {
        val old = times.getOrElseUpdate(s, 0l)
        times.update(s, old + duration)
    }

    /**
     * Returns the overall time spent by computations with the given symbol.
     */
    final def getTime(s: Symbol): Long = withReadLock { doGetTime(s) }

    /**
     * Called by the `getTime(Symbol)` method.
     *
     * ==Thread Safety==
     * The `getTime` method takes care of the synchronization.
     */
    protected[this] def doGetTime(s: Symbol): Long = {
        times.getOrElse(s, 0l)
    }

    /**
     * Resets the overall time spent by computations with the given symbol.
     */
    final def reset(s: Symbol): Unit = {
        withWriteLock { doReset(s) }
    }

    /**
     * Called by the `reset(Symbol)` method.
     *
     * ==Thread Safety==
     * The `reset` method takes care of the synchronization.
     */
    protected[this] def doReset(s: Symbol): Unit = {
        times.remove(s)
    }

    /**
     * Resets everything. The effect is comparable to creating a new
     * `PerformanceEvaluation` object, but is a bit more efficient.
     */
    final def resetAll(): Unit = {
        withWriteLock { doResetAll() }
    }

    /**
     * Called by the `resetAll` method.
     *
     * ==Thread Safety==
     * The `resetAll` method takes care of the synchronization.
     */
    protected[this] def doResetAll(): Unit = {
        times.clear()
    }

}
/**
 * Collection of helper functions useful when evaluating the performance of some
 * code.
 *
 * @author Michael Eichberg
 */
object PerformanceEvaluation {

    /**
     * Converts the specified number of bytes into the corresponding nubmer of mega bytes
     * and returns a textual representation.
     */
    def asMB(bytesCount: Long): String = {
        val mbs = bytesCount / 1024 / 1024.0
        f"$mbs%.2f MB" // String interpolation
    }

    /**
     * Converts the specified number of nanoseconds into seconds.
     */
    final def ns2sec(nanoseconds: Long): Double =
        nanoseconds.toDouble / 1000.0d / 1000.0d / 1000.0d

    /**
     * Converts the specified number of nanoseconds into milliseconds.
     */
    final def ns2ms(nanoseconds: Long): Double =
        nanoseconds.toDouble / 1000.0d / 1000.0d

    /**
     * Converts the specified time span and converts it into seconds.
     */
    final def asSec(startTimeInNanoseconds: Long, endTimeInNanoseconds: Long): Double =
        ns2sec(endTimeInNanoseconds - startTimeInNanoseconds)

    /**
     * Measures the amount of memory that is used as a side-effect
     * of executing the given function `f`.
     */
    def memory[T](f: ⇒ T)(mu: Long ⇒ Unit): T = {
        val memoryMXBean = java.lang.management.ManagementFactory.getMemoryMXBean
        memoryMXBean.gc(); System.gc()
        val usedBefore = memoryMXBean.getHeapMemoryUsage.getUsed
        val r = f
        memoryMXBean.gc(); System.gc()
        val usedAfter = memoryMXBean.getHeapMemoryUsage.getUsed
        mu(usedAfter - usedBefore)
        r
    }

    /**
     * Times the execution of a given function `f`.
     *
     * @param r A function that is passed the time (in nano seconds) that it
     *      took to evaluate `f`. `r` is called even if `f` fails with an exception.
     */
    def time[T](f: ⇒ T)(r: Long ⇒ Unit): T = {
        val startTime: Long = System.nanoTime
        val result =
            try {
                f
            } finally {
                val endTime: Long = System.nanoTime
                r(endTime - startTime)
            }
        result
    }

    /**
     * Times the execution of a given function `f` until the execution time has
     * stabilized and the average is only changing in a well-defined manner.
     *
     * In general `time` repeats the execution of `f` as long as the average changes
     * significantly. Furthermore, `f` is executed at least `minimalNumberOfRelevantRuns`
     * times and only those runs are taken into consideration for the calculation of the
     * average that are `consideredRunsEpsilon`% worse than the best run.
     *
     * ==Example Usage==
     * {{{
     * import de.tud.cs.st.util.debug.PerformanceEvaluation._
     *
     * time[String](2,4,3,{
     *      Thread.sleep(300).toString
     * }){ (avg, t, ts) =>
     *      val sTs = ts.map(t => f"\${ns2sec(t)}%1.4f").mkString(", ")
     *      println(f"Avg: \${ns2sec(avg.toLong)}%1.4f; T: \${ns2sec(t)}%1.4f; Ts: \$sTs")
     * }
     * }}}
     *
     * @note **If `f` has side effects this method cannot be used!**
     *
     * @note If epsilon is too small we can get an endless loop as the termination
     *      condition is never met. However, in practice often a value such as "1 or 2"
     *      is still useable.
     *
     * @note This method can generally only be used to measure the time of some process
     *      that does not require user interaction or disk/network access. In the latter
     *      case the variation between two runs will be too coarse grained to get
     *      meaningful results.
     *
     * @param epsilon The maximum percentage that a run is allowed to affect the average.
     *      If the effect of the last execution is less than `epsilon` percent. The
     *      evaluation halts and the result of the last run is returned.
     * @param consideredRunsEpsilon Controls which runs are taken into consideration
     *      when calculating the average. Only those runs are used that are at most
     *      `consideredRunsEpsilon%` slower than the last run. Additionally,
     *      the last run is only considered if it is at most `consideredRunsEpsilon%`
     *      slower than the average. Hence, it is possible that average may rise
     *      during the evaluation of `f`.
     * @param f The function that will be measured.
     * @param r A function that is called back whenever `f` was successfully evaluated.
     *      The signature is: 
     *      {{{
     *      def r(averageInNs:Double, lastExecutionTimeInNs : Long, consideredExecutionTimesInNs : Seq[Long]) : Unit
     *      }}}
     *      Hence, the first parameter is the current average.
     *      The second parameter is the last execution time of `f`.
     *      The last parameter are the times of the evaluation of `f` that are taken
     *      into consideration when calculating the average.
     */
    def time[T >: Null <: AnyRef](
        epsilon: Int,
        consideredRunsEpsilon: Int,
        minimalNumberOfRelevantRuns: Int,
        f: ⇒ T)(
            r: (Double, Long, Seq[Long]) ⇒ Unit): T = {

        require(minimalNumberOfRelevantRuns >= 3)
        require(consideredRunsEpsilon > epsilon)

        var result: T = null

        val e = epsilon.toDouble / 100.0d
        val filterE = (consideredRunsEpsilon + 100).toDouble / 100.0d

        var runsSinceLastUpdate = 0
        var times = List.empty[Long]
        time { f } { t ⇒ times = t :: times }
        var avg: Double = times.head
        do {
            time {
                result = f
            } { t ⇒
                if (t <= avg * filterE) {
                    // let's throw away all runs that are significantly slower than the last run
                    times = t :: times.filter(_ <= t * filterE)
                    avg = times.sum.toDouble / times.size.toDouble
                    runsSinceLastUpdate  = 0
                } else {
                    runsSinceLastUpdate += 1
                    if (runsSinceLastUpdate > 100) {
                        // for whatever reason the current average seems to be "too" slow
                        // let's add the last run to rise the average 
                        times = t :: times
                        avg = times.sum.toDouble / times.size.toDouble
                        runsSinceLastUpdate = 0
                    }
                }
                r(avg, t, times)
            }
        } while (times.size < minimalNumberOfRelevantRuns ||
            Math.abs(avg - times.head) > avg * e)

        result
    }

    /**
     * Times the execution of a given function `f`.
     *
     * @param r A function that is passed the time (in nano seconds) that it
     *      took to evaluate `f` and the result produced by `f`.
     *      `r` is only called if `f` suceeds.
     */
    def run[T, X](f: ⇒ T)(r: (Long, T) ⇒ X): X = {
        val startTime: Long = System.nanoTime
        val result = f
        val endTime: Long = System.nanoTime
        r(endTime - startTime, f)
    }
}
