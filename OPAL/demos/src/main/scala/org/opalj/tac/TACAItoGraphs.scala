/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.tac

import java.net.URL
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.ai.common.SimpleAIKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport

/**
 * Creates for all methods of a given project the Control-flow Graph and the DefUse Graph.
 *
 * @author Michael Eichberg
 */
object TACAItoGraphs extends DefaultOneStepAnalysis {

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
        else
            parameters.filterNot(_.startsWith("-o=")).map("unknown parameter: "+_)
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val methodCount = new AtomicInteger(0)

        val folder = new File(parameters.head.substring(3))
        folder.mkdirs()
        val pathName = folder.getAbsoluteFile + File.separator

        val aiResults = theProject.get(SimpleAIKey)
        val tacs = theProject.get(DefaultTACAIKey)

        val errors = theProject.parForeachMethodWithBody() { mi ⇒
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
        if (errors.nonEmpty) errors.foreach(e ⇒ e.printStackTrace)

        BasicReport(s"Created ${methodCount.get} def/use and control-flow graphs in: $folder.")
    }
}
