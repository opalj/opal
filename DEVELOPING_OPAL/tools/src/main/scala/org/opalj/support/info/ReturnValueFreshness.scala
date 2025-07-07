/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.MultiProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.ExtensibleGetter
import org.opalj.br.fpcf.properties.FreshReturnValue
import org.opalj.br.fpcf.properties.Getter
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

import org.rogach.scallop.ScallopConf

/**
 * Computes return value freshness information; see
 * [[org.opalj.br.fpcf.properties.ReturnValueFreshness]] for details.
 *
 * @author Florian Kuebler
 */
object ReturnValueFreshness extends MultiProjectAnalysisApplication {

    protected class ReturnValueFreshnessConfig(args: Array[String]) extends ScallopConf(args)
        with MultiProjectAnalysisConfig[ReturnValueFreshnessConfig]
        with PropertyStoreBasedCommandLineConfig with CGBasedCommandLineConfig {

        banner("Computes whether a method returns a value that is allocated in that method or its callees and has not yet escaped\n")

        init()
    }

    protected type ConfigType = ReturnValueFreshnessConfig

    protected def createConfig(args: Array[String]): ReturnValueFreshnessConfig = new ReturnValueFreshnessConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ReturnValueFreshnessConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyFieldLocalityAnalysis,
            EagerReturnValueFreshnessAnalysis
        )

        val fresh = ps.finalEntities(FreshReturnValue).toSeq
        val notFresh = ps.finalEntities(NoFreshReturnValue).toSeq
        val prim = ps.finalEntities(PrimitiveReturnValue).toSeq
        val getter = ps.finalEntities(Getter).toSeq
        val extGetter = ps.finalEntities(ExtensibleGetter).toSeq

        val message =
            s"""|${fresh.mkString("fresh methods:", "\t\n)}", "")}
                |${getter.mkString("getter methods:", "\t\n)}", "")}
                |${extGetter.mkString("external getter methods:", "\t\n)}", "")}
                |${prim.mkString("methods with primitive return value:", "\t\n)}", "")}
                |${notFresh.mkString("methods that are not fresh at all:", "\t\n)}", "")}
                |# of methods with fresh return value: ${fresh.size}
                |# of methods without fresh return value: ${notFresh.size}
                |# of methods with primitive return value: ${prim.size}
                |# of methods that are getters: ${getter.size}
                |# of methods that are extensible getters: ${extGetter.size}
                |"""

        (project, BasicReport(message.stripMargin('|')))
    }
}
