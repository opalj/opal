/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.PC
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.fpcf.analyses.cg.SimpleContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

package object pointsto {

    @inline def longToAllocationSite(
        encodedAllocationSite: AllocationSite
    )(
        implicit
        typeIterator: TypeIterator
    ): (Context, PC, Int) /* method, pc, typeid */ = {
        val contextID = encodedAllocationSite.toInt & 0x3FFFFFF
        (
            typeIterator.contextFromId(if (contextID == 0x3FFFFFF) -1 else contextID),
            (encodedAllocationSite >> 26).toInt & 0x1FFFF,
            (encodedAllocationSite >> 44).toInt
        )
    }

    /**
     * Given a definition site (value origin) in a certain method, this returns the
     * entity to be used to attach/retrieve points-to information from.
     */
    def toEntity(
        defSite: Int, context: Context, stmts: Array[Stmt[DUVar[ValueInformation]]]
    )(
        implicit
        formalParameters: VirtualFormalParameters,
        definitionSites:  DefinitionSites,
        typeIterator:     TypeIterator
    ): Entity = {
        val entity = if (ai.isMethodExternalExceptionOrigin(defSite)) {
            val pc = ai.pcOfMethodExternalException(defSite)
            CallExceptions(definitionSites(context.method.definedMethod, stmts(pc).pc))
        } else if (ai.isImmediateVMException(defSite)) {
            val pc = ai.pcOfImmediateVMException(defSite)
            definitionSites(context.method.definedMethod, stmts(pc).pc)
        } else if (defSite < 0) {
            formalParameters.apply(context.method)(-1 - defSite)
        } else {
            definitionSites(context.method.definedMethod, stmts(defSite).pc)
        }
        typeIterator match {
            case _: SimpleContextProvider => entity
            case _                        => (context, entity)
        }
    }
}
