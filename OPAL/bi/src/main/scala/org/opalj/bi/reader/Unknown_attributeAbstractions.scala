/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

trait Unknown_attributeAbstractions extends Constant_PoolAbstractions with AttributesAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Unknown_attribute <: Attribute

    def Unknown_attribute(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): Unknown_attribute

}
