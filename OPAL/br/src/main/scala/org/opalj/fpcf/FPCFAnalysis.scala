/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.ProjectBasedAnalysis

/**
 * Common super-trait of all analysis which use MISAF.
 * (Formerly known as Fixpoint-Computations Framework/PropertyStore.)
 */
trait FPCFAnalysis extends ProjectBasedAnalysis {

    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)

    final def ps: PropertyStore = propertyStore

}
