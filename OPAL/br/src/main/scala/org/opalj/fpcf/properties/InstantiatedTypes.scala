/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectLike
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1

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
        if ((other ne AllTypes) && !types.subsetOf(other.types)) {
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

    def initial(types: UIDSet[ObjectType]): InstantiatedTypes = {
        new InstantiatedTypes(types.toList ++ initialTypes.toList, types ++ initialTypes)
    }

    final val key: PropertyKey[InstantiatedTypes] = {
        PropertyKey.create[ProjectLike, InstantiatedTypes](
            "InstantiatedTypes",
            (_: PropertyStore, reason: FallbackReason, _: ProjectLike) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoTypes
                case PropertyIsNotComputedByAnyAnalysis               ⇒ AllTypes
            },
            (_, eps: EPS[ProjectLike, InstantiatedTypes]) ⇒ eps.ub,
            (_: PropertyStore, _: ProjectLike) ⇒ None
        )
    }

    def initialTypes: UIDSet[ObjectType] = {
        // TODO make this configurable
        UIDSet1(ObjectType.String)
    }
}

// todo we can not use null here, so we need special handling
object AllTypes extends InstantiatedTypes(null, null) {
    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {}

    override def updated(newTypes: Set[ObjectType]): InstantiatedTypes = this

    override def toString: String = "AllTypesInstantiated"

    override def getNewTypes(index: Int): Iterator[ObjectType] = Iterator.empty

    override def numElements: Int = throw new UnsupportedOperationException()
}

object NoTypes extends InstantiatedTypes(List.empty, UIDSet.empty)
