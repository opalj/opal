/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * @author Michael Eichberg
 */
trait InstructionsDeserializer extends DeferredInvokedynamicResolution {

    def Instructions(
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp:                  Constant_Pool,
        source:              Array[Byte]
    ): Instructions
}
