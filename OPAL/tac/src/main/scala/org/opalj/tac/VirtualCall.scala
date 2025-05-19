/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.collection.Set

import org.opalj.br.ClassType
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectLike
import org.opalj.value.ValueInformation

trait VirtualCall[+V <: Var[V]] { this: Call[V] =>

    def receiver: Expr[V]

    /**
     * Resolves the call targets taking the domain value information (`isPrecise` and `isNull`)
     * into consideration.
     *
     * @note __This method requires that we have a flat representation!__ (That is, the receiver
     *      is a `Var`.)
     */
    def resolveCallTargets(
        callingContext: ClassType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method] = {
        val receiverValue = receiver.asVar.value.asReferenceValue

        if (receiverValue.isNull.isYes) {
            Set.empty
        } else if (declaringClass.isArrayType) {
            p.instanceCall(ClassType.Object, ClassType.Object, name, descriptor).toSet
        } else if (receiverValue.isPrecise) {
            val receiverType = receiverValue.upperTypeBound.head
            p.instanceCall(callingContext, receiverType, name, descriptor).toSet
        } else {
            // IMPROVE use the upper type bound to find the relevant types and then locate the methods
            import p.classHierarchy.joinReferenceTypesUntilSingleUpperBound
            val receiverType = joinReferenceTypesUntilSingleUpperBound(receiverValue.upperTypeBound)
            if (isInterface) {
                p.interfaceCall(callingContext, receiverType.asClassType, name, descriptor)
            } else {
                p.virtualCall(callingContext, receiverType, name, descriptor)
            }
        }
    }

}
