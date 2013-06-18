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

import reader.Java6Framework.ClassFiles
import analyses.Project

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

/**
  * Tests BAT's support w.r.t. inner classes.
  *
  * @author Michael Eichberg
  */
@RunWith(classOf[JUnitRunner])
class InnerClassesTest extends FlatSpec with ShouldMatchers /*with BeforeAndAfterAll */ with TestSupport {

    //
    //
    // Setup
    //
    //

    val project = new Project ++ ClassFiles(locateTestResources("classfiles/innerclasses.zip"))

    val myRootClass$Formatter = ObjectType("innerclasses/MyRootClass$Formatter")
    val myRootClass = ObjectType("innerclasses/MyRootClass")
    val myRootClass$1 = ObjectType("innerclasses/MyRootClass$1")
    val myRootClass$1$1 = ObjectType("innerclasses/MyRootClass$1$1")

    //
    //
    // Verify
    //
    //

    behavior of "a named inner class"

    it should "should return true if isInnerClass is called" in {
        project.classes(myRootClass$Formatter).isInnerClass should be(true)
    }
    
    it should "correctly identify its outer class" in {
        project.classes(myRootClass$Formatter).outerType.get._1 should be(myRootClass)
    }

    behavior of "an anonymous inner class"

    it should "should return true when isInnerClass is called" in {
        println(project.classes(myRootClass$1).innerClasses)
        project.classes(myRootClass$1).isInnerClass should be(true)
    }

    it should "should return true when isInnerClass is called even if it is an inner class of another anonymous inner class" in {
        project.classes(myRootClass$Formatter).isInnerClass should be(true)
    }

    behavior of "an outer class that is not an inner class"

    it should "return false if isInnerClass is called" in {
        project.classes(myRootClass).isInnerClass should be(false)
    }

    //    it should "find a method declared by an indirectly implemented interface" in {
    //        val r = project.lookupMethodDeclaration(AbstractB, "someMethod", MethodDescriptor("()V"))
    //        r should be('Defined)
    //        assert(r.get._1.thisClass === ObjectType("methods/b/SomeInterface"))
    //    }

}
