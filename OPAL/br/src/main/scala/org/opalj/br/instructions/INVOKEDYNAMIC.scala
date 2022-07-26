/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Represents an `invokedynamic` instruction.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait INVOKEDYNAMIC extends InvocationInstruction {

    /*abstract*/ def bootstrapMethod: BootstrapMethod

    final override def opcode: Opcode = INVOKEDYNAMIC.opcode

    final override def mnemonic: String = "invokedynamic"

    final override def length: Int = 5

    final def isInstanceMethod: Boolean = false

    final override def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        methodDescriptor.parametersCount
    }

    final override def jvmExceptions: List[ObjectType] = INVOKEDYNAMIC.jvmExceptions

}

/**
 * Common constants and extractor methods related to [[INVOKEDYNAMIC]] instructions.
 */
object INVOKEDYNAMIC extends InstructionMetaInformation {

    final val jvmExceptions = List(ObjectType.BootstrapMethodError)

    final val opcode = 186

    /**
     * General extractor for objects of type `INVOKEDYNAMIC`.
     */
    def unapply(instruction: INVOKEDYNAMIC): Some[(BootstrapMethod, String, MethodDescriptor)] = {
        Some((instruction.bootstrapMethod, instruction.name, instruction.methodDescriptor))
    }
}

/**
 * Represents an "incomplete" invoke dynamic instruction. Here, incomplete refers
 * to the fact that not all information is yet available because it is not
 * yet loaded. In case of `invokedynamic` instructions it is necessary
 * to read a class file's attributes which are read in at the very end. This requires
 * to resolve INVOKEDYNAMIC instructions in a two step process.
 *
 * @author Michael Eichberg
 */
case object INCOMPLETE_INVOKEDYNAMIC extends INVOKEDYNAMIC {

    private def error: Nothing = {
        val message = "this invokedynamic is incomplete"
        throw new BytecodeProcessingFailedException(message)
    }

    final override def bootstrapMethod: BootstrapMethod = error

    final override def name: String = error

    final override def methodDescriptor: MethodDescriptor = error

}

/**
 * Represents an `invokedynamic` instruction where we have no further, immediately usable
 * information regarding the target.
 *
 * @param   bootstrapMethod This is the bootstrap method that needs to be executed in order
 *          to resolve the instruction's target.
 * @param   name This is the name of the method that this `invokedynamic` instruction intends
 *          to invoke.
 * @param   methodDescriptor This is the descriptor belonging to the instruction's intended
 *          invocation target.
 *
 * @author Arne Lottmann
 */
case class DEFAULT_INVOKEDYNAMIC(
        bootstrapMethod:  BootstrapMethod,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends INVOKEDYNAMIC {

    override def toString: String = {
        s"INVOKEDYNAMIC($bootstrapMethod, target=${methodDescriptor.toJava(name)})"
    }

}
