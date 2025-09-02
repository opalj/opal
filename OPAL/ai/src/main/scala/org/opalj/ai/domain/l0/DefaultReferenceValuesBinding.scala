/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.br.ArrayType
import org.opalj.br.ClassType
import org.opalj.collection.immutable.UIDSet

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding
    extends DefaultTypeLevelReferenceValues
    with DefaultExceptionsFactory {
    domain: IntegerValuesDomain & TypedValuesFactory & Configuration =>

    type AReferenceValue = ReferenceValueLike
    type DomainReferenceValue = AReferenceValue

    final val DomainReferenceValueTag: ClassTag[DomainReferenceValue] = implicitly

    type DomainNullValue = ANullValue
    type DomainObjectValue = AnObjectValue
    type DomainArrayValue = AnArrayValue

    val TheNullValue: DomainNullValue = new ANullValue()

    //
    // CONCRETE CLASSES AND FACTORY METHODS
    //

    protected case class DefaultSObjectValue(theUpperTypeBound: ClassType) extends SObjectValueLike {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultArrayValue(theUpperTypeBound: ArrayType) extends AnArrayValue {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultMObjectValue(
        upperTypeBound: UIDSet[ClassType]
    ) extends MObjectValueLike {
        override def isNull: Answer = Unknown
    }

    /**
     * @inheritdoc
     *
     * This implementation always returns the singleton instance [[TheNullValue]].
     */
    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def ObjectValue(origin: ValueOrigin, classType: ClassType): DomainObjectValue = {
        DefaultSObjectValue(classType)
    }

    override def ObjectValue(
        origin:         ValueOrigin,
        upperTypeBound: UIDSet[ClassType]
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
