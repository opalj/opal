/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * @author Michael Eichberg
 */
trait InstructionsDeserializer extends DeferredInvokedynamicResolution {

    def Instructions(cp: Constant_Pool, source: Array[Byte]): Instructions
}
