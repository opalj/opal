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
 * Counts the number of private methods that have at least one parameter with
 * a reference type.
 *
 * @author Michael Eichberg
 */
object PrivateMethodsWithClassTypeParameterCounter extends ProjectsAnalysisApplication {

    protected class MethodsWithClassTypeParameterConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts (package) private methods with at least one parameter that is a class type"
    }

    protected type ConfigType = MethodsWithClassTypeParameterConfig

    protected def createConfig(args: Array[String]): MethodsWithClassTypeParameterConfig =
        new MethodsWithClassTypeParameterConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodsWithClassTypeParameterConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val overallPotential = new java.util.concurrent.atomic.AtomicInteger(0)
        val methods = (
            for {
                classFile <- project.allClassFiles.par
                method <- classFile.methods
                if method.isPrivate // || method.isPackagePrivate
                if method.name != "readObject" && method.name != "writeObject"
                potential = (method.descriptor.parameterTypes.collect {
                    case ct: ClassType => project.classHierarchy.allSubtypes(ct, false).size
                    case _             => 0
                }).sum
                if potential >= 5
            } yield {
                overallPotential.addAndGet(potential)
                method.toJava(s" /* Potential: $potential */ ")
            }
        ).seq

        (
            project,
            BasicReport(
                methods.mkString(
                    "Methods:\n\t",
                    "\n\t",
                    s"\n\t${methods.size} methods found with an overall refinement potential of ${overallPotential.get}.\n"
                )
            )
        )
    }
}
