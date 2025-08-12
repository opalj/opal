/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time

/**
 * Computes the field assignability.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */
object FieldAssignability extends ProjectsAnalysisApplication {

    protected class FieldAssignabilityConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {
        val description = "Computes information about the immutability of fields"
    }

    protected type ConfigType = FieldAssignabilityConfig

    protected def createConfig(args: Array[String]): FieldAssignabilityConfig = new FieldAssignabilityConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: FieldAssignabilityConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        time {
            project.get(FPCFAnalysesManagerKey).runAll(
                EagerFieldAccessInformationAnalysis,
                LazyL2FieldAssignabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )
        } { t => println(s"Analysis took $t.") }

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

        (project, BasicReport(message.stripMargin('|')))
    }
}
