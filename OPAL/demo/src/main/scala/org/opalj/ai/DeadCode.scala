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
import org.opalj.br.instructions.{ Instruction, ConditionalControlTransferInstruction }

/**
 * A shallow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object DeadCode extends AnalysisExecutor {

    class AnalysisDomain(
        override val project: Project[java.net.URL],
        val method: Method)
            extends Domain
            with domain.DefaultDomainValueBinding
            with domain.ThrowAllPotentialExceptionsConfiguration
            with domain.l0.DefaultTypeLevelLongValues
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l0.TypeLevelFieldAccessInstructions
            with domain.l0.TypeLevelInvokeInstructions
            with domain.l1.DefaultIntegerRangeValues
            with domain.l1.DefaultReferenceValuesBinding
            with domain.DefaultHandlingOfMethodResults
            with domain.IgnoreSynchronization
            with domain.TheProject[java.net.URL]
            with domain.TheMethod
            with domain.ProjectBasedClassHierarchy {

        type Id = String

        def id = "Dead Code Analysis Domain"

        override protected def maxSizeOfIntegerRanges: Long = 64l
    }

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String =
            "Identifies Dead Code"

        override def description: String =
            "Identifies dead code."

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            val cpus = Runtime.getRuntime().availableProcessors()
            val methodsWithDeadCode = time {
                val results = new java.util.concurrent.ConcurrentLinkedQueue[DeadCode]()
                for {
                    classFiles ← theProject.groupedClassFilesWithCode(cpus / 2 /*no hyperthreaded cores!*/ ).par
                    classFile ← classFiles
                    method ← classFile.methods
                    if method.body.isDefined
                    body = method.body.get
                    domain = new AnalysisDomain(theProject, method)
                    result = BaseAI(classFile, method, domain)
                    if !result.wasAborted
                    operandsArray = result.operandsArray
                    (ctiPC, instruction, branchTargetPCs) ← body collectWithIndex {
                        case (ctiPC, instruction @ ConditionalControlTransferInstruction()) if operandsArray(ctiPC) != null ⇒
                            (ctiPC, instruction, instruction.nextInstructions(ctiPC, /*not required*/ null))
                    }
                    branchTarget ← branchTargetPCs
                    if operandsArray(branchTarget) == null
                } { // using "yield" is more convenient but a bit slower if there is not much to yield...
                    val operands = operandsArray(ctiPC).take(2)
                    results.add(
                        DeadCode(classFile, method, ctiPC, body.lineNumber(ctiPC), instruction, operands)
                    )
                }
                scala.collection.JavaConversions.collectionAsScalaIterable(results)
            } { t ⇒ println(f"Analysis time: ${ns2sec(t)}%2.2f seconds.") }

            BasicReport(
                methodsWithDeadCode.toList.sortWith((l, r) ⇒
                    l.classFile.thisType < r.classFile.thisType ||
                        (l.classFile.thisType == r.classFile.thisType && (
                            l.method < r.method || (
                                l.method == r.method &&
                                l.ctiInstruction < r.ctiInstruction
                            )
                        ))
                ).mkString(
                    "Dead code (number of dead branches: "+methodsWithDeadCode.size+"): \n",
                    "\n",
                    "\n"))
        }
    }
}

case class DeadCode(
        classFile: ClassFile,
        method: Method,
        ctiInstruction: PC,
        lineNumber: Option[Int],
        instruction: Instruction,
        operands: List[_]) {

    override def toString = {
        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Dead code in "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+"{ "+
            GREEN+"PC: "+ctiInstruction + lineNumber.map("; Line: "+_+" - ").getOrElse("; Line: N/A - ") +
            instruction + operands.reverse.mkString("(", ",", ")") +
            RESET+" }}"
    }

}



