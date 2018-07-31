/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * @author Michael Eichberg
 */
trait FConstInstruction extends LoadConstantInstruction[Float] with ImplicitValue {

    final override def computationalType = ComputationalTypeFloat

}

object FConstInstruction {

    def unapply(instr: FConstInstruction): Option[Float] = Some(instr.value)

}
