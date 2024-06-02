/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.ClassType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.SContext

object StringUtil {

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

    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies String constants.
     * Clients MUST handle dependencies where the depender is the given one and the dependee
     * provides allocation sites of Strings.
     */
    def getPossibleStrings[ContextType <: Context](
        value:    V,
        context:  ContextType,
        depender: Entity,
        stmts:    Array[Stmt[V]],
        failure:  () => Unit
    )(
        implicit
        typeIterator: TypeIterator,
        state:        TypeIteratorState,
        ps:           PropertyStore
    ): Set[String] = {
        var strings = Set.empty[String]

        AllocationsUtil.handleAllocations(
            value,
            context,
            depender,
            stmts,
            _ eq ClassType.String,
            failure
        ) { (_, defSite, _stmts) =>
            getString(defSite, _stmts) match {
                case Some(v) =>
                    strings += v
                case _ =>
                    failure()
            }
        }

        strings
    }

    /**
     * Returns a regex that models all strings the given value might evaluate to
     * Clients MUST handle dependencies where the depender is the given one and the dependee provides string constancy
     * information.
     */
    def getPossibleStringsRegex(
          pc: Int,
          value: V,
          context: Context,
          stmts: Array[Stmt[V]]
    )(
          implicit
          ps: PropertyStore
    ): String = {
        val entity: SContext = (pc, value.toPersistentForm(stmts), context.method.definedMethod)

        ps(entity, StringConstancyProperty.key) match {
            case UBP(ub) => ub.sci.toRegex
            case _ => ""
        }
    }

    def getString(stringDefSite: Int, stmts: Array[Stmt[V]]): Option[String] = {
        val expr = stmts(stringDefSite).asAssignment.expr
        expr match {
            case StringConst(_, v) => Some(v)
            case _                 => None
        }
    }
}
