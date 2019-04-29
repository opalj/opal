/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.implicitConversions

import java.net.URL

import org.opalj.log.OPALLogger.info

/**
 * Default implementation of the [[AnalysisApplication]] which facilitates the
 * development of analyses which are executed in one step.
 *
 * @author Michael Eichberg
 */
abstract class ProjectAnalysisApplication
    extends AnalysisApplication
    with OneStepAnalysis[URL, ReportableAnalysisResult] {

    implicit def String2BasicReport(report: String): BasicReport = BasicReport(report)

    final override val analysis: ProjectAnalysisApplication = this

}

/**
 * A small framework to implement analyses which should be executed for a given
 * set of methods.
 */
abstract class MethodAnalysisApplication extends ProjectAnalysisApplication {

    override def analysisSpecificParametersDescription: String = {
        "-class=<fully qualified name of the class>\n"+
            "-method=<name and/or parts of the signature>"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        if (parameters.size != 2 || parameters(0).substring(0, 7) == parameters(1).substring(0, 7))
            return List("missing parameters");

        parameters.foldLeft(List.empty[String]) { (notUnderstood, p) ⇒
            if (!p.startsWith("-class=") && !p.startsWith("-method="))
                p :: notUnderstood
            else
                notUnderstood
        }
    }

    override def doAnalyze(
        p:             Project[URL],
        params:        Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext = p.logContext

        // Find the class that we want to analyze.
        // (Left as an exercise: error handling...)
        val className = params.find(_.startsWith("-class=")).get.substring(7).replace('.', '/')
        val methodSignature = params.find(_.startsWith("-method=")).get.substring(8)
        info("progress", s"trying to find: $className{ $methodSignature }")

        val cf = p.classFile(ObjectType(className)) match {
            case Some(cf) ⇒ cf
            case None     ⇒ return s"Class $className could not be found!";
        }
        val m = cf.methods.find(_.signatureToJava(false).contains(methodSignature)) match {
            case Some(m) ⇒ m
            case None    ⇒ return s"Method $methodSignature could not be found!";
        }
        info("progress", s"analyzing: ${m.toJava}")

        // Run analysis
        renderResult(analyzeMethod(p, m))
    }

    type Result

    def analyzeMethod(p: Project[URL], m: Method): Result

    def renderResult(r: Result): String

}
