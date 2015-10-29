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

import scala.language.existentials
import java.net.URL
import org.opalj.br.analyses.{DefaultOneStepAnalysis, BasicReport, Project}
import org.opalj.br.Method
import org.opalj.br.{ReferenceType, ObjectType, IntegerType}
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.ai.analyses.cg.VTACallGraphKey
import org.opalj.ai.domain.l1.DefaultDomain
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.ClassFile
import org.opalj.br.MethodDescriptor

/**
 * Analyzes the parameters of called methods to determine if we have more precise type
 * information for one of the parameters.
 *
 * @author Michael Eichberg
 */
object MethodCallInformation extends DefaultOneStepAnalysis {

    override def title: String =
        "Extracting Actual Method Parameter Information"

    override def description: String =
        "Analyzes the parameters of called methods to determine if we have more precise type information."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {
        import theProject._

        //        val mutex = new Object // JUST USED TO GET A REASONABLE DEBUG OUTPUT

        val callsCount = new java.util.concurrent.atomic.AtomicInteger
        val refinedCallsCount = new java.util.concurrent.atomic.AtomicInteger
        val ch = theProject.classHierarchy

        def analyzeMethod(classFile: ClassFile, method: Method): Unit = {
            val domain = new ai.domain.l1.DefaultDomain(theProject, classFile, method)
            val result = BaseAI(classFile, method, domain)

            val code = method.body.get
            foreachPCWithOperands(domain)(code, result.operandsArray) { (pc, instruction, ops) ⇒

                def isPotentiallyRefineable(methodDescriptor: MethodDescriptor): Boolean = {
                    methodDescriptor.parametersCount > 0 &&
                        methodDescriptor.parameterTypes.exists { t ⇒
                            t.isArrayType || (t.isObjectType && ch.hasSubtypes(t.asObjectType).isYesOrUnknown)
                        }
                }

                instruction match {
                    case invoke: MethodInvocationInstruction if isPotentiallyRefineable(invoke.methodDescriptor) ⇒
                        callsCount.incrementAndGet()
                        val methodDescriptor = invoke.methodDescriptor
                        val parameterTypes = methodDescriptor.parameterTypes
                        val operands = ops.take(methodDescriptor.parametersCount).reverse

                        var index = 0
                        val hasMorePreciseType =
                            operands.exists { op ⇒
                                val foundMorePreciseType = op match {
                                    case v: domain.AReferenceValue ⇒
                                        val utb = v.upperTypeBound
                                        // If the upper type bound has multiple types
                                        // the type bound is necessarily more precise
                                        // the the method parameter's type.
                                        !utb.isSingletonSet || (utb.head ne parameterTypes(index))
                                    case _ ⇒ // we don't care about primitive types
                                        false
                                }
                                index += 1
                                foundMorePreciseType
                            }

                        if (hasMorePreciseType) {
                            // Found a method where the type information determined
                            // using the AI framework is more precise than the method's
                            // parameter.

                            // DEBUG
                            //                            mutex.synchronized {
                            //                                System.out.println(method.toJava(classFile))
                            //                                System.out.println("\t caller:   "+methodDescriptor.toUMLNotation)
                            //                                System.out.println("\t operands: "+operands.mkString(","))
                            //                                System.out.println()
                            //                                System.out.flush()
                            //                            }

                            refinedCallsCount.incrementAndGet()
                        }

                    case _ ⇒ // we don't care about all other instructions
                }
            }
        }

        parForeachMethodWithBody { isInterrupted } { projectMethod ⇒
            val (_ /*source*/ , classFile, method) = projectMethod
            try {
                // <= THIS IS STRICTLY NECESSARY AS parForeachMethodWithBody
                // (HAS TO) SWALLOW EXCEPTIONS
                analyzeMethod(classFile, method)
            } catch {
                case t: Throwable ⇒ t.printStackTrace()
            }
        }

        BasicReport(s"Found ${refinedCallsCount.get}/${callsCount.get} calls where we were able to get more precise type information.")
    }
}

