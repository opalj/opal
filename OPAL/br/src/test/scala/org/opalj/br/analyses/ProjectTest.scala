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
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

import org.opalj.bi.TestSupport.locateTestResources

import reader.Java8Framework.ClassFiles

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

    behavior of "A Project's resolveMethodReference method"

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
        project.classHierarchy.isSubtypeOf(DirectSub, SuperType) should be(Yes)

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
    
        behavior of "the ClassHierarchy's isSubTypeOf method w.r.t. generics"

    import Generics._

    it should "correctly reflect the type hierarchy related to primitve generics should return YES" in {
        genericProject.isSubtypeOf(baseContainer, baseContainer) should be(Yes)
        genericProject.isSubtypeOf(baseContainer, wildCardContainer) should be(Yes)
        genericProject.isSubtypeOf(wildCardContainer, wildCardContainer) should be(Yes)
        genericProject.isSubtypeOf(extBaseContainer, covariantContainer) should be(Yes)
        genericProject.isSubtypeOf(baseContainer, covariantContainer) should be(Yes)
        genericProject.isSubtypeOf(baseContainer, contravariantContainer) should be(Yes)
        genericProject.isSubtypeOf(doubleContainerET, baseContainer) should be(Yes)
        genericProject.isSubtypeOf(doubleContainerTE, baseContainer) should be(Yes)
        genericProject.isSubtypeOf(doubleContainerBase, baseContainer) should be(Yes)
    }

    it should "correctly reflect the type hierarchy related to primitve generics should return NO" in {
        genericProject.isSubtypeOf(baseContainer, extBaseContainer) should be(No)
        genericProject.isSubtypeOf(wildCardContainer, baseContainer) should be(No)
        genericProject.isSubtypeOf(altContainer, contravariantContainer) should be(No)
        genericProject.isSubtypeOf(extBaseContainer, contravariantBaseContainer) should be(No)
        genericProject.isSubtypeOf(altContainer, covariantContainer) should be(No)
        genericProject.isSubtypeOf(baseContainer, doubleContainerET) should be(No)
        genericProject.isSubtypeOf(baseContainer, doubleContainerTE) should be(No)
        genericProject.isSubtypeOf(wrongDoubleContainer, baseContainer) should be(No)
    }
    
    it should "correctly reflect the type hierarchy related to primitve generics should return Unknown" in {
        genericProject.isSubtypeOf(unknownContainer, baseContainer) should be(Unknown)
    }
    
    it should "correctly reflect the type hierarchy related to nested generics should return YES" in {
           genericProject.isSubtypeOf(nestedInnerCovariantContainer, nestedInnerCovariantContainer) should be(Yes)
           genericProject.isSubtypeOf(nestedExtBase, nestedInnerCovariantContainer) should be(Yes)
           genericProject.isSubtypeOf(nestedBase, nestedContravariantContainer) should be(Yes)
           genericProject.isSubtypeOf(nestedBase, contravariantWithContainer) should be(Yes)
           genericProject.isSubtypeOf(nestedBase, nestedOutterCovariantContainer) should be(Yes)
    }
    
    it should "correctly reflect the type hierarchy related to nested generics should return NO" in {
         genericProject.isSubtypeOf(nestedBase, nestedAltBase) should be(No)
         genericProject.isSubtypeOf(nestedAltBase, nestedInnerCovariantContainer) should be(No)
         genericProject.isSubtypeOf(nestedLvlTwoBase, nestedContravariantContainer) should be(No)
         genericProject.isSubtypeOf(nestedSubGenBase, nestedContravariantContainer) should be(No)
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
    val resources = locateTestResources("classfiles/Methods.jar", "bi")
    val libraryResources = locateTestResources("classfiles/Attributes.jar", "bi")
    val project = Project(ClassFiles(resources), ClassFiles(libraryResources))

    val codeJAR = locateTestResources("classfiles/Code.jar", "bi")
    val overallProject = Project.extend(project, ClassFiles(codeJAR))

    val SuperType = ObjectType("methods/a/Super")
    val DirectSub = ObjectType("methods/a/DirectSub")
    val AbstractB = ObjectType("methods/b/AbstractB")
    val DeprecatedByAnnotation = ObjectType("attributes/DeprecatedByAnnotation")

    val opal = locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
    val opalProject = Project(ClassFiles(opal), Traversable.empty)
    
    val generics = locateTestResources("classfiles/genericTypes.jar", "br")
    val genericProject = Project(ClassFiles(generics), Traversable.empty)
}

private object Generics {

    //
    //
    // Generic test setup
    //
    //

    /* package names*/
    val unkownPgk = Some("unknown/")
    val pgk = Some("classhierarchy/")

    /*SimpleClassTypeSignatures*/
    val baseSCTS = SimpleClassTypeSignature("Base", Nil)
    val extBaseSCTS = SimpleClassTypeSignature("ExtendedBase", Nil)
    val lvlTwoBaseSCTS = SimpleClassTypeSignature("lvlTwoBase", Nil)
    val altBaseSCTS = SimpleClassTypeSignature("AlternativBase", Nil)
    
    val genericSCTS = SimpleClassTypeSignature("SimpleGeneric", Nil)
    def createScts(cn : String, ptas: List[TypeArgument]) = SimpleClassTypeSignature(cn, ptas)
    
