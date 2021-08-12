/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.log.OPALLogger

/**
 * Template method to skip an unknown attribute. I.e., the information will
 * not be represented at runtime.
 */
trait SkipUnknown_attributeReader
    extends Unknown_attributeAbstractions
    with ClassFileReaderConfiguration {
    this: Constant_PoolReader =>

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Unknown_attribute = Null

    //
    // IMPLEMENTATION
    //

    def Unknown_attribute(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): Null = {
        val size: Int = in.readInt
        var skipped: Int = 0
        while (skipped < size) {
            val actuallySkipped = in skipBytes (size - skipped) // skip returns a long value...
            if (actuallySkipped > 0)
                skipped += actuallySkipped
            else {
                OPALLogger.error(
                    "class file reader",
                    s"skipping over unknown attribute ${cp(attribute_name_index).asString} failed"
                )
                return null;
            }
        }
        null
    }
}
