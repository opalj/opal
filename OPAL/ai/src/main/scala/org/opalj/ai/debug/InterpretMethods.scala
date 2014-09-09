/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package ai
package debug

import java.io.File
import java.net.URL

import scala.util.control.ControlThrowable

import org.opalj.br._
import org.opalj.br.analyses._

/**
 * Performs an abstract interpretation of all methods of the given class file(s) using
 * a configurable domain.
 *
 * This class is meant to support the development and testing of new domains.
 *
 * @author Michael Eichberg
 */
object InterpretMethods extends AnalysisExecutor {

    override def analysisSpecificParametersDescription: String =
        "[-domain=<Class of the domain that should be used for the abstract interpretation>]\n"+
            "[-verbose={true,false} If true, extensive information is shown.]\n"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean = {
        def isDomainParameter(parameter: String) =
            parameter.startsWith("-domain=") && parameter.length() > 8
        def isVerbose(parameter: String) =
            parameter == "-verbose=true" || parameter == "-verbose=false"

        parameters match {
            case Nil ⇒ true
            case Seq(parameter) ⇒
                isDomainParameter(parameter) || isVerbose(parameter)
            case Seq(parameter1, parameter2) ⇒
                isDomainParameter(parameter1) && isVerbose(parameter2)
        }
    }

    override val analysis = new InterpretMethodsAnalysis[URL]

}

/**
 * An analysis that analyzes all methods of all class files of a project using a
 * custom domain.
 *
 * @author Michael Eichberg
 */
class InterpretMethodsAnalysis[Source] extends Analysis[Source, BasicReport] {

    override def title = "Interpret Methods"

    override def description = "Performs an abstract interpretation of all methods."

    override def analyze(project: Project[Source], parameters: Seq[String] = List.empty) = {
        val verbose = parameters.size > 0 &&
            (parameters(0) == "-verbose=true" ||
                (parameters.size == 2 && parameters.tail.head == "-verbose=true"))
        val (message, detailedErrorInformationFile) =
            if (parameters.size > 0 && parameters(0).startsWith("-domain")) {
                InterpretMethodsAnalysis.interpret(
                    project,
                    Class.forName(parameters.head.substring(8)).asInstanceOf[Class[_ <: Domain]],
                    verbose)
            } else {
                InterpretMethodsAnalysis.interpret(
                    project,
                    classOf[domain.l0.BaseDomain[java.net.URL]],
                    verbose)

            }
        BasicReport(
            message +
                detailedErrorInformationFile.map(" (See "+_+" for details.)").getOrElse(""))
    }
}

object InterpretMethodsAnalysis {

    def println(s: String) {
        Console.out.println(s)
        Console.flush()
    }

    def interpret[Source](
        project: Project[Source],
        domainClass: Class[_ <: Domain],
        beVerbose: Boolean,
        maxEvaluationFactor: Int = 10): (String, Option[File]) = {

        import Console.{ BOLD, RED, YELLOW, GREEN, RESET }
        import org.opalj.util.PerformanceEvaluation.ns2sec
        val performanceEvaluationContext = new org.opalj.util.PerformanceEvaluation
        import performanceEvaluationContext.{ time, getTime }
        val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)

        val domainConstructor =
            domainClass.getConstructor(
                classOf[Project[java.net.URL]], classOf[ClassFile], classOf[Method])

        def analyzeClassFile(
            source: String,
            classFile: ClassFile): Seq[(String, ClassFile, Method, Throwable)] = {

            val collectedExceptions =
                for (method @ MethodWithBody(body) ← classFile.methods) yield {
                    try {
                        if (beVerbose)
                            println(classFile.thisType.toJava+"{ "+method.toJava + YELLOW+"[started]"+RESET+" }")
                        time('AI) {
                            val ai = new InstructionCountBoundedAI[Domain](body, maxEvaluationFactor)
                            val result =
                                ai.apply(
                                    classFile,
                                    method,
                                    domainConstructor.newInstance(project, classFile, method)
                                )
                            if (result.wasAborted) {
                                if (beVerbose)
                                    println(
                                        classFile.thisType.toJava+"{ "+method.toJava +
                                            RED+"[aborted after evaluating "+
                                            ai.currentEvaluationCount+
                                            " instructions (size of instructions array="+body.instructions.size+
                                            "; max="+ai.maxEvaluationCount+")]"+
                                            RESET+" }")

                                throw new InterruptedException(
                                    "evaluation bound (max="+ai.maxEvaluationCount+
                                        ") exceeded")
                            }
                        }
                        if (beVerbose)
                            println(classFile.thisType.toJava+"{ "+method.toJava + GREEN+"[finished]"+RESET+" }")
                        methodsCount.incrementAndGet()
                        None
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒
                            // basically, we want to catch everything!
                            Some((
                                project.source(classFile.thisType).get.toString,
                                classFile,
                                method,
                                t)
                            )
                    }
                }

            collectedExceptions.filter(_.isDefined).map(_.get)
        }

        val collectedExceptions = time('OVERALL) {
            val result = (
                for { (source, classFile) ← project.classFilesWithSources.par } yield {

                    if (beVerbose) println(BOLD + source.toString + RESET)

                    analyzeClassFile(source.toString, classFile)
                }
            ).flatten.seq.toSeq
            result.size //for the evaluation
            result
        }

        if (collectedExceptions.nonEmpty) {
            val body =
                for ((exResource, exInstances) ← collectedExceptions.groupBy(e ⇒ e._1)) yield {
                    val exDetails =
                        exInstances.map { ex ⇒
                            val (_, classFile, method, throwable) = ex
                            <div>
                                <b>{ classFile.thisType.fqn }</b>
                                <i>"{ method.toJava }"</i><br/>
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

            val node =
                XHTML.createXHTML(
                    Some("Exceptions Thrown During Interpretation"),
                    scala.xml.NodeSeq.fromSeq(body.toSeq))
            val file =
                org.opalj.util.writeAndOpen(
                    node,
                    "ExceptionsOfCrashedAbstractInterpretations", ".html")

            (
                "During the interpretation of "+
                methodsCount.get+" methods (of "+project.methodsCount+") in "+
                project.classFilesCount+" classes (real time: "+ns2sec(getTime('OVERALL))+
                "secs., ai (∑CPU Times): "+ns2sec(getTime('AI))+
                "secs.)"+collectedExceptions.size+" exceptions occured.",
                Some(file)
            )
        } else {
            (
                "No exceptions occured during the interpretation of "+
                methodsCount.get+" methods (of "+project.methodsCount+") in "+
                project.classFilesCount+" classes (real time: "+ns2sec(getTime('OVERALL))+
                "secs., ai (∑CPU Times): "+ns2sec(getTime('AI))+
                "secs.)",
                None
            )
        }
    }
}
