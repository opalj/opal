/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

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
import scala.collection.immutable.ListSet
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.MethodInvocationInstruction

/**
 * Simple analysis that takes the "unused"-Node from the def-use graph
 * and returns all its children, that is definitions and assignments that are not used again
 * locally.
 *
 * @author Stephan Neumann
 */
object SimpleDefUseAnalysis extends ProjectAnalysisApplication {

    override def title: String =
        "Identifies unused variables and unnecessary calculations"

    override def description: String =
        "Identifies variable declarations or assignments that are not used again locally"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        var analysisTime: Seconds = Seconds.None
        val unusedDefUseNodes = time {

            val results = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            theProject.parForeachMethodWithBody() { m =>
                val method = m.method
                if (!method.isSynthetic) {
                    val domain = new DefaultDomainWithCFGAndDefUse(theProject, method)
                    val result = ai(method, domain)
                    val instructions = result.domain.code.instructions
                    val unused = result.domain.unused
                    if (unused.nonEmpty) {
                        var values = ListSet.empty[String]
                        val implicitParameterOffset = if (!method.isStatic) 1 else 0
                        unused.foreach { vo =>
                            if (vo < 0) {
                                // we have to make sure that we do not create an issue report
                                // for instance methods that can be/are inherited
                                if (method.isStatic ||
                                    method.isPrivate ||
                                    // TODO check that the method parameter is never used... across all implementations of the method... only then report it...||
                                    method.name == "<init>") {
                                    if (vo == -1) {
                                        values += "this"
                                    } else {
                                        values += "param:"+(-(vo + implicitParameterOffset))
                                    }
                                }
                            } else {
                                val instruction = instructions(vo)
                                instruction.opcode match {
                                    case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                                        INVOKESTATIC.opcode | INVOKESPECIAL.opcode =>
                                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                                        values +=
                                            vo.toString+": invoke "+invoke.declaringClass.toJava+
                                            "{ "+
                                            invoke.methodDescriptor.toJava(invoke.name)+
                                            " }"
                                    case _ =>
                                        values += vo.toString+": "+instruction.toString(vo)
                                }

                            }

                        }
                        if (values.nonEmpty)
                            results.add(method.toJava(values.mkString("{", ",", "}")))
                    }
                }
            }
            results.asScala

        } { t => analysisTime = t.toSeconds }

        BasicReport(
            unusedDefUseNodes.mkString("Methods with unused values:\n", "\n", "\n")+
                "The analysis took "+analysisTime+" and found "+unusedDefUseNodes.size+" issues"
        )
    }

}
