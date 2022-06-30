/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Simple analysis that identifies unused and passed through parameters; i.e., those that are
 * directly returned by the given method.
 *
 * @author Michael Eichberg
 */
object ParameterUsageAnalysis extends ProjectAnalysisApplication {

    override def title: String = "Identifies methods which return a given parameter"

    override def description: String = {
        "Identifies parameters that are - at least on some paths - directly returned"
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        var analysisTime: Seconds = Seconds.None
        val (returnedParameters, unusedParameters) = time {

            val unusedParameters = new ConcurrentLinkedQueue[String]
            val returnedParameters = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            theProject.parForeachMethodWithBody() { m =>
                val method = m.method
                val psCount = method.actualArgumentsCount // includes "this" in case of instance methods
                if (psCount > 0) {
                    val isStatic = method.isStatic
                    val descriptor = method.descriptor
                    val domain = new BaseDomainWithDefUse(theProject, method)
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
        BasicReport(
            returnedParameters.mkString("Directly returned parameters:\n", "\n", "\n\n") +
                unusedParameters.mkString("Unused parameters:\n", "\n", "\n\n") +
                s"\nThe analysis took $analysisTime and found $occurences direct returns"

        )
    }

}
