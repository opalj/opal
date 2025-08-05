/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package debug

import scala.language.postfixOps

import java.net.URL
import java.util.Date

import org.rogach.scallop.flagConverter

import org.opalj.ai.AI
import org.opalj.ai.AIResult
import org.opalj.ai.AITracer
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.MultiTracer
import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.ai.cli.DomainArg
import org.opalj.ai.cli.TraceArg
import org.opalj.ai.common.XHTML.dump
import org.opalj.ai.common.XHTML.dumpAIState
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.TheCode
import org.opalj.ai.util
import org.opalj.ai.util.XHTML
import org.opalj.br.Method
import org.opalj.br.analyses.MethodAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.cli.PlainArg
import org.opalj.graphs.toDot
import org.opalj.io.writeAndOpen

/**
 * A small basic framework that facilitates the abstract interpretation of a
 * specific method using a configurable domain.
 *
 * @author Michael Eichberg
 */
object InterpretMethods extends MethodAnalysisApplication {

    protected class InterpretMethodsConfig(args: Array[String]) extends MethodAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Performs an abstract interpretation of specified methods"

        private val deadVarsArg = new PlainArg[Boolean] {
            override val name: String = "identifyDeadVariables"
            override val description: String = "Identify dead variables during abstract interpretation"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        args(
            TraceArg !,
            deadVarsArg !
        )
        init()

        val identifyDeadVars: Boolean = apply(deadVarsArg)
    }

    protected type ConfigType = InterpretMethodsConfig

    override type Result = String

    protected def createConfig(args: Array[String]): InterpretMethodsConfig = new InterpretMethodsConfig(args)

