/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

trait AlwaysSucceedingStackBasedBinaryArithmeticInstruction
    extends StackBasedBinaryArithmeticInstruction {

    final def nextInstructions(
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
