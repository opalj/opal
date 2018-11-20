/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package la

import org.opalj.br.Method
import org.opalj.ai.ValuesFactory
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.cg.MethodCallsDomainWithMethodLockup

/**
 *
 * @author Michael Eichberg
 */
trait RefinedTypeLevelInvokeInstructions extends MethodCallsDomainWithMethodLockup {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    val methodReturnValueInformation: MethodReturnValueInformation

    protected[this] def doInvoke(
        pc:       PC,
        method:   Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult
    ): MethodCallResult = {

        val returnValue =
            methodReturnValueInformation.getOrElse(method, {
                return fallback();
            })

        if (returnValue.isDefined) {
            val adaptedReturnValue = returnValue.get.adapt(this, pc)
            // println(s"${method.toJava()} returning refined value $adaptedReturnValue (returntye: ${method.returnType}; original value ${returnValue.get})")
            MethodCallResult(adaptedReturnValue, getPotentialExceptions(pc))
        } else {
            // the method always throws an exception... but we don't know which one
            val potentialExceptions = getPotentialExceptions(pc)
            ThrowsException(potentialExceptions)
        }
    }

}
