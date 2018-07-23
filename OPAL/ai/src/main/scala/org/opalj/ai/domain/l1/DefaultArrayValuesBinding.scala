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
trait DefaultArrayValuesBinding extends DefaultReferenceValuesBinding with ArrayValues {
    domain: CorrelationalDomain with ConcreteIntegerValues with TheClassHierarchy ⇒

    type DomainInitializedArrayValue = InitializedArrayValue
    final val DomainInitializedArrayValueTag: ClassTag[DomainInitializedArrayValue] = implicitly

    //
    // FACTORY METHODS
    //

    override def InitializedArrayValue(
        origin:    ValueOrigin,
        arrayType: ArrayType,
        counts:    Int,
        refId:     RefId
    ): DomainInitializedArrayValue = {
        new InitializedArrayValue(origin, arrayType, counts, refId)
    }

}
