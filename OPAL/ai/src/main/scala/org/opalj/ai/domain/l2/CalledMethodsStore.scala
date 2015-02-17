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
package l2

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.Method
import org.opalj.br.ClassFile
import scala.util.control.ControlThrowable

/**
 *
 * ==Thread Safety==
 * A "CalledMethodsStore" is not thread-safe.
 *
 * @param domain The domain that is used as the target domain for the adaptation of
 *      the operand values to make them comparable. '''The domain object is not used
 *      at construction time which enables the creation of the store along with/ as
 *      part of the creation of "its" domain.
 *
 * @param frequentEvaluationWarningLevel Determines when we issue a frequent evaluation
 *      warning because the same method is called with different parameters more than
 *      `frequentEvaluationWarningLevel` times. The default is `10`.
 *
 * @author Michael Eichberg
 */
class CalledMethodsStore(
        // domain MUST NOT BE USED at initialization time
        val domain: ValuesFactory with ReferenceValuesDomain with TheProject,
        val frequentEvaluationWarningLevel: Int = 10)(
                implicit val logContext: LogContext) {

    private[this] val calledMethods =
        scala.collection.mutable.HashMap.empty[Method, List[domain.Operands]]

    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: ValuesDomain#Operands): Boolean = {

        val adaptedOperands = operands.map(_.adapt(domain, -1))
        calledMethods.get(method) match {
            case None ⇒
                calledMethods.update(method, List(adaptedOperands))
                false
            case Some(previousOperandsList) ⇒
                for (previousOperands ← previousOperandsList) {
                    try {
                        val previousOperandsIterator = previousOperands.iterator
                        val operandsIterator = adaptedOperands.iterator
                        var abstractsOver = true
                        while (previousOperandsIterator.hasNext && abstractsOver) {
                            val previousOperand = previousOperandsIterator.next
                            val operand = operandsIterator.next
                            abstractsOver = previousOperand.abstractsOver(operand)
                        }
                        if (abstractsOver)
                            // we completely abstract over a previous computation
                            return true
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒
                            OPALLogger.error(
                                "internal error",
                                s"incompatible operands lists: $previousOperands and $adaptedOperands",
                                t)(
                                    domain.logContext)
                            throw t
                    }
                }
                val newOperandsList = adaptedOperands :: previousOperandsList

                if (((previousOperandsList.size + 1) % frequentEvaluationWarningLevel) == 0)
                    frequentEvalution(definingClass, method, newOperandsList)

                calledMethods.update(method, newOperandsList)
                false
        }
    }

    def frequentEvalution(
        definingClass: ClassFile,
        method: Method,
        operandsSet: List[domain.Operands]): Unit = {
        OPALLogger.warn(
            "analysis configuration",
            "the method "+
                definingClass.thisType.toJava+
                "{ "+method.toJava+" } "+
                "is frequently evaluated using different operands ("+operandsSet.size+"): "+
                operandsSet.map(_.mkString("[", ",", "]")).mkString("( ", " ; ", " )")
        )
    }
}

