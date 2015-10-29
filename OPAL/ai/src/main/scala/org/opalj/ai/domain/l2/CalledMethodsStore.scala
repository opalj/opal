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
 * Stores information about how methods were called.
 *
 * ==Thread Safety==
 * "CalledMethodsStore" are immutable.
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
trait CalledMethodsStore { rootStore ⇒
    // domain MUST NOT BE USED at initialization time
    implicit val logContext: LogContext

    val domain: CalledMethodsStore.BaseDomain

    val frequentEvaluationWarningLevel: Int

    val calledMethods: Map[Method, List[Array[domain.DomainValue]]]

    def updated(
        method:   Method,
        operands: List[Array[domain.DomainValue]]
    ): CalledMethodsStore { val domain: rootStore.domain.type } = {
        new CalledMethodsStore {
            val domain: rootStore.domain.type = rootStore.domain
            val frequentEvaluationWarningLevel = rootStore.frequentEvaluationWarningLevel
            val calledMethods = rootStore.calledMethods.updated(method, operands)
            implicit val logContext = rootStore.logContext
        }
    }

    def testOrElseUpdated(
        definingClass: ClassFile,
        method:        Method,
        operands:      ValuesDomain#Operands
    ): Option[CalledMethodsStore { val domain: rootStore.domain.type }] = {

        val adaptedOperands = mapOperands(operands, domain)
        calledMethods.get(method) match {
            case None ⇒ Some(updated(method, List(adaptedOperands)))
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
                            // a previous computation completely abstracts over this computation
                            return None;
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒
                            OPALLogger.error(
                                "internal error",
                                s"incompatible operands lists: $previousOperands and $adaptedOperands",
                                t
                            )(
                                    domain.logContext
                                )
                            throw t
                    }
                }
                val newOperandsList = adaptedOperands :: previousOperandsList

                if (((previousOperandsList.size + 1) % frequentEvaluationWarningLevel) == 0)
                    frequentEvalution(definingClass, method, newOperandsList)

                Some(updated(method, newOperandsList))
        }
    }

    def frequentEvalution(
        definingClass: ClassFile,
        method:        Method,
        operandsSet:   List[Array[domain.DomainValue]]
    ): Unit = {
        OPALLogger.info(
            "analysis configuration",
            "the method "+
                definingClass.thisType.toJava+
                "{ "+method.toJava+" } "+
                "is frequently evaluated using different operands ("+operandsSet.size+"): "+
                operandsSet.map(_.mkString("[", ",", "]")).mkString("( ", " ; ", " )")
        )
    }
}

object CalledMethodsStore {

    type BaseDomain = ValuesFactory with ReferenceValuesDomain with TheProject

    def empty(
        theDomain:                         BaseDomain,
        theFrequentEvaluationWarningLevel: Int        = 10
    )(
        implicit
        theLogContext: LogContext
    ): CalledMethodsStore { val domain: theDomain.type } = {
        new CalledMethodsStore {
            val domain: theDomain.type = theDomain
            val frequentEvaluationWarningLevel = theFrequentEvaluationWarningLevel
            val calledMethods = Map.empty[Method, List[Array[theDomain.DomainValue]]]
            implicit val logContext = theLogContext
        }
    }

    def apply(
        theDomain:                         BaseDomain,
        theFrequentEvaluationWarningLevel: Int        = 10
    )(
        method:   Method,
        operands: Array[theDomain.DomainValue]
    )(
        implicit
        theLogContext: LogContext
    ): CalledMethodsStore { val domain: theDomain.type } = {
        new CalledMethodsStore {
            val domain: theDomain.type = theDomain
            val frequentEvaluationWarningLevel = theFrequentEvaluationWarningLevel
            val calledMethods = Map[Method, List[Array[theDomain.DomainValue]]]((method, List(operands)))
            implicit val logContext = theLogContext
        }
    }
}

