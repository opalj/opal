/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * Lists all abstract classes and interfaces that have no concrete subclasses in
 * the given set of jars.
 *
 * @author Michael Eichberg
 */
object ClassesWithoutConcreteSubclasses extends ProjectAnalysisApplication {

    override def description: String =
        "Abstract classes and interfaces that have no concrete subclass in the given jars."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ) = {
        val classHierarchy = project.classHierarchy
        val abstractTypes =
            for {
                classFile <- project.allClassFiles.par
                if classFile.isAbstract
                thisType = classFile.thisType
                if classHierarchy.directSubtypesOf(thisType).isEmpty
            } yield {
                (
                    if (classFile.isInterfaceDeclaration)
                        "interface "
                    else
                        "abstract class "
                ) + thisType.toJava
            }

        val sortedAbstractTypes = abstractTypes.seq.toList.sorted
        val header = "Abstract types without concrete subclasses:\n\t"
        BasicReport(sortedAbstractTypes.mkString(header, "\n\t", "\n"))
    }
}
