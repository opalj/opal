/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType

/**
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding extends l1.ReferenceValues with DefaultExceptionsFactory {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration =>

    // Let's fix the type hierarchy

    type AReferenceValue = TheReferenceValue
    final val AReferenceValueTag: ClassTag[AReferenceValue] = implicitly
    type DomainReferenceValue = AReferenceValue
    final val DomainReferenceValueTag: ClassTag[DomainReferenceValue] = implicitly

    type DomainSingleOriginReferenceValue = SingleOriginReferenceValue
    final val DomainSingleOriginReferenceValueTag: ClassTag[DomainSingleOriginReferenceValue] = implicitly

    type DomainNullValue = NullValue
    final val DomainNullValueTag: ClassTag[DomainNullValue] = implicitly

    type DomainObjectValue = ObjectValue
    final val DomainObjectValueTag: ClassTag[DomainObjectValue] = implicitly

    type DomainArrayValue = ArrayValue
    final val DomainArrayValueTag: ClassTag[DomainArrayValue] = implicitly

    type DomainMultipleReferenceValues = MultipleReferenceValues
    final val DomainMultipleReferenceValuesTag: ClassTag[DomainMultipleReferenceValues] = implicitly

    //
    // CONCRETE CLASSES AND THEIR FACTORY METHODS
    //

    protected case class DefaultSObjectValue(
            origin:                 ValueOrigin,
            isNull:                 Answer,
            override val isPrecise: Boolean,
            theUpperTypeBound:      ObjectType,
            refId:                  RefId
    ) extends SObjectValue

    protected case class DefaultMObjectValue(
            origin:              ValueOrigin,
            override val isNull: Answer,
            upperTypeBound:      UIDSet[ObjectType],
            refId:               RefId
    ) extends MObjectValue

    private case class DefaultArrayValue(
            origin:                 ValueOrigin,
            isNull:                 Answer,
            override val isPrecise: Boolean,
            theUpperTypeBound:      ArrayType,
            refId:                  RefId
    ) extends ArrayValue

    override def NullValue(origin: ValueOrigin): DomainNullValue = new NullValue(origin)

    override protected[domain] def ObjectValue(
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ObjectType,
        refId:             RefId
    ): SObjectValue = {
        DefaultSObjectValue(
            origin,
            isNull,
            isPrecise || classHierarchy.isKnownToBeFinal(theUpperTypeBound),
            theUpperTypeBound, refId
        )
    }

    override protected[domain] def ObjectValue(
        origin:         ValueOrigin,
        isNull:         Answer,
        upperTypeBound: UIDSet[ObjectType],
        refId:          RefId
    ): DomainObjectValue = {
        if (upperTypeBound.isSingletonSet) {
            ObjectValue(origin, isNull, false, upperTypeBound.head, refId)
        } else
            DefaultMObjectValue(origin, isNull, upperTypeBound, refId)
    }

    override protected[domain] def ArrayValue(
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ArrayType,
        t:                 RefId
    ): DomainArrayValue = {
        DefaultArrayValue(
            origin,
            isNull,
            isPrecise || classHierarchy.isKnownToBeFinal(theUpperTypeBound),
            theUpperTypeBound,
            t
        )
    }

    override protected[domain] def MultipleReferenceValues(
        values: UIDSet[DomainSingleOriginReferenceValue]
    ): DomainMultipleReferenceValues = {
        new MultipleReferenceValues(values)
    }

    override protected[domain] def MultipleReferenceValues(
        values:            UIDSet[DomainSingleOriginReferenceValue],
        origins:           ValueOrigins,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: UIDSet[_ <: ReferenceType],
        refId:             RefId
    ): DomainMultipleReferenceValues = {
        new MultipleReferenceValues(values, origins, isNull, isPrecise, theUpperTypeBound, refId)
    }
}
