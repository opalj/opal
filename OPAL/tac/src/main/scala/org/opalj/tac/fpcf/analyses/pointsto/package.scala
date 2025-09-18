/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.analyses.SimpleContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.fpcf.Entity
import org.opalj.tac.common.DefinitionSites

package object pointsto {

    /**
     * Given a definition site PC (value origin) in a certain method, this returns the
     * entity to be used to attach/retrieve points-to information from.
     */
    def toEntity(
        defSitePC: Int,
        context:   Context
    )(
        implicit
        formalParameters: VirtualFormalParameters,
        definitionSites:  DefinitionSites,
        contextProvider:  ContextProvider
    ): Entity = {
        val entity = if (ai.isMethodExternalExceptionOrigin(defSitePC)) {
            val pc = ai.pcOfMethodExternalException(defSitePC)
            CallExceptions(definitionSites(context.method, pc))
        } else if (ai.isImmediateVMException(defSitePC)) {
            val pc = ai.pcOfImmediateVMException(defSitePC)
            definitionSites(context.method, pc)
        } else if (defSitePC < 0) {
            formalParameters.apply(context.method)(-1 - defSitePC)
        } else {
            definitionSites(context.method, defSitePC)
        }
        contextProvider match {
            case _: SimpleContextProvider => entity
            case _                        => (context, entity)
        }
    }
}
