/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.br.instructions.Instruction

/**
 * Common interface of all elements that have (at most one) sequence of instructions.
 *
 * @author Michael Eichberg
 */
trait InstructionsContainer {

    def instructionsOption: Option[Array[Instruction]]

}

