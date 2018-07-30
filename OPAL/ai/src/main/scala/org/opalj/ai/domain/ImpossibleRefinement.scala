/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Thrown to indicate that a refinement of some value was not possible.
 *
 * @author Michael Eichberg
 */
case class ImpossibleRefinement(
        value:          AnyRef,
        refinementGoal: String
) extends AIException(s"refining $value failed: $refinementGoal" /*,null, true, false*/ )
