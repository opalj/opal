/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.FieldTypes
import org.opalj.br.cfg.CFG

object MatcherUtil {
    private[reflection] def retrieveClassBasedMethodMatcher(
        ref:                       Expr[V],
        pc:                        Int,
        stmts:                     Array[Stmt[V]],
        project:                   SomeProject,
        onlyMethodsExactlyInClass: Boolean
    )(
        implicit
        incompleteCallSites: IncompleteCallSites,
        highSoundness:       Boolean
    ): MethodMatcher = {
        val typesOpt =
            TypesUtil.getPossibleTypes(ref, pc, stmts, project).map(_.map(_.asObjectType)).map(_.toSet)

        retrieveSuitableMatcher[Set[ObjectType]](
            typesOpt,
            pc,
            v ⇒ new ClassBasedMethodMatcher(v, onlyMethodsExactlyInClass)
        )
    }

    /**
     * Given an optional value of type `A` and a `factory` method for a [[MethodMatcher]],
     * it creates a method matcher (using the factory) if the value in `v` is defined.
     * Otherwise, depending on the project setting, it returns an [[AllMethodsMatcher]] or marks
     * the `pc` as incomplete.
     */
    private[reflection] def retrieveSuitableMatcher[A](
        v:       Option[A],
        pc:      Int,
        factory: A ⇒ MethodMatcher
    )(
        implicit
        incompleteCallSites: IncompleteCallSites,
        highSoundness:       Boolean
    ): MethodMatcher = {
        if (v.isEmpty) {
            if (highSoundness) {
                AllMethodsMatcher
            } else {
                incompleteCallSites.addIncompleteCallSite(pc)
                NoMethodsMatcher
            }
        } else {
            factory(v.get)
        }
    }

    /**
     * Given an optional value of type `A` and a `factory` method for a [[MethodMatcher]],
     * it either creates a mather, using the factory and the value in `v` or returns the
     * [[AllMethodsMatcher]].
     */
    private[reflection] def retrieveSuitableNonEssentialMatcher[A](
        v: Option[A],
        f: A ⇒ MethodMatcher
    ): MethodMatcher = {
        if (v.isEmpty) {
            AllMethodsMatcher
        } else {
            f(v.get)
        }
    }

    private[reflection] def retrieveParameterTypesBasedMethodMatcher(
        varArgs: Expr[V],
        pc:      Int,
        stmts:   Array[Stmt[V]],
        cfg:     CFG[Stmt[V], TACStmts[V]]
    )(implicit incompleteCallSites: IncompleteCallSites, highSoundness: Boolean): MethodMatcher = {
        val paramTypesO = VarargsUtil.getTypesFromVararg(varArgs, stmts, cfg)
        retrieveSuitableMatcher[FieldTypes](
            paramTypesO,
            pc,
            v ⇒ new ParameterTypesBasedMethodMatcher(v)
        )
    }

    private[reflection] def retrieveNameBasedMethodMatcher(
        expr:  Expr[V],
        pc:    Int,
        stmts: Array[Stmt[V]]
    )(implicit incompleteCallSites: IncompleteCallSites, highSoundness: Boolean): MethodMatcher = {
        val namesO = StringUtil.getPossibleStrings(expr, Some(pc), stmts)
        retrieveSuitableMatcher[Set[String]](
            namesO,
            pc,
            v ⇒ new NameBasedMethodMatcher(v)
        )
    }

    private[reflection] val constructorMatcher = new NameBasedMethodMatcher(Set("<init>"))
}
