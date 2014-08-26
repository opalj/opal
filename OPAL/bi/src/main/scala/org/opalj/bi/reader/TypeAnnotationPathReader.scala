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

import java.io.DataInputStream

import scala.reflect.ClassTag

/**
 * Generic parser for the `type_path` field of type annotations. This
 * reader is intended to be used in conjunction with the
 * [[TypeAnnotationsReader]].
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationPathReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type TypeAnnotationPath

    /**
     * The path's length was `0`.
     */
    def TypeAnnotationDirectlyOnType: TypeAnnotationPath

    type TypeAnnotationPathElement

    def TypeAnnotationPath(path: IndexedSeq[TypeAnnotationPathElement]): TypeAnnotationPath

    /**
     * The `type_path_kind` was `0` (and the type_argument_index was also `0`).
     */
    def TypeAnnotationDeeperInArrayType: TypeAnnotationPathElement

    /**
     * The `type_path_kind` was `1` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationDeeperInNestedType: TypeAnnotationPathElement

    /**
     * The `type_path_kind` was `2` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationOnBoundOfWildcardType: TypeAnnotationPathElement

    def TypeAnnotationOnTypeArgument(type_argument_index: Int): TypeAnnotationPathElement

    //
    // IMPLEMENTATION
    //

    def TypeAnnotationPath(cp: Constant_Pool, in: DataInputStream): TypeAnnotationPath = {
        val path_length = in.readUnsignedByte()
        if (path_length == 0) {
            TypeAnnotationDirectlyOnType
        } else {
            TypeAnnotationPath(
                repeat(path_length) {
                    val type_path_kind = in.readUnsignedByte()
                    (type_path_kind: @scala.annotation.switch) match {
                        case 0 ⇒
                            in.read() // <=> in.skip..
                            TypeAnnotationDeeperInArrayType
                        case 1 ⇒
                            in.read() // <=> in.skip..
                            TypeAnnotationDeeperInNestedType
                        case 2 ⇒
                            in.read() // <=> in.skip..
                            TypeAnnotationOnBoundOfWildcardType
                        case 3 ⇒
                            TypeAnnotationOnTypeArgument(in.readUnsignedByte())
                    }
                }
            )
        }
    }
}

