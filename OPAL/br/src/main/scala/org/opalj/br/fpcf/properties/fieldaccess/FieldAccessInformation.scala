/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import org.opalj.br.PC
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

import scala.collection.immutable.IntMap
import scala.collection.immutable.LongMap

sealed trait FieldAccessInformationPropertyMetaInformation[S <: FieldAccessInformation[S]] extends PropertyMetaInformation {
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

    protected val encodedDirectAccessReceivers: LongMap[AccessReceiver] // Caller Context and PC => Receiver
    protected val encodedDirectAccessParameters: LongMap[AccessParameter] // Caller Context and PC => Parameter
    protected val encodedIndirectAccessReceivers: LongMap[AccessReceiver] // Caller Context and PC => Receiver
    protected val encodedIndirectAccessParameters: LongMap[AccessParameter] // Caller Context and PC => Parameter

    def accesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] = directAccesses ++ indirectAccesses

    def directAccesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedDirectAccessReceivers.iterator, encodedDirectAccessParameters)

    def indirectAccesses: Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedIndirectAccessReceivers.iterator, encodedIndirectAccessParameters)

    private def decodeAccesses(
        encodedReceivers:  Iterator[(FieldAccess, AccessReceiver)],
        encodedParameters: LongMap[AccessParameter]
    ): Iterator[(Int, PC, AccessReceiver, AccessParameter)] = {
        encodedReceivers.map {
            kv =>
                {
                    val (fa: FieldAccess, receiver: AccessReceiver) = kv
                    val parameter = encodedParameters.getOrElse(fa, None)
                    val decodedFieldAccess = decodeFieldAccess(fa)
                    (decodedFieldAccess._1, decodedFieldAccess._2, receiver, parameter)
                }
        }
    }

    def getNewestAccesses(newestDirectAccesses: Int, newestIndirectAccesses: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        getNewestNDirectAccesses(newestDirectAccesses) ++ getNewestNIndirectAccesses(newestIndirectAccesses)

    def getNewestNDirectAccesses(n: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedDirectAccessReceivers.iterator.take(n), encodedDirectAccessParameters)

    def getNewestNIndirectAccesses(n: Int): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        decodeAccesses(encodedIndirectAccessReceivers.iterator.take(n), encodedIndirectAccessParameters)

    def numAccesses: Int = numDirectAccesses + numIndirectAccesses

    def numDirectAccesses: Int = encodedDirectAccessReceivers.valuesIterator.map { _.size }.sum

    def numIndirectAccesses: Int = encodedIndirectAccessReceivers.valuesIterator.map { _.size }.sum

    def included(other: S, seenDirectAccesses: Int = 0, seenIndirectAccesses: Int = 0): S

    protected def included(
        other:                S,
        seenDirectAccesses:   Int,
        seenIndirectAccesses: Int,
        propertyFactory:      (LongMap[AccessReceiver], LongMap[AccessParameter], LongMap[AccessReceiver], LongMap[AccessParameter]) => S
    ): S = {
        val newDirectReceivers = encodedDirectAccessReceivers ++
            other.encodedDirectAccessReceivers.iterator.take(other.numDirectAccesses - seenDirectAccesses)
        val newDirectParameters = encodedDirectAccessParameters ++
            other.encodedDirectAccessParameters.iterator.take(other.numDirectAccesses - seenDirectAccesses)
        val newIndirectReceivers = encodedIndirectAccessReceivers ++
            other.encodedIndirectAccessReceivers.iterator.take(other.numIndirectAccesses - seenIndirectAccesses)
        val newIndirectParameters = encodedIndirectAccessParameters ++
            other.encodedIndirectAccessParameters.iterator.take(other.numIndirectAccesses - seenIndirectAccesses)

        if (newDirectReceivers == encodedDirectAccessReceivers
            && newDirectParameters == encodedDirectAccessParameters
            && newIndirectReceivers == encodedIndirectAccessReceivers
            && newIndirectParameters == encodedIndirectAccessParameters)
            return this.asInstanceOf[S];

        propertyFactory(newDirectReceivers, newDirectParameters, newIndirectReceivers, newIndirectParameters)
    }

    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (numDirectAccesses > other.numDirectAccesses || numIndirectAccesses > other.numIndirectAccesses) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }
}

case class FieldReadAccessInformation(
        protected val encodedDirectAccessReceivers:         LongMap[AccessReceiver],
        protected[this] val encodedIndirectAccessReceivers: LongMap[AccessReceiver]
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
            (newDirectReceivers, _, newIndirectReceivers, _) => FieldReadAccessInformation(newDirectReceivers, newIndirectReceivers)
        )
}

case class FieldWriteAccessInformation(
        protected[this] val encodedDirectAccessReceivers:    LongMap[AccessReceiver],
        protected[this] val encodedDirectAccessParameters:   LongMap[AccessParameter],
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
            (newDirectReceivers, newDirectParameters, newIndirectReceivers, newIndirectParameters) =>
                FieldWriteAccessInformation(newDirectReceivers, newDirectParameters, newIndirectReceivers, newIndirectParameters)
        )
}

object FieldReadAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldReadAccessInformation] {

    final val key = createPropertyKey("opalj.FieldReadAccessInformation", NoFieldReadAccessInformation)

    def apply(
        directAccessReceivers:   AccessReceivers,
        indirectAccessReceivers: AccessReceivers = IntMap.empty
    ): FieldReadAccessInformation = FieldReadAccessInformation(
        getEncodedAccessSet(directAccessReceivers),
        getEncodedAccessSet(indirectAccessReceivers)
    )
}

object FieldWriteAccessInformation
    extends FieldAccessInformationPropertyMetaInformation[FieldWriteAccessInformation] {

    final val key = createPropertyKey("opalj.FieldWriteAccessInformation", NoFieldWriteAccessInformation)

    def apply(
        directAccessReceivers:    AccessReceivers,
        directAccessParameters:   AccessParameters,
        indirectAccessReceivers:  AccessReceivers  = IntMap.empty,
        indirectAccessParameters: AccessParameters = IntMap.empty
    ): FieldWriteAccessInformation = FieldWriteAccessInformation(
        getEncodedAccessSet(directAccessReceivers),
        getEncodedAccessSet(directAccessParameters),
        getEncodedAccessSet(indirectAccessReceivers),
        getEncodedAccessSet(indirectAccessParameters)
    )
}

object NoFieldReadAccessInformation
    extends FieldReadAccessInformation(LongMap.empty, LongMap.empty)
object NoFieldWriteAccessInformation
    extends FieldWriteAccessInformation(LongMap.empty, LongMap.empty, LongMap.empty, LongMap.empty)
