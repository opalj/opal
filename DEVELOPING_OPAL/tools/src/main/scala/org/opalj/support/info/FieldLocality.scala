/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.MultiProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.ExtensibleLocalField
import org.opalj.br.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.LocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoLocalField
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.util.PerformanceEvaluation.time

import org.rogach.scallop.ScallopConf

/**
 * Computes the field locality; see [[org.opalj.br.fpcf.properties.FieldLocality]] for details.
 *
 * @author Florian Kuebler
 */
object FieldLocality extends MultiProjectAnalysisApplication {

    protected class FieldLocalityConfig(args: Array[String]) extends ScallopConf(args)
        with MultiProjectAnalysisConfig[FieldLocalityConfig]
        with PropertyStoreBasedCommandLineConfig with CGBasedCommandLineConfig {

        banner("Computes information about field locality\n")

        init()
    }

    protected type ConfigType = FieldLocalityConfig

    protected def createConfig(args: Array[String]): FieldLocalityConfig = new FieldLocalityConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: FieldLocalityConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        time {
            project.get(FPCFAnalysesManagerKey).runAll(
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                EagerFieldLocalityAnalysis
            )
        } { t => println(s"Analysis took $t.") }

        val local = ps.finalEntities(LocalField).toSeq
        val nolocal = ps.finalEntities(NoLocalField).toSeq
        val extLocal = ps.finalEntities(ExtensibleLocalField).toSeq
        val getter = ps.finalEntities(LocalFieldWithGetter).toSeq
        val extGetter = ps.finalEntities(ExtensibleLocalFieldWithGetter).toSeq

        val message =
            s"""|# of local fields: ${local.size}
                |# of not local fields: ${nolocal.size}
                |# of extensible local fields: ${extLocal.size}
                |# of local fields with getter: ${getter.size}
                |# of extensible local fields with getter: ${extGetter.size}
                |"""

        (project, BasicReport(message.stripMargin('|')))
    }
}
