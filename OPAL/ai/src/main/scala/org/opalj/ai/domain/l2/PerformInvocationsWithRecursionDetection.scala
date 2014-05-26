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

import br.Method
import br.ClassFile

trait PerformInvocationsWithRecursionDetection[Source] extends PerformInvocations[Source] { rootDomain ⇒

    val calledMethodsStore: CalledMethodsStore

    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: Operands): Boolean =
        calledMethodsStore.isRecursive(definingClass, method, operands)

    trait InvokeExecutionHandler extends super.InvokeExecutionHandler {

        override val domain: Domain with MethodCallResults with PerformInvocationsWithRecursionDetection[Source] {
            // we want to make sure that all instances use the same CalledMethodsStore
            val calledMethodsStore: rootDomain.calledMethodsStore.type
        }

    }
}

class CalledMethodsStore(val domain: Domain) {

    /**
     * Determines when we issue a frequent evaluation warning.
     */
    val frequentEvaluationWarningLevel = 10

    private[this] val calledMethods = scala.collection.mutable.HashMap.empty[Method, List[domain.Operands]]

    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: Domain#Operands): Boolean = {
        val adaptedOperands = operands.map(_.adapt(domain, -1))
        calledMethods.get(method) match {
            case None ⇒
                calledMethods.update(method, List(adaptedOperands))
                false
            case Some(previousOperandsList) ⇒
                for (previousOperands ← previousOperandsList) {
                    import scala.util.control.Breaks.{ breakable, break }

                    val previousOperandsIterator = previousOperands.iterator
                    val operandsIterator = adaptedOperands.iterator
                    breakable {
                        while (previousOperandsIterator.hasNext) {
                            val previousOperand = previousOperandsIterator.next
                            var operand = operandsIterator.next
                            if (!previousOperand.abstractsOver(operand))
                                break
                        }
                        // we completely abstract over a previous computation
                        return true
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
        println(
            "[info] the method "+
                definingClass.thisType.toJava+
                "{ "+method.toJava+" } "+
                "is frequently evaluated using different operands ("+operandsSet.size+"): "+
                operandsSet.map(_.mkString("[", ",", "]")).mkString("( ", " ; ", " )")
        )
    }
}


