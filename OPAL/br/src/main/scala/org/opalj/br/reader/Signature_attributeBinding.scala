/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.AttributeParent
import org.opalj.bi.reader.Signature_attributeReader

/**
 * Implements the factory method defined and used by the generic signature attribute reader.
 *
 * @author Michael Eichberg
 */
trait Signature_attributeBinding
    extends Signature_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Signature_attribute = Signature

    def Signature_attribute(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        signature_index:      Constant_Pool_Index
    ): Signature_attribute = {
        cp(signature_index).asSignature(ap)
    }

}

