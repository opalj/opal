/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Invoke instance method; dispatch based on class.
 *
 * @author Michael Eichberg
 */
case class INVOKEVIRTUAL(
        declaringClass:   ReferenceType, // an class or array type to be precise
        name:             String,
        methodDescriptor: MethodDescriptor
) extends VirtualMethodInvocationInstruction {

    final def isInterfaceCall: Boolean = false

    final def opcode: Opcode = INVOKEVIRTUAL.opcode

    final def mnemonic: String = "invokevirtual"

    final def jvmExceptions: List[ObjectType] = MethodInvocationInstruction.jvmExceptions

    final def length: Int = 3

    final def isInstanceMethod: Boolean = true

    // Required to avoid that Scala generates a default toString method!
    override def toString = super.toString

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INVOKEVIRTUAL extends InstructionMetaInformation {

    final val opcode = 182

    /**
     * Factory method to create [[INVOKEVIRTUAL]] instructions.
     *
     * @param   declaringClass the method's declaring class name in JVM notation,
     *          e.g., `java/lang/Object` or `[java/lang/Object` in case of a method call on
     *          an array object. In the latter case, the called method has to be a method defined
     *          by `java/lang/Object`; e.g., `clone` or `wait`.
     * @param   isInterface has to be `true` if declaring class identifies an interface.
     *          (Determines how the target method is resolved - relevant for Java 8 onwards.)
     * @param   methodDescriptor the method descriptor in JVM notation,
     *          e.g., "()V" for a method without parameters which returns void.
     */
    def apply(
        declaringClass:   String,
        methodName:       String,
        methodDescriptor: String
    ): INVOKEVIRTUAL = {
        val declaringClassType = ReferenceType(declaringClass)
        INVOKEVIRTUAL(declaringClassType, methodName, MethodDescriptor(methodDescriptor))
    }

}
