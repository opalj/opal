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
    domain: CorrelationalDomain with ConcreteIntegerValues with TheClassHierarchy with LogContextProvider ⇒

    type DomainConcreteArrayValue = ConcreteArrayValue
    final val DomainConcreteArrayValueTag: ClassTag[DomainConcreteArrayValue] = implicitly

    //
    // FACTORY METHODS
    //

    override def ArrayValue(
        origin:  ValueOrigin,
        theType: ArrayType,
        values:  Array[DomainValue],
        refId:   RefId
    ): DomainConcreteArrayValue = {
        new ConcreteArrayValue(origin, theType, values, refId)
    }

}
