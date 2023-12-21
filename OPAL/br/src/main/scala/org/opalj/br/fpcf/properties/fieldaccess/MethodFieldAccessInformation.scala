/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import scala.collection.immutable.IntMap

import org.opalj.br.analyses.DeclaredFields
import org.opalj.collection.immutable.IntList
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait MethodFieldAccessInformationPropertyMetaInformation[S <: MethodFieldAccessInformation[S]]
    extends PropertyMetaInformation {
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

    protected val _incompleteAccessSites: IntMap[PCs] // Access Context => PCs
    protected val _directAccessedFields: IntMap[IntMap[IntList]] // Access Context => PC => DefinedFieldId
    protected val _directAccessedReceiversByField: IntMap[IntMap[IntMap[AccessReceiver]]] // Access Context => PC => DefinedFieldId => Receiver
    protected val _directAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] // Access Context => PC => DefinedFieldId => Parameter
    protected val _indirectAccessedFields: IntMap[IntMap[IntList]] // Access Context => PC => DefinedFieldId
    protected val _indirectAccessedReceiversByField: IntMap[IntMap[IntMap[AccessReceiver]]] // Access Context => PC => DefinedFieldId => Receiver
    protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] // Access Context => PC => DefinedFieldId => Parameter

    def getAccessSites(accessContext: Context): Iterator[PC] = {
        getDirectAccessSites(accessContext) ++ getIndirectAccessSites(accessContext)
    }

    def getDirectAccessSites(accessContext: Context): Iterator[PC] = {
        _directAccessedFields.getOrElse(accessContext.id, IntMap.empty).keysIterator
    }

    def getIndirectAccessSites(accessContext: Context): Iterator[PC] = {
        _indirectAccessedFields.getOrElse(accessContext.id, IntMap.empty).keysIterator
    }

    def getAccessedFields(
        accessContext: Context,
        pc:            PC
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        getDirectAccessedFields(accessContext, pc) ++ getIndirectAccessedFields(accessContext, pc)
    }

    def getDirectAccessedFields(
        accessContext: Context,
        pc:            PC
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        _directAccessedFields.get(accessContext.id).iterator
            .flatMap(_.get(pc))
            .flatMap(_.iterator)
            .map(declaredFields.apply)
    }

    def getIndirectAccessedFields(
        accessContext: Context,
        pc:            PC
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        _indirectAccessedFields.get(accessContext.id).iterator
            .flatMap(_.get(pc))
            .flatMap(_.iterator)
            .map(declaredFields.apply)
    }

    def getNewestAccessedFields(
        accessContext:             Context,
        pc:                        PC,
        newestDirectAccessSites:   Int,
        newestIndirectAccessSites: Int
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        getNewestNDirectAccessedFields(accessContext, pc, newestDirectAccessSites) ++
            getNewestNIndirectAccessedFields(accessContext, pc, newestIndirectAccessSites)
    }

    def getNewestNDirectAccessedFields(
        accessContext: Context,
        pc:            PC,
        n:             Int
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        _directAccessedFields.get(accessContext.id).iterator
            .flatMap(_.get(pc))
            .flatMap(_.iterator)
            .take(n).map(declaredFields.apply)
    }

    def getNewestNIndirectAccessedFields(
        accessContext: Context,
        pc:            PC,
        n:             Int
    )(implicit declaredFields: DeclaredFields): Iterator[DeclaredField] = {
        _indirectAccessedFields.get(accessContext.id).iterator
            .flatMap(_.get(pc))
            .flatMap(_.iterator)
            .take(n).map(declaredFields.apply)
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

    private def numDirectAccessesInAllAccessSites: Int =
        _directAccessedFields.valuesIterator.map { _.valuesIterator.map { _.iterator.size }.sum }.sum

    def numDirectAccesses(accessContext: Context, pc: PC): Int =
        _directAccessedFields.get(accessContext.id).iterator.flatMap(_.get(pc)).map(_.iterator.size).sum

    private def numIndirectAccessesInAllAccessSites: Int =
        _indirectAccessedFields.valuesIterator.map { _.valuesIterator.map { _.iterator.size }.sum }.sum

    def numIndirectAccesses(accessContext: Context, pc: PC): Int =
        _indirectAccessedFields.get(accessContext.id).iterator.flatMap(_.get(pc)).map(_.iterator.size).sum

    def numIncompleteAccessSites: Int =
        _incompleteAccessSites.valuesIterator.map { _.size }.sum

    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (numDirectAccessesInAllAccessSites > other.numDirectAccessesInAllAccessSites ||
            numIndirectAccessesInAllAccessSites > other.numIndirectAccessesInAllAccessSites) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    protected def integrateAccessedFieldsForContext(
        baseMap:     IntMap[IntMap[IntList]],
        contextId:   Int,
        updateValue: IntMap[IntList]
    ): IntMap[IntMap[IntList]] = {
        baseMap.updateWith(
            contextId,
            updateValue,
            (o, n) =>
                o.unionWith(
                    n,
                    (_, l, r) => r ++: l
                )
        )
    }

    protected def integrateAccessInformationForContext[AIT](
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
        protected val _incompleteAccessSites:            IntMap[PCs],
        protected val _directAccessedFields:             IntMap[IntMap[IntList]],
        protected val _directAccessedReceiversByField:   IntMap[IntMap[IntMap[AccessReceiver]]],
        protected val _indirectAccessedFields:           IntMap[IntMap[IntList]],
        protected val _indirectAccessedReceiversByField: IntMap[IntMap[IntMap[AccessReceiver]]]
) extends MethodFieldAccessInformation[MethodFieldReadAccessInformation]
    with MethodFieldAccessInformationPropertyMetaInformation[MethodFieldReadAccessInformation] {

    protected val _directAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] = IntMap.empty
    protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]] = IntMap.empty
    override def indirectAccessParameter(accessContext: Context, pc: PC, field: DeclaredField): AccessParameter = None

    final def key: PropertyKey[MethodFieldReadAccessInformation] = MethodFieldReadAccessInformation.key

    /** Creates a copy of the current object, including the additional access information specified in the parameters */
    def updateWithFieldAccesses(
        accessContext:           Context,
        incompleteAccessSites:   br.PCs,
        directAccessedFields:    IntMap[IntList],
        directAccessReceivers:   AccessReceivers,
        indirectAccessedFields:  IntMap[IntList],
        indirectAccessReceivers: AccessReceivers
    ): MethodFieldReadAccessInformation = {
        val cId = accessContext.id

        new MethodFieldReadAccessInformation(
            _incompleteAccessSites.updateWith(cId, incompleteAccessSites, (o, n) => o ++ n),
            integrateAccessedFieldsForContext(_directAccessedFields, accessContext.id, directAccessedFields),
            integrateAccessInformationForContext(
                _directAccessedReceiversByField, cId, directAccessReceivers,
                () => new UnknownError("Incompatible receivers for direct call")
            ),
            integrateAccessedFieldsForContext(_indirectAccessedFields, accessContext.id, indirectAccessedFields),
            integrateAccessInformationForContext(
                _indirectAccessedReceiversByField, cId, indirectAccessReceivers,
                () => new UnknownError("Incompatible receivers for indirect call")
            ),
        )
    }
}

