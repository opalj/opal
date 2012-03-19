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

import reader.Java6Framework
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ProjectTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAll {

    val project = new Project ++ Java6Framework.ClassFiles("test/classfiles/Methods.zip")

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")

    behavior of "Project's classes repository"

    it should "find the class methods.a.Super" in {
        assert(project.classes.get(SuperType).isDefined);
    }
    
    it should "find the class methods.b.AbstractB" in {
        assert(project.classes.get(AbstractB).isDefined);
    }

    it should "not find the class java.lang.Object" in {
        assert(!project.classes.get(ObjectType.Object).isDefined);
    }

    behavior of "Project's lookupMethodDeclaration method"

    it should "find a public method" in {
        assert(project.lookupMethodDeclaration(SuperType, "publicMethod", MethodDescriptor("()V")).isDefined)
    }

    it should "find a private method" in {
        assert(project.lookupMethodDeclaration(SuperType, "privateMethod", MethodDescriptor("()V")).isDefined)
    }

    it should "not find a method that does not exist" in {
        assert(project.lookupMethodDeclaration(SuperType, "doesNotExist", MethodDescriptor("()V")).isEmpty)
    }

    it should "find a method with default visibility" in {
        assert(project.lookupMethodDeclaration(SuperType, "defaultVisibilityMethod", MethodDescriptor("()V")).isDefined)
    }

    it should "find another method with default visibility" in {
        assert(project.lookupMethodDeclaration(SuperType, "anotherDefaultVisibilityMethod", MethodDescriptor("()V")).isDefined)
    }

    it should "find the super class' method anotherDefaultVisibilityMethod" in {
        assert(project.lookupMethodDeclaration(DirectSub, "anotherDefaultVisibilityMethod", MethodDescriptor("()V")).isDefined)
    }

    it should "not find Object's toString method, because we only have a partial view of the project" in {
        assert(project.lookupMethodDeclaration(SuperType, "toString", MethodDescriptor("()Ljava/lang/String;")).isEmpty)
    }

    it should "find a method declared by a directly implemented interface" in {
        val r = project.lookupMethodDeclaration(AbstractB, "someSubMethod", MethodDescriptor("()V"))
        assert(r.isDefined)
        assert(r.get._1.thisClass == ObjectType("methods/b/SomeSubInterface"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = project.lookupMethodDeclaration(AbstractB, "someMethod", MethodDescriptor("()V"))
        assert(r.isDefined)
        assert(r.get._1.thisClass == ObjectType("methods/b/SomeInterface"))
    }

}
