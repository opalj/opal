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

import analyses.Project
import ai.invokedynamic._
import instructions.INVOKEDYNAMIC
import reader.Java7Framework.ClassFiles
import DependencyType._
import org.scalatest.FunSuite
import java.net.URL

/**
 * Tests that the dependency extractor with support for invokedynamic does not miss some 
 * dependencies and that it does not extract "unexpected" dependencies.
 * 
 * @author Arne Lottmann
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DependencyExtractorInvokedynamicTest extends FunSuite {

    test("Dependency extraction") {

        var dependencies: List[(String, String, DependencyType)] = Nil

        var nodes = new scala.collection.mutable.ArrayBuffer[String](1000)

        object SourceElementIDsProvider extends SourceElementIDs {

            val FIELD_AND_METHOD_SEPARATOR = "."

            def sourceElementID(identifier: String): Int = {
                var index = nodes.indexOf(identifier)
                if (index == -1) {
                    nodes += identifier
                    index = nodes.length - 1
                }
                index
            }

            def sourceElementID(t: Type): Int =
                sourceElementID(getNameOfUnderlyingType(t))

            def sourceElementID(definingObjectType: ObjectType, fieldName: String): Int =
                sourceElementID(getNameOfUnderlyingType(definingObjectType) + FIELD_AND_METHOD_SEPARATOR + fieldName)

            def sourceElementID(definingReferenceType: ReferenceType, methodName: String, methodDescriptor: MethodDescriptor): Int =
                sourceElementID(getNameOfUnderlyingType(definingReferenceType) + FIELD_AND_METHOD_SEPARATOR + getMethodAsName(methodName, methodDescriptor))

            private def getMethodAsName(methodName: String, methodDescriptor: MethodDescriptor): String = {
                methodName+"("+methodDescriptor.parameterTypes.map(pT ⇒ getNameOfUnderlyingType(pT)).mkString(", ")+")"
            }

            private def getNameOfUnderlyingType(obj: Type): String =
                if (obj.isArrayType)
                    obj.asInstanceOf[ArrayType].elementType.toJava
                else
                    obj.toJava
        }

        val project = Project(TestSupport.locateTestResources("classfiles/invokedynamic_dependencies.jar", "ext/dependencies"))
        
        val dependencyExtractor = new DependencyExtractorForInvokedynamic(SourceElementIDsProvider, project) with NoSourceElementsVisitor {
            override def resolver: InvokedynamicResolver = new InvokedynamicResolver {
                override def resolveInvokedynamic(instruction: INVOKEDYNAMIC): ResolutionResult = ResolutionFailed(instruction)
            }
            
            def processDependency(src: Int, trgt: Int, dType: DependencyType) {
                val srcNode = nodes(src)
                val trgtNode = nodes(trgt)
                dependencies = (srcNode, trgtNode, dType) :: dependencies
            }
        }

        /**
         * Dependencies unrelated to invokedynamic.
         */
        def assertStaticDependencies() {
            assertDependency("dependencies.SameClassDependencies.noArgumentsMethod()", "dependencies.SameClassDependencies", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.SameClassDependencies.noArgumentsMethod()", "dependencies.SameClassDependencies", IS_INSTANCE_MEMBER_OF)
            assertDependency("dependencies.SameClassDependencies.dependencies()", "dependencies.SameClassDependencies", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.SameClassDependencies.dependencies()", "dependencies.SameClassDependencies", IS_INSTANCE_MEMBER_OF)
        }
        
        /**
         * Dependencies related to invokedynamic found by the generic DependencyExtractor that does not resolve any invokedynamic calls.
         */
        def assertDynamicDependencies() {
            assertDependency("dependencies.SameClassDependencies.dependencies()", "org.codehaus.groovy.vmplugin.v7.IndyInterface", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SameClassDependencies.dependencies()", "org.codehaus.groovy.vmplugin.v7.IndyInterface.bootstrap(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.String, int)", CALLS_METHOD)
            assertDependency("dependencies.SameClassDependencies.dependencies()", "dependencies.SameClassDependencies", USES_PARAMETER_TYPE)
            assertDependency("dependencies.SameClassDependencies.dependencies()", "java.lang.Object", USES_RETURN_TYPE)
        }
        
        /**
         * Those dependencies that you would expect to see just from looking at the code.
         */
        def assertExpectedDependencies() {
            assertDependency("dependencies.SameClassDependencies.dependencies()", "dependencies.SameClassDependencies.noArgumentsMethod()", CALLS_METHOD)
        }

        def assertDependency(src: String, trgt: String, dType: DependencyType) {
            val dependency = (src, trgt, dType)
            if (dependencies.contains(dependency)) {
                dependencies = dependencies diff List(dependency)
            } else {
                throw new AssertionError("Dependency "+dependency+" was not extracted successfully!")
            }
        }

        // extract dependencies
        for (cs @ classFile ← project.classFiles) {
            dependencyExtractor.process(classFile)
        }

        // test that the extracted dependencies are as expected
        assertStaticDependencies()
        assertDynamicDependencies()
        assertExpectedDependencies()

        // TODO we need a good way to filter out all of groovy's automatically generated methods
        // so we can easily see which dependencies we have missed as in DependencyExtractorTest
        dependencies.filter({ triple ⇒ triple._1.contains("noArgumentsMethod()") || triple._1.contains("dependencies()") }).foreach(println)
    }
}

