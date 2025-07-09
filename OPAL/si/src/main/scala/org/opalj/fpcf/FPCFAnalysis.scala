/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.si.ProjectBasedAnalysis

/**
 * Common super-trait of all analysis which use MISAF.
 * (Formerly known as Fixpoint-Computations Framework/PropertyStore.)
 */
trait FPCFAnalysis extends ProjectBasedAnalysis {

    implicit final val propertyStore: PropertyStore = project.get(PropertyStoreKey)

    final def ps: PropertyStore = propertyStore

}
