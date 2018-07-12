/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

sealed abstract class DependeeUpdateHandling

final case object EagerDependeeUpdateHandling extends DependeeUpdateHandling

final case class LazyDependeeUpdateHandling(
        delayHandlingOfNonFinalDependeeUpdates: Boolean = true,
        delayHandlingOfFinalDependeeUpdates:    Boolean = false
) extends DependeeUpdateHandling
