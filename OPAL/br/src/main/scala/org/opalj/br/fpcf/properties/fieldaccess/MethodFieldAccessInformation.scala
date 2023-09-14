/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import org.opalj.br.analyses.DeclaredFields
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation

import scala.collection.immutable.IntMap

sealed trait MethodFieldAccessInformationPropertyMetaInformation[S <: MethodFieldAccessInformation[S]] extends PropertyMetaInformation {
    final override type Self = S;

    /**
     * Creates a property key to be associated with every field access property of the respective type. The fallback is
     * computed via the fallback reason to ensure an analysis for the information was scheduled.
     */
    protected def createPropertyKey(propertyName: String, fallbackValue: S): PropertyKey[S] = {
        PropertyKey.create[Method, S](
            propertyName,
            (_: PropertyStore, reason: FallbackReason, _: Entity) =>
                reason match {
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => fallbackValue
                    case _ =>
                        throw new IllegalStateException(s"No analysis is scheduled for property $propertyName")
                }
        )
    }
}

/**
 * Describes all read and write accesses to a [[org.opalj.br.Field]].
 *
 * @author Maximilian Rüsch
 */
sealed trait MethodFieldAccessInformation[S <: MethodFieldAccessInformation[S]] extends OrderedProperty
    with MethodFieldAccessInformationPropertyMetaInformation[S] {

    protected val _directAccessedFields: IntMap[IntMap[IntTrieSet]] // Access Context => PC => DefinedFieldIds
    protected val _incompleteAccessSites: IntMap[PCs] // Access Context => PCs
    protected val _indirectAccessedReceiversByField: IntMap[IntMap[IntMap[AccessReceiver]]] // Access Context => PC => DefinedFieldId => Receiver
    protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] // Access Context => PC => DefinedFieldId => Parameter

    def directAccessedFields(
        accessContext: Context,
        pc:            PC
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        val directAccessedFields = _directAccessedFields.get(accessContext.id).flatMap(_.get(pc)).getOrElse(IntTrieSet.empty)
        directAccessedFields.iterator.map(declaredFields.apply)
    }

    def indirectAccessedFields(
        accessContext: Context,
        pc:            PC
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        val indirectAccessedFields = _indirectAccessedReceiversByField.get(accessContext.id).flatMap(_.get(pc)).getOrElse(IntMap.empty).keysIterator
        indirectAccessedFields.map(declaredFields.apply)
    }

    def getNewestNIndirectAccessSites(accessContext: Context, n: Int): Iterator[PC] = {
        _indirectAccessedReceiversByField.getOrElse(accessContext.id, IntMap.empty).keysIterator.take(n)
    }

    def getNewestNIndirectAccessedFields(
        accessContext: Context,
        pc:            PC,
        n:             Int
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        val indirectAccessedFields = _indirectAccessedReceiversByField.get(accessContext.id).flatMap(_.get(pc)).getOrElse(IntMap.empty).keysIterator
        indirectAccessedFields.take(n).map(declaredFields.apply)
    }

    def indirectAccessReceiver(
        accessContext: Context, pc: PC, field: DeclaredField
    ): AccessReceiver = {
        _indirectAccessedReceiversByField(accessContext.id)(pc)(field.id)
    }

    def indirectAccessParameter(
        accessContext: Context,
        pc:            PC,
        field:         DeclaredField
    ): AccessParameter = {
        _indirectAccessedParametersByField(accessContext.id)(pc)(field.id)
    }

    def numDirectAccesses: Int = _directAccessedFields.size

    private def numIndirectAccessesInAllAccessSites: Int =
        _indirectAccessedReceiversByField.valuesIterator.map { _.valuesIterator.map { _.size }.sum }.sum

    def numIndirectAccessSites(accessContext: Context): Int = _indirectAccessedReceiversByField(accessContext.id).size

    def numIndirectAccesses(accessContext: Context, pc: PC): Int = _indirectAccessedReceiversByField(accessContext.id)(pc).size

    def numIncompleteAccessSites: Int = _incompleteAccessSites.valuesIterator.map { _.size }.sum

    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (numDirectAccesses > other.numDirectAccesses ||
            numIndirectAccessesInAllAccessSites > other.numIndirectAccessesInAllAccessSites) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    protected def integrateDirectAccessInformationForContext(
        baseMap:     IntMap[IntMap[IntTrieSet]],
        contextId:   Int,
        updateValue: IntMap[IntTrieSet]
    ): IntMap[IntMap[IntTrieSet]] = {
        baseMap.updateWith(
            contextId,
            updateValue,
            (o, n) => o.unionWith(
                n,
                (_, l, r) => {
                    if (l == r) l
                    else throw new UnknownError("Incompatible accessed fields for direct call")
                }
            )
        )
    }

    protected def integrateIndirectAccessInformationForContext[AIT](
        baseMap:     IntMap[IntMap[IntMap[AIT]]],
        contextId:   Int,
        updateValue: IntMap[IntMap[AIT]],
        failure:     () => Throwable
    ): IntMap[IntMap[IntMap[AIT]]] = {
        baseMap.updateWith(
            contextId,
            updateValue,
            (o, n) =>
                o.unionWith(
                    n,
                    (_, l, r) => {
                        r.unionWith(
                            l,
                            (_, vl, vr) =>
                                if (vl == vr) vl
                                else throw failure()
                        )
                    }
                )
        )
    }
}

