/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import java.io.File

import org.opalj.br._
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.util.PerformanceEvaluation.time

/**
 * Demonstrates how to create a dependency matrix.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
object DependencyMatrix extends ProjectsAnalysisApplication {

    protected class DependencyMatrixConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Creates a dependency matrix of the classes of a project"
    }

    protected type ConfigType = DependencyMatrixConfig

    protected def createConfig(args: Array[String]): DependencyMatrixConfig = new DependencyMatrixConfig(args)

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: DependencyMatrixConfig,
        execution:      Int
    ): Unit = {

        val (project, _) = analysisConfig.setupProject(cp)

        import scala.collection.mutable.Map
        import scala.collection.mutable.Set
        val dependencyMatrix = Map[VirtualSourceElement, Set[(VirtualSourceElement, DependencyType)]]()
        val dependencyExtractor =
            new DependencyExtractor(
                new DependencyProcessorAdapter {
                    override def processDependency(
                        source: VirtualSourceElement,
                        target: VirtualSourceElement,
                        dType:  DependencyType
                    ): Unit = {
                        dependencyMatrix.get(source) match {
                            case Some(s) => s += ((target, dType))
                            case None    => dependencyMatrix += (source -> Set((target, dType)))
                        }
                        // [Scala 2.9.X Compiler crashes on:] dependencyMatrix.getOrElseUpdate(sourceID, emptySet)  + ((targetID, dType))
                    }
                }
            )

        var count = 0
        time {
            for {
                classFile <- project.allProjectClassFiles
            } {
                count += 1
                dependencyExtractor.process(classFile)
            }
        } { t => println(s"\nReading all $count class files and building the dependency matrix took ${t.toSeconds}") }
    }
}
