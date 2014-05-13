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
 * A dependency processor that just counts the number of dependencies.
 *
 * Typically, a `DependencyProcessor` is passed to a
 * [[DependencyExtractor]]. The latter calls back the `processDependency` methods
 * for each identified dependency.
 *
 * @author Michael Eichberg
 */
class DependencyCountingDependencyProcessor extends DependencyProcessor {

    import java.util.concurrent.atomic.AtomicInteger

    protected[this] val dependencyCount = new AtomicInteger(0)
    override def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {
        dependencyCount.incrementAndGet()
    }
    def currentDependencyCount = dependencyCount.get

    protected[this] val dependencyOnArraysCount = new AtomicInteger(0)
    override def processDependency(
        source: VirtualSourceElement,
        arrayType: ArrayType,
        dType: DependencyType): Unit = {
        dependencyOnArraysCount.incrementAndGet()
    }
    def currentDependencyOnArraysCount = dependencyOnArraysCount.get

    protected[this] val dependencyOnPrimitivesCount = new AtomicInteger(0)
    override def processDependency(
        source: VirtualSourceElement,
        baseType: BaseType,
        dType: DependencyType): Unit = {
        dependencyOnPrimitivesCount.incrementAndGet()
    }
    def currentDependencyOnPrimitivesCount = dependencyOnPrimitivesCount.get

    final val DummyClassType = ObjectType("<-DUMMY_CLASSTYPE->")

    final val DummyVirtualClass = VirtualClass(DummyClassType)

    override def asVirtualClass(objectType: ObjectType): VirtualClass = {
        DummyVirtualClass
    }

    final val DummyVirtualField =
        VirtualField(DummyClassType, "<-DUMMY_FIELD->", DummyClassType)

    override def asVirtualField(
        declaringClassType: ObjectType, // Recall...new Int[]{1,2,3,...}.length => arraylength
        name: String,
        fieldType: FieldType): VirtualField = {
        DummyVirtualField
    }

    final val DummyVirtualMethod =
        VirtualMethod(
            DummyClassType,
            "<-DUMMY_METHOD->",
            MethodDescriptor.NoArgsAndReturnVoid)

    override def asVirtualMethod(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.clone()
        name: String,
        descriptor: MethodDescriptor): VirtualMethod = {
        DummyVirtualMethod
    }

}

