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
package domain

import org.junit.runner.RunWith
import org.opalj.br.reader.{BytecodeInstructionsCache, Java8FrameworkWithCaching}
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.scalatest.FunSpec
import scala.collection.JavaConverters._
import org.opalj.util.PerformanceEvaluation.time

/**
 * Tests if we are able to collect def/use information for all methods of the JDK and OPAL and if
 * the collected def/use information makes sense.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RecordDefUseTest extends FunSpec with Matchers {

    class DefUseDomain[I](val method: Method, val project: Project[java.net.URL])
        extends CorrelationalDomain
        with TheProject
        with TheMethod
        with ProjectBasedClassHierarchy
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l1.DefaultReferenceValuesBinding
        with l1.NullPropertyRefinement
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelLongValuesShiftOperators
        with RecordDefUse // <=== we are going to test!

    def analyzeProject(name: String, project: Project[java.net.URL]): Unit = {
        info(s"the loaded project ($name) contains ${project.methodsCount} methods")

        val comparisonCount = new java.util.concurrent.atomic.AtomicLong(0)
        val failures = new java.util.concurrent.ConcurrentLinkedQueue[(String, Throwable)]

        val exceptions = project.parForeachMethodWithBody() { m ⇒
            val (_, classFile, method) = m

            // DEBUG[If the analysis does not terminate]
            // println("analysis of : "+method.toJava(classFile)+"- started")

            try {
                val domain = new DefUseDomain(method, project)
                val body = method.body.get
                val r = BaseAI(classFile, method, domain)
                r.operandsArray.zipWithIndex.foreach { opsPC ⇒
                    val (ops, pc) = opsPC
                    if ((ops ne null) &&
                        // Note:
                        // In case of handlers, the def/use information
                        // is slightly different when compared with the
                        // information recorded by the reference values domain.
                        !body.exceptionHandlers.exists(_.handlerPC == pc)) {
                        ops.zipWithIndex.foreach { opValueIndex ⇒

                            val (op, valueIndex) = opValueIndex
                            val domainOrigins = domain.origin(op).toSet

                            val defUseOrigins =
                                try {
                                    domain.operandOrigin(pc, valueIndex)
                                } catch {
                                    case t: Throwable ⇒
                                        fail(
                                            "no def/use information avaiable for "+
                                                s"pc=$pc and stack index=$valueIndex",
                                            t
                                        )
                                }
                            def haveSameOrigins: Boolean = {
                                domainOrigins forall { o ⇒
                                    defUseOrigins.contains(o) ||
                                        defUseOrigins.exists(duo ⇒
                                            body.exceptionHandlers.exists(_.handlerPC == duo))
                                }
                            }
                            if (!haveSameOrigins) {
                                val message =
                                    s"{pc=$pc: "+
                                    s"operands[$valueIndex] == domain: "+
                                    s"$domainOrigins vs defUse: $defUseOrigins}"
                                fail(message)
                            }
                            comparisonCount.incrementAndGet
                        }
                    }
                }
            } catch {
                case t: Throwable ⇒
                    val methodName = method.toJava(classFile)
                    failures.add((methodName, t))
            }
            // DEBUG[If the analysis does not terminate] 
            // println("analysis of : "+methodName+"- finished")
        }
        failures.addAll(exceptions.map(ex ⇒ ("additional exception", ex)).asJava)

        val baseMessage = s"compared origin information of ${comparisonCount.get} values"
        if (failures.size > 0) {
            val failureMessages = for { (failure, exception) ← failures.asScala } yield {
                var root: Throwable = exception
                while (root.getCause != null) root = root.getCause
                val location =
                    if (root.getStackTrace() != null && root.getStackTrace().length > 0) {
                        val stackTraceElement = root.getStackTrace()(0)
                        stackTraceElement.getClassName+" { "+
                            stackTraceElement.getMethodName+":"+stackTraceElement.getLineNumber+
                            " }"
                    } else {
                        "<location unavailable>"
                    }
                failure+" ["+root.getClass.getSimpleName+": "+root.getMessage+"; location: "+location+"] "
            }

            fail(failures.size + s" exceptions occured ($baseMessage) in: "+
                failureMessages.mkString("\n", "\n", "\n"))
        } else
            info(baseMessage)
    }

    describe("getting def/use information") {
        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)
        import reader.AllClassFiles

        it("should be possible to calculate the def/use information for all methods of the JDK") {
            val project = org.opalj.br.TestSupport.createJREProject
            time{analyzeProject("JDK", project)}{t =>info("the analysis took "+t.toSeconds)            }
        }

        it("should be possible to calculate the def/use information for all methods of the OPAL 0.3 snapshot") {
            val classFiles = org.opalj.bi.TestSupport.locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
            val project = Project(reader.ClassFiles(classFiles), Traversable.empty)
            time{analyzeProject("OPAL-0.3", project)}{t =>info("the analysis took "+t.toSeconds)            }
        }

        it("should be possible to calculate the def/use information for all methods of the OPAL-08-14-2014 snapshot") {
            val classFilesFolder = org.opalj.bi.TestSupport.locateTestResources("classfiles", "bi")
            val opalJARs = classFilesFolder.listFiles(new java.io.FilenameFilter() {
                def accept(dir: java.io.File, name: String) =
                    name.startsWith("OPAL-") && name.contains("SNAPSHOT-08-14-2014")
            })
            info(opalJARs.mkString("analyzing the following jars: ", ", ", ""))
            opalJARs.size should not be (0)
            val project = Project(AllClassFiles(opalJARs), Traversable.empty)

            time {
            analyzeProject("OPAL-08-14-2014 snapshot", project)            
            }{t =>                info("the analysis took "+t.toSeconds)            }
        }

    }
}
