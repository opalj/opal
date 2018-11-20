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

    final override def isExceptionHandlerElement: Boolean = false

    final override def isTry: Boolean = false

    final override def isCatch: Boolean = false

}
