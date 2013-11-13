/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st.bat
package resolved
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.ParallelTestExecution

import reader.Java7Framework.ClassFiles

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IndexBasedProjectTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    //
    //
    // Setup
    //
    //
    val resources = TestSupport.locateTestResources("classfiles/Methods.jar")
    val project = IndexBasedProject.empty[java.net.URL] ++ ClassFiles(resources)

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")

    //
    //
    // Verify
    //
    //

    behavior of "IndexBasedProject"

    import project.classFile

    it should "find the class methods.a.Super" in {
        classFile(SuperType) should be('Defined)
    }

    it should "find the class methods.b.AbstractB" in {
        classFile(AbstractB) should be('Defined)
    }

    it should "not find the class java.lang.Object" in {
        classFile(ObjectType.Object) should not be ('Defined)
    }

    behavior of "Project's lookupMethodDeclaration method"

    import project.classHierarchy.resolveMethodReference

    it should "find a public method" in {
        resolveMethodReference(
            SuperType,
            "publicMethod",
            MethodDescriptor("()V"),
            project
        ) should be('Defined)
    }

    it should "find a private method" in {
        resolveMethodReference(
            SuperType,
            "privateMethod",
            MethodDescriptor("()V"),
            project
        ) should be('Defined)
    }

    it should "not find a method that does not exist" in {
        resolveMethodReference(
            SuperType,
            "doesNotExist",
            MethodDescriptor("()V"),
            project
        ) should be('Empty)
    }

    it should "find a method with default visibility" in {
        resolveMethodReference(
            SuperType,
            "defaultVisibilityMethod",
            MethodDescriptor("()V"),
            project
        ) should be('Defined)
    }

    it should "find the super class' static method staticDefaultVisibilityMethod" in {
        resolveMethodReference(
            DirectSub,
            "staticDefaultVisibilityMethod",
            MethodDescriptor("()V"),
            project
        ) should be('Defined)
    }

    it should "not find Object's toString method, because we only have a partial view of the project" in {
        resolveMethodReference(
            DirectSub,
            "toString",
            MethodDescriptor("()Ljava/lang/String;"),
            project
        ) should be('Empty)
    }

    it should "find a method declared by a directly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB,
            "someSubMethod",
            MethodDescriptor("()V"),
            project)

        r should be('Defined)
        assert(project.classFile(r.get).thisClass === ObjectType("methods/b/SubI"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB,
            "someMethod",
            MethodDescriptor("()V"),
            project)

        r should be('Defined)
        assert(project.classFile(r.get).thisClass === ObjectType("methods/b/SuperI"))
    }

}
