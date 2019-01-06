/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UIDSet

sealed trait LoadedClassesMetaInformation extends PropertyMetaInformation {
    final type Self = LoadedClasses
}

/**
 * Represent the set of types (classes) that were loaded by the VM during execution of the
 * respective [[org.opalj.br.analyses.Project]] (which is the entity for this property).
 *
 * @author Florian Kübler
 */
sealed class LoadedClasses private[properties] (
        final val orderedClasses: List[ObjectType],
        final val classes:        UIDSet[ObjectType]
) extends OrderedProperty with LoadedClassesMetaInformation {

    assert(orderedClasses == null || orderedClasses.size == classes.size)

    override def checkIsEqualOrBetterThan(e: Entity, other: LoadedClasses): Unit = {
        if (other.classes != null && !classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    def updated(newClasses: Set[ObjectType]): LoadedClasses = {
        var updatedOrderedClasses = orderedClasses
        var updatedClasses = classes
        for { c ← newClasses } {
            val nextUpdatedClasses = updatedClasses + c
            if (nextUpdatedClasses ne updatedClasses /* <= used as a contains check */ ) {
                updatedOrderedClasses ::= c
                updatedClasses = nextUpdatedClasses
            }
        }
        new LoadedClasses(updatedOrderedClasses, updatedClasses)
    }

    // TODO Rename "take" (document that always the newest one(s) will be taken.
    // TODO Consider adding/using a bounded ForeachIterator?
    def getNewClasses(index: Int): Iterator[ObjectType] = {
        orderedClasses.iterator.take(classes.size - index)
    }

    // TODO Rename "size"
    def numElements: Int = classes.size

    override def key: PropertyKey[LoadedClasses] = LoadedClasses.key

    override def toString: String = s"LoadedClasses(${classes.size})"
}

object NoLoadedClasses extends LoadedClasses(classes = UIDSet.empty, orderedClasses = List.empty)

object LoadedClasses extends LoadedClassesMetaInformation {

    def apply(classes: UIDSet[ObjectType]): LoadedClasses = {
        new LoadedClasses(classes.toList, classes)
    }

    final val key: PropertyKey[LoadedClasses] = {
        val name = "opalj.LoadedClasses"
        PropertyKey.create(
            name,
            // FIXME The following doesn't seem to make sense... there will always be at least on class, doesn't it?
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoLoadedClasses
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }
}
