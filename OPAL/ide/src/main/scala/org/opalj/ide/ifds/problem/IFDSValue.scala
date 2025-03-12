/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package ifds
package problem

import org.opalj.ide.problem.IDEValue

/**
 * Type for modeling values for IFDS problems that are solved with an IDE solver.
 *
 * @author Robin Körkemeier
 */
trait IFDSValue extends IDEValue

/**
 * Top value
 *
 * @author Robin Körkemeier
 */
case object Top extends IFDSValue

/**
 * Bottom value (all result facts have the bottom value)
 *
 * @author Robin Körkemeier
 */
case object Bottom extends IFDSValue
