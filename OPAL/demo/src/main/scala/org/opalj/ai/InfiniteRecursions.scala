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

import java.net.URL
import org.opalj.collection.immutable.{ UIDSet, UIDSet1 }
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project, SomeProject }
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.{ ReferenceType }
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.ai.domain._
import org.opalj.br.MethodWithBody
import org.opalj.br.instructions._
import org.opalj.ai.domain.l2.PerformInvocations
import org.opalj.br.Code
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProgressManagement
import scala.annotation.tailrec

/**
 * A shallow analysis that tries find infinite recursions.
 *
 * @author Marco Jacobasch
 */
object InfiniteRecursions extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {
        
        val iterationDepth = 5
        val minCountOfConsecutivelyUnchangedOperands = 3

        override def title: String =
            "Infinite Recursions"

        override def description: String =
            "Identifies methods which causes infinite recursions."

        override def analyze(
            theProject: Project[URL],
            parameters: Seq[String] = List.empty,
            initProgressManagement: (Int) ⇒ ProgressManagement) = {

            // Select every method which invokes itself (static, virtual, special or interface)
            val result = for {
                classFile ← theProject.classFiles
                if !classFile.isInterfaceDeclaration && !classFile.isAnnotationDeclaration
                method @ MethodWithBody(body) ← classFile.methods
                objecType = classFile.thisType
                pcs = body.collectWithIndex {
                    case (pc, INVOKEVIRTUAL(`objecType`, method.name, method.descriptor))   ⇒ pc
                    case (pc, INVOKESTATIC(`objecType`, method.name, method.descriptor))    ⇒ pc
                    case (pc, INVOKESPECIAL(`objecType`, method.name, method.descriptor))   ⇒ pc
                    case (pc, INVOKEINTERFACE(`objecType`, method.name, method.descriptor)) ⇒ pc
                }
                if pcs.nonEmpty
            } yield {
                val domain = new InfiniteRecursionsDomain(theProject, method)

//                println("\n------------------------------------------------")
//                println("%s %s PCs(%s)" format (classFile.thisType, method.toJava, pcs.mkString(", ")))
//                println("---")

                val result = findInfiniteRecursion(iterationDepth, minCountOfConsecutivelyUnchangedOperands, classFile, method, pcs, domain, body)

                val resultAsString = result.collect { case Some(infiniteRecursion) ⇒ infiniteRecursion }.mkString("\n")
//                println(resultAsString)

                resultAsString
            }

            BasicReport(result.mkString("\n"))
        }

        /**
         * Find possible infinite recursions of the passed method for every passed PC invoking a call to the same method and methodescriptor
         *
         * The depth of the DFS is limited by the value of iterations.
         * minCountOfUnchangedArguments is the amount of needed consecutive steps without a change in the operands to be considered a infinite recursion.
         */
        def findInfiniteRecursion(iterations: Int, minCountOfUnchangedArguments: Int, classFile: ClassFile, method: Method, pcs: Seq[PC], domain: Domain, body: Code): Seq[Option[InfiniteRecursion]] = {

            /**
             * Perform a single step AI for each possible PC and analyze all previous AI results of the current taken path for an occurence of a possible infinite recursion.
             */
            def analyzeRecursionStep(iterations: Int, aiResults: Seq[(PC, AIResult)] = Seq()): Seq[Option[InfiniteRecursion]] = {
                if (iterations <= 0)
                    return Seq(None)

                val intermediateStepResult = for {
                    pc ← pcs
                    (lastPC, lastAiResult) = aiResults.head
                    if (lastAiResult.operandsArray(pc) != null)
                } yield {
                    // Abstract Interpretation
                    val value = PerformInvocations mapOperandsToParameters (lastAiResult.operandsArray(pc), method, domain)
                    val perform = BaseAI.perform(body, domain)(Nil, value)
                    val combinedAIResults = (pc, perform) +: aiResults

                    // Analyse Result
                    val operandsOfInterpretedBranches = combinedAIResults filter {
                        case (localPC, localAiResult) ⇒ localAiResult.operandsArray(localPC) != null
                    }

                    val adaptedOperands = operandsOfInterpretedBranches map {
                        case (localPC, localAiResult) ⇒
                            val operandsToAdapt = localAiResult.operandsArray(localPC)
                            PerformInvocations.mapOperandsToParameters(operandsToAdapt, method, domain)
                    }

                    val operandsComparedForEquality = for {
                        Seq(currentOperands, followingOperands) ← adaptedOperands.sliding(2)
                    } yield {
                        currentOperands == followingOperands
                    }

                    val isInfiniteRecursion = operandsComparedForEquality.fold(adaptedOperands.length >= minCountOfUnchangedArguments)(_ && _)
                    if (isInfiniteRecursion) {
                        Seq(Some(InfiniteRecursion(classFile, method, pc, adaptedOperands)))
                    } else {
                        analyzeRecursionStep(iterations - 1, combinedAIResults)
                    }

                }
                intermediateStepResult.flatten
            }

            val perform = BaseAI(classFile, method, domain)
            val result = for {
                pc ← pcs
            } yield {
                analyzeRecursionStep(
                    iterations,
                    Seq((pc, BaseAI(classFile, method, domain))))
            }
            result.flatten
        }

    }
}

class InfiniteRecursionsDomain(
    override val project: Project[java.net.URL],
    override val method: Method,
    override val maxCardinalityOfIntegerRanges: Long = 16l)
        extends Domain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.MaxArrayLengthRefinement
        with domain.l1.ConstraintsBetweenIntegerValues
        //with domain.l1.DefaultIntegerSetValues
        with domain.l1.DefaultLongValues
        with domain.l1.LongValuesShiftOperators
        with domain.l1.DefaultConcretePrimitiveValuesConversions
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject[java.net.URL]
        with domain.TheMethod
        with domain.ProjectBasedClassHierarchy

case class InfiniteRecursion(
        classFile: ClassFile,
        method: Method,
        infinitRecursionPC: PC,
        operands: Seq[_]) {

    def infinitRecursionLineNumber: Option[PC] =
        method.body.get.lineNumber(infinitRecursionPC)

    def message: String = ""

    override def toString = {
        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Infinite recursion in "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+"{ "+
            GREEN+"PC: "+infinitRecursionPC+" Line: "+infinitRecursionLineNumber.getOrElse(-1) +
            RESET+" "+operands.mkString(", ")+" }}"
    }
}

