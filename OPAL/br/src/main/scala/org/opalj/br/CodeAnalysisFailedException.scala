/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Exception that is thrown when an analysis of a method's implementation has failed.
 *
 * @param message The message that describes the reason.
 * @param code The code block for which the analysis failed.
 * @param pc The program counter of the last instruction that was tried to analyze.
 *
 * @author Michael Eichberg
 */
case class CodeAnalysisFailedException(
        message: String,
        code:    Code,
        pc:      PC
) extends RuntimeException(message)

