/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.LongLinkedSet
import org.opalj.collection.immutable.LongLinkedTrieSet
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
        private val encodedReadAccesses:  LongLinkedSet,
        private val encodedWriteAccesses: LongLinkedSet
) extends OrderedProperty
    with FieldAccessInformationPropertyMetaInformation {

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldAccessInformation): Unit = {
        if (numReadAccesses > other.numReadAccesses || numWriteAccesses > other.numWriteAccesses) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    final def key: PropertyKey[FieldAccessInformation] = FieldAccessInformation.key

    def readAccesses(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedReadAccesses.iterator.map(decodeFieldAccess(_))

    def writeAccesses(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedWriteAccesses.iterator.map(decodeFieldAccess(_))

    def getNewestNReadAccesses(n: Int)(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedReadAccesses.iterator.take(n).map(decodeFieldAccess(_))

    def getNewestNWriteAccesses(n: Int)(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedWriteAccesses.iterator.take(n).map(decodeFieldAccess(_))

    def numReadAccesses: Int = encodedReadAccesses.size

    def numWriteAccesses: Int = encodedWriteAccesses.size

    def included(other: FieldAccessInformation): FieldAccessInformation = included(other, 0, 0)

    def included(
        other:             FieldAccessInformation,
        seenReadAccesses:  Int,
        seenWriteAccesses: Int
    ): FieldAccessInformation = {
        var newReadAccesses = encodedReadAccesses
        other.encodedReadAccesses.forFirstN(other.numReadAccesses - seenReadAccesses)(newReadAccesses += _)
        var newWriteAccesses = encodedWriteAccesses
        other.encodedWriteAccesses.forFirstN(other.numWriteAccesses - seenWriteAccesses)(newWriteAccesses += _)

        if (newReadAccesses == encodedReadAccesses && newWriteAccesses == encodedWriteAccesses)
            return this;

        FieldAccessInformation(newReadAccesses, newWriteAccesses)
    }
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

    def apply(
        readAccesses:  Set[(DefinedMethod, PCs)],
        writeAccesses: Set[(DefinedMethod, PCs)]
    ): FieldAccessInformation = {
        def getEncodedAccessSet(accesses: Set[(DefinedMethod, PCs)]): LongLinkedTrieSet =
            accesses
              .flatMap(methodPCs => methodPCs._2.foldLeft(Set.empty[FieldAccess])(_ + encodeFieldAccess(methodPCs._1, _)))
              .foldLeft(LongLinkedTrieSet.empty)(_ + _)

        FieldAccessInformation(getEncodedAccessSet(readAccesses), getEncodedAccessSet(writeAccesses))
    }
}

object NoFieldAccessInformation extends FieldAccessInformation(LongLinkedTrieSet.empty, LongLinkedTrieSet.empty)
