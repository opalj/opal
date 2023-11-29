/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.fieldaccess.IncompleteFieldAccesses
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil
import org.opalj.tac.fpcf.analyses.cg.reflection.TypesUtil

object MatcherUtil {

    /**
     * Given an optional value of type `A` and a `factory` method for a [[FieldMatcher]],
     * it creates a method matcher (using the factory) if the value in `v` is defined.
     * Otherwise, depending on the project setting, it returns an [[AllFieldsMatcher]] or marks
     * the `pc` as incomplete.
     */
    private[reflection] def retrieveSuitableMatcher[A](
        v:       Option[A],
        pc:      Int,
        factory: A => FieldMatcher
    )(
        implicit
        incompleteFieldAccesses: IncompleteFieldAccesses,
        highSoundness:           Boolean
    ): FieldMatcher = {
        if (v.isEmpty) {
            if (highSoundness) {
                AllFieldsMatcher
            } else {
                incompleteFieldAccesses.addIncompleteAccessSite(pc)
                NoFieldsMatcher
            }
        } else {
            factory(v.get)
        }
    }

    /**
     * Given an optional value of type `A` and a `factory` method for a [[FieldMatcher]], it either creates a matcher,
     * using the factory and the value in `v` or returns the [[AllFieldsMatcher]].
     */
    private[reflection] def retrieveSuitableNonEssentialMatcher[A](
        v: Option[A],
        f: A => FieldMatcher
    ): FieldMatcher = if (v.isEmpty) AllFieldsMatcher else f(v.get)

    /**
     * Given an expression that evaluates to a String, creates a FieldMatcher to match fields with
     * the respective name.
     * Clients MUST handle dependencies where the depender is the given one and the dependee
     * provides allocation sites of Strings to be used as the method name.
     */
    private[reflection] def retrieveNameBasedFieldMatcher(
        context:  Context,
        expr:     V,
        depender: Entity,
        pc:       Int,
        stmts:    Array[Stmt[V]],
        failure:  () => Unit
    )(
        implicit
        typeIterator:            TypeIterator,
        state:                   TypeIteratorState,
        ps:                      PropertyStore,
        highSoundness:           Boolean,
        incompleteFieldAccesses: IncompleteFieldAccesses
    ): FieldMatcher = {
        val names = StringUtil.getPossibleStrings(expr, context, depender, stmts, failure)
        retrieveSuitableMatcher[Set[String]](
            Some(names),
            pc,
            v => new NameBasedFieldMatcher(v)
        )
    }

    /**
     * Given an expression that evaluates to a Class<?> object, creates a MethodMatcher to match
     * methods which are defined on the respective class.
     * Clients MUST handle TWO types of dependencies:
     * - One where the depender is the given one and the dependee provides allocation sites of Class
     * objects on which the method in question is defined AND
     * - One where the depender is a tuple of the given depender and the String "getPossibleTypes"
     * and the dependee provides allocation sites of Strings that give class names of such classes
     */
    private[reflection] def retrieveClassBasedFieldMatcher(
        context:                  Context,
        ref:                      V,
        depender:                 Entity,
        pc:                       Int,
        stmts:                    Array[Stmt[V]],
        project:                  SomeProject,
        failure:                  () => Unit,
        onlyFieldsExactlyInClass: Boolean,
        onlyObjectTypes:          Boolean        = false,
        considerSubclasses:       Boolean        = false
    )(
        implicit
        typeIterator:            TypeIterator,
        state:                   TypeIteratorState,
        ps:                      PropertyStore,
        highSoundness:           Boolean,
        incompleteFieldAccesses: IncompleteFieldAccesses
    ): FieldMatcher = {
        val typesOpt = Some(TypesUtil.getPossibleClasses(
            context, ref, depender, stmts, failure, onlyObjectTypes
        ).flatMap { tpe =>
            if (considerSubclasses) project.classHierarchy.allSubtypes(tpe.asObjectType, reflexive = true)
            else Set(if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object)
        })

        retrieveSuitableMatcher[Set[ObjectType]](
            typesOpt,
            pc,
            v => new ClassBasedFieldMatcher(v, onlyFieldsExactlyInClass)
        )
    }
}
