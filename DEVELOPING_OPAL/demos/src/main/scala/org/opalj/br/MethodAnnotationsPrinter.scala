/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import analyses.AnalysisApplication
import analyses.BasicReport

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Prints out the method-level annotations of all methods. (I.e., class, field and
 * parameter annotations are not printed.)
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object MethodAnnotationsPrinter extends AnalysisApplication {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def title: String = "Collect Method-level Annotations"

        override def description: String = "Prints out the annotations of methods."

        def doAnalyze(project: Project[URL], params: Seq[String], isInterrupted: () => Boolean) = {
            val annotations =
                for {
                    classFile <- project.allClassFiles.par
                    method <- classFile.methods
                    annotation <- method.runtimeVisibleAnnotations ++ method.runtimeInvisibleAnnotations
                } yield {
                    method.toJava +
                        annotation.elementValuePairs.
                        map { pair => "%-15s: %s".format(pair.name, pair.value.toJava) }.
                        mkString(s"\n\t@${annotation.annotationType.toJava}\n\t", "\n\t", "\n")
                }

            BasicReport(
                annotations.mkString(s"${annotations.size} annotations found:\n", "\n", "\n")
            )
        }
    }
}
