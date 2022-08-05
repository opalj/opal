/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

/**
 * Thrown if an error occurs in [[ApkParser]].
 *
 * @author Nicolas Gross
 */
case class ApkParserException(
        message: String,
        cause:   Throwable = null
) extends Exception(message, cause)
