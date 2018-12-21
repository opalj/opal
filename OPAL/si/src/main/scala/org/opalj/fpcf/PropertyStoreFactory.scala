/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext

/**
 * Common interface implemented by all [[PropertyStore]] factories.
 *
 * @author Michael Eichberg
 */
trait PropertyStoreFactory {

    final def apply[T <: AnyRef](
        key:     Class[T],
        context: T
    )(
        implicit
        logContext: LogContext
    ): PropertyStore = {
        apply(PropertyStoreContext(key, context))
    }

    /* abstract */ def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PropertyStore
}
