/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Invoke instance method; special handling for superclass, private,
 * and instance initialization method invocations.
 *
 * @author Michael Eichberg
 */
case class INVOKESPECIAL(
        declaringClass:   ObjectType, // an interface or class type to be precise
        isInterface:      Boolean,
        name:             String, // an interface or class type to be precise
        methodDescriptor: MethodDescriptor
) extends NonVirtualMethodInvocationInstruction {

    final def isInterfaceCall: Boolean = isInterface

    final def asINVOKESTATIC: INVOKESTATIC = throw new ClassCastException();

    final def asINVOKESPECIAL: INVOKESPECIAL = this

    final def opcode: Opcode = INVOKESPECIAL.opcode

    final def mnemonic: String = "invokespecial"

    final def jvmExceptions: List[ObjectType] = MethodInvocationInstruction.jvmExceptions

    final def length: Int = 3

    final def isInstanceMethod: Boolean = true

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        methodDescriptor.parametersCount + 1
    }

    // Required to avoid that Scala generates a default toString method!
    override def toString: String = super.toString

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INVOKESPECIAL extends InstructionMetaInformation {

    final val opcode = 183

    /**
     * Factory method to create [[INVOKESPECIAL]] instructions.
     *
     * @param   declaringClass the method's declaring class name in JVM notation,
     *          e.g., "java/lang/Object".
     * @param   isInterface has to be `true` if declaring class identifies an interface.
     *          (Determines how the target method is resolved - relevant for Java 8 onwards.)
     * @param   methodDescriptor the method descriptor in JVM notation,
     *          e.g., "()V" for a method without parameters which returns void.
     */
    def apply(
        declaringClass:   String,
        isInterface:      Boolean,
        methodName:       String,
        methodDescriptor: String
    ): INVOKESPECIAL = {
        val declaringClassType = ObjectType(declaringClass)
        INVOKESPECIAL(declaringClassType, isInterface, methodName, MethodDescriptor(methodDescriptor))
    }

}
