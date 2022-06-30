/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Mixin this trait if you want to reify the constraints that are stated by the
 * abstract interpretation framework.
 * This is particularly useful for testing and debugging purposes.
 *
 * @author Michael Eichberg
 */
trait RecordConstraints extends ReifiedConstraints { domain: ValuesDomain =>

    private[this] var constraints: Set[ReifiedConstraint] = Set.empty

    /**
     * Returns the set of constraints.
     */
    def allConstraints: Set[ReifiedConstraint] = constraints

    override def nextConstraint(constraint: ReifiedConstraint): Unit = {
        constraints += constraint
    }
}

