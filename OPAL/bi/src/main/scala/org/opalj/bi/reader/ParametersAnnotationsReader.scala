/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillAnyRefArray
import org.opalj.collection.immutable.AnyRefArray

/**
 * Generic parser for a method parameter's visible or invisible annotations.
 */
trait ParametersAnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ParameterAnnotations = AnyRefArray[Annotation]

    type ParametersAnnotations = AnyRefArray[ParameterAnnotations]

    //
    // IMPLEMENTATION
    //

    def ParametersAnnotations(cp: Constant_Pool, in: DataInputStream): ParametersAnnotations = {
        fillAnyRefArray(in.readUnsignedByte) {
            fillAnyRefArray(in.readUnsignedShort) {
                Annotation(cp, in)
            }
        }
    }
}
