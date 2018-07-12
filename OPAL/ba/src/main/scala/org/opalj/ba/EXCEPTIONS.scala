/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

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
        br.ExceptionTable(exceptionTypes.map(br.ObjectType.apply))
    }

}
