/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SystemProperties

object StringUtil {

    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies local use of String constants.
     */
    def getPossibleStrings[ContextType <: Context](
        value: Expr[V],
        stmts: Array[Stmt[V]]
    )(
        implicit
        state:          TypeIteratorState,
        ps:             PropertyStore,
        classHierarchy: ClassHierarchy
    ): Option[Set[String]] = {
        Some(value.asVar.definedBy.map[Set[String]] { index =>
            getString(index, stmts) match {
                case Some(v) => Set(v)
                case _ =>
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
        typeIterator:   TypeIterator,
        state:          TypeIteratorState,
        ps:             PropertyStore,
        classHierarchy: ClassHierarchy
    ): Set[String] = {
        var strings = Set.empty[String]

        AllocationsUtil.handleAllocations(
            value, context, depender, stmts, _ eq ObjectType.String, failure
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

    private[reflection] def getString(
        stringDefSite: Int,
        stmts:         Array[Stmt[V]]
    )(
        implicit
        state:          TypeIteratorState,
        ps:             PropertyStore,
        classHierarchy: ClassHierarchy
    ): Option[String] = {
        if (stringDefSite >= 0) {
            val expr = stmts(stringDefSite).asAssignment.expr
            expr match {
                case StringConst(_, v) => Some(v)
                // TODO These don't return anything for now as the necessary dependency handling must be implemented first!
                case VirtualFunctionCall(_, declaringClass, _, name, _, _, params) if (name == "getProperty" || name == "get") && classHierarchy.isSubtypeOf(declaringClass, ObjectType("java/util/Properties")) =>
                    getPossiblePropertyValues(params, stmts); None
                case StaticFunctionCall(_, ObjectType.System, _, "getProperty", _, params) =>
                    getPossiblePropertyValues(params, stmts); None
                case _ => None
            }
        } else {
            None
        }
    }

    private[this] def getPossiblePropertyValues(
        params: Seq[Expr[V]],
        stmts:  Array[Stmt[V]]
    )(
        implicit
        state:          TypeIteratorState,
        ps:             PropertyStore,
        classHierarchy: ClassHierarchy
    ): Option[Set[String]] = {
        val possibleKeys = getPossibleStrings(params.head, stmts) // TODO Use points-to based method

        if (possibleKeys.isDefined) {
            val defaultValue = if (params.size == 2)
                getPossibleStrings(params(1), stmts).getOrElse(Set.empty) // TODO Use points-to based method
            else
                Set.empty

            val properties: EOptionP[SomeProject, SystemProperties] =
                ps(ps.context(classOf[SomeProject]), SystemProperties.key)

            if (properties.isRefinable)
                state.addDependency(null /*TODO*/ , properties)

            val propertyValues = properties match {
                case UBP(ub) => possibleKeys.get.flatMap(key => ub.properties(key))
                case _       => Set.empty
            }

            Some(propertyValues ++ defaultValue)
        } else
            None
    }
}
