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
package de.tud.cs.st.bat
package resolved
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

import reader.Java8Framework.ClassFiles

/**
 * Tests the `ProjectIndex`.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ProjectIndexTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import ProjectIndexTest._
    //
    //
    // Verify
    //
    //

    behavior of "A ProjectIndex w.r.t. finding methods"

    it should ("find a unique method by its signature") in {
        val name = "someMethod"
        val descriptor = MethodDescriptor.NoArgsAndReturnVoid
        val methods = methodsProjectIndex.findMethods(name, descriptor)
        methods should have size (1)
        methods.forall(m ⇒ m.name == name && m.descriptor == descriptor) should be(true)
    }

    it should ("find multiple methods with the same signature if the method is overridden or reimplemented") in {
        val name = "publicMethod"
        val descriptor = MethodDescriptor.NoArgsAndReturnVoid
        val methods = methodsProjectIndex.findMethods(name, descriptor)
        methods should have size (4)
        methods.forall(m ⇒ m.name == name && m.descriptor == descriptor) should be(true)
    }

    it should ("not find a method that does not exist") in {
        val methods = methodsProjectIndex.findMethods(
            "someRandomMethodNameOfANonExistingMethod",
            MethodDescriptor.NoArgsAndReturnVoid)
        methods should have size (0)
    }

    behavior of "A ProjectIndex w.r.t. finding fields"

    it should ("find a unique field by its nane and type") in {
        val name = "y"
        val fieldType = IntegerType
        val results = fieldsProjectIndex.findFields(name, fieldType)
        results should have size (1)
        results.forall(f ⇒ f.name == name && f.fieldType == fieldType) should be(true)
    }

    it should ("find multiple fields if the fields have the same name and type") in {
        val name = "x"
        val fieldType = IntegerType
        val matches = fieldsProjectIndex.findFields(name, fieldType)
        matches should have size (2)
        matches.forall(f ⇒ f.name == name && f.fieldType == fieldType) should be(true)
    }

    it should ("not find a field that does not exist") in {
        val name = "nameOfAFieldThatDoesNotExistXYZ"
        val fieldType = IntegerType
        val matches = fieldsProjectIndex.findFields(name, fieldType)
        matches should have size (0)
    }

    it should ("not find a field that has the required name, but the wrong type") in {
        val name = "x"
        val fieldType = DoubleType
        val matches = fieldsProjectIndex.findFields(name, fieldType)
        matches should have size (0)
    }
}

private object ProjectIndexTest {

    //
    //
    // Setup
    //
    //
    val methodsProjectIndex =
        Project(TestSupport.locateTestResources("classfiles/Methods.jar")).
            get(ProjectIndexKey)

    val fieldsProjectIndex =
        Project(TestSupport.locateTestResources("classfiles/Fields.jar")).
            get(ProjectIndexKey)

}
