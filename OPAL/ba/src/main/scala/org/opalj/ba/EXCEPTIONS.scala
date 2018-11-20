/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.RefArray

/**
 * Builder for the [[org.opalj.br.ExceptionTable]] attribute.
 *
 * @author Michael Eichberg
 */
case class EXCEPTIONS(exceptionTypes: String*) extends br.MethodAttributeBuilder {

    def apply(
        accessFlags: Int,
        name:        String,
        descriptor:  br.MethodDescriptor
    ): br.ExceptionTable = {
        br.ExceptionTable(
            RefArray.from(exceptionTypes.toArray[AnyRef])._UNSAFE_mapped(ObjectType.apply)
        )
    }

}
