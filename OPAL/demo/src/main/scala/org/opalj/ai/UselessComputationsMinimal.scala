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

import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions.IFICMPInstruction

/**
 * A shallow analysis that tries to identify useless computations.
 *
 * @author Michael Eichberg
 */
object UselessComputationsMinimal extends AnalysisExecutor with Analysis[URL, BasicReport] {

    val analysis = this

    class AnalysisDomain(val project: Project[URL], val method: Method)
        extends Domain
        with domain.DefaultDomainValueBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.DefaultLongValues
        with domain.l1.DefaultConcretePrimitiveValuesConversions
        with domain.l1.LongValuesShiftOperators
        with domain.TheProject[java.net.URL]
        with domain.TheMethod
        with domain.ProjectBasedClassHierarchy

    def analyze(theProject: Project[URL], parameters: Seq[String]) = {

        val results = (for {
            classFile ← theProject.classFiles.par
            method @ MethodWithBody(body) ← classFile.methods
            result = BaseAI(classFile, method, new AnalysisDomain(theProject, method))
        } yield {
            import result.domain.ConcreteIntegerValue
            collectWithOperandsAndIndex(result.domain)(body, result.operandsArray) {
                case (
                    pc, _: IFICMPInstruction, Seq(ConcreteIntegerValue(a), ConcreteIntegerValue(b), _*)
                    ) ⇒
                    s"${classFile.thisType.toJava}{ ${method.toJava}{ /*pc=$pc:*/ comparison of constant values: $a and $b } }"
            }
        }).flatten.seq

        BasicReport(results.mkString(s"${results.size} Useless computations:\n", "\n", "\n"))
    }
}

