/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * @author Michael Eichberg
 */
trait IConstInstruction extends LoadConstantInstruction[Int] with ImplicitValue {

    final override def computationalType = ComputationalTypeInt
}
object IConstInstruction {

    def unapply(instr: IConstInstruction): Option[Int] = Some(instr.value)

}
