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
package fpcf

import java.io.File

import org.opalj.br.analyses.Project
import java.net.URL

import org.opalj.br.DefinedMethod
import org.opalj.br.Annotation
import org.opalj.br.ElementValuePair
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
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
        ObjectType("org/opalj/fpcf/properties/declared_methods/DeclaredMethod")
    val multiAnnotationType =
        ObjectType("org/opalj/fpcf/properties/declared_methods/DeclaredMethods")

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
            cf.thisType.packageName.startsWith("org/opalj/fpcf/fixtures/declared_methods")
        }

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(projectClassFiles)
    }

    var declaredMethods = FixtureProject.get(DeclaredMethodsKey).declaredMethods.toList

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

    it("should not create excess declared methods") {
        assert(declaredMethods.isEmpty)
    }

    def checkDeclaredMethod(classType: ObjectType, annotation: Annotation): Unit = {
        val name = getValue(annotation, "name").asStringValue.value
        val descriptor = MethodDescriptor(getValue(annotation, "descriptor").asStringValue.value)
        val declaringClass = getValue(annotation, "declaringClass").asClassValue.value.asObjectType

        val method = FixtureProject.classFile(declaringClass).get.findMethod(name, descriptor).get

        val (matching, newDMs) = declaredMethods.partition(_ == DefinedMethod(classType, method))

        it(s"${classType.simpleName}: ${declaringClass.simpleName}.$name$descriptor") {
            assert(matching.size == 1)
        }
        declaredMethods = newDMs
    }

    def getValue(a: Annotation, name: String) = {
        a.elementValuePairs.collectFirst { case ElementValuePair(`name`, value) ⇒ value }.get
    }
}
