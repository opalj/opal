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

import java.net.URL

import org.scalatest.FunSuite

import org.opalj.bi.TestSupport.locateTestResources

import br._
import br.reader.Java8Framework.ClassFiles

import DependencyType._

/**
 * Functionality useful when testing a dependency extractor.
 *
 * @author Michael Eichberg
 */
object DependencyExtractorFixture extends FunSuite {

    val FIELD_AND_METHOD_SEPARATOR = "."

    def sourceElementName(t: Type): String =
        if (t.isArrayType) t.asArrayType.elementType.toJava else t.toJava

    def sourceElementName(vClass: VirtualClass): String =
        sourceElementName(vClass.thisType)

    def sourceElementName(vField: VirtualField): String =
        sourceElementName(vField.declaringClassType) +
            FIELD_AND_METHOD_SEPARATOR +
            vField.name

    def sourceElementName(vMethod: VirtualMethod): String =
        sourceElementName(vMethod.declaringClassType) +
            FIELD_AND_METHOD_SEPARATOR +
            methodDescriptorToString(vMethod.name, vMethod.descriptor)

    def methodDescriptorToString(
        name: String,
        descriptor: MethodDescriptor): String = {
        name+"("+descriptor.parameterTypes.map { sourceElementName(_) }.mkString(", ")+")"
    }

    def vseToString(vse: VirtualSourceElement): String = {
        vse match {
            case vc: VirtualClass  ⇒ sourceElementName(vc)
            case vm: VirtualMethod ⇒ sourceElementName(vm)
            case vf: VirtualField  ⇒ sourceElementName(vf)
        }
    }

    def extractDependencies(
        folder: String,
        jarFile: String,
        createDependencyExtractor: (DependencyProcessor) ⇒ DependencyExtractor): Map[(String, String, DependencyType), Int] = {
        var dependencies: Map[(String, String, DependencyType), Int] = Map.empty

        val dependencyExtractor =
            createDependencyExtractor(
                new DependencyProcessorAdapter() {

                    override def processDependency(
                        source: VirtualSourceElement,
                        target: VirtualSourceElement,
                        dType: DependencyType): Unit = {
                        val key = ((vseToString(source), vseToString(target), dType))
                        dependencies = dependencies.updated(key,
                            dependencies.getOrElse(key, 0) + 1
                        )
                    }
                }
            )
        def resources() = locateTestResources(jarFile, folder)
        for ((classFile, _) ← ClassFiles(resources())) {
            dependencyExtractor.process(classFile)
        }
        dependencies
    }

}

