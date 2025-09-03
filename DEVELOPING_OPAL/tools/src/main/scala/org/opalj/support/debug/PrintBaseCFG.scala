/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package debug

import org.opalj.br.Method
import org.opalj.br.analyses.MethodAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg
import org.opalj.graphs.toDot
import org.opalj.io.writeAndOpen

/**
 * Prints the CFG of a method using a data-flow independent analysis.
 *
 * @author Michael Eichberg
 */
object PrintBaseCFG extends MethodAnalysisApplication {

    protected class BaseCFGConfig(args: Array[String]) extends MethodAnalysisConfig(args) {
        val description = "Prints the CFG of a method using a data-flow independent analysis"
    }

    protected type ConfigType = BaseCFGConfig

    override type Result = String

    protected def createConfig(args: Array[String]): BaseCFGConfig = new BaseCFGConfig(args)

    override def analyzeMethod(p: SomeProject, m: Method, analysisConfig: BaseCFGConfig): String = {

        val classFile = m.classFile
        val code = m.body.get
        val theCFG = cfg.CFGFactory(code)

        val rootNodes = Set(theCFG.startBlock) ++ theCFG.catchNodes
        val graph = toDot(rootNodes)

        writeAndOpen(graph, classFile.thisType.toJava + "." + m.name, ".cfg.dot")

        val result = new StringBuilder()

        result.append(code.cfJoins.mkString("JoinPCs (conservative):", ", ", "\n"))
        val (cfJoins, _, cfForks) = code.cfPCs
        val cfForksInfo = cfForks.map { e =>
            val (k, v) = e; s"$k => ${v.mkString("{", ",", "}")}"
        }
        result.append(cfJoins.mkString("CFJoins               :", ", ", "\n"))
        result.append(cfForksInfo.mkString("CFForks               :", ", ", "\n"))

        val (predecessorPCs, exitPCs, _) = code.predecessorPCs(p.classHierarchy)
        result.append(predecessorPCs.zipWithIndex.map(_.swap).mkString("Predecessors:\n\t", "\n\t", "\n\n"))
        result.append(exitPCs.mkString("ExitPCs:", ",", "\n\n"))
        val liveVariables = code.liveVariables(predecessorPCs, exitPCs, cfJoins)
        val liveVariableInfo = liveVariables.zipWithIndex.map(_.swap).filter(_._2 ne null).map { e =>
            val (pc, liveVariableInfo) = e; liveVariableInfo.mkString(s"$pc:{", ",", "}\n")
        }.mkString("LiveVariables:\n\t", "\t", "")
        result.append(liveVariableInfo)
        result.append("\n\n")

        result.toString()
    }

    override def renderResult(result: String): String = result
}
