/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java11Framework.ClassFiles

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ProjectTest extends AnyFlatSpec with Matchers {

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
        classFile(SuperType) shouldBe defined
    }

    it should "find the class methods.b.AbstractB" in {
        classFile(AbstractB) shouldBe defined
    }

    it should "find the library class deprecated.DeprecatedByAnnotation" in {
        classFile(DeprecatedByAnnotation) shouldBe defined
    }

    it should "not find the class java.lang.Object" in {
        classFile(ObjectType.Object) should not be defined
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
        overallProject.source(SuperType) shouldBe defined
        overallProject.source(DeprecatedByAnnotation) shouldBe defined
        overallProject.source(ObjectType("code/Quicksort")) shouldBe defined
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
        ) shouldBe defined
    }

    it should "find a private method" in {
        resolveMethodReference(
            SuperType, "privateMethod", MethodDescriptor("()V")
        ) shouldBe defined
    }

    it should "not find a method that does not exist" in {
        resolveMethodReference(SuperType, "doesNotExist", MethodDescriptor("()V")) should be(Symbol("Empty"))
    }

    it should "find a method with default visibility" in {
        resolveMethodReference(
            SuperType, "defaultVisibilityMethod", MethodDescriptor("()V")
        ) shouldBe defined
    }

    it should "find the super class' static method staticDefaultVisibilityMethod" in {
        // let's make sure the method exists...
        resolveMethodReference(
            SuperType, "staticDefaultVisibilityMethod", MethodDescriptor("()V")
        ) shouldBe defined
        // let's make sure the class is a super class
        project.classHierarchy.isSubtypeOf(DirectSub, SuperType) should be(true)

        // let's test the resolving
        resolveMethodReference(
            DirectSub, "staticDefaultVisibilityMethod", MethodDescriptor("()V")
        ) shouldBe defined
    }

    it should "not find Object's toString method, because we only have a partial view of the project" in {
        resolveMethodReference(
            DirectSub, "toString", MethodDescriptor("()Ljava/lang/String;")
        ) shouldBe empty
    }

    it should "find a method declared by a directly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB, "someSubMethod", MethodDescriptor("()V"),
            forceLookupInSuperinterfacesOnFailure = true
        )
        r shouldBe defined
        assert(r.get.classFile.thisType === ObjectType("methods/b/SubI"))
    }

    it should "find a method declared by an indirectly implemented interface" in {
        val r = resolveMethodReference(
            AbstractB, "someMethod", MethodDescriptor("()V"),
            forceLookupInSuperinterfacesOnFailure = true
        )
        r shouldBe defined
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
        val piks = for (i <- (0 until 100)) yield {
            val pik = new TestProjectInformationKey
            project.get(pik)
            pik.uniqueId should be >= i
            pik
        }
        for (pik <- piks) {
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
                c <- project.allClassFiles
                m <- c.methods
                if m.body.isDefined
            } yield {
                if (allConcreteMethods.contains(m)) {
                    allConcreteMethods -= m
                    None
                } else {
                    Some(m)
                }
            }).flatten
            missedMethods should be(Symbol("Empty"))
        }
    }
    testAllMethodsWithBodyWithContext(project, "Methods.jar")
    testAllMethodsWithBodyWithContext(overallProject, "Code.jar")
    testAllMethodsWithBodyWithContext(opalProject, "OPAL")
    testAllMethodsWithBodyWithContext(java11nestsProject, "Java11Nests.jar")

    def testParForeachMethodWithBody(project: SomeProject, name: String): Unit = {
        it should s"return that same methods for $name as a manual search" in {
            val mutex = new Object
            var methods = List.empty[Method]
            project.parForeachMethodWithBody()(mi => mutex.synchronized { methods ::= mi.method })
            val missedMethods = for {
                c <- project.allClassFiles
                m <- c.methods
                if m.body.isDefined
                if !methods.contains(m)
            } yield {
                (c, m)
            }
            assert(
                missedMethods.isEmpty, {
                    s"; missed ${missedMethods.size} methods: "+
                        missedMethods.map { mm =>
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
    testParForeachMethodWithBody(java11nestsProject, "Java11Nests.jar")

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF FIELD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    {
        val fieldsProject = {
            val classFiles = ClassFiles(locateTestResources("fields-g=none-5.jar", "bi"))
            Project(classFiles, Iterable.empty, true)
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
            Project(classFiles, Iterable.empty, true)
        }

        val superI = ObjectType("methods/b/SuperI")

        behavior of "a Project's methods to resolve method references"

        it should "handle the case if an interface has no implementing class" in {
            val implementingMethods =
                methodsProject.interfaceCall(
                    superI, superI, "someMethod", MethodDescriptor.NoArgsAndReturnVoid
                )
            implementingMethods.size should be(0)
        }

        it should "find a method in a super class" in {
            val classType = ObjectType("methods/b/B")
            val implementingMethods =
                methodsProject.virtualCall(
                    classType, classType, "publicMethod", MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("publicMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid),
                Symbol("declaringClassFile")(methodsProject.classFile(ObjectType("methods/b/DirectSub")).get)
            )
        }

        it should "find private method for virtual calls" in {
            val classType = ObjectType("methods/c/Super")
            val implementingMethods =
                methodsProject.virtualCall(
                    classType,
                    classType,
                    "originallyPrivateMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("originallyPrivateMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid),
                Symbol("declaringClassFile")(methodsProject.classFile(classType).get)
            )
        }

        it should "not find private method for virtual calls on subclasses" in {
            val classType = ObjectType("methods/c/Sub1")
            val implementingMethods =
                methodsProject.virtualCall(
                    classType,
                    classType,
                    "originallyPrivateMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("originallyPrivateMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid),
                Symbol("declaringClassFile")(methodsProject.classFile(classType).get)
            )
        }

        it should "not find private method for virtual calls declared on subclasses" in {
            val classType = ObjectType("methods/c/Sub2")
            val implementingMethods =
                methodsProject.virtualCall(
                    classType,
                    classType,
                    "originallyPrivateMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(0)
        }

        it should "find private method for instance calls" in {
            val classType = ObjectType("methods/c/Super")
            val implementingMethods =
                methodsProject.instanceCall(
                    classType,
                    classType,
                    "originallyPrivateMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(true)
            implementingMethods.value should have(
                Symbol("name")("originallyPrivateMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid),
                Symbol("declaringClassFile")(methodsProject.classFile(classType).get)
            )
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF METHOD REFERENCES FOR JAVA 11+ NestHost/NestMembers
    //
    // -----------------------------------------------------------------------------------

    {
        behavior of "instanceCall to resolve method references w.r.t. Java 11+ Nest Attributes"

        it should "resolve a private method in the NestHost" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NestMember1Type,
                    NestHost,
                    "nestHostMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(true)
            implementingMethods.value should have(
                Symbol("name")("nestHostMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "resolve a private method in a NestMember" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NestHost,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(true)
            implementingMethods.value should have(
                Symbol("name")("nestMember1Method"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "resolve a private method in a NestMate" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NestMember2Type,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(true)
            implementingMethods.value should have(
                Symbol("name")("nestMember1Method"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "not resolve a private method in an unrelated class from a NestHost" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NestHost,
                    NoNestMember,
                    "noNestMemberMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(false)
        }

        it should "not resolve a private method in an unrelated class from a NestMember" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NestMember1Type,
                    NoNestMember,
                    "noNestMemberMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(false)
        }

        it should "not resolve a private method in a NestHost from an unrelated class" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NoNestMember,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(false)
        }

        it should "not resolve a private method in a NestMember from an unrelated class" in {
            val implementingMethods =
                java11nestsProject.instanceCall(
                    NoNestMember,
                    NestHost,
                    "nestHostMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.hasValue should be(false)
        }

        behavior of "virtualCall to resolve method references w.r.t. Java 11+ Nest Attributes"

        it should "resolve a private method in the NestHost" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NestMember1Type,
                    NestHost,
                    "nestHostMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("nestHostMethod"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "resolve a private method in a NestMember" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NestHost,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("nestMember1Method"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "resolve a private method in a NestMate" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NestMember2Type,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(1)
            implementingMethods.head should have(
                Symbol("name")("nestMember1Method"),
                Symbol("descriptor")(MethodDescriptor.NoArgsAndReturnVoid)
            )
        }

        it should "not resolve a private method in an unrelated class from a NestHost" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NestHost,
                    NoNestMember,
                    "noNestMemberMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(0)
        }

        it should "not resolve a private method in an unrelated class from a NestMember" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NestMember1Type,
                    NoNestMember,
                    "noNestMemberMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(0)
        }

        it should "not resolve a private method in a NestHost from an unrelated class" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NoNestMember,
                    NestMember1Type,
                    "nestMember1Method",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(0)
        }

        it should "not resolve a private method in a NestMember from an unrelated class" in {
            val implementingMethods =
                java11nestsProject.virtualCall(
                    NoNestMember,
                    NestHost,
                    "nestHostMethod",
                    MethodDescriptor.NoArgsAndReturnVoid
                )

            implementingMethods.size should be(0)
        }
    }
}

private class TestProjectInformationKey extends ProjectInformationKey[Object, Nothing] {

    val theResult = new Object()

    override def compute(project: SomeProject): Object = theResult

    override def requirements(project: SomeProject): Seq[ProjectInformationKey[_ <: AnyRef, Nothing]] = Nil

}

private class TestProjectInformationWithDependenciesKey extends ProjectInformationKey[Object, Nothing] {

    val theResult = new Object()

    val depdencies = List(new TestProjectInformationKey, new TestProjectInformationKey)

    override def compute(project: SomeProject): Object = theResult

    override def requirements(project: SomeProject): Seq[ProjectInformationKey[_ <: AnyRef, _ <: AnyRef]] = depdencies

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
    val opalProject = Project(ClassFiles(opal), Iterable.empty, true)

    val java11nestsArchive =
        locateTestResources("java11nests-g-11-parameters-genericsignature", "bi")
    val java11nestsProject = Project(ClassFiles(java11nestsArchive), Iterable.empty, true)

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

    val NestHost = ObjectType("java11nests/NestHost")
    val NestMember1Type = ObjectType("""java11nests/NestHost$NestMember1""")
    val NestMember2Type = ObjectType("java11nests/NestHost$NestMember2")
    val NoNestMember = ObjectType("java11nests/NoNestMember")
}
