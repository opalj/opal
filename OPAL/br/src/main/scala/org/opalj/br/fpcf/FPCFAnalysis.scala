/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.ProjectBasedAnalysis
import org.opalj.br.analyses.SomeProject

/**
 * Common super-trait of all analysis which use MISAF.
 * (Formerly known as Fixpoint-Computations Framework/PropertyStore.)
 */
trait FPCFAnalysis extends org.opalj.fpcf.FPCFAnalysis with ProjectBasedAnalysis

abstract class DefaultFPCFAnalysis(val project: SomeProject) extends FPCFAnalysis
