/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.language.postfixOps

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.util.AIBasedCommandLineConfig
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.bytecode.JDKArg
import org.opalj.cli.MultiProjectsArg
import org.opalj.cli.OutputDirArg

/**
 * Creates for all methods of a given project the Control-flow Graph and the DefUse Graph.
 *
 * @author Michael Eichberg
 */
object TACAItoGraphs extends ProjectsAnalysisApplication {

    protected class TACAIConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Creates for all methods of a given project the Control-flow Graph and the DefUse Graph"

        args(OutputDirArg !)
    }

    protected type ConfigType = TACAIConfig

    protected def createConfig(args: Array[String]): TACAIConfig = new TACAIConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val methodCount = new AtomicInteger(0)

        val folder = if (analysisConfig(MultiProjectsArg))
            new File(analysisConfig(OutputDirArg), if (analysisConfig(JDKArg).isDefined) "JDK" else cp.head.getName)
        else analysisConfig(OutputDirArg)
        val pathName = s"${folder.getAbsoluteFile}${File.separator}"

        val aiResults = project.get(SimpleAIKey)
        val tacs = project.get(LazyTACUsingAIKey)

        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            val methodName = m.toJava
            val outputFileName = pathName + org.opalj.io.sanitizeFileName(methodName)

            // 1. create the def/use graphs
            {
                val defUseGraph = aiResults(m).domain.createDefUseGraph(m.body.get)
                // let's remap the identifiers to natural numbers (0...)
                val nodeToId = defUseGraph.zipWithIndex
                val nodeToIdMap = nodeToId.toMap
                val idToNodeMap = nodeToId.map(_.swap).toMap
                val maxNodeId = nodeToIdMap.size - 1
                val outputDUGFile = new File(outputFileName + ".dug.csv").toPath
                def successors(i: Int) = idToNodeMap(i).children.map(nodeToIdMap).toSet
                val g = org.opalj.graphs.toAdjacencyMatrix(maxNodeId, successors)
                println(s"writing graph to: " + outputDUGFile)
                Files.write(outputDUGFile, g)
            }

            // 2. create the CFGs
            {
                val nodeToIdMap = tacs(m).cfg.allNodes.zipWithIndex.toMap
                val idToNodeMap = nodeToIdMap.iterator.map(_.swap).toMap
                val maxNodeId = nodeToIdMap.size - 1
                val outputCFGFile = new File(outputFileName + ".cfg.csv").toPath
                def successors(i: Int) = idToNodeMap(i).successors.map(nodeToIdMap)
                val g = org.opalj.graphs.toAdjacencyMatrix(maxNodeId, successors)
                println(s"writing graph to: " + outputCFGFile)
                Files.write(outputCFGFile, g)
            }

            methodCount.incrementAndGet()
        }

        (project, BasicReport(s"Created ${methodCount.get} def/use and control-flow graphs in: $folder."))
    }
}
