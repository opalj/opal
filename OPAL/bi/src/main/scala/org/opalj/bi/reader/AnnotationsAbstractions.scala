/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Annotation related definitions.
 */
trait AnnotationAbstractions extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type ElementValuePairs

    def ElementValuePairs(cp: Constant_Pool, in: DataInputStream): ElementValuePairs

    // A TypeAnnotation's/an Annotation's element value can be an annotation.
    type Annotation

    def Annotation(
        constant_pool:       Constant_Pool,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): Annotation

    //
    // IMPLEMENTATION
    //

    def Annotation(cp: Constant_Pool, in: DataInputStream): Annotation = {
        Annotation(cp, in.readUnsignedShort, ElementValuePairs(cp, in))
    }
}
