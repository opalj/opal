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
package ba

import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UShortPair

/**
 * Builder for a [[org.opalj.br.MethodTemplate]].
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
class METHOD[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            Option[br.CodeAttributeBuilder[T]],
        attributes:      Seq[br.MethodAttributeBuilder]
) {

    /**
     * Returns the build [[org.opalj.br.MethodTemplate]] and its annotations (if any).
     */
    def result(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType
    )(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): (br.MethodTemplate, Option[T]) = {
        val descriptor = br.MethodDescriptor(this.descriptor)
        val accessFlags = accessModifiers.accessFlags

        val attributes = this.attributes.map(attributeBuilder ⇒
            attributeBuilder(accessFlags, name, descriptor))

        if (code.isDefined) {
            val (attribute, annotations) =
                code.get(classFileVersion, declaringClassType, accessFlags, name, descriptor)
            val method = br.Method(accessFlags, name, descriptor, attributes :+ attribute)
            (method, Some(annotations))
        } else {
            val method = br.Method(accessFlags, name, descriptor, attributes)
            (method, None)
        }
    }

}

object METHOD {

    def apply[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            Option[br.CodeAttributeBuilder[T]],
        attributes:      Seq[br.MethodAttributeBuilder]
    ): METHOD[T] = {
        new METHOD(accessModifiers, name, descriptor, code, attributes)
    }

    def apply[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            br.CodeAttributeBuilder[T],
        attributes:      Seq[br.MethodAttributeBuilder] = Seq.empty
    ): METHOD[T] = {
        new METHOD(accessModifiers, name, descriptor, Some(code), attributes)
    }

}
