/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

/**
 * @author Michael Eichberg
 */
trait DefaultStringValuesBinding extends DefaultReferenceValuesBinding with StringValues {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration =>

    type DomainStringValue = StringValue
    final val DomainStringValueTag: ClassTag[DomainStringValue] = implicitly

    //
    // FACTORY METHODS
    //

    protected case class DefaultStringValue(
            origin: ValueOrigin,
            value:  String,
            refId:  RefId
    ) extends StringValue

    override def StringValue(
        origin: ValueOrigin,
        value:  String,
        refId:  RefId
    ): DomainStringValue = {
        DefaultStringValue(origin, value, refId)
    }
}
