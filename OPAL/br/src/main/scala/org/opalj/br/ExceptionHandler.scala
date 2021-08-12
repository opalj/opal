/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An entry in the exceptions table of a [[org.opalj.br.Code]] block.
 *
 * @param  startPC A valid index into the code array. It points to the first instruction
 *         in the "try-block" (inclusive).
 * @param  endPC An index into the code array that points to the instruction after the
 *         "try-block" (exclusive).
 * @param  handlerPC Points to the first instruction of the exception handler.
 * @param  catchType The type of the exception that is catched. `None` in case of
 *         a finally block.
 *
 * @author Michael Eichberg
 */
case class ExceptionHandler(
        startPC:   Int,
        endPC:     Int,
        handlerPC: Int,
        catchType: Option[ObjectType]
) {
    override def toString: String = {
        val exceptionType = catchType.map(_.toJava).getOrElse("<FINALLY>")
        s"ExceptionHandler([$startPC, $endPC) -> $handlerPC, $exceptionType)"
    }
}
