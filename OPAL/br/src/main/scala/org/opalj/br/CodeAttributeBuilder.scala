/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.collection.immutable.UShortPair

/**
 * Given a method's signature and access flags the code attribute is build
 * and some meta information - depending on the type of the code attribute builder - is collected.
 *
 * @see The BytecodeAssember framework for an example usage.
 * @author Michael Eichberg
 */
trait CodeAttributeBuilder[T] {

    /**
     * @param classFileVersion The class file version determines which attributes are allowed/
     *                         required. In particular required to determine if a
     *                         [[org.opalj.br.StackMapTable]] attribute needs to be computed.
     * @param accessFlags The access flags; required to compute max locals if necessary
     *                    (static or not?).
     * @param name The name of the method.
     * @param descriptor The method's descriptor; required to compute max locals if necessary.
     * @param classHierarchy Required if a new [[org.opalj.br.StackMapTable]] attribute needs
     *                       to be computed.
     * @return The newly build code attribute.
     */
    def apply(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType,
        accessFlags:        Int,
        name:               String,
        descriptor:         MethodDescriptor
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): (Attribute, T)

}
