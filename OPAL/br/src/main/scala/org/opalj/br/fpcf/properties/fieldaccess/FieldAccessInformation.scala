/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import scala.collection.immutable.IntMap
import scala.collection.immutable.LongMap

import org.opalj.br.PC
import org.opalj.collection.immutable.LongLinkedSet
import org.opalj.collection.immutable.LongLinkedTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait FieldAccessInformationPropertyMetaInformation[S <: FieldAccessInformation[S]]
    extends PropertyMetaInformation {
    final override type Self = S;

    /**
     * Creates a property key to be associated with every field access property of the respective type. The fallback is
     * computed via the fallback reason to ensure an analysis for the information was scheduled.
     */
    protected def createPropertyKey(propertyName: String, fallbackValue: S): PropertyKey[S] = {
        PropertyKey.create[DeclaredField, S](
            propertyName,
            (_: PropertyStore, reason: FallbackReason, _: DeclaredField) =>
                reason match {
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => fallbackValue
                    case _ =>
                        throw new IllegalStateException(s"No analysis is scheduled for property $propertyName")
                }
        )
    }

    protected def getEncodedAccessSet[V](accesses: IntMap[IntMap[V]]): LongMap[V] = {
        var result = LongMap.empty[V]
        for {
            (contextId, valuesByPC) <- accesses
            (pc, value) <- valuesByPC
        } {
            result += encodeFieldAccess(contextId, pc) -> value
        }
        result
    }
}

/**
 * Describes all read and write accesses to a [[org.opalj.br.DeclaredField]].
 *
 * @author Maximilian Rüsch
 */
sealed trait FieldAccessInformation[S <: FieldAccessInformation[S]] extends OrderedProperty
    with FieldAccessInformationPropertyMetaInformation[S] {

    protected val encodedDirectAccesses: LongLinkedSet // Ordered, Caller Context and PC
    protected val encodedDirectAccessReceivers: LongMap[AccessReceiver] // Caller Context and PC => Receiver
    protected val encodedDirectAccessParameters: LongMap[AccessParameter] // Caller Context and PC => Parameter
    protected val encodedIndirectAccesses: LongLinkedSet // Ordered, Caller Context and PC
    protected val encodedIndirectAccessReceivers: LongMap[AccessReceiver] // Caller Context and PC => Receiver
    protected val encodedIndirectAccessParameters: LongMap[AccessParameter] // Caller Context and PC => Parameter

    def accesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] = directAccesses ++ indirectAccesses

    def directAccesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedDirectAccesses.iterator, encodedDirectAccessReceivers, encodedDirectAccessParameters)

    def indirectAccesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedIndirectAccesses.iterator, encodedIndirectAccessReceivers, encodedIndirectAccessParameters)

    private def decodeAccesses(
        encodedAccesses:   Iterator[FieldAccess],
        encodedReceivers:  LongMap[AccessReceiver],
        encodedParameters: LongMap[AccessParameter]
    ): Iterator[(Int, PC, AccessReceiver, AccessParameter)] = {
        encodedAccesses.map {
            fa =>
                {
                    val receiver: AccessReceiver = encodedReceivers.getOrElse(fa, None)
                    val parameter = encodedParameters.getOrElse(fa, None)
                    val decodedFieldAccess = decodeFieldAccess(fa)
                    (decodedFieldAccess._1, decodedFieldAccess._2, receiver, parameter)
                }
        }
    }

    def getNewestAccesses(newestDirectAccesses: Int, newestIndirectAccesses: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        getNewestNDirectAccesses(newestDirectAccesses) ++ getNewestNIndirectAccesses(newestIndirectAccesses)

    def getNewestNDirectAccesses(n: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(
            encodedDirectAccesses.iterator.take(n),
            encodedDirectAccessReceivers,
            encodedDirectAccessParameters
        )

    def getNewestNIndirectAccesses(n: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(
            encodedIndirectAccesses.iterator.take(n),
            encodedIndirectAccessReceivers,
            encodedIndirectAccessParameters
        )

    def numAccesses: Int = numDirectAccesses + numIndirectAccesses

    def numDirectAccesses: Int = encodedDirectAccessReceivers.size

    def numIndirectAccesses: Int = encodedIndirectAccessReceivers.size

    def included(other: S, seenDirectAccesses: Int = 0, seenIndirectAccesses: Int = 0): S

    protected def included(
        other:                S,
        seenDirectAccesses:   Int,
        seenIndirectAccesses: Int,
        propertyFactory:      (LongLinkedSet, LongMap[AccessReceiver], LongMap[AccessParameter], LongLinkedSet, LongMap[AccessReceiver], LongMap[AccessParameter]) => S
    ): S = {
        var newDirectAccesses = encodedDirectAccesses
        var newDirectReceivers = encodedDirectAccessReceivers
        var newDirectParameters = encodedDirectAccessParameters
        for {
            fa <- other.encodedDirectAccesses.iterator.take(other.numDirectAccesses - seenDirectAccesses)
        } {
            newDirectAccesses += fa

            val otherReceiver = other.encodedDirectAccessReceivers.get(fa)
            if (otherReceiver.isDefined)
                newDirectReceivers += fa -> otherReceiver.get
            val otherParameter = other.encodedDirectAccessParameters.get(fa)
            if (otherParameter.isDefined)
                newDirectParameters += fa -> otherParameter.get
        }

        var newIndirectAccesses = encodedIndirectAccesses
        var newIndirectReceivers = encodedIndirectAccessReceivers
        var newIndirectParameters = encodedIndirectAccessParameters
        for {
            fa <- other.encodedIndirectAccesses.iterator.take(other.numIndirectAccesses - seenIndirectAccesses)
        } {
            newIndirectAccesses += fa

            val otherReceiver = other.encodedIndirectAccessReceivers.get(fa)
            if (otherReceiver.isDefined)
                newIndirectReceivers += fa -> otherReceiver.get
            val otherParameter = other.encodedIndirectAccessParameters.get(fa)
            if (otherParameter.isDefined)
                newIndirectParameters += fa -> otherParameter.get
        }

        if (newDirectAccesses == encodedDirectAccesses && newIndirectAccesses == encodedIndirectAccesses)
            return this.asInstanceOf[S];

        propertyFactory(
            newDirectAccesses,
            newDirectReceivers,
            newDirectParameters,
            newIndirectAccesses,
            newIndirectReceivers,
            newIndirectParameters
        )
    }

    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (numDirectAccesses > other.numDirectAccesses || numIndirectAccesses > other.numIndirectAccesses) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }
}

case class FieldReadAccessInformation(
        protected val encodedDirectAccesses:          LongLinkedSet,
        protected val encodedDirectAccessReceivers:   LongMap[AccessReceiver],
        protected val encodedIndirectAccesses:        LongLinkedSet,
        protected val encodedIndirectAccessReceivers: LongMap[AccessReceiver]
) extends FieldAccessInformation[FieldReadAccessInformation]
    with FieldAccessInformationPropertyMetaInformation[FieldReadAccessInformation] {

    val encodedDirectAccessParameters: LongMap[AccessParameter] = LongMap.empty
    val encodedIndirectAccessParameters: LongMap[AccessParameter] = LongMap.empty

    final def key: PropertyKey[FieldReadAccessInformation] = FieldReadAccessInformation.key

    def included(other: FieldReadAccessInformation, seenDirectAccesses: Int, seenIndirectAccesses: Int): FieldReadAccessInformation =
        included(
            other,
            seenDirectAccesses,
            seenIndirectAccesses,
            (newDirectAccesses, newDirectReceivers, _, newIndirectAccesses, newIndirectReceivers, _) =>
                FieldReadAccessInformation(
                    newDirectAccesses,
                    newDirectReceivers,
                    newIndirectAccesses,
                    newIndirectReceivers
                )
        )
}

case class FieldWriteAccessInformation(
        protected val encodedDirectAccesses:                 LongLinkedSet,
        protected[this] val encodedDirectAccessReceivers:    LongMap[AccessReceiver],
        protected[this] val encodedDirectAccessParameters:   LongMap[AccessParameter],
        protected val encodedIndirectAccesses:               LongLinkedSet,
        protected[this] val encodedIndirectAccessReceivers:  LongMap[AccessReceiver],
        protected[this] val encodedIndirectAccessParameters: LongMap[AccessParameter]
) extends FieldAccessInformation[FieldWriteAccessInformation]
    with FieldAccessInformationPropertyMetaInformation[FieldWriteAccessInformation] {

    final def key: PropertyKey[FieldWriteAccessInformation] = FieldWriteAccessInformation.key

    def included(other: FieldWriteAccessInformation, seenDirectAccesses: Int, seenIndirectAccesses: Int): FieldWriteAccessInformation =
        included(
            other,
            seenDirectAccesses,
            seenIndirectAccesses,
            (newDirectAccesses, newDirectReceivers, newDirectParameters, newIndirectAccesses, newIndirectReceivers, newIndirectParameters) =>
                FieldWriteAccessInformation(
                    newDirectAccesses,
                    newDirectReceivers,
                    newDirectParameters,
                    newIndirectAccesses,
                    newIndirectReceivers,
                    newIndirectParameters
                )
        )
}

object FieldReadAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldReadAccessInformation] {

