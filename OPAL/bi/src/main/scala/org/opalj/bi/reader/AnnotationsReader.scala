/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.collection.immutable.RefArray
import org.opalj.control.fillRefArray

/**
 * Generic parser to parse a list of annotations. This reader is intended to be used in
 * conjunction with the Runtime(In)Visible(Parameter)Annotations_attributeReaders.
 */
trait AnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    // NONE...

    //
    // IMPLEMENTATION
    //

    type Annotations = RefArray[Annotation]

    /**
     * Reads the annotations of a annotations attributes.
     *
     * ''' From the Specification'''
     * <pre>
     * annotation {
     *      u2 type_index;
     *      u2 num_element_value_pairs;
     *      {   u2 element_name_index;
     *          element_value value;
     *      }   element_value_pairs[num_element_value_pairs]
     * }
     * </pre>
     */
    def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations = {
        fillRefArray(in.readUnsignedShort) {
            Annotation(cp, in)
        }
    }
}
