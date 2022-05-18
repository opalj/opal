/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package cg

import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.ObjectType

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
        final val orderedClasses: List[ObjectType],
        final val classes:        UIDSet[ObjectType]
) extends OrderedProperty with LoadedClassesMetaInformation {

    assert(orderedClasses == null || orderedClasses.size == classes.size)

    override def checkIsEqualOrBetterThan(e: Entity, other: LoadedClasses): Unit = {
        if (other.classes != null && !classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    def updated(newClasses: IterableOnce[ObjectType]): LoadedClasses = {
        var updatedOrderedClasses = orderedClasses
        var updatedClasses = classes
        for { c <- newClasses.iterator } {
            val nextUpdatedClasses = updatedClasses + c
            if (nextUpdatedClasses ne updatedClasses /* <= used as a contains check */ ) {
                updatedOrderedClasses ::= c
                updatedClasses = nextUpdatedClasses
            }
        }
        new LoadedClasses(updatedOrderedClasses, updatedClasses)
    }

    /**
     * Will return the loaded classes added most recently, dropping the `num` oldest ones.
     */
    def dropOldest(num: Int): Iterator[ObjectType] = {
        orderedClasses.iterator.take(classes.size - num)
    }

    def size: Int = classes.size

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
            (ps: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                    OPALLogger.error("call graph analysis", "there was no class loaded")(ps.logContext)
                    NoLoadedClasses
                case _ =>
                    throw new IllegalStateException(s"analysis required for property: $name")
            }
        )
    }
}
