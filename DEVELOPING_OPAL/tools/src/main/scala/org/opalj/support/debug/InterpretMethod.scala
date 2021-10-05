/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import java.util.Date
import java.net.URL

import org.opalj.io.writeAndOpen
import org.opalj.graphs.toDot

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.AI
import org.opalj.ai.Domain
import org.opalj.ai.MultiTracer
import org.opalj.ai.BaseAI
import org.opalj.ai.util
import org.opalj.ai.AITracer
import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.ai.domain.TheCode
import org.opalj.ai.util.XHTML
import org.opalj.ai.common.XHTML.dump
import org.opalj.ai.common.XHTML.dumpAIState
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.RecordDefUse

/**
 * A small basic framework that facilitates the abstract interpretation of a
 * specific method using a configurable domain.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    private class IMAI(IdentifyDeadVariables: Boolean) extends AI[Domain](IdentifyDeadVariables) {

        override def isInterrupted: Boolean = Thread.interrupted()

        val consoleTracer: AITracer = new ConsoleTracer { override val printOIDs = true }
        //new ConsoleEvaluationTracer {}

        val xHTMLTracer: XHTMLTracer = new XHTMLTracer {}

        override val tracer = Some(new MultiTracer(consoleTracer, xHTMLTracer))
    }

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the name of a class and the third must denote the name of a method
     *      of the respective class. If the method is overloaded the first method
     *      is returned.
     */
    def main(args: Array[String]): Unit = {
        import Console.RED
        import Console.RESET
        import language.existentials

        def printUsage(issue: Option[String]): Unit = {
            issue.foreach(issue => println(s"Failure: $issue."))
            println("You have to specify the following parameters.")
            println("\t1: a jar/class file or a directory containing jar/class files.")
            println("\t2: the name of a class.")
            println("\t3: the simple name or signature of a method of the specified class.")
            println("\t4[Optional]: -domain=CLASS the name of the class of the configurable domain to use.")
            println("\t5[Optional]: -trace={true,false} default:true")
            println("\t6[Optional]: -identifyDeadVariables={true,false} default:true")
        }

        if (args.length < 3 || args.length > 5) {
            printUsage(Some("wrong number of parameters"))
            return ;
        }
        var remainingArgs = args.toList
        val fileName = remainingArgs.head; remainingArgs = remainingArgs.tail
        val className = remainingArgs.head; remainingArgs = remainingArgs.tail
        val methodName = remainingArgs.head; remainingArgs = remainingArgs.tail
        val domainClass = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-domain=")) {
                val domainRawClass = Class.forName(remainingArgs.head.substring(8))
                val clazz = domainRawClass.asInstanceOf[Class[_ <: Domain]]
                remainingArgs = remainingArgs.tail
                clazz
            } else // default domain
                classOf[BaseDomain[java.net.URL]]
        }
        val doTrace = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-trace=")) {
                val result = remainingArgs.head == "-trace=true" || remainingArgs.head == "-trace=1"
                remainingArgs = remainingArgs.tail
                result
            } else
                true
        }
        val doIdentifyDeadVariables = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-identifyDeadVariables=")) {
                val result = (
                    remainingArgs.head == "-identifyDeadVariables=true" ||
                    remainingArgs.head == "-identifyDeadVariables=1"
                )
                remainingArgs = remainingArgs.tail
                result
            } else
                true
        }
        if (remainingArgs.nonEmpty) {
            printUsage(Some(remainingArgs.mkString("unexpected arguments: ", ", ", "")))
            return ;
        }

        def createDomain[Source: reflect.ClassTag](project: SomeProject, method: Method): Domain = {

            scala.util.control.Exception.ignoring(classOf[NoSuchMethodException]) {
                val constructor = domainClass.getConstructor(classOf[Object])
                return constructor.newInstance(method.classFile);
            }

            val constructor = domainClass.getConstructor(classOf[Project[URL]], classOf[Method])
            constructor.newInstance(project, method)
        }

        val file = new java.io.File(fileName)
        if (!file.exists()) {
            println(RED+"[error] The file does not exist: "+fileName+"."+RESET)
            return ;
        }

        val project =
            try {
                Project(file)
            } catch {
                case e: Exception =>
                    println(RED+"[error] Cannot process file: "+e.getMessage+"."+RESET)
                    return ;
            }

        val classFile = {
            val fqn = className.replace('.', '/')
            project.allClassFiles.find(_.fqn == fqn).getOrElse {
                println(RED+"[error] Cannot find the class: "+className+"."+RESET)
                return ;
            }
        }

        val method =
            (
                if (methodName.contains("("))
                    classFile.methods.find(m => m.descriptor.toJava(m.name).contains(methodName))
                else
                    classFile.methods.find(_.name == methodName)
            ) match {
                    case Some(method) =>
                        if (method.body.isDefined)
                            method
                        else {
                            println(RED+
                                "[error] The method: "+methodName+" does not have a body"+RESET)
                            return ;
                        }
                    case None =>
                        println(RED+
                            "[error] Cannot find the method: "+methodName+"."+RESET +
                            classFile.methods.map(m => m.descriptor.toJava(m.name)).toSet.
                            toSeq.sorted.mkString(" Candidates: ", ", ", "."))
                        return ;
                }

        var ai: AI[Domain] = null;
        try {
            val result =
                if (doTrace) {
                    ai = new IMAI(doIdentifyDeadVariables)
                    ai(method, createDomain(project, method))
                } else {
                    val body = method.body.get
                    println("Starting abstract interpretation of: ")
                    println("\t"+classFile.thisType.toJava+"{")
                    println("\t\t"+method.signatureToJava(true)+
                        "[instructions="+body.instructions.size+
                        "; #max_stack="+body.maxStack+
                        "; #locals="+body.maxLocals+"]")
                    println("\t}")
                    ai = new BaseAI(doIdentifyDeadVariables)
                    val result = ai(method, createDomain(project, method))
                    println("Finished abstract interpretation.")
                    result
                }

            if (result.domain.isInstanceOf[RecordCFG]) {
                val evaluatedInstructions = result.evaluatedInstructions
                val cfgDomain = result.domain.asInstanceOf[RecordCFG]

                val cfgAsDotGraph = toDot(Set(cfgDomain.cfgAsGraph()), ranksep = "0.3").toString
                val cfgFile = writeAndOpen(cfgAsDotGraph, "AICFG", ".gv")
                println("AI CFG: "+cfgFile)

                val domAsDot = cfgDomain.dominatorTree.toDot(evaluatedInstructions.contains)
                val domFile = writeAndOpen(domAsDot, "DominatorTreeOfTheAICFG", ".gv")
                println("AI CFG - Dominator tree: "+domFile)

                val postDomAsDot = cfgDomain.postDominatorTree.toDot(evaluatedInstructions.contains)
                val postDomFile = writeAndOpen(postDomAsDot, "PostDominatorTreeOfTheAICFG", ".gv")
                println("AI CFG - Post-Dominator tree: "+postDomFile)

                val cdg = cfgDomain.pdtBasedControlDependencies
                val rdfAsDotGraph = cdg.toDot(evaluatedInstructions.contains)
                val rdfFile = writeAndOpen(rdfAsDotGraph, "ReverseDominanceFrontiersOfAICFG", ".gv")
                println("AI CFG - Reverse Dominance Frontiers: "+rdfFile)
            }

            if (result.domain.isInstanceOf[RecordDefUse]) {
                val duInfo = result.domain.asInstanceOf[RecordDefUse]
                writeAndOpen(duInfo.dumpDefUseInfo(), "DefUseInfo", ".html")

                val dotGraph = toDot(duInfo.createDefUseGraph(method.body.get)).toString()
                writeAndOpen(dotGraph, "ImplicitDefUseGraph", ".gv")
            }

            writeAndOpen(
                dump(
                    Some(classFile),
                    Some(method),
                    method.body.get,
                    Some(
                        s"Analyzed: ${new Date}<br>Domain: ${domainClass.getName}<br>"+
                            (
                                if (doIdentifyDeadVariables)
                                    "<b>Dead Variables Identification</b><br>"
                                else
                                    "<u>Dead Variables are not filtered</u>.<br>"
                            ) +
                                XHTML.instructionsToXHTML("PCs where paths join", result.cfJoins) +
                                (
                                    if (result.subroutinePCs.nonEmpty) {
                                        XHTML.instructionsToXHTML(
                                            "Subroutine instructions", result.subroutinePCs
                                        )
                                    } else {
                                        ""
                                    }
                                ) + XHTML.evaluatedInstructionsToXHTML(result.evaluatedPCs)
                    ),
                    result.domain
                )(result.cfJoins, result.operandsArray, result.localsArray),
                "AIResult",
                ".html"
            )
        } catch {
            case ife: InterpretationFailedException =>

                ai match {
                    case ai: IMAI => ai.xHTMLTracer.result(null);
                    case _        => /*nothing to do*/
                }

                def causeToString(ife: InterpretationFailedException, nested: Boolean): String = {
                    val domain = ife.domain
                    val parameters =
                        if (nested) {
                            ife.localsArray(0).toSeq.reverse.
                                filter(_ != null).map(_.toString).
                                mkString("Parameters:<i>", ", ", "</i><br>")
                        } else
                            ""

                    val aiState =
                        s"<p><i>${domain.getClass.getName}</i><b>( ${domain.toString} )</b></p>"+
                            parameters+
                            "Current instruction: "+ife.pc+"<br>"+
                            XHTML.evaluatedInstructionsToXHTML(ife.evaluatedPCs) + {
                                if (ife.worklist.nonEmpty)
                                    ife.worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                                else
                                    "Remaining worklist: <i>EMPTY</i><br>"
                            }

                    val metaInformation = ife.cause match {
                        case ife: InterpretationFailedException =>
                            aiState + ife.cause.
                                getStackTrace.
                                mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n")+
                                "<div style='margin-left:5em'>"+causeToString(ife, true)+"</div>"
                        case e: Throwable =>
                            val message = e.getMessage()
                            if (message != null)
                                aiState+"<br>Underlying cause: "+util.XHTML.htmlify(message)
                            else
                                aiState+"<br>Underlying cause: <NULL>"
                        case _ =>
                            aiState
                    }

                    if (nested && ife.domain.isInstanceOf[TheCode])
                        metaInformation +
                            dumpAIState(
                                ife.domain.asInstanceOf[TheCode].code,
                                ife.domain
                            )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                    else
                        metaInformation
                }

                val resultHeader = Some(causeToString(ife, false))
                val evaluationDump =
                    dump(
                        Some(classFile), Some(method), method.body.get,
                        resultHeader, ife.domain
                    )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                writeAndOpen(evaluationDump, "StateOfCrashedAbstractInterpretation", ".html")
                throw ife

        }
    }
}
