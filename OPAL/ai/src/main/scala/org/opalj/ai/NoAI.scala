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

import scala.util.control.ControlThrowable
import scala.collection.BitSet

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br._
import org.opalj.br.instructions._

object NoAI {

    private object TheNoAI extends AI[Domain] {
        final override def continueInterpretation(
            code: Code,
            theDomain: Domain)(
                initialWorkList: List[PC],
                alreadyEvaluated: List[PC],
                theOperandsArray: theDomain.OperandsArray,
                theLocalsArray: theDomain.LocalsArray,
                theMemoryLayoutBeforeSubroutineCall: List[(theDomain.OperandsArray, theDomain.LocalsArray)]): AIResult { val domain: theDomain.type } = {

            val result =
                AIResultBuilder.aborted(
                    code, theDomain)(
                        List(0), List.empty, theOperandsArray, theLocalsArray, List.empty)
            theDomain.abstractInterpretationEnded(result)
            if (tracer.isDefined) tracer.get.result(result)
            result
        }
    }

    def apply[D <: Domain](): AI[D] = TheNoAI.asInstanceOf[AI[D]]

}