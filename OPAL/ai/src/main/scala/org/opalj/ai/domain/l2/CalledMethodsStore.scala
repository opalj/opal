/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import scala.util.control.ControlThrowable

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.Method

/**
 * Stores information about how methods were called.
 *
 * ==Thread Safety==
 * "CalledMethodsStore" are immutable.
 *
 * @author Michael Eichberg
 */
trait CalledMethodsStore { rootStore =>

    implicit val logContext: LogContext

    /**
     * The domain that is used as the target domain for the adaptation of
     * the operand values to make them comparable. '''The domain object is not used
     * at construction time which enables the creation of the store along with/ as
     * part of the creation of "its" domain.
     */
    // domain MUST NOT BE USED at initialization time
    val domain: CalledMethodsStore.BaseDomain

    /**
     * Determines when we issue a frequent evaluation warning because the same method is
     * called with different parameters more than `frequentEvaluationWarningLevel` times.
     *
     * The default is `10`.
     */
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
        method:   Method,
        operands: ValuesDomain#Operands
    ): Option[CalledMethodsStore { val domain: rootStore.domain.type }] = {

        val adaptedOperands = mapOperands(operands, domain)
        calledMethods.get(method) match {
            case None => Some(updated(method, List(adaptedOperands)))
            case Some(previousOperandsList) =>
                for (previousOperands <- previousOperandsList) {
                    try {
                        val previousOperandsIterator = previousOperands.iterator
                        val operandsIterator = adaptedOperands.iterator
                        var abstractsOver = true
                        while (previousOperandsIterator.hasNext && abstractsOver) {
                            val previousOperand = previousOperandsIterator.next()
                            val operand = operandsIterator.next()
                            abstractsOver = previousOperand.abstractsOver(operand)
                        }
                        if (abstractsOver)
                            // a previous computation completely abstracts over this computation
                            return None;
                    } catch {
                        case ct: ControlThrowable => throw ct
                        case t: Throwable =>
                            OPALLogger.error(
                                "internal error",
                                s"incompatible operands lists: $previousOperands and $adaptedOperands",
                                t
                            )(domain.logContext)
                            throw t
                    }
                }
                val newOperandsList = adaptedOperands :: previousOperandsList

                if (((previousOperandsList.size + 1) % frequentEvaluationWarningLevel) == 0)
                    frequentEvaluation(method, newOperandsList)

                Some(updated(method, newOperandsList))
        }
    }

    def frequentEvaluation(method: Method, operandsSet: List[Array[domain.DomainValue]]): Unit = {
        OPALLogger.info(
            "analysis configuration",
            method.toJava(
                "is frequently evaluated using different operands ("+operandsSet.size+"): "+
                    operandsSet.map(_.mkString("[", ",", "]")).mkString("( ", " ; ", " )")
            )
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
