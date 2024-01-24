/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Pseudo instruction that generates an entry in the [[org.opalj.br.LineNumberTable]] with the
 * program counter of the following instruction.
 *
 * @author Malte Limmeroth
 */
case class LINENUMBER(lineNumber: Int) extends PseudoInstruction {

    override final def isExceptionHandlerElement: Boolean = false

    override final def isTry: Boolean = false

    override final def isCatch: Boolean = false

}
