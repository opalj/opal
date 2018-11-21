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
 * @author Florian Kuebler
 */
sealed class LoadedClasses private[properties] (
        private val orderedClasses: List[ObjectType],
        val classes:                UIDSet[ObjectType]
) extends Property with OrderedProperty with LoadedClassesMetaInformation {

    assert(orderedClasses == null || orderedClasses.size == classes.size)

    override def checkIsEqualOrBetterThan(e: Entity, other: LoadedClasses): Unit = {
        if (other.classes != null && !classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    def updated(newClasses: Set[ObjectType]): LoadedClasses = {
        var newOrderedClasses = orderedClasses
        for { c ← newClasses if !classes.contains(c) } {
            newOrderedClasses ::= c
        }
        new LoadedClasses(newOrderedClasses, classes ++ newClasses)
    }

    def getNewClasses(index: Int): Iterator[ObjectType] = {
        orderedClasses.iterator.take(classes.size - index)
    }

    def numElements: Int = classes.size

    override def key: PropertyKey[LoadedClasses] = LoadedClasses.key

    override def toString: String = s"LoadedClasses(${classes.size})"
}

object NoLoadedClasses extends LoadedClasses(classes = UIDSet.empty, orderedClasses = List.empty)

object LoadedClasses extends LoadedClassesMetaInformation {

    // todo do we have "initial" loaded classes?
    def initial(
        classes: UIDSet[ObjectType]
    ): LoadedClasses = {
        new LoadedClasses(classes.toList, classes)
    }

    final val key: PropertyKey[LoadedClasses] = {
        PropertyKey.forSimpleProperty("LoadedClasses", NoLoadedClasses)
    }
}
