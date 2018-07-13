/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext

/**
 * Template method to skip an unknown attribute. I.e., the information will
 * not be represented at runtime.
 */
trait SkipUnknown_attributeReader extends Unknown_attributeAbstractions {
    this: Constant_PoolReader ⇒

    type Unknown_attribute = Null

    //
    // IMPLEMENTATION
    //

    def Unknown_attribute(
        ap:                   AttributeParent,
        cp:                   Constant_Pool,
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
                )(GlobalLogContext)
                return null;
            }
        }
        null
    }
}
