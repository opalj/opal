/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An efficient (i.e., no (un)boxing...) representation of an instruction (identified)
 * by its pc in a method.
 *
 * @param pc The program counter of an instruction.
 * @param method The declaring method.
 *
 * @author Michael Eichberg
 */
/*no case class!*/ final class PCInMethod(val method: Method, val pc: Int /* PC */ ) {

    override def hashCode(): Opcode = method.hashCode() * 113 + pc

    override def equals(other: Any): Boolean = {
        other match {
            case that: PCInMethod => that.pc == this.pc && that.method == this.method
            case _                => false
        }

    }

    override def toString: String = s"PCInMethod(method=${method.toJava},pc=$pc)"
}

object PCInMethod {
    def apply(method: Method, pc: Int): PCInMethod = new PCInMethod(method, pc)
}
