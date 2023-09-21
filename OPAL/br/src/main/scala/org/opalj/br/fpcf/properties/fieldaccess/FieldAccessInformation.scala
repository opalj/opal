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
import org.opalj.value.ValueInformation

import scala.collection.immutable.IntMap
import scala.collection.immutable.LongMap

sealed trait FieldAccessInformationPropertyMetaInformation[S <: FieldAccessInformation[S]] extends PropertyMetaInformation {
    final override type Self = S;

    /**
     * Creates a property key to be associated with every field access property of the respective type. The fallback is
     * computed via the fallback reason to ensure an analysis for the information was scheduled.
     */
    protected def createPropertyKey(propertyName: String, fallbackValue: S): PropertyKey[S] = {
        PropertyKey.create[Field, S](
            propertyName,
            (_: PropertyStore, reason: FallbackReason, _: Entity) =>
                reason match {
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => fallbackValue
                    case _ =>
                        throw new IllegalStateException(s"No analysis is scheduled for property $propertyName")
                }
        )
    }

    protected def getEncodedDirectAccessSet(accesses: Set[(DefinedMethod, PCs)]): LongLinkedTrieSet = {
        var result = LongLinkedTrieSet.empty
        for {
            (definedMethod, pcs) <- accesses
            pc <- pcs
        } result += encodeDirectFieldAccess(definedMethod, pc)
        result
    }

    protected def getEncodedIndirectAccessSet[V](accesses: IntMap[IntMap[V]]): LongMap[V] = {
        var result = LongMap.empty[V]
        for {
            (contextId, valuesByPC) <- accesses
            (pc, value) <- valuesByPC
        } {
            result += encodeIndirectFieldAccess(contextId, pc) -> value
        }
        result
    }
}

/**
 * Describes all read and write accesses to a [[org.opalj.br.Field]].
 *
 * @author Maximilian Rüsch
 */
sealed trait FieldAccessInformation[S <: FieldAccessInformation[S]] extends OrderedProperty
    with FieldAccessInformationPropertyMetaInformation[S] {

    protected val encodedDirectAccesses: LongLinkedSet
    protected val encodedIndirectAccessReceivers: LongMap[AccessReceiver] // Caller Context and PC => Receiver
    protected val encodedIndirectAccessParameters: LongMap[AccessParameter] // Caller Context and PC => Parameter

    def directAccesses(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedDirectAccesses.iterator.map(decodeDirectFieldAccess(_))

    def indirectAccesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] = {
        encodedIndirectAccessReceivers.iterator.map {
            kv =>
                {
                    val (fa: FieldAccess, receiver: Option[(ValueInformation, br.PCs)]) = kv
                    val parameter = encodedIndirectAccessParameters.getOrElse(fa, None)
                    val decodedFieldAccess = decodeIndirectFieldAccess(fa)
                    (decodedFieldAccess._1, decodedFieldAccess._2, receiver, parameter)
                }
        }
    }

    def getNewestNDirectAccesses(n: Int)(implicit declaredMethods: DeclaredMethods): Iterator[(DefinedMethod, PC)] =
        encodedDirectAccesses.iterator.take(n).map(decodeDirectFieldAccess(_))

    def getNewestNIndirectAccesses(n: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] = {
        encodedIndirectAccessReceivers.iterator.take(n).map {
            kv =>
                {
                    val (fa: FieldAccess, receiver: Option[(ValueInformation, br.PCs)]) = kv
                    val parameter = encodedIndirectAccessParameters.getOrElse(fa, None)
                    val decodedFieldAccess = decodeIndirectFieldAccess(fa)
                    (decodedFieldAccess._1, decodedFieldAccess._2, receiver, parameter)
                }
        }
    }

    def numAccesses: Int = numDirectAccesses + numIndirectAccesses

    def numDirectAccesses: Int = encodedDirectAccesses.size

    def numIndirectAccesses: Int = encodedIndirectAccessReceivers.valuesIterator.map { _.size }.sum

    def included(other: S, seenAccesses: Int = 0): S

    protected def included(
        other:           S,
        seenAccesses:    Int,
        propertyFactory: (LongLinkedSet, LongMap[AccessReceiver], LongMap[AccessParameter]) => S
    ): S = {
        var newAccesses = encodedDirectAccesses
        other.encodedDirectAccesses.forFirstN(other.numDirectAccesses - seenAccesses)(newAccesses += _)
        val newReceivers = encodedIndirectAccessReceivers ++ other.encodedIndirectAccessReceivers
        val newParameters = encodedIndirectAccessParameters ++ other.encodedIndirectAccessParameters

        if ((newAccesses eq encodedDirectAccesses)
            && newReceivers == encodedIndirectAccessReceivers
            && newParameters == encodedIndirectAccessParameters)
            return this.asInstanceOf[S];

        propertyFactory(newAccesses, newReceivers, newParameters)
    }

    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (numDirectAccesses > other.numDirectAccesses || numIndirectAccesses > other.numIndirectAccesses) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }
}

