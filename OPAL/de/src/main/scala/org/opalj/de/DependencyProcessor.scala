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
package de

import br._

/**
 * A dependency processor processes dependencies between two source elements.
 *
 * Typically, a `DependencyProcessor` is passed to a
 * [[DependencyExtractor]]. The latter calls back the `processDependency` methods
 * for each identified dependency.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
trait DependencyProcessor {

    /**
     * Called for each dependency between two source elements.
     *
     * @param source The source element that has a dependency on the `target` element.
     * @param target The source element that the `source` element depends on.
     * @param dependencyType The type of the dependency.
     */
    def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit

    /**
     * Called for each dependency of a source element on an array type.
     *
     * @note A dependency on an array type also introduces another dependency on the
     *      element type of the array type and the dependency extractor will
     *      notify the dependency processor about such calls.
     *
     * @param source The source element that has a dependency on the array type.
     * @param arrayType The array type that the `source` element depends on.
     * @param dependencyType The type of the dependency.
     */
    def processDependency(
        source: VirtualSourceElement,
        arrayType: ArrayType,
        dType: DependencyType): Unit

    /**
     * Called for each dependency of a source element on a base type (aka primitive type).
     *
     * @param source The source element that has a dependency on the base type.
     * @param baseType The base type on which the `source` element depends on.
     * @param dependencyType The type of the dependency.
     */
    def processDependency(
        source: VirtualSourceElement,
        baseType: BaseType,
        dType: DependencyType): Unit

    /**
     * Used, e.g., by the [[DependencyExtractor]] to create representations of
     * `VirtualClass`es.
     *
     * @note The [[DependencyExtractor]] creates all representations of `VirtualClass`es
     *      using this Method.
     */
    def asVirtualClass(objectType: ObjectType): VirtualClass =
        VirtualClass(objectType)

    /**
     * Used, e.g., by the [[DependencyExtractor]] to create representations of
     * `VirtualField`s.
     *
     * @note The [[DependencyExtractor]] creates all representations of `VirtualField`s
     *      using this Method.
     */
    def asVirtualField(
        declaringClassType: ObjectType, // Recall...new Int[]{1,2,3,...}.length
        name: String,
        fieldType: FieldType): VirtualField =
        VirtualField(declaringClassType, name, fieldType)

    /**
     * Used, e.g., by the [[DependencyExtractor]] to create representations of
     * `VirtualMethod`s.
     *
     * @note The [[DependencyExtractor]] creates all representations of `VirtualMethod`s
     *      using this Method.
     */
    def asVirtualMethod(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.clone()
        name: String,
        descriptor: MethodDescriptor): VirtualMethod =
        VirtualMethod(declaringClassType, name, descriptor)
}


