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
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IndexBasedProjectTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import ProjectTest._
    //
    //
    // Verify
    //
    //

    behavior of "A Project"

    import project.classFile
    import project.isLibraryType

    it should "find the class methods.a.Super" in {
        classFile(SuperType) should be('Defined)
    }

    it should "find the class methods.b.AbstractB" in {
        classFile(AbstractB) should be('Defined)
    }

    it should "find the library class attributes.DeprecatedByAnnotation" in {
        classFile(DeprecatedByAnnotation) should be('Defined)
    }

    it should "not find the class java.lang.Object" in {
        classFile(ObjectType.Object) should not be ('Defined)
    }

    it should "identify the class java.lang.Object as belonging to the library" in {
        isLibraryType(ObjectType.Object) should be(true)
    }

    it should "identify the class methods.a.Super as belonging to the core code" in {
        isLibraryType(SuperType) should be(false)

        isLibraryType(classFile(SuperType).get) should be(false)
    }

    it should "identify the class attributes.DeprecatedByAnnotation as belonging to the library code" in {
        isLibraryType(DeprecatedByAnnotation) should be(true)

        isLibraryType(classFile(DeprecatedByAnnotation).get) should be(true)
    }

    behavior of "An IndexBasedProject's lookupMethodDeclaration method"

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
        // let's make sure the method exists...
        resolveMethodReference(
            SuperType,
            "staticDefaultVisibilityMethod",
            MethodDescriptor("()V"),
            project
        ) should be('Defined)
        // let's make sure the class is a super class
        project.classHierarchy.isSubtypeOf(DirectSub, SuperType) should be(de.tud.cs.st.util.Yes)

        // let's test the resolving 
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
        assert(project.classFile(r.get).thisType === ObjectType("methods/b/SubI"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB,
            "someMethod",
            MethodDescriptor("()V"),
            project)

        r should be('Defined)
        assert(project.classFile(r.get).thisType === ObjectType("methods/b/SuperI"))
    }

    behavior of "An IndexBasedProject's information management methods"

    it should "be able to compute some project wide information on demand" in {
        val pik = new TestProjectInformationKey
        project.get(pik) should not be (null)
    }

    it should "always return the same information when we use the same ProjectInformation object" in {
        val pik = new TestProjectInformationKey
        project.get(pik) should be(project.get(pik))
    }

    it should "return the project information when we ask if some information was previously computed and that was actually done" in {
        val pik = new TestProjectInformationKey
        project.get(pik)
        project.has(pik) should be(Some(project.get(pik)))
    }

    it should "not compute project information when we just test for its existence" in {
        val pik = new TestProjectInformationKey
        // ask...
        project.has(pik) should be(None)
        // test...
        project.has(pik) should be(None)
    }

    it should "return all project information that was requested" in {

        val pik = new TestProjectInformationKey
        project.get(pik)
        // the other tests may also attach information..
        project.availableProjectInformation.length should be >= 1
        project.availableProjectInformation should contain(pik.theResult)
    }

    it should "be able to store a large amount of information" in {
        val piks = for (i ← (0 until 100)) yield {
            val pik = new TestProjectInformationKey
            project.get(pik)
            pik.uniqueId should be >= i
            pik
        }
        for (pik ← piks) {
            project.availableProjectInformation should contain(pik.theResult)
        }
    }

    it should "be able to compute project information that has requirements" in {
        val pik = new TestProjectInformationWithDependenciesKey
        project.get(pik) should be(pik.theResult)
        // the other tests may also attach information..
        project.availableProjectInformation.length should be >= 3
        project.availableProjectInformation should contain(pik.depdencies.head.theResult)
        project.availableProjectInformation should contain(pik.depdencies.tail.head.theResult)
    }
}

private class TestProjectInformationKey extends ProjectInformationKey[Object] {

    val theResult = new Object()

    protected def compute(project: SomeProject): Object = theResult

    protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef]] = Nil

}

private class TestProjectInformationWithDependenciesKey extends ProjectInformationKey[Object] {

    val theResult = new Object()

    val depdencies = List(new TestProjectInformationKey, new TestProjectInformationKey)

    protected def compute(project: SomeProject): Object = theResult

    protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef]] = depdencies

}

private object ProjectTest {

    //
    //
    // Setup
    //
    //
    val resources = TestSupport.locateTestResources("classfiles/Methods.jar")
    val libraryResources = TestSupport.locateTestResources("classfiles/Attributes.jar")
    val project = Project(ClassFiles(resources), ClassFiles(libraryResources))

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")
    val DeprecatedByAnnotation = ObjectType("attributes/DeprecatedByAnnotation")
}
