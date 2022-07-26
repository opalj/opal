/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Invoke a class (static) method.
 *
 * @author Michael Eichberg
 */
case class INVOKESTATIC(
        declaringClass:   ObjectType, // a class or interface (Java 8) type
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends NonVirtualMethodInvocationInstruction {

    final override def isInvokeStatic: Boolean = true

    final def isInterfaceCall: Boolean = isInterface

    final def asINVOKESTATIC: INVOKESTATIC = this

    final def asINVOKESPECIAL: INVOKESPECIAL = throw new ClassCastException();

    final def opcode: Opcode = INVOKESTATIC.opcode

    final def mnemonic: String = "invokestatic"

    final def jvmExceptions: List[ObjectType] = Nil

    final def length: Int = 3

    final def isInstanceMethod: Boolean = false

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        methodDescriptor.parametersCount
    }

    final def parametersCount: Int = methodDescriptor.parametersCount

    override def toString: String = {
        if (isInterface)
            "/* interface */"+super.toString
        else
            super.toString
    }

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INVOKESTATIC extends InstructionMetaInformation {

    final val opcode = 184

    /**
     * Factory method to create [[INVOKESTATIC]] instructions.
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
    ): INVOKESTATIC = {
        val declaringClassType = ObjectType(declaringClass)
        INVOKESTATIC(declaringClassType, isInterface, methodName, MethodDescriptor(methodDescriptor))
    }

}
