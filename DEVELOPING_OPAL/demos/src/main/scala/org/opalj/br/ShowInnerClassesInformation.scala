/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Shows the inner classes attributes of given class files.
 *
 * @author Daniel Klauer
 * @author Michael Eichberg
 */
object ShowInnerClassesInformation extends ProjectAnalysisApplication {

    override def description: String = "Prints out the inner classes tables."

    def doAnalyze(p: Project[URL], params: Seq[String], isInterrupted: () => Boolean): BasicReport = {

        val messages =
            for {
                classFile <- p.allClassFiles.par
                if classFile.innerClasses.isDefined
            } yield {
                val header =
                    classFile.fqn+"(ver:"+classFile.majorVersion+")"+":\n\t"+(
                        classFile.enclosingMethod.
                        map(_.toString).
                        getOrElse("<no enclosing method defined>")
                    )+"\n\t"
                classFile.innerClasses.get.mkString(header, "\n\t", "\n")
            }

        BasicReport(messages.mkString("\n"))
    }

}
