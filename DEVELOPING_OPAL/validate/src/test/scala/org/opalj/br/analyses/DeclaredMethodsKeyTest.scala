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
package analyses

import java.io.File
import java.net.URL

import org.opalj.util.ScalaMajorVersion
import org.scalatest.Matchers
import org.scalatest.FunSpec

/**
 * Tests whether the DeclaredMethodsKey creates the correct declared method objects for each class
 *
 * @author Dominik Helm
 */
class DeclaredMethodsKeyTest extends FunSpec with Matchers {

    val singleAnnotationType =
        ObjectType("org/opalj/br/analyses/properties/declared_methods/DeclaredMethod")
    val multiAnnotationType =
        ObjectType("org/opalj/br/analyses/properties/declared_methods/DeclaredMethods")

    /**
     * The representation of the fixture project.
     */
    final val FixtureProject: Project[URL] = {
        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val sourceFolder = s"DEVELOPING_OPAL/validate/target/scala-$ScalaMajorVersion/test-classes"
        val fixtureFiles = new File(sourceFolder)
        val fixtureClassFiles = ClassFiles(fixtureFiles)
        if (fixtureClassFiles.isEmpty) fail(s"no class files at $fixtureFiles")

        val projectClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/br/analyses/fixtures/declared_methods")
        }

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(projectClassFiles)
    }

    val declaredMethodsKey: DeclaredMethods = FixtureProject.get(DeclaredMethodsKey)
    val numDeclaredMethods: Int = declaredMethodsKey.declaredMethods.size

    val declaredMethods: Set[DeclaredMethod] = declaredMethodsKey.declaredMethods.toSet

    it("should not have duplicate declared methods") {
        assert(declaredMethods.size == numDeclaredMethods, "duplicate methods found")
    }

    var numAnnotated = 0

    for {
        cf ← FixtureProject.allProjectClassFiles
        annotations = cf.runtimeInvisibleAnnotations
        if annotations.nonEmpty
        annotation ← annotations
        annotationType = annotation.annotationType
        if annotationType == singleAnnotationType || annotationType == multiAnnotationType
    } {
        val classType = cf.thisType
        if (annotationType == singleAnnotationType)
            checkDeclaredMethod(classType, annotation)
        else {
            for (value ← getValue(annotation, "value").asArrayValue.values) {
                val annotation = value.asAnnotationValue.annotation
                checkDeclaredMethod(classType, annotation)
            }
        }
    }

    // The number of defined methods should be the same as the number of annotations processed, i.e.
    // there may not be any additional defined method. There may be VirtualDeclaredMethods for
    // predefined methods from the JDK, though.
    it("should not create excess declared methods") {
        val definedMethods = declaredMethods.filter(_.hasDefinition)
        assert(definedMethods.size == numAnnotated, "found unexpected defined methods")
    }

    def checkDeclaredMethod(classType: ObjectType, annotation: Annotation): Unit = {
        val name = getValue(annotation, "name").asStringValue.value
        val descriptor = MethodDescriptor(getValue(annotation, "descriptor").asStringValue.value)
        val declaringClass = getValue(annotation, "declaringClass").asClassValue.value.asObjectType

        val methodO = FixtureProject.classFile(declaringClass).get.findMethod(name, descriptor)

        if (methodO.isEmpty)
            fail(s"method $declaringClass.${descriptor.toJava(name)} not found in fixture project")
        val method = methodO.get

        val expected = DefinedMethod(classType, method)

        it(s"${classType.simpleName}: ${declaringClass.simpleName}.${descriptor.toJava(name)}") {
            assert(declaredMethods.contains(expected))
        }

        numAnnotated += 1
    }

    def getValue(a: Annotation, name: String): ElementValue = {
        a.elementValuePairs.collectFirst { case ElementValuePair(`name`, value) ⇒ value }.get
    }
}
