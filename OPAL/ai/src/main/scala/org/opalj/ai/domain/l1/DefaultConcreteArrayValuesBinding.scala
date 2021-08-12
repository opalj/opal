/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.br.ArrayType

/**
 * @author Michael Eichberg
 */
trait DefaultConcreteArrayValuesBinding
    extends DefaultArrayValuesBinding
    with ConcreteArrayValues {
    domain: CorrelationalDomain with ConcreteIntegerValues with LogContextProvider =>

    type DomainConcreteArrayValue = ConcreteArrayValue
    final val DomainConcreteArrayValueTag: ClassTag[DomainConcreteArrayValue] = implicitly

    //
    // FACTORY METHODS
    //

    private case class DefaultConcreteArrayValue(
            origin:            ValueOrigin,
            theUpperTypeBound: ArrayType,
            values:            Array[DomainValue],
            refId:             RefId
    ) extends ConcreteArrayValue

    override def ArrayValue(
        origin:            ValueOrigin,
        theUpperTypeBound: ArrayType,
        values:            Array[DomainValue],
        refId:             RefId
    ): DomainConcreteArrayValue = {
        DefaultConcreteArrayValue(origin, theUpperTypeBound, values, refId)
    }

}
