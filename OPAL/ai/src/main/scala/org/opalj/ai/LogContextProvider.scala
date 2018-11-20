/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext

/**
 * Provides log context information.
 *
 * @author Michael Eichberg
 */
trait LogContextProvider {

    implicit def logContext: LogContext
}

trait GlobalLogContextProvider extends LogContextProvider {

    implicit def logContext: LogContext = GlobalLogContext

}
