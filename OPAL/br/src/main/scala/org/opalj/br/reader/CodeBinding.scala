/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.br.instructions.Instruction

/**
 *
 * @author Michael Eichberg
 */
trait CodeBinding {

    type Instructions = Array[Instruction]

}

