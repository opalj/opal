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
package br
package reader

import org.opalj.bi.reader.AnnotationsReader
import org.opalj.bi.reader.RuntimeInvisibleAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeVisibleAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeInvisibleParameterAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeVisibleParameterAnnotations_attributeReader
import org.opalj.bi.reader.ParameterAnnotationsReader
import org.opalj.bi.reader.AnnotationDefault_attributeReader

/**
 * Factory methods to create representations of the attributes related to
 * Java annotations.
 *
 * @author Michael Eichberg
 */
trait AnnotationAttributesBinding
        extends AnnotationsBinding
        with AnnotationsReader
        with ParameterAnnotationsReader
        with RuntimeInvisibleAnnotations_attributeReader
        with RuntimeVisibleAnnotations_attributeReader
        with RuntimeInvisibleParameterAnnotations_attributeReader
        with RuntimeVisibleParameterAnnotations_attributeReader
        with AnnotationDefault_attributeReader
        with AttributeBinding {

    type AnnotationDefault_attribute = ElementValue

    type RuntimeVisibleAnnotations_attribute = RuntimeVisibleAnnotationTable

    type RuntimeInvisibleAnnotations_attribute = RuntimeInvisibleAnnotationTable

    type RuntimeVisibleParameterAnnotations_attribute = RuntimeVisibleParameterAnnotationTable

    type RuntimeInvisibleParameterAnnotations_attribute = RuntimeInvisibleParameterAnnotationTable

    def AnnotationDefault_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        element_value: ElementValue) = {
        element_value
    }

    def RuntimeVisibleAnnotations_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        annotations: Annotations) =
        new RuntimeVisibleAnnotations_attribute(annotations)

    def RuntimeInvisibleAnnotations_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        annotations: Annotations) =
        new RuntimeInvisibleAnnotations_attribute(annotations)

    def RuntimeVisibleParameterAnnotations_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        parameter_annotations: ParameterAnnotations) =
        new RuntimeVisibleParameterAnnotations_attribute(parameter_annotations)

    def RuntimeInvisibleParameterAnnotations_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        parameter_annotations: ParameterAnnotations) =
        new RuntimeInvisibleParameterAnnotations_attribute(parameter_annotations)

}


