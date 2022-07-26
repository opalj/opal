/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection
package immutable

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * @author Dominik Helm
 */
case class TypesSet(
        final val concreteTypes:   Set[ObjectType],
        final val upperTypeBounds: Set[ObjectType]
)(implicit classHierarchy: ClassHierarchy) extends collection.TypesSet {

    import classHierarchy.isSubtypeOf

    def ++(tpes: Iterable[ObjectType]): TypesSet = {
        var newConcreteTypes = concreteTypes
        tpes foreach { tpe =>
            if (!newConcreteTypes.contains(tpe) &&
                !upperTypeBounds.exists(utb => isSubtypeOf(tpe, utb))) {
                newConcreteTypes += tpe
            }
        }
        TypesSet(newConcreteTypes, upperTypeBounds)
    }
}
