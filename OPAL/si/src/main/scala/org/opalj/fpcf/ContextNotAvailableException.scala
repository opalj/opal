/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Thrown if a context object is requested, but could not be found.
 *
 * Context objects are generally unrelated to entities and properties. They just store
 * information that may be required by fixpoint computations executed using the property store.
 *
 * @note If the `org.opalj.br.ProjectInformationKey` is used to get the property store, the
 *       `Project` is stored in the context.
 *
 * @author Michael Eichberg
 */
case class ContextNotAvailableException(
        context:         AnyRef,
        completeContext: Map[Class[_], AnyRef]
) extends RuntimeException(
    completeContext.keys.mkString(s"unknown context $context; available: ", ", ", "")
)
