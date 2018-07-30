/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Exception raised while the analysis is executed.
 *
 * @author Michael Eichberg
 */
case class AnalysisException(
        message: String,
        cause:   Throwable = null
) extends RuntimeException(message, cause)
