/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * A complete domain that performs all computations at the type level.
 *
 * This domain is called the zero domain as it represents the most basic configuration
 * that is useful for performing data-flow analyses.
 *
 * ==Example Usage==
 * {{{
 * class ZDomain extends { // we need the "early initializer"
 *      val project: SomeProject = theProject
 *      val code: Code = body
 * } with ZeroDomain with ThrowNoPotentialExceptionsConfiguration
 * }}}
 *
 * @author Michael Eichberg
 */
trait ZeroDomain
    extends TypeLevelDomain
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with TheProject
    with TheCode { domain: Configuration =>

}
