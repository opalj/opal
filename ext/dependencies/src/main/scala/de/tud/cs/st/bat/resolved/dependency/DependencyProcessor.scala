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
package de.tud.cs.st
package bat
package resolved
package dependency

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

    def asVirtualClass(objectType: ObjectType): VirtualClass =
        VirtualClass(objectType)

    def asVirtualField(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.length
        name: String,
        fieldType: FieldType): VirtualField =
        VirtualField(declaringClassType, name, fieldType)

    def asVirtualMethod(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.clone()
        name: String,
        descriptor: MethodDescriptor): VirtualMethod =
        VirtualMethod(declaringClassType, name, descriptor)
}

class DependencyCountingDependencyProcessor extends DependencyProcessor {

    protected[this] val dendencyCount = new java.util.concurrent.atomic.AtomicInteger(0)

    override def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {
        dendencyCount.incrementAndGet()
    }

    protected[this] val dendencyOnArraysCount = new java.util.concurrent.atomic.AtomicInteger(0)

    override def processDependency(
        source: VirtualSourceElement,
        arrayType: ArrayType,
        dType: DependencyType): Unit = {
        dendencyOnArraysCount.incrementAndGet()
    }

    protected[this] val dendencyOnPrimitivesCount = new java.util.concurrent.atomic.AtomicInteger(0)
    override def processDependency(
        source: VirtualSourceElement,
        baseType: BaseType,
        dType: DependencyType): Unit = {
        dendencyOnPrimitivesCount.incrementAndGet()
    }

    protected val DummyClassType = ObjectType("<-DUMMY_CLASSTYPE->")

    protected val DummyVirtualClass =
        VirtualClass(DummyClassType)

    override def asVirtualClass(objectType: ObjectType): VirtualClass = {
        DummyVirtualClass
    }

    protected val DummyVirtualField =
        VirtualField(DummyClassType, "<-DUMMY_FIELD->", DummyClassType)

    override def asVirtualField(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.length
        name: String,
        fieldType: FieldType): VirtualField = {
        DummyVirtualField
    }

    protected val DummyVirtualMethod =
        VirtualMethod(DummyClassType, "<-DUMMY_FIELD->", MethodDescriptor.NoArgsAndReturnVoid)

    override def asVirtualMethod(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.clone()
        name: String,
        descriptor: MethodDescriptor): VirtualMethod = {
        DummyVirtualMethod
    }

}

