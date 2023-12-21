/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An efficient (i.e., no (un)boxing...) representation of an instruction and a value.
 *
 * @author Michael Eichberg
 */
/* no case class */ final class PCAndAnyRef[T <: AnyRef](val pc: Int /* PC */ , val value: T) {

    override def hashCode(): Opcode = value.hashCode() * 117 + pc

    override def equals(other: Any): Boolean = {
        other match {
            case that: PCAndAnyRef[_] => this.pc == that.pc && this.value == that.value
            case _                    => false
        }
    }

    override def toString: String = s"PCAndAnyRef(pc=$pc,$value)"
}

object PCAndAnyRef {
    def apply[T <: AnyRef](pc: Int /* PC */ , value: T): PCAndAnyRef[T] = {
        new PCAndAnyRef(pc, value)
    }

    // TODO Figure out if the (un)boxing related to the matcher is relevant or optimized away by the JVM
    def unapply[T <: AnyRef](pcAndValue: PCAndAnyRef[T]): Some[(Int /* PC */ , T)] = {
        Some((pcAndValue.pc, pcAndValue.value))
    }
}
