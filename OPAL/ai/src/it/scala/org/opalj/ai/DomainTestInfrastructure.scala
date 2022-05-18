/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.util.control.ControlThrowable
import scala.xml.NodeSeq

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.PerformanceEvaluation
import org.opalj.io.writeAndOpen
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.bi.TestResources
import org.opalj.br.{TestSupport => BRTestSupport}
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.reader.Java9FrameworkWithInvokedynamicSupportAndCaching
import org.opalj.ai.util.XHTML

/**
 * Provides the basic infrastructure to load a very large number of class files and to perform
 * an abstract interpretation of all methods.
 *
 * The primary mechanism to adapt this framework is to override the `analyzeAIResult` method
 * and to throw some exception if an expected property is violated.
 *
 * @param   domainName A descriptive name of the domain.
 *
 * @author  Michael Eichberg
 */
abstract class DomainTestInfrastructure(domainName: String) extends AnyFlatSpec with Matchers {

    private[this] implicit val logContext: LogContext = GlobalLogContext

    type AnalyzedDomain <: Domain

    def Domain(project: Project[URL], method: Method): AnalyzedDomain

    /**
     * Called for each method that was successfully analyzed.
     */
    def analyzeAIResult(
        project: Project[URL],
        method:  Method,
        result:  AIResult { val domain: AnalyzedDomain }
    ): Unit = {
        // validate that we can get the computational type of each value stored on the stack
        // (this test will fail by throwing an exception)
        result.operandsArray.forall { ops =>
            (ops eq null) || { ops.foreach(op => op.computationalType); true }
        }
    }

    /**
     * Performs an abstract interpretation of all concrete methods of the
     * project and records and reports the exceptions thrown while doing so.
     */
    def analyzeProject(
        projectName:         String,
        project:             Project[URL],
        maxEvaluationFactor: Double
    ): Unit = {

        val performanceEvaluationContext = new PerformanceEvaluation
        import performanceEvaluationContext.{time, getTime}
        val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)

        def analyzeClassFile(
            source: String,
            method: Method
        ): Option[(String, ClassFile, Method, Throwable)] = {
            val classFile = method.classFile
            val body = method.body.get
            try {
                time(Symbol("AI")) {
                    val ai = new InstructionCountBoundedAI[Domain](body, maxEvaluationFactor)
                    val result = ai(method, Domain(project, method))
                    if (result.wasAborted) {
                        throw new InterruptedException(
                            s"evaluation bound (max=${ai.maxEvaluationCount} exceeded"+
                                s" (maxStack=${body.maxStack}; maxLocals=${body.maxLocals})"
                        )
                    } else {
                        analyzeAIResult(project, method, result)
                    }
                }
                methodsCount.incrementAndGet()
                None
            } catch {
                case ct: ControlThrowable => throw ct
                case t: Throwable =>
                    // basically, we want to catch everything!
                    Some((project.source(classFile).get.toString, classFile, method, t))
            }
        }

        // Interpret Methods
        //
        val collectedExceptions = time(Symbol("OVERALL")) {
            val exceptions = new ConcurrentLinkedQueue[(String, ClassFile, Method, Throwable)]()
            project.parForeachMethodWithBody() { (m) =>
                val MethodInfo(source, method) = m
                analyzeClassFile(source.toString, method) foreach { exceptions.add(_) }
            }
            import scala.jdk.CollectionConverters._
            exceptions.asScala
        }

        // Create Report
        //
        if (collectedExceptions.nonEmpty) {
            val body =
                for ((exResource, exInstances) <- collectedExceptions.groupBy(e => e._1)) yield {
                    val exDetails =
                        exInstances.map { ex =>
                            val (_, classFile, method, throwable) = ex
                            <div>
                                <b>{ classFile.thisType.fqn }</b>
                                <i>"{ method.signatureToJava(false) }"</i><br/>
                                { "Length: "+method.body.get.instructions.length }
                                <div>{ XHTML.throwableToXHTML(throwable) }</div>
                            </div>
                        }

                    <section>
                        <h1>{ exResource }</h1>
                        <p>Number of thrown exceptions: { exInstances.size }</p>
                        { exDetails }
                    </section>
                }
            val node = XHTML.createXHTML(Some("Thrown Exceptions"), NodeSeq.fromSeq(body.toSeq))
            val file = writeAndOpen(node, "FailedAbstractInterpretations-"+projectName, ".html")

            fail(
                projectName+": "+
                    "During the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+getTime(Symbol("OVERALL")).toSeconds+
                    ", ai (∑CPU Times): "+getTime(Symbol("AI")).toSeconds+
                    ")"+collectedExceptions.size+
                    " exceptions occured (details: "+file.toString+")."
            )
        } else {
            info(
                s"$projectName: no exceptions occured during the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+getTime(Symbol("OVERALL")).toSeconds+
                    ", ai (∑CPU Times): "+getTime(Symbol("AI")).toSeconds+")"
            )
        }
    }

    //
    // The Configured Projects
    //

    val cache = new BytecodeInstructionsCache
    val reader = new Java9FrameworkWithInvokedynamicSupportAndCaching(cache)

    behavior of domainName

    it should ("be useable to perform an abstract interpretation of the JRE's classes") in {
        val project = BRTestSupport.createJREProject()

        analyzeProject("JDK", project, 4d)
    }

    it should ("be useable to perform an abstract interpretation of OPAL-SNAPSHOT-08-14-2014") in {

        import reader.AllClassFiles
        val classFilesFolder = TestResources.locateTestResources("classfiles", "bi")
        val opalJARs = classFilesFolder.listFiles(new java.io.FilenameFilter() {
            def accept(dir: java.io.File, name: String) = {
                name.startsWith("OPAL-") && name.contains("SNAPSHOT-08-14-2014")
            }
        })
        info(opalJARs.mkString("analyzing the following jars: ", ", ", ""))
        opalJARs.size should not be (0)
        val project = Project(AllClassFiles(opalJARs), Iterable.empty, true)

        analyzeProject("OPAL-SNAPSHOT-08-14-2014", project, 1.5d)
    }

    it should ("be useable to perform an abstract interpretation of OPAL-SNAPSHOT-0.3.jar") in {
        val classFiles = TestResources.locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
        val project = Project(reader.ClassFiles(classFiles), Iterable.empty, true)

        analyzeProject("OPAL-0.3", project, 2.5d)
    }

}
