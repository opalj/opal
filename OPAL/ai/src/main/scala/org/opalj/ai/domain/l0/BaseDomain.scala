/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * A complete domain that performs all computations at the type level.
 *
 * @note    This domain is intended to be used for '''demo purposes only'''.
 *          '''Tests should create their own domains to make sure that
 *          the test results remain stable. The configuration of this
 *          domain just reflects a reasonable configuration that may
 *          change without further notice.'''
 *
 * @author Michael Eichberg
 */
class BaseDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends TypeLevelDomain
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with DefaultTypeLevelHandlingOfMethodResults
    with TheProject
    with TheMethod

object BaseDomain {

    /**
     * @tparam Source The type of the underlying source files (e.g., java.net.URL)
     * @return A new instance of a `BaseDomain`.
     */
    def apply[Source](project: Project[Source], method: Method): BaseDomain[Source] = {
        new BaseDomain(project, method)
    }

}

/**
 * Configuration of a domain that uses the `l0` domains and
 * which also records the abstract-interpretation time control flow graph and def/use
 * information.
 * @tparam Source The source file's type.
 */
class BaseDomainWithDefUse[Source](
        project: Project[Source],
        method:  Method
) extends BaseDomain[Source](project, method) with RecordDefUse
