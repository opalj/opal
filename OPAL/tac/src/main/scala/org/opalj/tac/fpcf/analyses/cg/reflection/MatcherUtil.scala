/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.FieldTypes
import org.opalj.br.fpcf.properties.Context

object MatcherUtil {

    /**
     * Given an optional value of type `A` and a `factory` method for a [[MethodMatcher]],
     * it creates a method matcher (using the factory) if the value in `v` is defined.
     * Otherwise, depending on the project setting, it returns an [[AllMethodsMatcher]] or marks
     * the `pc` as incomplete.
     */
    private[reflection] def retrieveSuitableMatcher[A](
        v:       Option[A],
        pc:      Int,
        factory: A => MethodMatcher
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
        f: A => MethodMatcher
    ): MethodMatcher = {
        if (v.isEmpty) {
            AllMethodsMatcher
        } else {
            f(v.get)
        }
    }

    /**
     * Given the expression for a varargs array of types (i.e., Class<?> objects), creates a
     * MethodMatcher to match methods with the respective parameter types.
     */
    private[reflection] def retrieveParameterTypesBasedMethodMatcher(
        varArgs: Expr[V],
        pc:      Int,
        stmts:   Array[Stmt[V]]
    )(implicit incompleteCallSites: IncompleteCallSites, highSoundness: Boolean): MethodMatcher = {
        val paramTypesO = VarargsUtil.getTypesFromVararg(varArgs, stmts)
        retrieveSuitableMatcher[FieldTypes](
            paramTypesO,
            pc,
            v => new ParameterTypesBasedMethodMatcher(v)
        )
    }

    /**
     * Given an expression that evaluates to a String, creates a MethodMatcher to match methods with
     * the respective name.
     * Clients MUST handle dependencies where the depender is the given one and the dependee
     * provides allocation sites of Strings to be used as the method name.
     */
    private[reflection] def retrieveNameBasedMethodMatcher(
        context:  Context,
        expr:     V,
        depender: Entity,
        pc:       Int,
        stmts:    Array[Stmt[V]],
        failure:  () => Unit
    )(
        implicit
        typeProvider:        TypeProvider,
        state:               TypeProviderState,
        ps:                  PropertyStore,
        incompleteCallSites: IncompleteCallSites,
        highSoundness:       Boolean
    ): MethodMatcher = {
        val names = StringUtil.getPossibleStrings(expr, context, depender, stmts, failure)
        retrieveSuitableMatcher[Set[String]](
            Some(names),
            pc,
            v => new NameBasedMethodMatcher(v)
        )
    }

    private[reflection] val constructorMatcher = new NameBasedMethodMatcher(Set("<init>"))

    /**
     * Given an expression that evaluates to a Class<?> object, creates a MethodMatcher to match
     * methods which are defined on the respective class.
     * Clients MUST handle TWO types of dependencies:
     * - One where the depender is the given one and the dependee provides allocation sites of Class
     * objects on which the method in question is defined AND
     * - One where the depender is a tuple of the given depender and the String "getPossibleTypes"
     * and the dependee provides allocation sites of Strings that give class names of such classes
     */
    private[reflection] def retrieveClassBasedMethodMatcher(
        context:                   Context,
        ref:                       V,
        depender:                  Entity,
        pc:                        Int,
        stmts:                     Array[Stmt[V]],
        project:                   SomeProject,
        failure:                   () => Unit,
        onlyMethodsExactlyInClass: Boolean,
        onlyObjectTypes:           Boolean        = false,
        considerSubclasses:        Boolean        = false
    )(
        implicit
        typeProvider:        TypeProvider,
        state:               TypeProviderState,
        ps:                  PropertyStore,
        incompleteCallSites: IncompleteCallSites,
        highSoundness:       Boolean
    ): MethodMatcher = {
        val typesOpt = Some(TypesUtil.getPossibleClasses(
            context, ref, depender, stmts, project, failure, onlyObjectTypes
        ).flatMap { tpe =>
            if (considerSubclasses) project.classHierarchy.allSubtypes(tpe.asObjectType, true)
            else Set(if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object)
        })

        retrieveSuitableMatcher[Set[ObjectType]](
            typesOpt,
            pc,
            v => new ClassBasedMethodMatcher(v, onlyMethodsExactlyInClass)
        )
    }

}