    /*Nested ClassTypeSignatures*/

    val baseCTS = ClassTypeSignature(pgk, baseSCTS, Nil)
    val extBaseCTS = ClassTypeSignature(pgk, extBaseSCTS, Nil)
    val lvlTwoBaseCTS = ClassTypeSignature(pgk, lvlTwoBaseSCTS, Nil)
    val altBaseCTS = ClassTypeSignature(pgk, altBaseSCTS, Nil)
    
    val genericCTS = ClassTypeSignature(pgk, genericSCTS, Nil)
    def createCts(cn : String, ptas: List[TypeArgument]) = ClassTypeSignature(pgk,createScts(cn, ptas),Nil)
    
    /*creates ProperTypeArguments*/
    def elementType(cts: ClassTypeSignature) = ProperTypeArgument(None, cts)
    def extendedElementType(cts: ClassTypeSignature) = ProperTypeArgument(Some(CovariantIndicator), cts)
    def superedElementType(cts: ClassTypeSignature) = ProperTypeArgument(Some(ContravariantIndicator), cts)
    
    /*
     * ClassTypeSignature definitions that are used within the tests
     */
    
    /** UContainer<UnknownType> */
    val unknownContainer = ClassTypeSignature(pgk, SimpleClassTypeSignature("UContainer", List(elementType(ClassTypeSignature(Some("unknown/"), SimpleClassTypeSignature("UnkownType", Nil), Nil)))), Nil)
    /** SimpleGeneric<Base> */
    val baseContainer = createCts("SimpleGeneric", List(elementType(baseCTS))) 
    /**SimpleGeneric<AlternativeBase> */
    val altContainer = createCts("SimpleGeneric", List(elementType(altBaseCTS)))
    /** SimpleGeneric<lvlTwoBase>*/
    val lvlTwoContainer = createCts("SimpleGeneric", List(elementType(lvlTwoBaseCTS)))
    /** SimpleGeneric<Base> */ /**  */
    val extBaseContainer = createCts("SimpleGeneric", List(elementType(extBaseCTS))) 
    /** ExtendedGeneric<Base> */
    val extGenContainer = createCts("ExtendedGeneric", List(elementType(baseCTS)))
    /** SimpleGeneric<*> */
    val wildCardContainer = createCts("SimpleGeneric", List(Wildcard)) 
    /**  SimpleGeneric<? extends Base>*/
    val covariantContainer = createCts("SimpleGeneric", List(extendedElementType(baseCTS))) 
    /**  SimpleGeneric<? super Base>*/
    val contravariantContainer = createCts("SimpleGeneric", List(superedElementType(extBaseCTS)))
    /**  SimpleGeneric<? super SimpleGenericBase>*/
    val contravariantWithContainer = createCts("SimpleGeneric", List(superedElementType(baseContainer)))
    /**  SimpleGeneric<? super Base> */
    val contravariantBaseContainer = createCts("SimpleGeneric", List(superedElementType(baseCTS)))
    /** SubGenericET<SimpleGeneric<Base>, SimpleGeneric<ExtendedBase>>*/ 
    val doubleContainerET = createCts("SubGenericET", List(elementType(baseCTS), elementType(extBaseCTS)))
    /** SubGenericTE<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/ 
    val doubleContainerTE = createCts("SubGenericTE", List(elementType(extBaseCTS), elementType(baseCTS)))
    /** IndependentSubclass<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val doubleContainerBase = createCts("IndependentSubclass", List(elementType(extBaseCTS), elementType(baseCTS)))
    /** SubGenericET<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val wrongDoubleContainer =  createCts("SubGenericET", List(elementType(extBaseCTS), elementType(baseCTS)))
    /** SimpleGeneric<SimpleGeneric<Base>> */
    val nestedBase = createCts("SimpleGeneric", List(elementType(baseContainer)))
    /** SimpleGeneric<SimpleGeneric<ExtendedBase>> */
    val nestedExtBase = createCts("SimpleGeneric", List(elementType(extBaseContainer)))
    /** SimpleGeneric<SimpleGeneric<lvlTwoContainer>> */
    val nestedLvlTwoBase = createCts("SimpleGeneric", List(elementType(lvlTwoContainer)))
    /** SimpleGeneric<SimpleGeneric<AlternativeBase>> */
    val nestedAltBase = createCts("SimpleGeneric", List(elementType(altContainer)))
    /** SimpleGeneric<ExtendedGeneric<Base>> */
    val nestedSubGenBase = createCts("SimpleGeneric", List(elementType(extGenContainer)))
    /** SimpleGeneric<SimpleGeneric<? extends Base>> */
    val nestedInnerCovariantContainer = createCts("SimpleGeneric", List(elementType(covariantContainer)))
    /** SimpleGeneric<? extends SimpleGeneric<Base>> */   
    val nestedOutterCovariantContainer = createCts("SimpleGeneric", List(extendedElementType(baseContainer)))
    /** SimpleGeneric<? super SimpleGeneric<Base>> */
    val nestedContravariantContainer = createCts("SimpleGeneric", List(elementType(contravariantBaseContainer)))
}