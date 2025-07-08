/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File
import java.net.URL
import scala.collection.mutable

import org.opalj.ai.domain.Origin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.domain.L1DefaultDomainWithCFGAndDefUseAndSignatureRefinement
import org.opalj.br.Method
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.NEW
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.NoPropertyStoreArg
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig

/**
 * Extracts the information about receivers of method calls.
 *
 * @author Michael Eichberg
 */
object MethodReceivers extends ProjectsAnalysisApplication {

    protected class MethodReceiversConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with PropertyStoreBasedCommandLineConfig {
        val description = "Collects information about method call receiver"
        args(NoPropertyStoreArg)
    }

    protected type ConfigType = MethodReceiversConfig

    protected def createConfig(args: Array[String]): MethodReceiversConfig = new MethodReceiversConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodReceiversConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val performAI: (Method) => AIResult { val domain: Domain with RecordDefUse } =
            if (!analysisConfig.get(NoPropertyStoreArg).getOrElse(false)) {
                analysisConfig.setupPropertyStore(project)
                val analysesManager = project.get(FPCFAnalysesManagerKey)
                analysesManager.runAll(
                    org.opalj.ai.fpcf.analyses.EagerLBFieldValuesAnalysis,
                    org.opalj.ai.fpcf.analyses.EagerLBMethodReturnValuesAnalysis
                )
                (m: Method) => {
                    BaseAI(m, new L1DefaultDomainWithCFGAndDefUseAndSignatureRefinement(project, m))
                }
            } else {
                project.updateProjectInformationKeyInitializationData(org.opalj.ai.common.SimpleAIKey) { // new org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse(p, m)
                    _ => (m: Method) => new org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse(project, m)
                }
                project.get(org.opalj.ai.common.SimpleAIKey)
            }

        val counts: mutable.Map[String, Int] = mutable.Map.empty.withDefaultValue(0)

        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            lazy val aiResult = performAI(m).asInstanceOf[AIResult { val domain: Domain with Origin }]
            for {
                PCAndInstruction(pc, i) <- m.body.get
                if i.isMethodInvocationInstruction
                invocationInstruction = i.asMethodInvocationInstruction
                if invocationInstruction.isVirtualMethodCall
                if invocationInstruction.methodDescriptor.returnType.isClassType
                receiverPosition = invocationInstruction.methodDescriptor.parametersCount
                operands = aiResult.operandsArray(pc)
                if operands != null
            } {
                val receiver = operands(receiverPosition)
                // TODO Add special support for union types.
                val value = receiver.asDomainReferenceValue
                val utb = value.upperTypeBound
                var s =
                    if (utb.isSingletonSet)
                        utb.head.toJava
                    else
                        utb.map(_.toJava).mkString("⋂(", " with ", ")")

                s += ", " + value.isPrecise
                val triviallyPrecise =
                    value.isPrecise &&
                        value.upperTypeBound.isSingletonSet &&
                        !value.upperTypeBound.head.isArrayType && (
                            project.classFile(value.upperTypeBound.head.asClassType).get.isFinal ||
                            project.classHierarchy.hasSubtypes(value.upperTypeBound.head.asClassType).isNoOrUnknown
                        )
                if (triviallyPrecise)
                    s += " (trivially)"

                s += ", " + value.isNull
                if (aiResult.domain.origins(value.asInstanceOf[aiResult.domain.DomainValue]).forall(pc =>
                        pc >= 0 && {
                            val i = m.body.get.instructions(pc)
                            i.opcode == NEW.opcode || i.isLoadConstantInstruction
                        }
                    )
                )
                    s += " (trivially)"

                this.synchronized {
                    val newCount = counts(s) + 1
                    counts(s) = newCount
                }
            }
        }

        (
            project,
            BasicReport(
                "type, isPrecise, isNull, count\n" +
                    counts
                        .iterator
                        .map(e => e._1 + ", " + e._2)
                        .mkString("\n")
            )
        )
    }
}
