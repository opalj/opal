/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

import reader.Java7Framework.ClassFiles

/**
  * Tests the support for "project" related functionality.
  *
  * @author Michael Eichberg
  */
@RunWith(classOf[JUnitRunner])
class ProjectTest extends FlatSpec with ShouldMatchers /*with BeforeAndAfterAll */ with TestSupport {

    //
    //
    // Setup
    //
    //
    val resources = locateTestResources("classfiles/Methods.zip")
    val project = new Project ++ ClassFiles(resources)

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")

    //
    //
    // Verify
    //
    //

    behavior of "Project's classes repository"

    it should "find the class methods.a.Super" in {
        project.classes.get(SuperType) should be('Defined)
    }

    it should "find the class methods.b.AbstractB" in {
        project.classes.get(AbstractB) should be('Defined)
    }

    it should "not find the class java.lang.Object" in {
        project.classes.get(ObjectType.Object) should not be ('Defined)
    }

    behavior of "Project's lookupMethodDeclaration method"

    it should "find a public method" in {
        project.lookupMethodDeclaration(SuperType, "publicMethod", MethodDescriptor("()V")) should be('Defined)
    }

    it should "find a private method" in {
        project.lookupMethodDeclaration(SuperType, "privateMethod", MethodDescriptor("()V")) should be('Defined)
    }

    it should "not find a method that does not exist" in {
        project.lookupMethodDeclaration(SuperType, "doesNotExist", MethodDescriptor("()V")) should be('Empty)
    }

    it should "find a method with default visibility" in {
        project.lookupMethodDeclaration(SuperType, "defaultVisibilityMethod", MethodDescriptor("()V")) should be('Defined)
    }

    it should "find another method with default visibility" in {
        project.lookupMethodDeclaration(SuperType, "anotherDefaultVisibilityMethod", MethodDescriptor("()V")) should be('Defined)
    }

    it should "find the super class' method anotherDefaultVisibilityMethod" in {
        project.lookupMethodDeclaration(DirectSub, "anotherDefaultVisibilityMethod", MethodDescriptor("()V")) should be('Defined)
    }

    it should "not find Object's toString method, because we only have a partial view of the project" in {
        project.lookupMethodDeclaration(SuperType, "toString", MethodDescriptor("()Ljava/lang/String;")) should be('Empty)
    }

    it should "find a method declared by a directly implemented interface" in {
        val r = project.lookupMethodDeclaration(AbstractB, "someSubMethod", MethodDescriptor("()V"))
        r should be('Defined)
        assert(r.get._1.thisClass === ObjectType("methods/b/SomeSubInterface"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = project.lookupMethodDeclaration(AbstractB, "someMethod", MethodDescriptor("()V"))
        r should be('Defined)
        assert(r.get._1.thisClass === ObjectType("methods/b/SomeInterface"))
    }

}
