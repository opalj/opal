/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.MethodParameters_attributeReader

/**
 * Implements the factory methods to create method parameter tables and their entries.
 *
 * @author Michael Eichberg
 */
trait MethodParameters_attributeBinding
    extends MethodParameters_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type MethodParameter = br.MethodParameter
    override val MethodParameterManifest: ClassTag[MethodParameter] = implicitly

    type MethodParameters_attribute = br.MethodParameterTable

    override def MethodParameters_attribute(
        cpconstant_pool:      Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        parameters:           MethodParameters
    ): MethodParameters_attribute = {
        new MethodParameterTable(parameters)
    }

    override def MethodParameter(
        constant_pool: Constant_Pool,
        name_index:    Constant_Pool_Index,
        access_flags:  Int
    ): MethodParameter = {
        val parameterName =
            if (name_index == 0)
                None // it is a so-called formal parameter
            else
                Some(constant_pool(name_index).asString)
        new MethodParameter(parameterName, access_flags)
    }
}
