/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.immutable.Chain

trait AlwaysSucceedingStackBasedBinaryArithmeticInstruction
    extends StackBasedBinaryArithmeticInstruction {

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        Chain.singleton(indexOfNextInstruction(currentPC))
    }

}
