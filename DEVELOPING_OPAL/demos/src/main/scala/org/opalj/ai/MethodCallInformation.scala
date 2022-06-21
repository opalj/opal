/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.Method
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.MethodDescriptor
import org.opalj.ai.domain.PerformAI

/**
 * Analyzes the parameters of called methods to determine if we have more precise type
 * information for one of the parameters.
 *
 * @author Michael Eichberg
 */
object MethodCallInformation extends ProjectAnalysisApplication {

    override def title: String = "Extracting Actual Method Parameter Information"

    override def description: String = {
        "Analyzes the parameters of called methods to determine if we have more precise type information."
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        //        val mutex = new Object // JUST USED TO GET A REASONABLE DEBUG OUTPUT

        val callsCount = new java.util.concurrent.atomic.AtomicInteger
        val refinedCallsCount = new java.util.concurrent.atomic.AtomicInteger
        val ch = theProject.classHierarchy

        def analyzeMethod(method: Method): Unit = {
            val domain = new ai.domain.l1.DefaultDomain(theProject, method)
            val result = PerformAI(domain)

            val code = method.body.get
            foreachPCWithOperands(domain)(code, result.operandsArray) { (pc, instruction, ops) =>

                def isPotentiallyRefinable(methodDescriptor: MethodDescriptor): Boolean = {
                    methodDescriptor.parametersCount > 0 &&
                        methodDescriptor.parameterTypes.exists { t =>
                            t.isArrayType || (t.isObjectType && ch.hasSubtypes(t.asObjectType).isYesOrUnknown)
                        }
                }

                instruction match {
                    case invoke: MethodInvocationInstruction if isPotentiallyRefinable(invoke.methodDescriptor) =>
                        callsCount.incrementAndGet()
                        val methodDescriptor = invoke.methodDescriptor
                        val parameterTypes = methodDescriptor.parameterTypes
                        val operands = ops.take(methodDescriptor.parametersCount).reverse

                        var index = 0
                        val hasMorePreciseType =
                            operands.exists { op =>
                                val foundMorePreciseType = op match {
                                    case v: domain.AReferenceValue =>
                                        val utb = v.upperTypeBound
                                        // If the upper type bound has multiple types
                                        // the type bound is necessarily more precise
                                        // the the method parameter's type.
                                        !utb.isSingletonSet || (utb.head ne parameterTypes(index))
                                    case _ => // we don't care about primitive types
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

                    case _ => // we don't care about all other instructions
                }
            }
        }

        theProject.parForeachMethodWithBody(isInterrupted)(mi => analyzeMethod(mi.method))

        BasicReport(s"Found ${refinedCallsCount.get}/${callsCount.get} calls where we were able to get more precise type information.")
    }
}
