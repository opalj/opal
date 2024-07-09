/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis

/**
 * Computes the field assignability.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */
object FieldAssignabilityRunner extends ProjectAnalysisApplication {

    override def title: String = "Field immutability"

    override def description: String = { "Provides information about the immutability of fields." }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        project.get(RTACallGraphKey)
        val (ps, _) = project
            .get(FPCFAnalysesManagerKey)
            .runAll(
                LazyL2FieldAssignabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )

        val nonAssignable = ps.finalEntities(NonAssignable).toSeq
        val effectivelyNonAssignable = ps.finalEntities(EffectivelyNonAssignable).toSeq
        val lazilyInitialized = ps.finalEntities(LazilyInitialized).toSeq
        val unsafelyLazilyInitialized = ps.finalEntities(UnsafelyLazilyInitialized).toSeq
        val assignable = ps.finalEntities(Assignable).toSeq

        val message =
            s"""|# non assignable fields: ${nonAssignable.size}
                |# effectively non assignable fields: ${effectivelyNonAssignable.size}
                |# lazily initialized fields: ${lazilyInitialized.size}
                |# unsafely lazily initialized fields: ${unsafelyLazilyInitialized.size}
                |# assignable fields ${assignable.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
