/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.tac.common.DefinitionSites

package object pointsto {

    def toEntity(
        defSite: Int, method: DefinedMethod, stmts: Array[Stmt[DUVar[ValueInformation]]]
    )(
        implicit
        formalParameters: VirtualFormalParameters, definitionSites: DefinitionSites
    ): Entity = {
        if (defSite < 0) {
            formalParameters.apply(method)(-1 - defSite)
        } else {
            definitionSites(method.definedMethod, stmts(defSite).pc)
        }
    }
}
