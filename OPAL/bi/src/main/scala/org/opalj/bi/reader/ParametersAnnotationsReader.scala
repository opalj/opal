/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq

/**
 * Generic parser for a method parameter's visible or invisible annotations.
 */
trait ParametersAnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ParameterAnnotations = ArraySeq[Annotation]

    type ParametersAnnotations = ArraySeq[ParameterAnnotations]

    //
    // IMPLEMENTATION
    //

    def ParametersAnnotations(cp: Constant_Pool, in: DataInputStream): ParametersAnnotations = {
        fillArraySeq(in.readUnsignedByte) {
            fillArraySeq(in.readUnsignedShort) {
                Annotation(cp, in)
            }
        }
    }
}
