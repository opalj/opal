/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.br.Method

/**
 * Enables to perform invocations.
 *
 * ==Example==
 * (PerformInvocationsWithRecursionDetection is in particular used by BugPicker's domain.)
 *
 * @author Michael Eichberg
 */
trait PerformInvocationsWithRecursionDetection extends PerformInvocations with TheMemoryLayout {
    callingDomain: ValuesFactory & ReferenceValuesDomain & TheProject & TheMethod & Configuration =>

    override type CalledMethodDomain <: Domain & TargetDomain & ChildPerformInvocationsWithRecursionDetection & MethodCallResults

    val coordinatingDomain: CalledMethodsStore.BaseDomain

    def frequentEvaluationWarningLevel: Int

    def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type }

    // The childCalledMethodsStore is valid for one invocation only and is set by
    // doInvoke...
    private[l2] var childCalledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = null

    override protected def doInvoke(
        pc:       Int,
        method:   Method,
        operands: Operands,
        fallback: () => MethodCallResult
    ): MethodCallResult = {

        callingDomain.calledMethodsStore.testOrElseUpdated(method, operands) match {
            case Some(newCalledMethodsStore) =>
                childCalledMethodsStore = newCalledMethodsStore
                super.doInvoke(pc, method, operands, fallback)
            case None =>
                fallback();
        }
    }
}

trait ChildPerformInvocationsWithRecursionDetection extends PerformInvocationsWithRecursionDetection {
    callingDomain: ValuesFactory & ReferenceValuesDomain & Configuration & TheProject & TheMethod =>

    val callerDomain: PerformInvocationsWithRecursionDetection

    override final val coordinatingDomain: callerDomain.coordinatingDomain.type = {
        callerDomain.coordinatingDomain
    }

    def frequentEvaluationWarningLevel: Int = callerDomain.frequentEvaluationWarningLevel

    final def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        callerDomain.childCalledMethodsStore
    }

}