    def analyzeMethod(p: SomeProject, m: Method, analysisConfig: ConfigType): Result = {
        val classFile = m.classFile
        val domainClass = analysisConfig(DomainArg)

        def createDomain[Source: reflect.ClassTag](project: SomeProject, method: Method): Domain = {

            scala.util.control.Exception.ignoring(classOf[NoSuchMethodException]) {
                val constructor = domainClass.getConstructor(classOf[Object])
                return constructor.newInstance(method.classFile);
            }

            val constructor = domainClass.getConstructor(classOf[Project[URL]], classOf[Method])
            constructor.newInstance(project, method)
        }

        var ai: AI[Domain] = null;
        try {
            val result = new StringBuilder()

            val aiResult: AIResult =
                if (analysisConfig(TraceArg)) {
                    ai = new IMAI(analysisConfig.identifyDeadVars)
                    ai(m, createDomain(p, m))
                } else {
                    val body = m.body.get
                    result.append("Starting abstract interpretation of: \n")
                    result.append("\t" + classFile.thisType.toJava + "{\n")
                    result.append("\t\t" + m.signatureToJava(true) +
                        "[instructions=" + body.instructions.length +
                        "; #max_stack=" + body.maxStack +
                        "; #locals=" + body.maxLocals + "]\n")
                    result.append("\t}\n")
                    ai = new BaseAI(analysisConfig.identifyDeadVars)
                    val aiResult = ai(m, createDomain(p, m))
                    result.append("Finished abstract interpretation.\n")
                    aiResult
                }

            if (aiResult.domain.isInstanceOf[RecordCFG]) {
                val evaluatedInstructions = aiResult.evaluatedInstructions
                val cfgDomain = aiResult.domain.asInstanceOf[RecordCFG]

                val cfgAsDotGraph = toDot(Set(cfgDomain.cfgAsGraph()), ranksep = "0.3").toString
                val cfgFile = writeAndOpen(cfgAsDotGraph, "AICFG", ".gv")
                result.append("AI CFG: " + cfgFile + "\n")

                val domAsDot = cfgDomain.dominatorTree.toDot(evaluatedInstructions.contains)
                val domFile = writeAndOpen(domAsDot, "DominatorTreeOfTheAICFG", ".gv")
                result.append("AI CFG - Dominator tree: " + domFile + "\n")

                val postDomAsDot = cfgDomain.postDominatorTree.toDot(evaluatedInstructions.contains)
                val postDomFile = writeAndOpen(postDomAsDot, "PostDominatorTreeOfTheAICFG", ".gv")
                result.append("AI CFG - Post-Dominator tree: " + postDomFile + "\n")

                val cdg = cfgDomain.pdtBasedControlDependencies
                val rdfAsDotGraph = cdg.toDot(evaluatedInstructions.contains)
                val rdfFile = writeAndOpen(rdfAsDotGraph, "ReverseDominanceFrontiersOfAICFG", ".gv")
                result.append("AI CFG - Reverse Dominance Frontiers: " + rdfFile + "\n")
            }

            if (aiResult.domain.isInstanceOf[RecordDefUse]) {
                val duInfo = aiResult.domain.asInstanceOf[RecordDefUse]
                writeAndOpen(duInfo.dumpDefUseInfo(), "DefUseInfo", ".html")

                val dotGraph = toDot(duInfo.createDefUseGraph(m.body.get))
                writeAndOpen(dotGraph, "ImplicitDefUseGraph", ".gv")
            }

            writeAndOpen(
                dump(
                    Some(classFile),
                    Some(m),
                    m.body.get,
                    Some(
                        s"Analyzed: ${new Date}<br>Domain: ${domainClass.getName}<br>" +
                            (
                                if (analysisConfig.identifyDeadVars)
                                    "<b>Dead Variables Identification</b><br>"
                                else
                                    "<u>Dead Variables are not filtered</u>.<br>"
                            ) +
                            XHTML.instructionsToXHTML("PCs where paths join", aiResult.cfJoins) +
                            (
                                if (aiResult.subroutinePCs.nonEmpty) {
                                    XHTML.instructionsToXHTML("Subroutine instructions", aiResult.subroutinePCs)
                                } else {
                                    ""
                                }
                            ) + XHTML.evaluatedInstructionsToXHTML(aiResult.evaluatedPCs)
                    ),
                    aiResult.domain
                )(aiResult.cfJoins, aiResult.operandsArray, aiResult.localsArray),
                "AIResult",
                ".html"
            )

            result.toString()
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
                            ife.localsArray(0).toSeq.reverse
                                .filter(_ != null).map(_.toString)
                                .mkString("Parameters:<i>", ", ", "</i><br>")
                        } else
                            ""

                    val aiState =
                        s"<p><i>${domain.getClass.getName}</i><b>( ${domain.toString} )</b></p>" +
                            parameters +
                            "Current instruction: " + ife.pc + "<br>" +
                            XHTML.evaluatedInstructionsToXHTML(ife.evaluatedPCs) + {
                                if (ife.worklist.nonEmpty)
                                    ife.worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                                else
                                    "Remaining worklist: <i>EMPTY</i><br>"
                            }

                    val metaInformation = ife.cause match {
                        case ife: InterpretationFailedException =>
                            aiState + ife.cause.getStackTrace.mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n") +
                                "<div style='margin-left:5em'>" + causeToString(ife, true) + "</div>"
                        case e: Throwable =>
                            val message = e.getMessage()
                            if (message != null)
                                aiState + "<br>Underlying cause: " + util.XHTML.htmlify(message)
                            else
                                aiState + "<br>Underlying cause: <NULL>"
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
                        Some(classFile),
                        Some(m),
                        m.body.get,
                        resultHeader,
                        ife.domain
                    )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                writeAndOpen(evaluationDump, "StateOfCrashedAbstractInterpretation", ".html")
                throw ife

        }
    }

    private class IMAI(IdentifyDeadVariables: Boolean) extends AI[Domain](IdentifyDeadVariables) {

        override def isInterrupted: Boolean = Thread.interrupted()

        val consoleTracer: AITracer = new ConsoleTracer { override val printOIDs = true }
        // new ConsoleEvaluationTracer {}

        val xHTMLTracer: XHTMLTracer = new XHTMLTracer {}

        override val tracer = Some(new MultiTracer(consoleTracer, xHTMLTracer))
    }

    override def renderResult(result: String): String = result
}