    final val key = createPropertyKey("opalj.FieldReadAccessInformation", NoFieldReadAccessInformation)

    def apply(
        directAccesses:          LongLinkedSet,
        directAccessReceivers:   AccessReceivers,
        indirectAccesses:        LongLinkedSet   = LongLinkedTrieSet.empty,
        indirectAccessReceivers: AccessReceivers = IntMap.empty
    ): FieldReadAccessInformation = {
        FieldReadAccessInformation(
            directAccesses,
            getEncodedAccessSet(directAccessReceivers),
            indirectAccesses,
            getEncodedAccessSet(indirectAccessReceivers)
        )
    }
}

object FieldWriteAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldWriteAccessInformation] {

    final val key = createPropertyKey("opalj.FieldWriteAccessInformation", NoFieldWriteAccessInformation)

    def apply(
        directAccesses:           LongLinkedSet,
        directAccessReceivers:    AccessReceivers,
        directAccessParameters:   AccessParameters,
        indirectAccesses:         LongLinkedSet    = LongLinkedTrieSet.empty,
        indirectAccessReceivers:  AccessReceivers  = IntMap.empty,
        indirectAccessParameters: AccessParameters = IntMap.empty
    ): FieldWriteAccessInformation = FieldWriteAccessInformation(
        directAccesses,
        getEncodedAccessSet(directAccessReceivers),
        getEncodedAccessSet(directAccessParameters),
        indirectAccesses,
        getEncodedAccessSet(indirectAccessReceivers),
        getEncodedAccessSet(indirectAccessParameters)
    )
}

object NoFieldReadAccessInformation
    extends FieldReadAccessInformation(LongLinkedTrieSet.empty, LongMap.empty, LongLinkedTrieSet.empty, LongMap.empty)
object NoFieldWriteAccessInformation
    extends FieldWriteAccessInformation(
        LongLinkedTrieSet.empty,
        LongMap.empty,
        LongMap.empty,
        LongLinkedTrieSet.empty,
        LongMap.empty,
        LongMap.empty
    )
