/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * @author Michael Eichberg
 */
trait InstructionsDeserializer
    extends DeferredInvokedynamicResolution
    with DeferredDynamicConstantResolution {

    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        source:              Array[Byte]
    ): Instructions
}
