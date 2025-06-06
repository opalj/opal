/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection
package mutable

import org.opalj.br.ClassHierarchy
import org.opalj.br.ClassType

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * ==Thread Safety==
 * This class is not thread safe.
 *
 * @author Michael Eichberg
 */
class TypesSet(final val classHierarchy: ClassHierarchy) extends collection.TypesSet {

    import classHierarchy.isSubtypeOf

    protected[this] var theConcreteTypes: Set[ClassType] = Set.empty
    protected[this] var theUpperTypeBounds: Set[ClassType] = Set.empty

    /**
     * The set of concrete types which are not subtypes of any type which
     * is returned by `upperTypeBounds`.
     */
    final def concreteTypes: Set[ClassType] = theConcreteTypes
    final def upperTypeBounds: Set[ClassType] = theUpperTypeBounds

    def toImmutableTypesSet: immutable.TypesSet =
        immutable.TypesSet(theConcreteTypes, theUpperTypeBounds)(classHierarchy)

    def +=(tpe: ClassType): Unit = {
        if (!theConcreteTypes.contains(tpe) &&
            !theUpperTypeBounds.exists(utb => isSubtypeOf(tpe, utb))
        ) {
            theConcreteTypes += tpe
        }
    }

    def ++=(tpes: Iterable[ClassType]): Unit = tpes.foreach { += }

    def ++<:=(tpes: Iterable[ClassType]): Unit = tpes.foreach { +<:= }

    /**
     * Adds the given upper type bound to this `TypesSet` unless a supertype
     * of the given type is already added as an upper type bound.
     *
     * All subtypes – whether concrete or upper types bounds – are removed.
     */
    def +<:=(tpe: ClassType): Unit = {
        if (theConcreteTypes.contains(tpe)) {
            theConcreteTypes -= tpe
            theUpperTypeBounds =
                theUpperTypeBounds.filter(utb => !isSubtypeOf(utb, tpe)) + tpe
        } else {
            var doNotAddTPE: Boolean = false
            var newUpperTypeBounds = theUpperTypeBounds.filter { utb =>
                val keepExistingUTB = !isSubtypeOf(utb, tpe)
                if (keepExistingUTB && !doNotAddTPE && isSubtypeOf(tpe, utb)) {
                    doNotAddTPE = true
                }
                keepExistingUTB
            }
            theConcreteTypes = theConcreteTypes.filter { ct => !isSubtypeOf(ct, tpe) }
            if (!doNotAddTPE) newUpperTypeBounds += tpe
            theUpperTypeBounds = newUpperTypeBounds
        }
    }
}
