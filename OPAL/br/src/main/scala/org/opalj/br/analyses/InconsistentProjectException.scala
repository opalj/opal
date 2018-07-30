/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.log.Level
import org.opalj.log.Warn

/**
 * Thrown when the framework determines that the project is not consistent.
 *
 * @author Michael Eichberg
 */
case class InconsistentProjectException(
        message:  String,
        severity: Level  = Warn
) extends Exception(message)
