/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package project

/**
 * Defines a method to optionally report some result.
 *
 * Primarily intended to be mixed-in by domains that are used by an `AIProject`.
 *
 * @author Michael Eichberg
 */
trait OptionalReport {

    /**
     * Returns `Some(&lt;String&gt;)` if there is something to report and `None`
     * otherwise.
     */
    def report: Option[String]

}
