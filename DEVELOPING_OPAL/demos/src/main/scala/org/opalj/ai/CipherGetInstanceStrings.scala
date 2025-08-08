/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.ClassType
import org.opalj.br.MethodDescriptor.JustReturnsString
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.LoadString

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
object CipherGetInstanceStrings extends ProjectsAnalysisApplication {

    protected class CipherInstanceConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects parameter values of Cipher.getInstance calls"
    }

    protected type ConfigType = CipherInstanceConfig

    protected def createConfig(args: Array[String]): CipherInstanceConfig = new CipherInstanceConfig(args)

    // #################### CONSTANTS ####################

    val Cipher = ClassType("javax/crypto/Cipher")

    val Key = ClassType("java/security/Key")

    // #################### ANALYSIS ####################

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: CipherInstanceConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

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

                    case get @ GETFIELD(_, _, _) => println("uknown value: " + get)
                    case i                       => println("unsupported instruction: " + i)
                }
            }
        }

        (project, BasicReport(report.asScala.mkString("\n")))
    }
}
