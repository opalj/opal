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
package br
package analyses

/**
 * Used by analyses to report progress. Typically an analysis is expected to report
 * progress every 250 to 2000 milliseconds. It should - however - check every ~100
 * milliseconds the interrupted status to enable a timely termination.
 *
 * @note Implementations of this class must be thread safe if the analysis is multi-
 *      threaded.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait ProgressManagement {

    /**
     * This method is called by the analysis to report progress.
     *
     * The analysis is allowed to just report `End` events. However, if it
     * reports start events it must also report `End` events and it must use
     * the same id to do so. This enables the correlation of the events. The analyis
     * must never report more than one `Start`/`End` event per step id.
     *
     * If the analysis is interrupted it may either signal (as the very last event)
     * a `Killed` event or an `End` event if the analysis completed normally.
     *
     * @param step The step/id of the event. The first event reported by the analysis
     *      has to use the number "1". The step id of the `Killed` event is "-1".
     * @param message An optional message. Typically used in combination with `Start`
     *      events.
     */
    def progress(step: Int, event: Event, message: Option[String]): Unit

    final def start(step: Int, message: String): Unit =
        progress(step, EventType.Start, Some(message))

    final def end(step: Int): Unit =
        progress(step, EventType.End, None)

    final def end(step: Int, message: String): Unit =
        progress(step, EventType.End, Some(message))

    /**
     * This method is called by the analysis method
     * to check whether the analysis should be aborted.
     */
    def isInterrupted(): Boolean

}

/**
 * Factory for a function to create a default progress management object that
 * basically does not track the progress.
 *
 * @author Michael Eichberg
 */
object ProgressManagement {

    val None: (Int) ⇒ ProgressManagement = (maxSteps) ⇒ {
        new ProgressManagement {

            final def progress(step: Int, evt: Event, msg: Option[String]): Unit = {}

            final def isInterrupted: Boolean = false

        }
    }
}

