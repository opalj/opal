/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that "invokes" something. This can, e.g., be the invocation of a method
 * or – using [[INCOMPLETE_INVOKEDYNAMIC]] – the read of a field value.
 *
 * @author Michael Eichberg
 */
abstract class InvocationInstruction
    extends Instruction
    with ConstantLengthInstruction
    with NoLabels {

    final override def asInvocationInstruction: this.type = this

    final override def isInvocationInstruction: Boolean = true

    final def mayThrowExceptions: Boolean = true

    /**
     * The simple name of the called method.
     *
     * @note    This information is – in case of [[INCOMPLETE_INVOKEDYNAMIC]] instructions
     *          – only available after loading the entire class file.
     */
    def name: String

    /**
     * The method descriptor of the called method.
     *
     * @note    This information is – in case of [[INCOMPLETE_INVOKEDYNAMIC]] instructions
     *          – only available after loading the entire class file.
     */
    def methodDescriptor: MethodDescriptor

    /**
     * Returns `true` if this method takes an implicit parameter "this".
     */
    def isInstanceMethod: Boolean

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (methodDescriptor.returnType.isVoidType) 0 else 1
    }

    final def stackSlotsChange: Int = {
        val returnType = methodDescriptor.returnType
        (if (isInstanceMethod) -1 /* pop "receiver object */ else 0) -
            methodDescriptor.parameterTypes.foldLeft(0)(_ + _.computationalType.operandSize) +
            (if (returnType.isVoidType) 0 else returnType.computationalType.operandSize)
    }

    final def expressionResult: ExpressionResultLocation = {
        if (methodDescriptor.returnType.isVoidType) NoExpression else Stack
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    /**
     * Given that we have – without any sophisticated analysis – no idea which
     * exceptions may be thrown by the called method, we make the safe assumption that
     * any handler is a potential successor!
     *
     * The result may contain duplicates iff multiple different exceptions are handled by
     * the same handler. E.g., as generated in case of "Java's multicatch instruction":
     * {{{
     * try {} catch(IOException | NullPointerException ex) {...}
     * }}}
     */
    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List(indexOfNextInstruction(currentPC))
        else {
            val exceptionHandlerPCs = code.handlerInstructionsFor(currentPC)
            indexOfNextInstruction(currentPC) :: exceptionHandlerPCs
        }
    }

    final override def toString(currentPC: Int): String = toString()
}
