/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Enables the management of the progress of a long running computation.
 * Typically a long running progress, such as an analysis, is expected to report
 * progress every 250 to 2000 milliseconds. It should -- however -- check every ~100
 * milliseconds the interrupted status to enable a timely termination.
 *
 * This trait defines a call-back interface that is implemented by some class that
 * runs an analysis and which passes an instance of it to some analysis to report
 * progress.
 *
 * @note   Implementations of this class must be thread safe if the analysis is multi-
 *         threaded.
 * @note   Implementations must handle the case where a step that was started later
 *         finishes earlier than a previous step. In other words, even if the last step
 *         has ended, that does not mean that the analysis as a whole has already finished.
 *         Instead an implementation has to track how many steps have ended to determine
 *         when the whole analysis has ended.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait ProgressManagement {

    /**
     * This method is called by the analysis to report progress.
     *
     * An analysis is allowed to just report `End` events. However, if it
     * reports `Start` events it must also report `End` events and it must use
     * the same id to do so. This enables the correlation of the events. The analysis
     * must never report more than one `Start`/`End` event per step id.
     *
     * If the analysis is interrupted it may either signal (as the very last event)
     * a `Killed` event or an `End` event if the analysis completed normally.
     *
     * @param  step The step/id of the event. The first event reported by the analysis
     *         has to use the number "1". The step id of the `Killed` event is "-1".
     * @param  message An optional message. Typically used in combination with `Start`
     *         events.
     */
    def progress(step: Int, event: ProgressEvent, message: Option[String]): Unit

    final def start(step: Int, message: String): Unit = {
        progress(step, ProgressEvents.Start, Some(message))
    }

    final def end(step: Int): Unit = progress(step, ProgressEvents.End, None)

    final def end(step: Int, message: String): Unit = end(step, Some(message))

    final def end(step: Int, message: Option[String]): Unit = {
        progress(step, ProgressEvents.End, message)
    }

    /**
     * A convenience method to execute one analysis step. If executing the step
     * takes longer you have to call `isInterrupted` to check the interrupt status.
     */
    final def step[T](
        step:         Int,
        startMessage: String
    )(
        f: => (T, Option[String])
    ): T = {
        start(step, startMessage)
        val (t, endMessage) = try {
            f
        } catch {
            case t: Throwable => end(step, "failed: "+t.getMessage); throw t
        }
        end(step, endMessage)
        t
    }

    /**
     * This method is called by the analysis method to check whether the analysis should be aborted.
     * The analysis will abort the computation if this method returns `true`.
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

    val None: Int => ProgressManagement = maxSteps => new ProgressManagement {

        final override def progress(step: Int, event: ProgressEvent, msg: Option[String]): Unit = {}

        final override def isInterrupted(): Boolean = false

    }
}
