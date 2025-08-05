/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package debug

import java.io.File

import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.graphs.toDot
import org.opalj.io.writeAndOpen

/**
 * Creates a `dot` (Graphviz) based representation of the class hierarchy.
 *
 * @author Michael Eichberg
 */
object ClassHierarchyVisualizer extends ProjectsAnalysisApplication {

    protected class ClassHierarchyVisualizerConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Creates a `dot` (Graphviz) based representation of the class hierarchy"
    }

    protected type ConfigType = ClassHierarchyVisualizerConfig

    protected def createConfig(args: Array[String]): ClassHierarchyVisualizerConfig =
        new ClassHierarchyVisualizerConfig(args)

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: ClassHierarchyVisualizerConfig,
        execution:      Int
    ): Unit = {

        val (project, _) = analysisConfig.setupProject(cp)

        val dotGraph = toDot(Set(project.classHierarchy.toGraph()), "back")
        val file = writeAndOpen(dotGraph, "ClassHierarchy", ".gv")
        println(s"Wrote class hierarchy graph to: $file.")
    }
}
