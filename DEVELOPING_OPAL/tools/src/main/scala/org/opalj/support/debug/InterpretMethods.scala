/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package debug

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import scala.util.control.ControlThrowable
import scala.xml.NodeSeq

import org.opalj.ai.Domain
import org.opalj.ai.InstructionCountBoundedAI
import org.opalj.ai.domain.DomainArg
import org.opalj.ai.util.AIBasedCommandLineConfig
import org.opalj.ai.util.XHTML
import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.DebugArg
import org.opalj.io.writeAndOpen
import org.opalj.log.LogContext

/**
 * Performs an abstract interpretation of all methods of the given class file(s) using
 * a configurable domain.
 *
 * This class is meant to support the development and testing of new domains.
 *
 * @author Michael Eichberg
 */
object InterpretMethods extends ProjectsAnalysisApplication {

    protected class MethodAnnotationsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Performs an abstract interpretation of all methods"

        args(DebugArg)
    }

    protected type ConfigType = MethodAnnotationsConfig

    protected def createConfig(args: Array[String]): MethodAnnotationsConfig = new MethodAnnotationsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodAnnotationsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val verbose = analysisConfig(DebugArg)

        val (message, detailedErrorInformationFile) =
            InterpretMethodsAnalysis.interpret(
                project,
                analysisConfig(DomainArg),
                verbose,
                6d
            )(project.logContext)

        (
            project,
            BasicReport(
                message + detailedErrorInformationFile.map(" (See " + _ + " for details.)").getOrElse("")
            )
        )
    }
}

object InterpretMethodsAnalysis {

    def println(s: String): Unit = {
        Console.out.println(s)
        Console.flush()
    }

    def interpret[Source](
        project:             Project[Source],
        domainClass:         Class[_ <: Domain],
        beVerbose:           Boolean,
        maxEvaluationFactor: Double = 3d
    )(
        implicit logContext: LogContext
    ): (String, Option[File]) = {

        // TODO Add support for reporting the progress and to interrupt the analysis.

        import Console.GREEN
        import Console.RED
        import Console.RESET
        import Console.YELLOW

        val performanceEvaluationContext = new org.opalj.util.PerformanceEvaluation
        import performanceEvaluationContext.getTime
        import performanceEvaluationContext.time
        val methodsCount = new AtomicInteger(0)
        val instructionEvaluationsCount = new AtomicLong(0)

        val domainConstructor = domainClass.getConstructor(classOf[Project[URL]], classOf[Method])

        def analyzeMethod(
            source: String,
            method: Method
        ): Option[(String, ClassFile, Method, Throwable)] = {

            val body = method.body.get
            try {
                if (beVerbose) println(method.toJava(YELLOW + "[started]" + RESET))

                val evaluatedCount = time(Symbol("AI")) {
                    val ai = new InstructionCountBoundedAI[Domain](body, maxEvaluationFactor, true)
                    val domain = domainConstructor.newInstance(project, method)
                    val result = ai(method, domain)
                    if (result.wasAborted) {
                        if (beVerbose)
                            println(
                                method.toJava(
                                    RED + "[aborted after evaluating " +
                                        ai.currentEvaluationCount +
                                        " instructions (size of instructions array=" +
                                        body.instructions.size +
                                        "; max=" + ai.maxEvaluationCount + ")]" +
                                        RESET
                                )
                            )

                        val message = s"evaluation bound (max=${ai.maxEvaluationCount}) exceeded"
                        throw new InterruptedException(message)
                    }
                    val evaluatedCount = ai.currentEvaluationCount.toLong
                    instructionEvaluationsCount.addAndGet(evaluatedCount)
                    evaluatedCount
                }
                val naiveEvaluatedCount = time(Symbol("NAIVE_AI")) {
                    val ai = new InstructionCountBoundedAI[Domain](body, maxEvaluationFactor, false)
                    val domain = domainConstructor.newInstance(project, method)
                    ai(method, domain)
                    ai.currentEvaluationCount
                }

                if (beVerbose && naiveEvaluatedCount > evaluatedCount) {
                    val codeLength = body.instructions.length
                    val message = method.toJava(
                        s"evaluation steps (code size:$codeLength): " +
                            s"$naiveEvaluatedCount (w/o dead variables analysis) vs. $evaluatedCount"
                    )
                    println(message)
                }

                if (beVerbose) println(method.toJava(GREEN + "[finished]" + RESET))
                methodsCount.incrementAndGet()
                None
            } catch {
                case ct: ControlThrowable => throw ct
                case t: Throwable         =>
                    // basically, we want to catch everything!
                    val classFile = method.classFile
                    val source = project.source(classFile.thisType).get.toString
                    Some((source, classFile, method, t))
            }
        }

        val collectedExceptions = time(Symbol("OVERALL")) {
            val results = new ConcurrentLinkedQueue[(String, ClassFile, Method, Throwable)]()
            project.parForeachMethodWithBody() { m => analyzeMethod(m.source.toString, m.method).map(results.add) }
            import scala.jdk.CollectionConverters._
            results.asScala
        }

        if (collectedExceptions.nonEmpty) {
            val header = <p>Generated {new java.util.Date()}</p>

            val body = Seq(header) ++
                (for ((exResource, exInstances) <- collectedExceptions.groupBy(e => e._1)) yield {
                    val exDetails =
                        exInstances.map { ex =>
                            val (_, classFile, method, throwable) = ex
                            <div>
                                <b>{classFile.thisType.fqn}</b>
                                <i>"{method.signatureToJava(true)}"</i><br/>
                                {"Length: " + method.body.get.instructions.length}
                                <div>{XHTML.throwableToXHTML(throwable)}</div>
                            </div>
                        }

                    <section>
                        <h1>{exResource}</h1>
                        <p>Number of thrown exceptions: {exInstances.size}</p>
                        {exDetails}
                    </section>
                })

            val node =
                XHTML.createXHTML(
                    Some("Exceptions Thrown During Interpretation"),
                    NodeSeq.fromSeq(body)
                )
            val file = writeAndOpen(node, "ExceptionsOfCrashedAbstractInterpretations", ".html")

            (
                "During the interpretation of " +
                    methodsCount.get + " methods (of " + project.methodsCount + ") in " +
                    project.classFilesCount + " classes (real time: " + getTime(Symbol("OVERALL")).toSeconds +
                    ", ai (∑CPU Times): " + getTime(Symbol("AI")).toSeconds +
                    ")" + collectedExceptions.size + " exceptions occured.",
                Some(file)
            )
        } else {
            (
                "No exceptions occured during the interpretation of " +
                    methodsCount.get + " methods (of " + project.methodsCount + ") in " +
                    project.classFilesCount + " classes\nreal time: " + getTime(Symbol("OVERALL")).toSeconds + "\n" +
                    "ai (∑CPU Times): " + getTime(Symbol("AI")).toSeconds +
                    s"; evaluated ${instructionEvaluationsCount.get} instructions\n" +
                    "naive ai (∑CPU Times): " + getTime(Symbol("NAIVE_AI")).toSeconds + "\n",
                None
            )
        }
    }
}
