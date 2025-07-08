/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File

import org.opalj.ai.domain.PerformAI
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.MethodInvocationInstruction

/**
 * Analyzes the parameters of called methods to determine if we have more precise type
 * information for one of the parameters.
 *
 * @author Michael Eichberg
 */
object MethodCallInformation extends ProjectsAnalysisApplication {

    protected class StatisticsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects method call parameters where more precise type information can be computed"
    }

    protected type ConfigType = StatisticsConfig

    protected def createConfig(args: Array[String]): StatisticsConfig = new StatisticsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: StatisticsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        //        val mutex = new Object // JUST USED TO GET A REASONABLE DEBUG OUTPUT

        val callsCount = new java.util.concurrent.atomic.AtomicInteger
        val refinedCallsCount = new java.util.concurrent.atomic.AtomicInteger
        val ch = project.classHierarchy

        def analyzeMethod(method: Method): Unit = {
            val domain = new ai.domain.l1.DefaultDomain(project, method)
            val result = PerformAI(domain)

            val code = method.body.get
            foreachPCWithOperands(domain)(code, result.operandsArray) { (pc, instruction, ops) =>
                def isPotentiallyRefinable(methodDescriptor: MethodDescriptor): Boolean = {
                    methodDescriptor.parametersCount > 0 &&
                    methodDescriptor.parameterTypes.exists { t =>
                        t.isArrayType || (t.isClassType && ch.hasSubtypes(t.asClassType).isYesOrUnknown)
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
                                        // If the upper type bound has multiple types the type bound
                                        // is necessarily more precise the method parameter's type.
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

        project.parForeachMethodWithBody()(mi => analyzeMethod(mi.method))

        (
            project,
            BasicReport(
                s"Found ${refinedCallsCount.get}/${callsCount.get} calls where we were able to get more precise type information."
            )
        )
    }
}
