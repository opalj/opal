/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import org.opalj.br.ObjectType
import org.opalj.br.PCAndInstruction
import org.opalj.br.MethodDescriptor.JustReturnsString
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.LoadString
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse

/**
 * The analysis demonstrates how to find values passed to Chipher.getInstance:
 * {{{
 * static Chipher getInstance(String transformation)
 * static Cipher  getInstance(String transformation, Provider provider)
 * static Cipher  getInstance(String transformation, String provider)
 * }}}
 *
 * @author Michael Reif
 */
object CipherGetInstanceStringUsage extends ProjectAnalysisApplication {

    override def title: String = "input value analysis for Chipher.getInstance calls"

    override def description: String = "Analyzes the input values of Chipher.getInstance calls."

    // #################### CONSTANTS ####################

    val Cipher = ObjectType("javax/crypto/Cipher")

    val Key = ObjectType("java/security/Key")

    // #################### ANALYSIS ####################

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val report = new ConcurrentLinkedQueue[String]

        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            val result = BaseAI(m, new DefaultDomainWithCFGAndDefUse(project, m))
            val code = result.domain.code
            for {
                PCAndInstruction(pc, INVOKESTATIC(Cipher, false, "getInstance", _)) <- code
                vos <- result.domain.operandOrigin(pc, 0)
            } {
                // getInstance is static, algorithm is first param
                code.instructions(vos) match {
                    case LoadString(value) =>
                        report.add(m.toJava(s"passed value ($pc): $value"))
                    case invoke @ INVOKEINTERFACE(Key, "getAlgorithm", JustReturnsString) =>
                        report.add(m.toJava(s"return value of ($pc): ${invoke.toString}"))

                    case get @ GETFIELD(_, _, _) => println("uknown value: "+get)
                    case i                       => println("unsupported instruction: "+i)
                }
            }
        }

        BasicReport(report.asScala.mkString("\n"))
    }
}
