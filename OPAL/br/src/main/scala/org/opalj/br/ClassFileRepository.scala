/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.log.LogContext

/**
 * Enables the lookup of [[ClassFile]]s.
 *
 * @author Michael Eichberg
 */
trait ClassFileRepository {

    implicit def logContext: LogContext

    def classFile(objectType: ObjectType): Option[ClassFile]

}
