/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.{ClassHierarchy => BRClassHierarchy}
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStore
import org.opalj.log.LogContext

/**
 * Provides information about the underlying project.
 *
 * ==Usage==
 * If a (partial-) domain needs information about the project declare a corresponding
 * self-type dependency.
 * {{{
 * trait MyIntegerValuesDomain extends IntegerValues { this : TheProject =>
 * }}}
 *
 * ==Providing Information about a Project==
 * A domain that provides information about the currently analyzed project should inherit
 * from this trait and implement the respective method.
 *
 * ==Core Properties==
 *  - Defines the public interface.
 *  - Makes the analyzed [[org.opalj.br.analyses.Project]] available.
 *  - Thread safe.
 *
 * @note '''It is recommended that the domain that provides the project information
 *      does not use the `override` access flag.'''
 *      This way the compiler will issue a warning if two implementations are used
 *      to create a final domain.
 *
 * @author Michael Eichberg
 */
trait TheProject extends ThePropertyStore with LogContextProvider {

    /**
     * Returns the project that is currently analyzed.
     */
    implicit def project: SomeProject

    override implicit final def logContext: LogContext = project.logContext

    /**
     * Returns the project's class hierarchy.
     */
    @inline implicit final def classHierarchy: BRClassHierarchy = project.classHierarchy

    override implicit final lazy val propertyStore: PropertyStore = project.get(PropertyStoreKey)
}
