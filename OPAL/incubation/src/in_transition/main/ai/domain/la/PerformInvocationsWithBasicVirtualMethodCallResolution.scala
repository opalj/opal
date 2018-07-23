/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package la

import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.br.PC
import org.opalj.ai.analyses.cg.Callees
import org.opalj.ai.domain.l2.PerformInvocations
import org.opalj.br.analyses.cg.TypeExtensibilityKey

/**
 * Mix in this trait if methods that are called by `invokeXYZ` instructions should
 * actually be interpreted using a custom domain.
 *
 * @author Michael Eichberg
 */
trait PerformInvocationsWithBasicVirtualMethodCallResolution
    extends PerformInvocations
    with Callees {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheMethod ⇒

    lazy val isExtensible = project.get(TypeExtensibilityKey)

    /**
     * The default implementation only supports the case where we can precisely resolve the target.
     */
    override def doInvokeVirtual(
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands,
        fallback:       () ⇒ MethodCallResult
    ): MethodCallResult = {

        def handleVirtualInvokeFallback(): MethodCallResult = {
            val receiver = operands(descriptor.parametersCount)

            receiver match {
                case DomainReferenceValueTag(refValue) if (
                    refValue.isNull.isNo && // IMPROVE the case if the value maybe null
                    refValue.upperTypeBound.isSingletonSet &&
                    refValue.upperTypeBound.head.isObjectType
                ) ⇒
                    val receiverType = refValue.upperTypeBound.head.asObjectType
                    if (isExtensible(receiverType).isYesOrUnknown)
                        return fallback();

                    val methods = classHierarchy.isInterface(receiverType) match {
                        case Yes ⇒ callees(method, receiverType, true, name, descriptor)
                        case No  ⇒ callees(method, receiverType, false, name, descriptor)
                        case _   ⇒ return fallback();
                    }
                    if (methods.size == 1) {
                        testAndDoInvoke(pc, methods.head, operands, fallback)
                    } else {
                        fallback()
                    }

                case _ ⇒
                    fallback()
            }
        }

        super.doInvokeVirtual(
            pc,
            declaringClass, isInterface, name, descriptor,
            operands,
            handleVirtualInvokeFallback _
        )
    }

}
