/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package ifds
package problem

import org.opalj.ide.problem.MeetLattice

/**
 * Lattice to use for IFDS problems that are solved with an IDE solver.
 *
 * @author Robin Körkemeier
 */
object IFDSLattice extends MeetLattice[IFDSValue] {
    override def top: IFDSValue = Top

    override def bottom: IFDSValue = Bottom

    override def meet(x: IFDSValue, y: IFDSValue): IFDSValue = (x, y) match {
        case (Top, Top) => Top
        case _          => Bottom
    }
}
