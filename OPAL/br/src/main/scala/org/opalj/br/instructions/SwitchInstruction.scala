/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Extractor for SwitchInstructions.
 *
 * @author Michael Eichberg
 */
object SwitchInstruction {

    /**
     * Extracts the default offset and the jump offsets (RELATIVE - NOT ABSOLUTE!).
     */
    def unapply(i: CompoundConditionalBranchInstruction): Some[(Int, Iterable[Int])] = {
        Some((i.defaultOffset, i.jumpOffsets))
    }

}
