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
    callingDomain: ValuesFactory with ReferenceValuesDomain with TheProject with TheMethod with Configuration =>

    override type CalledMethodDomain <: TargetDomain with ChildPerformInvocationsWithRecursionDetection with MethodCallResults

    val coordinatingDomain: CalledMethodsStore.BaseDomain

    def frequentEvaluationWarningLevel: Int

    def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type }

    // The childCalledMethodsStore is valid for one invocation only and is set by
    // doInvoke...
    private[l2] var childCalledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = null

    override protected[this] def doInvoke(
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
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheMethod =>

    val callerDomain: PerformInvocationsWithRecursionDetection

    final override val coordinatingDomain: callerDomain.coordinatingDomain.type = {
        callerDomain.coordinatingDomain
    }

    def frequentEvaluationWarningLevel: Int = callerDomain.frequentEvaluationWarningLevel

    final def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        callerDomain.childCalledMethodsStore
    }

}
