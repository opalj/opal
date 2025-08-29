/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.VariableContext

object StringUtil {

    /**
     * Returns a regex that models all strings the given value might evaluate to
     * Clients MUST handle dependencies where the depender is the given one and the dependee provides string constancy
     * information.
     */
    def getPossibleStrings(
        pc:       Int,
        value:    V,
        context:  Context,
        depender: Entity,
        stmts:    Array[Stmt[V]]
    )(
        implicit
        ps:    PropertyStore,
        state: TypeIteratorState
    ): Option[StringTreeNode] = {
        val stringEOptP = ps(VariableContext(pc, value.toPersistentForm(stmts), context), StringConstancyProperty.key)
        if (stringEOptP.isRefinable) {
            state.addDependency(depender, stringEOptP)
        }

        stringEOptP match {
            case UBP(ub) => Some(ub.tree)
            case _       => None
        }
    }

    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies local use of String constants.
     */
    def getPossibleStrings[ContextType <: Context](
        value: Expr[V],
        stmts: Array[Stmt[V]]
    ): Option[Set[String]] = {
        Some(value.asVar.definedBy.map[Set[String]] { index =>
            if (index >= 0) {
                getString(index, stmts) match {
                    case Some(v) => Set(v)
                    case _       =>
                        return None;
                }
            } else {
                return None;
            }
        }.flatten)
    }

    private def getString(stringDefSite: Int, stmts: Array[Stmt[V]]): Option[String] = {
        stmts(stringDefSite).asAssignment.expr match {
            case StringConst(_, v) => Some(v)
            case _                 => None
        }
    }
}
