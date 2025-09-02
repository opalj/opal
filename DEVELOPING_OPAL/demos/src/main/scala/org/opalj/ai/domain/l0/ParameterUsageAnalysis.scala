/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Simple analysis that identifies unused and passed through parameters; i.e., those that are
 * directly returned by the given method.
 *
 * @author Michael Eichberg
 */
object ParameterUsageAnalysis extends ProjectsAnalysisApplication {

    protected class MethodsReturningParameterConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Identifies parameters that are - at least on some paths - directly returned or unused"
    }

    protected type ConfigType = MethodsReturningParameterConfig

    protected def createConfig(args: Array[String]): MethodsReturningParameterConfig =
        new MethodsReturningParameterConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodsReturningParameterConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        var analysisTime: Seconds = Seconds.None
        val (returnedParameters, unusedParameters) = time {

            val unusedParameters = new ConcurrentLinkedQueue[String]
            val returnedParameters = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            project.parForeachMethodWithBody() { m =>
                val method = m.method
                val psCount = method.actualArgumentsCount // includes "this" in case of instance methods
                if (psCount > 0) {
                    val isStatic = method.isStatic
                    val descriptor = method.descriptor
                    val domain = new BaseDomainWithDefUse(project, method)
                    val result = ai(method, domain)
                    val instructions = result.domain.code.instructions
                    val methodSignature = method.toJava
                    def validateArgument(valueOrigin: ValueOrigin): Unit = {
                        val usedBy = result.domain.usedBy(valueOrigin)
                        if (usedBy eq null) {
                            val use = s" the value with origin $valueOrigin is not used"
                            unusedParameters.add(methodSignature + use)
                        } else {
                            usedBy.foreach { usage =>
                                if (instructions(usage).isReturnInstruction) {
                                    val use = s" the argument with origin $valueOrigin is returned by $usage"
                                    returnedParameters.add(methodSignature + use)
                                }
                            }
                        }
                    }

                    var pIndex = method.descriptor.parametersCount - 1
                    while (pIndex >= 0) {
                        validateArgument(parameterIndexToValueOrigin(isStatic, descriptor, pIndex))
                        pIndex -= 1
                    }

                    if (!isStatic) { // check implicit "this" parameter
                        validateArgument(valueOrigin = -1)
                    }
                }
            }

            (returnedParameters.asScala.toList.sorted, unusedParameters.asScala.toList.sorted)

        } { t => analysisTime = t.toSeconds }

        val occurences = returnedParameters.size
        val report = BasicReport(
            returnedParameters.mkString("Directly returned parameters:\n", "\n", "\n\n") +
                unusedParameters.mkString("Unused parameters:\n", "\n", "\n\n") +
                s"\nThe analysis took $analysisTime and found $occurences direct returns"
        )
        (project, report)
    }

}
