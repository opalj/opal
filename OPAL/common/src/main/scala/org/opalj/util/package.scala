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
package org
package opalj

import java.lang.management.MemoryMXBean
import java.lang.management.ManagementFactory

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext

/**
 * Utility methods.
 *
 * @author Michael Eichberg
 */
package object util {

    def avg(ts: Traversable[Nanoseconds]): Nanoseconds = {
        if (ts.isEmpty)
            return Nanoseconds.None;

        Nanoseconds(ts.map(_.timeSpan).sum / ts.size)
    }

    /**
     * Converts the specified number of bytes into the corresponding number of megabytes
     * and returns a textual representation.
     */
    def asMB(bytesCount: Long): String = {
        val mbs = bytesCount / 1024.0d / 1024.0d
        f"$mbs%.2f MB" // String interpolation
    }

    /**
     * Converts the specified number of nanoseconds into milliseconds.
     */
    final def ns2ms(nanoseconds: Long): Double = nanoseconds.toDouble / 1000.0d / 1000.0d

    /**
     *  Tries it best to run the garbage collector and to wait until all objects are also
     *  finalized.
     */
    final def gc(
        memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean,
        maxGCTime:    Milliseconds = new Milliseconds(333)
    )(
        implicit
        logContext: Option[LogContext] = None
    ): Unit = {
        val startTime = System.nanoTime()
        var run = 0
        do {
            if (logContext.isDefined) {
                val pendingCount = memoryMXBean.getObjectPendingFinalizationCount()
                OPALLogger.info(
                    "performance",
                    s"garbage collection run $run (pending finalization: $pendingCount)"
                )(logContext.get)
            }
            // In general it is **not possible to guarantee** that the garbage collector is really
            // run, but we still do our best.
            memoryMXBean.gc()
            if (memoryMXBean.getObjectPendingFinalizationCount() > 0) {
                // It may be the case that some finalizers (of just gc'ed object) are still running
                // and -- therefore -- some further objects are freed after the gc run.
                Thread.sleep(50)
                memoryMXBean.gc()
            }
            run += 1
        } while (memoryMXBean.getObjectPendingFinalizationCount() > 0 &&
            ns2ms(System.nanoTime() - startTime) < maxGCTime.timeSpan)
    }

}