case class MethodFieldWriteAccessInformation(
        protected val _incompleteAccessSites:             IntMap[PCs],
        protected val _directAccessedFields:              IntMap[IntMap[IntList]],
        protected val _directAccessedReceiversByField:    IntMap[IntMap[IntMap[AccessReceiver]]],
        protected val _directAccessedParametersByField:   IntMap[IntMap[IntMap[AccessParameter]]],
        protected val _indirectAccessedFields:            IntMap[IntMap[IntList]],
        protected val _indirectAccessedReceiversByField:  IntMap[IntMap[IntMap[AccessReceiver]]],
        protected val _indirectAccessedParametersByField: IntMap[IntMap[IntMap[AccessParameter]]]
) extends MethodFieldAccessInformation[MethodFieldWriteAccessInformation]
    with MethodFieldAccessInformationPropertyMetaInformation[MethodFieldWriteAccessInformation] {

    final def key: PropertyKey[MethodFieldWriteAccessInformation] = MethodFieldWriteAccessInformation.key

    /** Creates a copy of the current object, including the additional access information specified in the parameters */
    def updateWithFieldAccesses(
        accessContext:            Context,
        incompleteAccessSites:    br.PCs,
        directAccessedFields:     IntMap[IntList],
        directAccessReceivers:    IntMap[IntMap[AccessReceiver]],
        directAccessParameters:   IntMap[IntMap[AccessParameter]],
        indirectAccessedFields:   IntMap[IntList],
        indirectAccessReceivers:  IntMap[IntMap[AccessReceiver]],
        indirectAccessParameters: IntMap[IntMap[AccessParameter]]
    ): MethodFieldWriteAccessInformation = {
        val cId = accessContext.id

        new MethodFieldWriteAccessInformation(
            _incompleteAccessSites.updateWith(cId, incompleteAccessSites, (o, n) => o ++ n),
            integrateAccessedFieldsForContext(_directAccessedFields, accessContext.id, directAccessedFields),
            integrateAccessInformationForContext(
                _directAccessedReceiversByField, cId, directAccessReceivers,
                () => new UnknownError("Incompatible receivers for direct call")
            ),
            integrateAccessInformationForContext(
                _indirectAccessedParametersByField, cId, directAccessParameters,
                () => new UnknownError("Incompatible parameters for direct call")
            ),
            integrateAccessedFieldsForContext(_indirectAccessedFields, accessContext.id, indirectAccessedFields),
            integrateAccessInformationForContext(
                _indirectAccessedReceiversByField, cId, indirectAccessReceivers,
                () => new UnknownError("Incompatible receivers for indirect call")
            ),
            integrateAccessInformationForContext(
                _indirectAccessedParametersByField, cId, indirectAccessParameters,
                () => new UnknownError("Incompatible parameters for indirect call")
            ),
        )
    }
}

object MethodFieldReadAccessInformation
    extends MethodFieldAccessInformationPropertyMetaInformation[MethodFieldReadAccessInformation] {

    final val key = createPropertyKey("opalj.MethodFieldReadAccessInformation", NoMethodFieldReadAccessInformation)
}

object MethodFieldWriteAccessInformation
    extends MethodFieldAccessInformationPropertyMetaInformation[MethodFieldWriteAccessInformation] {

    final val key = createPropertyKey("opalj.MethodFieldWriteAccessInformation", NoMethodFieldWriteAccessInformation)
}

object NoMethodFieldReadAccessInformation
    extends MethodFieldReadAccessInformation(
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty
    )
object NoMethodFieldWriteAccessInformation
    extends MethodFieldWriteAccessInformation(
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty,
        IntMap.empty
    )
