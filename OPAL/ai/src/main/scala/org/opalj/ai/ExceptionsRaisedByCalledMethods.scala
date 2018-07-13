/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Enumeration of how method calls are treated when the set of exceptions thrown by the target
 * method is not completely known.
 *
 * @author Michael Eichberg
 */
case object ExceptionsRaisedByCalledMethods extends Enumeration {

    /**
     * If no explicit information about the thrown exceptions by a method is available the
     * assumption is made that the called method may throw any exception.
     */
    final val Any = Value

    /**
     * If no information about the potentially thrown exceptions by a method is available the
     * assumption is made that a method is (at least) throwing those exceptions that are
     * explicitly handled by the calling method.
     */
    final val AllExplicitlyHandled = Value

    /**
     * Only those exceptions are considered to be thrown by a method which are explicitly known
     * to be potentially raised by the called method. This may include exceptions that are not
     * explicitly handled by the calling method. However, if no explicit information is available,
     * then '''no exceptions''' will be thrown.
     */
    final val Known = Value
}
