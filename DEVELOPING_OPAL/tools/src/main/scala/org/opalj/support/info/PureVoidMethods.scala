/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.MultiProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis

import org.rogach.scallop.ScallopConf

/**
 * Identifies pure/side-effect free methods with a void return type.
 *
 * @author Dominik Helm
 */
object PureVoidMethods extends MultiProjectAnalysisApplication {

    protected class PureVoidMethodsConfig(args: Array[String]) extends ScallopConf(args)
        with MultiProjectAnalysisConfig[PureVoidMethodsConfig]
        with PropertyStoreBasedCommandLineConfig with CGBasedCommandLineConfig {
        banner("Finds useless methods because they are side effect free and do not return a value (void)\n")
    }

    protected type ConfigType = PureVoidMethodsConfig

    protected def createConfig(args: Array[String]): PureVoidMethodsConfig = new PureVoidMethodsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: PureVoidMethodsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        project.get(FPCFAnalysesManagerKey).runAll(
            EagerFieldAccessInformationAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldAssignabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            EagerL2PurityAnalysis
        )

        val entities = ps.entities(br.fpcf.properties.Purity.key)

        val voidReturn = entities.collect {
            case FinalEP(m: DefinedMethod, p @ (CompileTimePure | Pure | SideEffectFree)) // Do not report empty methods, they are e.g. used for base implementations of listeners
                // Empty methods still have a return instruction and therefore a body size of 1
                if m.definedMethod.returnType.isVoidType && !m.definedMethod.isConstructor &&
                    m.definedMethod.body.isDefined && m.definedMethod.body.get.instructions.length != 1 =>
                (m, p)
        }

        (
            project,
            BasicReport(
                voidReturn.iterator.to(Iterable) map { mp =>
                    val (m, p) = mp
                    s"${m.toJava} has a void return type but it is $p"
                }
            )
        )
    }
}
