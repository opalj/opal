/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Thrown if a context object is requested, but could not be found.
 *
 * Context objects are generally totally unrelated to entities and properties. They just store
 * information that may be required by analyses using the property store. In general, an analysis
 * should always be able to compute the properties if the context object is not
 * available; e.g., by using a fall back value. However, in case of `Project` based analyses
 * the context will always contain the project.
 *
 * @author Michael Eichberg
 */
case class ContextNotAvailableException(
        context:         AnyRef,
        completeContext: Map[Class[_], AnyRef]
) extends RuntimeException(
    completeContext.keys.mkString(s"unknown context $context; available: ", ", ", "")
)
