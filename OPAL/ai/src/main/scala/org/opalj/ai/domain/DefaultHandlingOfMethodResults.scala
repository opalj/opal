/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * A `Domain` that does nothing if a method returns ab-/normally.
 *
 * @note This trait's methods are generally not intended to be overridden.
 *      If you need to do some special processing just directly implement
 *      the respective method and mixin the traits that ignore the rest.
 *
 * @author Michael Eichberg
 */
trait DefaultHandlingOfMethodResults
    extends DefaultHandlingForThrownExceptions
    with DefaultHandlingOfVoidReturns
    with DefaultHandlingForReturnInstructions {
    domain: ValuesDomain with ExceptionsFactory with Configuration =>

}

