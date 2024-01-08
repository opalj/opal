/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.ProjectBasedAnalysis
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore

/**
 * Common super-trait of all analysis which use MISAF.
 * (Formerly known as Fixpoint-Computations Framework/PropertyStore.)
 */
trait FPCFAnalysis extends ProjectBasedAnalysis {

    implicit final val propertyStore: PropertyStore = project.get(PropertyStoreKey)

    final def ps: PropertyStore = propertyStore

}

abstract class DefaultFPCFAnalysis(val project: SomeProject) extends FPCFAnalysis
