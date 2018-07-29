/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * A general, non-recoverable exception occurred during the abstract interpretation
 * of a method.
 *
 * @param message A short message describing the exception. Can be `null`.
 * @param cause The root cause. Can be `null`.
 *
 * @author Michael Eichberg
 */
class AIException(
        message:            String    = null,
        cause:              Throwable = null,
        enableSuppression:  Boolean   = false,
        writableStackTrace: Boolean   = true
) extends RuntimeException(message, cause, enableSuppression, writableStackTrace)
