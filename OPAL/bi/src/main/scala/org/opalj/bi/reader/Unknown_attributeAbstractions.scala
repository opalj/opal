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
        ap: AttributeParent,
        // The scope in which the attribute is defined
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index,
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): Unknown_attribute

}
