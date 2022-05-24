/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import java.net.URL
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.ai.common.SimpleAIKey
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport

/**
 * Creates for all methods of a given project the Control-flow Graph and the DefUse Graph.
 *
 * @author Michael Eichberg
 */
object TACAItoGraphs extends ProjectAnalysisApplication {

    override def title: String = "CFG and Def/Use Creator"

    override def description: String = {
        "Creates for all methods of a given project the Control-flow Graph and the DefUse Graph."
    }

    override def analysisSpecificParametersDescription: String = {
        "-o=<the folder to which the generated graphs are written>"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        if (parameters.size == 1 && parameters.head.startsWith("-o="))
            Seq.empty
        else if (parameters.isEmpty)
            Seq("output folder is missing")
        else
            parameters.filterNot(_.startsWith("-o=")).map("unknown parameter: "+_)
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val methodCount = new AtomicInteger(0)

        val folder = new File(parameters.head.substring(3))
        folder.mkdirs()
        val pathName = s"${folder.getAbsoluteFile}${File.separator}"

        val aiResults = theProject.get(SimpleAIKey)
        val tacs = theProject.get(LazyTACUsingAIKey)

        theProject.parForeachMethodWithBody() { mi =>
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
                val outputDUGFile = new File(outputFileName+".dug.csv").toPath
                def successors(i: Int) = idToNodeMap(i).children.map(nodeToIdMap).toSet
                val g = org.opalj.graphs.toAdjacencyMatrix(maxNodeId, successors)
                println(s"writing graph to: "+outputDUGFile)
                Files.write(outputDUGFile, g)
            }

            // 2. create the CFGs
            {
                val nodeToIdMap = tacs(m).cfg.allNodes.zipWithIndex.toMap
                val idToNodeMap = nodeToIdMap.iterator.map(_.swap).toMap
                val maxNodeId = nodeToIdMap.size - 1
                val outputCFGFile = new File(outputFileName+".cfg.csv").toPath
                def successors(i: Int) = idToNodeMap(i).successors.map(nodeToIdMap)
                val g = org.opalj.graphs.toAdjacencyMatrix(maxNodeId, successors)
                println(s"writing graph to: "+outputCFGFile)
                Files.write(outputCFGFile, g)
            }

            methodCount.incrementAndGet()
        }

        BasicReport(s"Created ${methodCount.get} def/use and control-flow graphs in: $folder.")
    }
}
