/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that divides two primitive floating point values.
 *
 * @author Michael Eichberg
 */
abstract class FloatingPointDivideInstruction extends DivideInstruction {

    final override def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

}