case class MethodFieldReadAccessInformation(
        protected val _directAccessedFields:             IntMap[IntMap[IntTrieSet]],
        protected val _incompleteAccessSites:            IntMap[PCs],
        protected val _indirectAccessedReceiversByField: IntMap[IntMap[IntMap[AccessReceiver]]]
) extends MethodFieldAccessInformation[MethodFieldReadAccessInformation] with MethodFieldAccessInformationPropertyMetaInformation[MethodFieldReadAccessInformation] {

    protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] = IntMap.empty
    override def indirectAccessParameter(accessContext: Context, pc: PC, field: DeclaredField): AccessParameter = None

    final def key: PropertyKey[MethodFieldReadAccessInformation] = MethodFieldReadAccessInformation.key

    /** Creates a copy of the current object, including the additional access information specified in the parameters */
    def updateWithFieldAccesses(
        accessContext:           Context,
        directAccesses:          IntMap[IntTrieSet],
        incompleteAccessSites:   br.PCs,
        indirectAccessReceivers: IntMap[IntMap[Option[(ValueInformation, br.PCs)]]]
    ): MethodFieldReadAccessInformation = {
        val cId = accessContext.id

        new MethodFieldReadAccessInformation(
            integrateDirectAccessInformationForContext(_directAccessedFields, cId, directAccesses),
            _incompleteAccessSites.updateWith(cId, incompleteAccessSites, (o, n) => o ++ n),
            integrateIndirectAccessInformationForContext(
                _indirectAccessedReceiversByField,
                cId,
                indirectAccessReceivers,
                () => new UnknownError("Incompatible receivers for indirect call")
            ),
        )
    }
}

case class MethodFieldWriteAccessInformation(
        protected val _directAccessedFields:              IntMap[IntMap[IntTrieSet]],
        protected val _incompleteAccessSites:             IntMap[PCs],
        protected val _indirectAccessedReceiversByField:  IntMap[IntMap[IntMap[AccessReceiver]]],
        protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]]
) extends MethodFieldAccessInformation[MethodFieldWriteAccessInformation] with MethodFieldAccessInformationPropertyMetaInformation[MethodFieldWriteAccessInformation] {

    final def key: PropertyKey[MethodFieldWriteAccessInformation] = MethodFieldWriteAccessInformation.key

    /** Creates a copy of the current object, including the additional access information specified in the parameters */
    def updateWithFieldAccesses(
        accessContext:            Context,
        directAccesses:           IntMap[IntTrieSet],
        incompleteAccessSites:    br.PCs,
        indirectAccessReceivers:  IntMap[IntMap[Option[(ValueInformation, br.PCs)]]],
        indirectAccessParameters: IntMap[IntMap[Option[(ValueInformation, br.PCs)]]]
    ): MethodFieldWriteAccessInformation = {
        val cId = accessContext.id

        new MethodFieldWriteAccessInformation(
            integrateDirectAccessInformationForContext(_directAccessedFields, cId, directAccesses),
            _incompleteAccessSites.updateWith(cId, incompleteAccessSites, (o, n) => o ++ n),
            integrateIndirectAccessInformationForContext(
                _indirectAccessedReceiversByField,
                cId,
                indirectAccessReceivers,
                () => new UnknownError("Incompatible receivers for indirect call")
            ),
            integrateIndirectAccessInformationForContext(
                _indirectAccessedParametersByField,
                cId,
                indirectAccessParameters,
                () => new UnknownError("Incompatible parameters for indirect call")
            ),
        )
    }
}

object MethodFieldReadAccessInformation extends MethodFieldAccessInformationPropertyMetaInformation[MethodFieldReadAccessInformation] {

    final val key = createPropertyKey("MethodFieldReadAccessInformation", NoMethodFieldReadAccessInformation)

    def apply(accesses: IntMap[IntMap[IntTrieSet]]): MethodFieldReadAccessInformation =
        MethodFieldReadAccessInformation(accesses, IntMap.empty, IntMap.empty)

    def apply(accesses: IntMap[IntMap[IntTrieSet]], incompleteAccessSites: IntMap[PCs]): MethodFieldReadAccessInformation =
        MethodFieldReadAccessInformation(accesses, incompleteAccessSites, IntMap.empty)
}

object MethodFieldWriteAccessInformation extends MethodFieldAccessInformationPropertyMetaInformation[MethodFieldWriteAccessInformation] {

    final val key = createPropertyKey("MethodFieldWriteAccessInformation", NoMethodFieldWriteAccessInformation)

    def apply(accesses: IntMap[IntMap[IntTrieSet]]): MethodFieldWriteAccessInformation =
        MethodFieldWriteAccessInformation(accesses, IntMap.empty, IntMap.empty, IntMap.empty)

    def apply(accesses: IntMap[IntMap[IntTrieSet]], incompleteAccessSites: IntMap[PCs]): MethodFieldWriteAccessInformation =
        MethodFieldWriteAccessInformation(accesses, incompleteAccessSites, IntMap.empty, IntMap.empty)
}

object NoMethodFieldReadAccessInformation extends MethodFieldReadAccessInformation(IntMap.empty, IntMap.empty, IntMap.empty)
object NoMethodFieldWriteAccessInformation extends MethodFieldWriteAccessInformation(IntMap.empty, IntMap.empty, IntMap.empty, IntMap.empty)
