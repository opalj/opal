/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import java.io.DataInputStream

/**
 * Generic parser for the `RuntimeInvisibleParameterAnnotations` attribute.
 *
 * @author Michael Eichberg
 */
trait RuntimeInvisibleParameterAnnotations_attributeReader extends AttributeReader {

    type ParameterAnnotations
    type ParametersAnnotations <: Traversable[ParameterAnnotations]
    /**
     * Method that delegates to another reader to parse the annotations of the parameters.
     */
    def ParametersAnnotations(cp: Constant_Pool, in: DataInputStream): ParametersAnnotations

    type RuntimeInvisibleParameterAnnotations_attribute >: Null <: Attribute

    /**
     * Factory method to create a representation of a
     * `RuntimeInvisibleParameterAnnotations_attribute`.
     */
    protected def RuntimeInvisibleParameterAnnotations_attribute(
        constant_pool:         Constant_Pool,
        attribute_name_index:  Constant_Pool_Index,
        parameter_annotations: ParametersAnnotations
    ): RuntimeInvisibleParameterAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * RuntimeInvisibleParameterAnnotations_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u1 num_parameters;
     *  {
     *      u2 num_annotations;
     *      annotation annotations[num_annotations];
     *  } parameter_annotations[num_parameters];
     * }
     * </pre>
     */
    private[this] def parser(
        ap:                   AttributeParent,
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): RuntimeInvisibleParameterAnnotations_attribute = {
        /*val attribute_length = */ in.readInt()
        val parameter_annotations = ParametersAnnotations(cp, in)
        if (parameter_annotations.nonEmpty || reifyEmptyAttributes) {
            RuntimeInvisibleParameterAnnotations_attribute(
                cp, attribute_name_index, parameter_annotations
            )
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeInvisibleParameterAnnotationsAttribute.Name → parser)

}

