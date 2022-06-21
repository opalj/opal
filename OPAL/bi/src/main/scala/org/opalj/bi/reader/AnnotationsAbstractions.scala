/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import scala.reflect.ClassTag

/**
 * Annotation related definitions.
 */
trait AnnotationsAbstractions extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    // A TypeAnnotation's/an Annotation's element value can be an annotation.
    type Annotation <: AnyRef
    implicit val annotationType: ClassTag[Annotation] // TODO: Replace in Scala 3 with `type Annotation: ClassTag`

    type ElementValuePairs

    def Annotation(
        constant_pool:       Constant_Pool,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): Annotation

    def ElementValuePairs(cp: Constant_Pool, in: DataInputStream): ElementValuePairs

    //
    // IMPLEMENTATION
    //

    def Annotation(cp: Constant_Pool, in: DataInputStream): Annotation = {
        Annotation(cp, in.readUnsignedShort, ElementValuePairs(cp, in))
    }
}
