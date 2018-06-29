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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ProjectTest extends FlatSpec with Matchers {

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

    it should "find the library class deprecated.DeprecatedByAnnotation" in {
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

    it should "identify the class deprecated.DeprecatedByAnnotation as belonging to the library code" in {
        isLibraryType(DeprecatedByAnnotation) should be(true)

        isLibraryType(classFile(DeprecatedByAnnotation).get) should be(true)
    }

    behavior of "Project's extend method"

    it should "create a new Project that contains all class files" in {
        overallProject.source(SuperType) should be('defined)
        overallProject.source(DeprecatedByAnnotation) should be('defined)
        overallProject.source(ObjectType("code/Quicksort")) should be('defined)
    }

    it should "create a Project with the correct classification for the class files" in {
        overallProject.isLibraryType(SuperType) should be(false)
        overallProject.isLibraryType(DeprecatedByAnnotation) should be(true)
        overallProject.isLibraryType(ObjectType("code/Quicksort")) should be(false)
    }

    behavior of "a Project's resolveMethodReference method"

    import project.resolveMethodReference

    it should "find a public method" in {
        resolveMethodReference(
            SuperType, "publicMethod", MethodDescriptor("()V")
        ) should be('Defined)
    }

    it should "find a private method" in {
        resolveMethodReference(
            SuperType, "privateMethod", MethodDescriptor("()V")
        ) should be('Defined)
    }

    it should "not find a method that does not exist" in {
        resolveMethodReference(SuperType, "doesNotExist", MethodDescriptor("()V")) should be('Empty)
    }

    it should "find a method with default visibility" in {
        resolveMethodReference(
            SuperType, "defaultVisibilityMethod", MethodDescriptor("()V")
        ) should be('Defined)
    }

    it should "find the super class' static method staticDefaultVisibilityMethod" in {
        // let's make sure the method exists...
        resolveMethodReference(
            SuperType, "staticDefaultVisibilityMethod", MethodDescriptor("()V")
        ) should be('Defined)
        // let's make sure the class is a super class
        project.classHierarchy.isSubtypeOf(DirectSub, SuperType) should be(Yes)

        // let's test the resolving
        resolveMethodReference(
            DirectSub, "staticDefaultVisibilityMethod", MethodDescriptor("()V")
        ) should be('Defined)
    }

    it should "not find Object's toString method, because we only have a partial view of the project" in {
        resolveMethodReference(
            DirectSub, "toString", MethodDescriptor("()Ljava/lang/String;")
        ) should be('Empty)
    }

    it should "find a method declared by a directly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB, "someSubMethod", MethodDescriptor("()V"),
            forceLookupInSuperinterfacesOnFailure = true
        )
        r should be('Defined)
        assert(r.get.classFile.thisType === ObjectType("methods/b/SubI"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB, "someMethod", MethodDescriptor("()V"),
            forceLookupInSuperinterfacesOnFailure = true
        )
        r should be('Defined)
        assert(r.get.classFile.thisType === ObjectType("methods/b/SuperI"))
    }

    behavior of "a Project's instance methods"

    import project.instanceMethods

    it should "find inherited default method in interface with multiple parent interfaces" in {
        assert(instanceMethods(SubSub2).exists(_.name == "foo"))
    }

    it should "not return an abstractly overridden default method in interface with multiple parent interfaces" in {
        assert(!instanceMethods(SubSub).exists(_.name == "foo"))
    }

    it should "not return an abstractly overridden default method in a class that implements the interface twice" in {
        assert(!instanceMethods(Subclass1).exists(_.name == "foo"))
    }

    it should "not return an abstractly overridden default method in a class that inherits the default method through its superclass" in {
        assert(!instanceMethods(Subclass2).exists(_.name == "foo"))
    }

    behavior of "A Project's information management methods"

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

    behavior of "a Project's projectPackages methods"

    it should "return all packages defined in the project" in {

        opalProject.projectPackages should be(Set(
            "de/tud/cs/st/bat/generic/reader", "de/tud/cs/st/util/graphs",
            "de/tud/cs/st/util/collection", "de/tud/cs/st/bat/resolved",
            "de/tud/cs/st/util/perf", "de/tud/cs/st", "de/tud/cs/st/bat/prolog",
            "de/tud/cs/st/bat/prolog/reader", "de/tud/cs/st/sae/parser", "de/tud/cs/st/sae",
            "de/tud/cs/st/bat/native", "de/tud/cs/st/util/trees",
            "de/tud/cs/st/bat/resolved/reader", "de/tud/cs/st/bat", "de/tud/cs/st/util",
            "de/tud/cs/st/bat/native/reader", "de/tud/cs/st/prolog"
        ))

    }

    behavior of "a Project's libraryPackages methods"

    it should "return no packages if no libraries are loaded" in {
        opalProject.libraryPackages should be(empty)
    }

    it should "return the packages of the loaded libraries" in {
        project.libraryPackages should not be (empty)
    }

    behavior of "a Project's packages methods"

    it should "return the same packages as projectPackages if no libraries are loaded" in {
        opalProject.packages should be(opalProject.projectPackages)
    }

    it should "return all packages of a project that has libraries" in {
        project.packages should be(project.projectPackages ++ project.libraryPackages)
    }

    behavior of "a Project's parForeachMethodWithBody method"

    def testAllMethodsWithBodyWithContext(project: SomeProject, name: String): Unit = {
        it should s"allMethodsWithBodyWithContext should return ALL concrete methods for $name" in {
            var allConcreteMethods = project.allMethodsWithBodyWithContext.map(_.method).toSet
            val missedMethods: Iterable[Method] = (for {
                c ← project.allClassFiles
                m ← c.methods
                if m.body.isDefined
            } yield {
                if (allConcreteMethods.contains(m)) {
                    allConcreteMethods -= m
                    None
                } else {
                    Some(m)
                }
            }).flatten
            missedMethods should be('empty)
        }
    }
    testAllMethodsWithBodyWithContext(project, "Methods.jar")
    testAllMethodsWithBodyWithContext(overallProject, "Code.jar")
    testAllMethodsWithBodyWithContext(opalProject, "OPAL")

    def testParForeachMethodWithBody(project: SomeProject, name: String): Unit = {
        it should s"return that same methods for $name as a manual search" in {
            val mutex = new Object
            var methods = List.empty[Method]
            project.parForeachMethodWithBody()(mi ⇒ mutex.synchronized { methods ::= mi.method })
            val missedMethods = for {
                c ← project.allClassFiles
                m ← c.methods
                if m.body.isDefined
                if !methods.contains(m)
            } yield {
                (c, m)
            }
            assert(
                missedMethods.isEmpty, {
                    s"; missed ${missedMethods.size} methods: "+
                        missedMethods.map { mm ⇒
                            val (c, m) = mm
                            val belongsToProject = project.isProjectType(c.thisType)
                            m.toJava(
                                m.body.get.instructions.length.toString+
                                    "; belongs to project = "+belongsToProject
                            )
                        }.mkString("\n\t", "\n\t", "\n")
                }
            )
            val methodsCount = methods.size
            info(s"parForeachMethodWithBody iterated over $methodsCount methods")
            assert(methodsCount == methods.toSet.size)
        }
    }
    testParForeachMethodWithBody(project, "Methods.jar")
    testParForeachMethodWithBody(overallProject, "Code.jar")
    testParForeachMethodWithBody(opalProject, "OPAL")

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF FIELD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    {
        val fieldsProject = {
            val classFiles = ClassFiles(locateTestResources("fields-g=none-5.jar", "bi"))
            Project(classFiles, Traversable.empty, true)
        }
        import fieldsProject.classFile
        import fieldsProject.resolveFieldReference

        val SuperSuperType = ObjectType("fields/SuperSuper")
        val SuperSuperClass = classFile(SuperSuperType).get
        val SuperType = ObjectType("fields/Super")
        val SuperClass = classFile(SuperType).get

        val SuperIType = ObjectType("fields/SuperI")
        val SuperIClass = classFile(SuperIType).get
        val SubIType = ObjectType("fields/SubI")
        val SubIClass = classFile(SubIType).get

        val SubType = ObjectType("fields/Sub")
        val SubClass = classFile(SubType).get
        val SubSubType = ObjectType("fields/SubSub")
        //val SubSubClass = classFile(SubSubType).get

        behavior of "a Project's methods to resolve field references"

        import fieldsProject.resolveFieldReference

        it should "correctly resolve a reference to a static field in a superclass" in {
            resolveFieldReference(SuperType, "x", IntegerType) should be(
                Some(SuperSuperClass.fields(0))
            )
        }

        it should "correctly resolve a reference to a field defined in an interface" in {
            resolveFieldReference(SubIType, "THE_SUB_I", IntegerType) should be(
                Some(SubIClass.fields(0))
            )
        }

        it should "correctly resolve a reference to a field defined in a superinterface of an interface" in {
            resolveFieldReference(SubIType, "THE_I", IntegerType) should be(
                Some(SuperIClass.fields(0))
            )
        }

        it should "correctly resolve a reference to a field defined in a superinterface" in {
            resolveFieldReference(SubType, "THE_I", IntegerType) should be(
                Some(SuperIClass.fields(0))
            )
        }

        it should "correctly resolve a reference to a field defined in a superclass" in {
            resolveFieldReference(SubSubType, "x", IntegerType) should be(
                Some(SubClass.fields(0))
            )
        }

        it should "correctly resolve a reference to a private field defined in a superclass" in {
            resolveFieldReference(SubSubType, "y", IntegerType) should be(
                Some(SuperClass.fields(0))
            )
        }

        it should "not fail (throw an exception) if the field cannot be found" in {
            resolveFieldReference(SubSubType, "NOT_DEFINED", IntegerType) should be(None)
        }

        it should "not fail if the type cannot be found" in {
            resolveFieldReference(
                ObjectType("NOT/DEFINED"), "NOT_DEFINED", IntegerType
            ) should be(None)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF METHOD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    {

        val methodsProject = {
            val classFiles = ClassFiles(ProjectTest.methodsArchive)
            Project(classFiles, Traversable.empty, true)
        }

        val superI = ObjectType("methods/b/SuperI")

        behavior of "a Project's methods to resolve method references"

        it should "handle the case if an interface has no implementing class" in {
            val implementingMethods =
                methodsProject.interfaceCall(
                    superI, "someMethod", MethodDescriptor.NoArgsAndReturnVoid
                )
            implementingMethods.size should be(0)
        }

        it should "find a method in a super class" in {
            val classType = ObjectType("methods/b/B")
            val implementingMethods =
                methodsProject.virtualCall(
                    "methods/b", classType, "publicMethod", MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                'name("publicMethod"),
                'descriptor(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }
    }
}

private class TestProjectInformationKey extends ProjectInformationKey[Object, Nothing] {

    val theResult = new Object()

    protected def compute(project: SomeProject): Object = theResult

    protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef, Nothing]] = Nil

}

private class TestProjectInformationWithDependenciesKey extends ProjectInformationKey[Object, Nothing] {

    val theResult = new Object()

    val depdencies = List(new TestProjectInformationKey, new TestProjectInformationKey)

    protected def compute(project: SomeProject): Object = theResult

    protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef, _ <: AnyRef]] = depdencies

}

private object ProjectTest {

    //
    //
    // Setup
    //
    //
    val methodsArchive = locateTestResources("methods.jar", "bi")
    val deprecatedArchive = locateTestResources("deprecated.jar", "bi")
    val project = Project(ClassFiles(methodsArchive), ClassFiles(deprecatedArchive), false)

    val codeJAR = locateTestResources("code.jar", "bi")
    val overallProject = Project.extend(project, ClassFiles(codeJAR))

    val opal = locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
    val opalProject = Project(ClassFiles(opal), Traversable.empty, true)

    //
    //
    // Types used by the tests
    //
    //

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")
    val DeprecatedByAnnotation = ObjectType("deprecated/DeprecatedByAnnotation")

    val SubSub = ObjectType("interfaces/SubSub")
    val SubSub2 = ObjectType("interfaces/SubSub2")
    val Subclass1 = ObjectType("interfaces/Subclass1")
    val Subclass2 = ObjectType("interfaces/Subclass2")
}
