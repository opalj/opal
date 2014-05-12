/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bi
package reader

import reflect.ClassTag

import java.io.DataInputStream

/**
 * Generic parser for type annotations. This
 * reader is intended to be used in conjunction with the
 * Runtime(In)VisibleTypeAnnotations_attributeReaders.
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationsReader extends AnnotationAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type TypeAnnotationTarget
    def TypeAnnotationTarget(cp: Constant_Pool, in: DataInputStream): TypeAnnotationTarget

    type TypeAnnotationPath
    def TypeAnnotationPath(cp: Constant_Pool, in: DataInputStream): TypeAnnotationPath

    type TypeAnnotation
    implicit val TypeAnnotationManifest: ClassTag[TypeAnnotation]

    def TypeAnnotation(
        constant_pool: Constant_Pool,
        target: TypeAnnotationTarget,
        path: TypeAnnotationPath,
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs): TypeAnnotation

    //
    // IMPLEMENTATION
    //

    type TypeAnnotations = IndexedSeq[TypeAnnotation]

    /**
     * Reads a Runtime(In)VisibleTypeAnnotations attribute.
     *
     * ''' From the Specification'''
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
            TypeAnnotationTarget(cp, in),
            TypeAnnotationPath(cp, in),
            in.readUnsignedShort() /*type_index*/ ,
            ElementValuePairs(cp, in))
    }
}


