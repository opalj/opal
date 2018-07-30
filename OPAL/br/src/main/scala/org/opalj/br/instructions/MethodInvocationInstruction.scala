/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that invokes another method (does not consider invokedynamic
 * instructions).
 *
 * @author Michael Eichberg
 */
abstract class MethodInvocationInstruction extends InvocationInstruction {

    final override def isMethodInvocationInstruction: Boolean = true
    final override def asMethodInvocationInstruction: MethodInvocationInstruction = this

    /* abstract */ def declaringClass: ReferenceType

    def isInterfaceCall: Boolean

    /**
     * Returns the number of registers required to store the method's arguments
     * including (if required) the self reference "this".
     */
    def count: Int = {
        // c.f. JVM 8 Spec. Section 6.5.
        (if (isInstanceMethod) 1 else 0) + methodDescriptor.requiredRegisters
    }

    /**
     * Returns `true` if the called method is an instance method and virtual method
     * call resolution has to take place. I.e., if the underlying instruction is an
     * invokevirtual or an invokeinterface instruction.
     */
    /* abstract */ def isVirtualMethodCall: Boolean

    def asVirtualMethod: VirtualMethod = VirtualMethod(declaringClass, name, methodDescriptor)

    override def toString: String = {
        s"${this.getClass.getSimpleName}(${methodDescriptor.toJava(declaringClass.toJava, name)})"
    }

}

/**
 * Defines commonly used constants and an extractor method to match [[MethodInvocationInstruction]]
 * instructions.
 */
object MethodInvocationInstruction {

    def unapply(
        instruction: MethodInvocationInstruction
    ): Option[(ReferenceType, Boolean, String, MethodDescriptor)] = {
        Some((
            instruction.declaringClass,
            instruction.isInterfaceCall,
            instruction.name,
            instruction.methodDescriptor
        ))
    }

    val jvmExceptions = List(ObjectType.NullPointerException)

}
