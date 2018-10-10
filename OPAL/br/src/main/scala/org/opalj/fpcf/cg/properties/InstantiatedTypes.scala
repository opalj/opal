/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.collection.immutable.UIDSet

import scala.collection.Set

/**
 * Represent the set of types that have allocations reachable from the respective entry points.
 *
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = InstantiatedTypes
}

case class InstantiatedTypes private[properties] (
        private val orderedTypes: List[ObjectType],
        types:                    UIDSet[ObjectType]
) extends Property with OrderedProperty with InstantiatedTypesPropertyMetaInformation {

    assert(orderedTypes == null || orderedTypes.size == types.size)

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key

    override def toString: String = s"InstantiatedTypes(size=${types.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    def updated(newTypes: Set[ObjectType]): InstantiatedTypes = {

        var newOrderedTypes = orderedTypes
        for { t ← newTypes if !types.contains(t) } {
            newOrderedTypes ::= t
        }
        new InstantiatedTypes(newOrderedTypes, types ++ newTypes)
    }

    def getNewTypes(index: Int): Iterator[ObjectType] = {
        orderedTypes.iterator.take(types.size - index)
    }

    def numElements: Int = types.size
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def initial(types: UIDSet[ObjectType], project: SomeProject): InstantiatedTypes = {
        new InstantiatedTypes(
            types.toList ++ initialTypes(project).toList,
            types ++ initialTypes(project)
        )
    }

    final val key: PropertyKey[InstantiatedTypes] = {
        PropertyKey.forSimpleProperty("InstantiatedTypes", NoTypes)
    }

    def initialTypes(project: SomeProject): Traversable[ObjectType] = {
        project.get(InitialInstantiatedTypesKey)
    }
}

object NoTypes extends InstantiatedTypes(List.empty, UIDSet.empty)
