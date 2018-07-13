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
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration with TheClassHierarchy ⇒

    // Let's fix the type hierarchy

    type AReferenceValue = ReferenceValue
    final val AReferenceValue: ClassTag[AReferenceValue] = implicitly
    type DomainReferenceValue = AReferenceValue
    final val DomainReferenceValue: ClassTag[DomainReferenceValue] = implicitly

    type DomainSingleOriginReferenceValue = SingleOriginReferenceValue
    final val DomainSingleOriginReferenceValue: ClassTag[DomainSingleOriginReferenceValue] = implicitly

    type DomainNullValue = NullValue
    final val DomainNullValue: ClassTag[DomainNullValue] = implicitly

    type DomainObjectValue = ObjectValue
    final val DomainObjectValue: ClassTag[DomainObjectValue] = implicitly

    type DomainArrayValue = ArrayValue
    final val DomainArrayValue: ClassTag[DomainArrayValue] = implicitly

    type DomainMultipleReferenceValues = MultipleReferenceValues
    final val DomainMultipleReferenceValues: ClassTag[DomainMultipleReferenceValues] = implicitly

    //
    // FACTORY METHODS
    //

    override def NullValue(origin: ValueOrigin): DomainNullValue = new NullValue(origin)

    override protected[domain] def ObjectValue(
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ObjectType,
        refId:             RefId
    ): SObjectValue = {
        new SObjectValue(
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
            new MObjectValue(origin, isNull, upperTypeBound, refId)
    }

    override protected[domain] def ArrayValue(
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ArrayType,
        t:                 RefId
    ): DomainArrayValue = {
        new ArrayValue(
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
