/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

final case class DependeeUpdateHandling(
        delayHandlingOfNonFinalDependeeUpdates: Boolean = true,
        delayHandlingOfFinalDependeeUpdates:    Boolean = false
)
