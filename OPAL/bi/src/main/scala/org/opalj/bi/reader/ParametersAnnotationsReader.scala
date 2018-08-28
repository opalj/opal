/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Generic parser for a method parameter's visible or invisible annotations.
 */
trait ParametersAnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    implicit val AnnotationManifest: ClassTag[Annotation]

    //
    // IMPLEMENTATION
    //

    type ParameterAnnotations = IndexedSeq[Annotation]

    type ParametersAnnotations = IndexedSeq[ParameterAnnotations]

    def ParametersAnnotations(cp: Constant_Pool, in: DataInputStream): ParametersAnnotations = {
        repeat(in.readUnsignedByte) { repeat(in.readUnsignedShort) { Annotation(cp, in) } }
    }
}
