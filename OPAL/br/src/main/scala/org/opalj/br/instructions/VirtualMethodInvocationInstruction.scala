/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all Invoke instructions that require virtual method resolution.
 *
 * @author Michael Eichberg
 */
abstract class VirtualMethodInvocationInstruction extends MethodInvocationInstruction {

    override def isVirtualMethodCall: Boolean = true

    final override def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        1 + methodDescriptor.parametersCount
    }

}

object VirtualMethodInvocationInstruction {

    def unapply(
        instruction: VirtualMethodInvocationInstruction
    ): Option[(ReferenceType, String, MethodDescriptor)] = {
        Some((instruction.declaringClass, instruction.name, instruction.methodDescriptor))
    }

}
