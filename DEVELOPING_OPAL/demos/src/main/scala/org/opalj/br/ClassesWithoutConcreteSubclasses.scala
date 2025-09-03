/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Lists all abstract classes and interfaces that have no concrete subclasses in
 * the given set of jars.
 *
 * @author Michael Eichberg
 */
object ClassesWithoutConcreteSubclasses extends ProjectsAnalysisApplication {

    protected class ClassesWithoutSubclassesConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Finds abstract classes and interfaces that have no concrete subclass"
    }

    protected type ConfigType = ClassesWithoutSubclassesConfig

    protected def createConfig(args: Array[String]): ClassesWithoutSubclassesConfig =
        new ClassesWithoutSubclassesConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ClassesWithoutSubclassesConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
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
        (project, BasicReport(sortedAbstractTypes.mkString(header, "\n\t", "\n")))
    }
}
