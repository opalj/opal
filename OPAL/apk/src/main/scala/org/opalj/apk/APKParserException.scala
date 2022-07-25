package org.opalj.apk

/**
 * Thrown if an error occurs in [[APKParser]].
 *
 * @author Nicolas Gross
 */
case class APKParserException(
        message: String,
        cause:   Throwable = null
) extends Exception(message, cause)
