/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package cg

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.log.OPALLogger

sealed trait ForNameClassesMetaInformation extends PropertyMetaInformation {
    final type Self = ForNameClasses
}

/**
 * Represent the set of types (classes) that were loaded via a particular call to Class.forName.
 *
 * @author Dominik Helm
 */
sealed class ForNameClasses private[properties] (
    final val orderedClasses: List[ReferenceType],
    final val classes:        UIDSet[ReferenceType]
) extends OrderedProperty with ForNameClassesMetaInformation {

    assert(orderedClasses == null || orderedClasses.size == classes.size)

    override def checkIsEqualOrBetterThan(e: Entity, other: ForNameClasses): Unit = {
        if (other.classes != null && !classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    def updated(newClasses: IterableOnce[ReferenceType]): ForNameClasses = {
        var updatedOrderedClasses = orderedClasses
        var updatedClasses = classes
        for { c <- newClasses.iterator } {
            val nextUpdatedClasses = updatedClasses + c
            if (nextUpdatedClasses ne updatedClasses /* <= used as a contains check */ ) {
                updatedOrderedClasses ::= c
                updatedClasses = nextUpdatedClasses
            }
        }
        new ForNameClasses(updatedOrderedClasses, updatedClasses)
    }

    /**
     * Will return the loaded classes added most recently, dropping the `num` oldest ones.
     */
    def dropOldest(num: Int): Iterator[ReferenceType] = {
        orderedClasses.iterator.take(classes.size - num)
    }

    def size: Int = classes.size

    override def key: PropertyKey[ForNameClasses] = ForNameClasses.key

    override def toString: String = s"ForNameClasses(${classes.size})"
}

object NoForNameClasses extends ForNameClasses(classes = UIDSet.empty, orderedClasses = List.empty)

object ForNameClasses extends ForNameClassesMetaInformation {

    def apply(classes: UIDSet[ReferenceType]): ForNameClasses = {
        new ForNameClasses(classes.toList, classes)
    }

    final val key: PropertyKey[ForNameClasses] = {
        val name = "opalj.ForNameClasses"
        PropertyKey.create(
            name,
            (ps: PropertyStore, reason: FallbackReason, _: Entity) =>
                reason match {
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                        OPALLogger.error("call graph analysis", "no analysis executed for Class.forName")(ps.logContext)
                        NoForNameClasses
                    case _ =>
                        throw new IllegalStateException(s"analysis required for property: $name")
                }
        )
    }
}
