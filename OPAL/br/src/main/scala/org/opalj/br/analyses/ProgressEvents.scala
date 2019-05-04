/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Characterizes the type of an event related to a running analysis.
 *
 * @see [[ProgressManagement]] for further details.
 *
 * @author Michael Eichberg
 */
object ProgressEvents extends Enumeration {

    /**
     * Used to signal the start of a (longer-running) computation.
     * Each computation that signals a start '''must''' also signal an end of the computation
     * using `End` or `Killed`.
     */
    val Start = Value("start")

    /**
     * Used to signal the end of a computation.
     */
    val End = Value("end")

    /**
     * Used to signal that a computation was killed.
     *
     * '''After signaling a `Killed` event the underlying computation is not
     * allowed to signal any further events.'''
     */
    val Killed = Value("killed")
}
