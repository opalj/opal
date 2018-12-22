/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Invocation of a method where the target method is statically resolved.
 *
 * @author Michael Eichberg
 */
abstract class NonVirtualMethodInvocationInstruction
    extends MethodInvocationInstruction
    with InstructionMetaInformation {

    override def isVirtualMethodCall: Boolean = false

    def asINVOKESTATIC: INVOKESTATIC

    def asINVOKESPECIAL: INVOKESPECIAL

}
