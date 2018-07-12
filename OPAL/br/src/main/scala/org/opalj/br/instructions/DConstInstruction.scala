/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * @author Michael Eichberg
 */
trait DConstInstruction extends LoadConstantInstruction[Double] with ImplicitValue {

    final override def computationalType = ComputationalTypeDouble

}
object DConstInstruction {

    def unapply(instr: DConstInstruction): Option[Double] = Some(instr.value)

}
