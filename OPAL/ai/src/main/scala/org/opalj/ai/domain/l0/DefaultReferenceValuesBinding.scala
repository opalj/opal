/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding
    extends DefaultTypeLevelReferenceValues
    with DefaultExceptionsFactory {
    domain: IntegerValuesDomain with TypedValuesFactory with Configuration ⇒

    type AReferenceValue = ReferenceValue
    type DomainReferenceValue = AReferenceValue

    final val DomainReferenceValueTag: ClassTag[DomainReferenceValue] = implicitly

    type DomainNullValue = NullValue
    type DomainObjectValue = ObjectValue
    type DomainArrayValue = ArrayValue

    val TheNullValue: DomainNullValue = new NullValue()

    //
    // CONCRETE CLASSES AND FACTORY METHODS
    //

    protected case class DefaultSObjectValue(theUpperTypeBound: ObjectType) extends SObjectValue {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultArrayValue(theUpperTypeBound: ArrayType) extends ArrayValue {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultMObjectValue(
            upperTypeBound: UIDSet[ObjectType]
    ) extends MObjectValue {
        override def isNull: Answer = Unknown
    }

    /**
     * @inheritdoc
     *
     * This implementation always returns the singleton instance [[TheNullValue]].
     */
    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def ObjectValue(origin: ValueOrigin, objectType: ObjectType): DomainObjectValue = {
        DefaultSObjectValue(objectType)
    }

    override def ObjectValue(
        origin:         ValueOrigin,
        upperTypeBound: UIDSet[ObjectType]
    ): DomainObjectValue = {
        if (upperTypeBound.isSingletonSet)
            ObjectValue(origin, upperTypeBound.head)
        else
            DefaultMObjectValue { upperTypeBound }
    }

    override def ArrayValue(origin: ValueOrigin, arrayType: ArrayType): DomainArrayValue = {
        DefaultArrayValue(arrayType)
    }

}
