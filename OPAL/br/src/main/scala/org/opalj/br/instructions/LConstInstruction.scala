/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * @author Michael Eichberg
 */
trait LConstInstruction extends LoadConstantInstruction[Long] with ImplicitValue {

    final override def computationalType = ComputationalTypeLong

}
object LConstInstruction {

    def unapply(instr: LConstInstruction): Option[Long] = Some(instr.value)

}
