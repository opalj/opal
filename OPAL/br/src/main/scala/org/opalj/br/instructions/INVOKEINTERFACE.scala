/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Invoke interface method.
 *
 * @author Michael Eichberg
 */
case class INVOKEINTERFACE(
        override val declaringClass:   ObjectType, // an interface type
        override val name:             String,
        override val methodDescriptor: MethodDescriptor
) extends VirtualMethodInvocationInstruction {

    final override def isInterfaceCall: Boolean = true

    final override def opcode: Opcode = INVOKEINTERFACE.opcode

    final override def mnemonic: String = "invokeinterface"

    final override def jvmExceptions: List[ObjectType] = MethodInvocationInstruction.jvmExceptions

    final override def length: Int = 5

    final override def isInstanceMethod: Boolean = true

    // Required to avoid that Scala generates a default toString method!
    override def toString = super.toString

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INVOKEINTERFACE extends InstructionMetaInformation {

    final val opcode = 185

    /**
     * Factory method to create [[INVOKEINTERFACE]] instructions.
     *
     * @param   declaringClass the method's declaring class name in JVM notation,
     *          e.g. "java/lang/Object".
     * @param   methodDescriptor the method descriptor in JVM notation,
     *          e.g. "()V" for a method without parameters which returns void.
     */
    def apply(
        declaringClass:   String,
        methodName:       String,
        methodDescriptor: String
    ): INVOKEINTERFACE = {
        INVOKEINTERFACE(ObjectType(declaringClass), methodName, MethodDescriptor(methodDescriptor))
    }
}
