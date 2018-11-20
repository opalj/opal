/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillRefArray
import org.opalj.collection.immutable.RefArray

/**
 * Generic parser for a method parameter's visible or invisible annotations.
 */
trait ParametersAnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ParameterAnnotations = RefArray[Annotation]

    type ParametersAnnotations = RefArray[ParameterAnnotations]

    //
    // IMPLEMENTATION
    //

    def ParametersAnnotations(cp: Constant_Pool, in: DataInputStream): ParametersAnnotations = {
        fillRefArray(in.readUnsignedByte) {
            fillRefArray(in.readUnsignedShort) {
                Annotation(cp, in)
            }
        }
    }
}
