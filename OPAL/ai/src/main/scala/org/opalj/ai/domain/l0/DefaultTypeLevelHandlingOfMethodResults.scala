/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * A `Domain` that does nothing if a method returns ab-/normally.
 *
 * @note This trait's methods are generally not intended to be overridden.
 *      If you need to do some special processing just directly implement
 *      the respective method and mixin the traits that ignore the rest.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelHandlingOfMethodResults
    extends DefaultTypeLevelHandlingForThrownExceptions
    with DefaultTypeLevelHandlingOfVoidReturns
    with DefaultTypeLevelHandlingForReturnInstructions {
    domain: ValuesDomain with ExceptionsFactory with Configuration =>

}

