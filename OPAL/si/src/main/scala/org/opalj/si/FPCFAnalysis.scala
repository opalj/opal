/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.si

import org.opalj.fpcf.PropertyStore
import org.opalj.log.LogContext

/**
 * Common super-trait of all analysis which use MISAF.
 * (Formerly known as Fixpoint-Computations Framework/PropertyStore.)
 */
trait FPCFAnalysis {
    val project: MetaProject

    implicit def p: MetaProject = project
    implicit final def logContext: LogContext = project.logContext

    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)

    final def ps: PropertyStore = propertyStore
}
