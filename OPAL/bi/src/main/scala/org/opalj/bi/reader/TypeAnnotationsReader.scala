/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream
import org.opalj.control.repeat

/**
 * Generic parser for type annotations. This
 * reader is intended to be used in conjunction with the
 * Runtime(In)VisibleTypeAnnotations_attributeReaders.
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type TypeAnnotationTarget
    def TypeAnnotationTarget(in: DataInputStream): TypeAnnotationTarget

    type TypeAnnotationPath
    def TypeAnnotationPath(in: DataInputStream): TypeAnnotationPath

    type TypeAnnotation
    implicit val TypeAnnotationManifest: ClassTag[TypeAnnotation]

    def TypeAnnotation(
        constant_pool:       Constant_Pool,
        target:              TypeAnnotationTarget,
        path:                TypeAnnotationPath,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): TypeAnnotation

    //
    // IMPLEMENTATION
    //

    type TypeAnnotations = IndexedSeq[TypeAnnotation]

    /**
     * Reads a Runtime(In)VisibleTypeAnnotations attribute.
     *
     * <pre>
     * type_annotation {
     *            u1 target_type;
     *            union {
     *                type_parameter_target;
     *                supertype_target;
     *                type_parameter_bound_target;
     *                empty_target;
     *                method_formal_parameter_target;
     *                throws_target;
     *                localvar_target;
     *                catch_target;
     *                offset_target;
     *                type_argument_target;
     *            } target_info;
     *            type_path target_path;
     *            u2        type_index;
     *            u2        num_element_value_pairs;
     *            { u2              element_name_index;
     *              element_value   value;
     *            } element_value_pairs[num_element_value_pairs];
     * }
     * </pre>
     */
    def TypeAnnotations(cp: Constant_Pool, in: DataInputStream): TypeAnnotations = {
        repeat(in.readUnsignedShort) {
            TypeAnnotation(cp, in)
        }
    }

    def TypeAnnotation(cp: Constant_Pool, in: DataInputStream): TypeAnnotation = {
        TypeAnnotation(
            cp,
            TypeAnnotationTarget(in),
            TypeAnnotationPath(in),
            in.readUnsignedShort() /*type_index*/ ,
            ElementValuePairs(cp, in)
        )
    }
}
