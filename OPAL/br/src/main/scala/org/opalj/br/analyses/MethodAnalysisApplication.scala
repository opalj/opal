/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.PartialSignatureArg
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info

/**
 * A small framework to implement analyses which should be executed for a given
 * set of methods.
 */
abstract class MethodAnalysisApplication extends ProjectsAnalysisApplication {

    abstract protected class MethodAnalysisConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        args(PartialSignatureArg !)
    }

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val result = new StringBuilder()

        // Find the class(es) that we want to analyze.
        // (Left as an exercise: error handling...)
        for { (className, methodName, signature) <- analysisConfig(PartialSignatureArg) } {
            implicit val logContext: LogContext = project.logContext

            val methodSignature = methodName + signature
            info("progress", s"trying to find: $className{ $methodSignature }")

            project.classFile(ClassType(className)) match {
                case Some(cf) =>
                    cf.methods.find(_.signatureToJava(false).contains(methodSignature)) match {
                        case Some(m) =>
                            info("progress", s"analyzing: ${m.toJava}")
                            // Run analysis
                            result.append(renderResult(analyzeMethod(project, m, analysisConfig)) + "\n")
                        case None => result.append(s"Method $className.$methodSignature could not be found!\n")
                    }
                case None => result.append(s"Class $className could not be found!\n")
            }
        }

        (project, BasicReport(result.toString()))
    }

    type Result

    def analyzeMethod(p: SomeProject, m: Method, analysisConfig: ConfigType): Result

    def renderResult(r: Result): String

}
