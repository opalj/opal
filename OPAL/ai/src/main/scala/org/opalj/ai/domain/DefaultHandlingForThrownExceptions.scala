/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Basic implementation of a `Domain`'s `abruptMethodExecution` method that does
 * nothing.
 *
 * @note Mix-in this trait if the analysis does not need to do anything special in case
 *      of an exception or if you have multiple stackable traits and you need a base
 *      implementation.
 *      Example:
 *      {{{
 *      MySpecialDomain
 *          extends ...
 *          with DefaultHandlingForThrownExceptions
 *          with RecordThrownExceptions
 *          with ...
 *      }}}
 *
 * @author Michael Eichberg
 */
trait DefaultHandlingForThrownExceptions extends ReturnInstructionsDomain {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    /*base impl.*/ def abruptMethodExecution(pc: Int, exception: ExceptionValue): Unit = {
        /* Nothing to do. */
    }
}

