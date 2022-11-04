/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import java.net.URL

import org.opalj.io.writeAndOpen
import org.opalj.graphs.toDot
import org.opalj.br.cfg
import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * Prints the CFG of a method using a data-flow independent analysis.
 *
 * @author Michael Eichberg
 */
object PrintBaseCFG {

    def main(args: Array[String]): Unit = {
        import Console.RED
        import Console.RESET

        if (args.length != 3) {
            println("You have to specify the method that should be analyzed.")
            println("\t1: a jar/jmod/class file or a directory containing jar/jmod/class files.")
            println("\t2: the name of a class.")
            println("\t3: the simple name or signature of a method of the class.")
            return ;
        }
        val fileName = args(0)
        val className = args(1)
        val methodName = args(2)

        val file = new java.io.File(fileName)
        if (!file.exists()) {
            println(RED+"[error] the file does not exist: "+fileName+"."+RESET)
            return ;
        }

        val project = try { Project(file) } catch {
            case e: Exception =>
                println(RED+"[error] cannot process file: "+e.getMessage+"."+RESET)
                return ;
        }

        val classFile = {
            val fqn =
                if (className.contains('.'))
                    className.replace('.', '/')
                else
                    className
            project.allClassFiles.find(_.fqn == fqn).getOrElse {
                println(RED+"[error] cannot find the class: "+className+"."+RESET)
                return ;
            }
        }

        val methodOption =
            if (methodName.contains("("))
                classFile.methods.find(m => m.descriptor.toJava(m.name).contains(methodName))
            else
                classFile.methods.find(_.name == methodName)

        val method = methodOption match {
            case Some(method) =>
                if (method.body.isDefined)
                    method
                else {
                    println(RED+"[error] the method: "+methodName+" does not have a body"+RESET)
                    return ;
                }
            case None =>
                val allMethods = classFile.methods.map(_.signatureToJava(false)).toSet
                val altMethods = allMethods.toSeq.sorted.mkString(" Candidates: ", ", ", ".")
                println(s"$RED[error] cannot find the method: $methodName.$RESET $altMethods")
                return ;
        }

        analyzeMethod(project, method)
    }

    def analyzeMethod(project: Project[URL], method: Method): Unit = {
        val classFile = method.classFile
        val code = method.body.get
        val theCFG = cfg.CFGFactory(code)

        val rootNodes = Set(theCFG.startBlock) ++ theCFG.catchNodes
        val graph = toDot(rootNodes)

        writeAndOpen(graph, classFile.thisType.toJava+"."+method.name, ".cfg.dot")

        println(code.cfJoins.mkString("JoinPCs (conservative):", ", ", ""))
        val (cfJoins, _, cfForks) = code.cfPCs
        val cfForksInfo = cfForks.map { e => val (k, v) = e; s"$k => ${v.mkString("{", ",", "}")}" }
        println(cfJoins.mkString("CFJoins               :", ", ", ""))
        println(cfForksInfo.mkString("CFForks               :", ", ", ""))

        val (predecessorPCs, exitPCs, _) = code.predecessorPCs(project.classHierarchy)
        println(predecessorPCs.zipWithIndex.map(_.swap).mkString("Predecessors:\n\t", "\n\t", "\n"))
        println(exitPCs.mkString("ExitPCs:", ",", "\n"))
        val liveVariables = code.liveVariables(predecessorPCs, exitPCs, cfJoins)
        val liveVariableInfo = liveVariables.
            zipWithIndex.map(_.swap).filter(_._2 ne null).
            map { e => val (pc, liveVariableInfo) = e; liveVariableInfo.mkString(s"$pc:{", ",", "}\n") }.
            mkString("LiveVariables:\n\t", "\t", "")
        println(liveVariableInfo)
    }
}
