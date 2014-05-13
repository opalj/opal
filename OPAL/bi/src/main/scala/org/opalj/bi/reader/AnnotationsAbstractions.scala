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
 * Annotation related definitions.
 *
 * @author Michael Eichberg
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
        constant_pool: Constant_Pool,
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs): Annotation

    //
    // IMPLEMENTATION
    //

    def Annotation(cp: Constant_Pool, in: DataInputStream): Annotation = {
        Annotation(cp, in.readUnsignedShort, ElementValuePairs(cp, in))
    }
}
