/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore

/**
 * Interprocedural control flow graph for Java projects used in IFDS Analysis
 *
 * @author Marc Clement
 */
trait JavaICFG extends org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG {
    protected val declaredMethods: DeclaredMethods
    protected val propertyStore: PropertyStore

    /**
     * Returns all methods, that can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @return All methods, that can be called from outside the library.
     */
    def methodsCallableFromOutside: Set[DeclaredMethod] = {
        declaredMethods.declaredMethods.filter(canBeCalledFromOutside).toSet
    }

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    def canBeCalledFromOutside(method: DeclaredMethod): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }

    def canBeCalledFromOutside(method: Method): Boolean =
        canBeCalledFromOutside(declaredMethods(method))
}
