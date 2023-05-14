/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait FieldAccessInformationPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = FieldAccessInformation
}

/**
 * Describes all read and write accesses to a [[org.opalj.br.Field]].
 *
 * @author Maximilian Rüsch
 */
case class FieldAccessInformation(
        readAccesses:  Set[(Method, PCs)],
        writeAccesses: Set[(Method, PCs)]
) extends OrderedProperty
    with FieldAccessInformationPropertyMetaInformation {

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldAccessInformation): Unit = {
        if (!readAccesses.subsetOf(other.readAccesses) || !writeAccesses.subsetOf(other.writeAccesses)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    final def key: PropertyKey[FieldAccessInformation] = FieldAccessInformation.key
}

object FieldAccessInformation extends FieldAccessInformationPropertyMetaInformation {

    /**
     * The key associated with every field access property. The name is "FieldAccessInformation"; the fallback is
     * computed via the fallback reason to ensure an analysis for the information was scheduled.
     */
    final val key = PropertyKey.create[Field, FieldAccessInformation](
        "FieldAccessInformation",
        (_: PropertyStore, reason: FallbackReason, _: Entity) =>
            reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => NoFieldAccessInformation
                case _ =>
                    throw new IllegalStateException("No analysis is scheduled for property FieldAccessInformation")
            }
    )
}

object NoFieldAccessInformation
    extends FieldAccessInformation(Set.empty[(Method, PCs)], Set.empty[(Method, PCs)])
