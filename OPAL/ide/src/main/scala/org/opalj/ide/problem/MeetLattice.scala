/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package problem

/**
 * Interface representing the lattice that orders the IDE values
 */
trait MeetLattice[Value <: IDEValue] {
    /**
     * The top value of the lattice
     */
    def top: Value

    /**
     * The bottom value of the lattice
     */
    def bottom: Value

    /**
     * Compute the result of meeting two values
     */
    def meet(x: Value, y: Value): Value
}
