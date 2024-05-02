/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.ai.ValueOrigin
import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.alias.PersistentUVar
import org.opalj.tac.DUVar
import org.opalj.value.ValueInformation

package object alias {

    type V = DUVar[ValueInformation]

    type AllocationSite = (Context, PC)

    /**
     * Converts the given [[UVar]] into an equivalent [[PersistentUVar]].
     *
     * @param uVar The UVar to convert.
     * @param stmts The statements of the method the UVar is defined in.
     * @return A persistent UVar for the given UVar.
     */
    final def persistentUVar(uVar: UVar[ValueInformation])(implicit stmts: Array[Stmt[V]]): PersistentUVar = {
        PersistentUVar(uVar.value, uVar.definedBy.map(pcOfDefSite _))
    }

    /**
     * Returns the program counter of the given definition site.
     * @param valueOrigin The value origin of the definition site.
     * @param stmts The statements of the method the definition site is in.
     * @return The program counter of the definition site.
     */
    final def pcOfDefSite(valueOrigin: ValueOrigin)(implicit stmts: Array[Stmt[V]]): PC = {
        org.opalj.tac.fpcf.analyses.cg.pcOfDefSite(valueOrigin)
    }

}