case class FieldReadAccessInformation(
        protected val encodedDirectAccesses:                LongLinkedSet,
        protected[this] val encodedIndirectAccessReceivers: LongMap[AccessReceiver]
) extends FieldAccessInformation[FieldReadAccessInformation]
    with FieldAccessInformationPropertyMetaInformation[FieldReadAccessInformation] {

    val encodedIndirectAccessParameters: LongMap[AccessParameter] = LongMap.empty

    final def key: PropertyKey[FieldReadAccessInformation] = FieldReadAccessInformation.key

    def included(other: FieldReadAccessInformation, seenAccesses: Int): FieldReadAccessInformation =
        included(
            other,
            seenAccesses,
            (newAccesses, newReceivers, _) => FieldReadAccessInformation(newAccesses, newReceivers)
        )
}

case class FieldWriteAccessInformation(
        protected val encodedDirectAccesses:                 LongLinkedSet,
        protected[this] val encodedIndirectAccessReceivers:  LongMap[AccessReceiver],
        protected[this] val encodedIndirectAccessParameters: LongMap[AccessParameter]
) extends FieldAccessInformation[FieldWriteAccessInformation]
    with FieldAccessInformationPropertyMetaInformation[FieldWriteAccessInformation] {

    final def key: PropertyKey[FieldWriteAccessInformation] = FieldWriteAccessInformation.key

    def included(other: FieldWriteAccessInformation, seenAccesses: Int): FieldWriteAccessInformation =
        included(
            other,
            seenAccesses,
            (newAccesses, newReceivers, newParameters) => FieldWriteAccessInformation(
                newAccesses,
                newReceivers,
                newParameters
            )
        )
}

object FieldReadAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldReadAccessInformation] {

    final val key = createPropertyKey("FieldReadAccessInformation", NoFieldReadAccessInformation)

    def apply(
        accesses:              Set[(DefinedMethod, PCs)],
        indirectCallReceivers: IndirectAccessReceivers   = IntMap.empty
    ): FieldReadAccessInformation = FieldReadAccessInformation(
        getEncodedDirectAccessSet(accesses),
        getEncodedIndirectAccessSet(indirectCallReceivers)
    )
}

object FieldWriteAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldWriteAccessInformation] {

    final val key = createPropertyKey("FieldWriteAccessInformation", NoFieldWriteAccessInformation)

    def apply(
        accesses:               Set[(DefinedMethod, PCs)],
        indirectCallReceivers:  IndirectAccessReceivers   = IntMap.empty,
        indirectCallParameters: IndirectAccessParameters  = IntMap.empty
    ): FieldWriteAccessInformation = FieldWriteAccessInformation(
        getEncodedDirectAccessSet(accesses),
        getEncodedIndirectAccessSet(indirectCallReceivers),
        getEncodedIndirectAccessSet(indirectCallParameters)
    )
}

object NoFieldReadAccessInformation
    extends FieldReadAccessInformation(LongLinkedTrieSet.empty, LongMap.empty)
object NoFieldWriteAccessInformation
    extends FieldWriteAccessInformation(LongLinkedTrieSet.empty, LongMap.empty, LongMap.empty)
