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
package de.tud.cs.st
package bat
package resolved
package analyses

import util.{ Answer, Yes, No, Unknown }

import reader.Java8Framework.ClassFiles

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

/**
 * Basic tests of the class hierarchy.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ClassHierarchyTest
        extends FlatSpec
        with Matchers /*with BeforeAndAfterAll */ {

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE SUBTYPE RELATION RELATED FUNCTIONALITY
    //
    // -----------------------------------------------------------------------------------

    //
    // Setup
    //
    val preInitCH =
        ClassHierarchy.preInitializedClassHierarchy
    val javaLangCH =
        ClassHierarchy(
            Traversable.empty,
            List(() ⇒ getClass.getResourceAsStream("JavaLangClassHierarchy.ths"))
        )

    val Object = ObjectType.Object
    val Throwable = ObjectType.Throwable
    val Exception = ObjectType.Exception
    val Error = ObjectType.Error
    val RuntimeException = ObjectType.RuntimeException
    val ArithmeticException = ObjectType.ArithmeticException
    val Cloneable = ObjectType.Cloneable
    val Serializable = ObjectType.Serializable
    val SeriablizableArray = ArrayType(Serializable)
    val SeriablizableArrayOfArray = ArrayType(SeriablizableArray)
    val AnUnknownType = ObjectType("myTest/AnUnknownType")
    val AnUnknownTypeArray = ArrayType(AnUnknownType)
    val CloneableArray = ArrayType(Cloneable)
    val ObjectArray = ArrayType.ArrayOfObjects
    val intArray = ArrayType(IntegerType)
    val arrayOfIntArray = ArrayType(ArrayType(IntegerType))
    val longArray = ArrayType(LongType)

    //
    // Verify
    //

    behavior of "the default ClassHierarchy"

    it should "be upwards closed (complete)" in {
        if (preInitCH.rootTypes.size != 4) {
            fail(
                "The default class hierarchy has unexpected root types (expected: java/lang/Object and java/security and sun/reflect related classes): "+
                    preInitCH.rootTypes.mkString(", "))
        }
    }

    behavior of "the default ClassHierarchy's isKnown method"

    it should "return true for all known types" in {
        preInitCH.isKnown(Throwable) should be(true)
    }

    it should "return false for all unknown types" in {
        preInitCH.isKnown(AnUnknownType) should be(false)
    }

    behavior of "the default ClassHierarchy's isDirectSupertypeInformationComplete method"

    it should "return true if a type's super type information is definitive complete" in {
        javaLangCH.isDirectSupertypeInformationComplete(Object) should be(true)
        javaLangCH.isDirectSupertypeInformationComplete(Throwable) should be(true)
    }

    it should "return false if a type's super type information is not guaranteed to be complete" in {
        javaLangCH.isDirectSupertypeInformationComplete(Serializable) should be(false)
        javaLangCH.isDirectSupertypeInformationComplete(AnUnknownType) should be(false)
    }

    behavior of "the default ClassHierarchy's isSubtypeOf method w.r.t. class types"

    it should "return Unknown if the \"subtype\" is unknown" in {
        preInitCH.isSubtypeOf(AnUnknownType, Throwable) should be(Unknown)
    }

    it should "return Yes if a class-type indirectly inherits an interface-type" in {
        preInitCH.isSubtypeOf(ArithmeticException, Serializable) should be(Yes)
    }

    it should "always return Yes if both types are identical" in {
        preInitCH.isSubtypeOf(ArithmeticException, ArithmeticException) should be(Yes)
        preInitCH.isSubtypeOf(AnUnknownType, AnUnknownType) should be(Yes)
    }

    it should "return Yes for interface types when the given super type is Object even if the interface type's supertypes are not known" in {
        preInitCH.isSubtypeOf(Serializable, Object) should be(Yes)
    }

    it should "return No for a type that is not a subtype of another type and all type information is known" in {
        // "only" classes
        preInitCH.isSubtypeOf(Error, Exception) should be(No)
        preInitCH.isSubtypeOf(Exception, Error) should be(No)
        preInitCH.isSubtypeOf(Exception, RuntimeException) should be(No)

        // "only" interfaces
        preInitCH.isSubtypeOf(Serializable, Cloneable) should be(No)

        // class and interface
        preInitCH.isSubtypeOf(ArithmeticException, Cloneable) should be(No)
    }

    it should "return Unknown if two types are not in an inheritance relationship but the subtype's supertypes are not guaranteed to be known" in {
        javaLangCH.isSubtypeOf(Serializable, Cloneable) should be(Unknown)
    }

    behavior of "the preInitialized ClassHierarchy's isSubtypeOf method w.r.t. Exceptions"

    it should "correctly reflect the base exception hierarchy" in {

        preInitCH.isSubtypeOf(Throwable, Object) should be(Yes)
        preInitCH.isSubtypeOf(Error, Throwable) should be(Yes)
        preInitCH.isSubtypeOf(RuntimeException, Exception) should be(Yes)
        preInitCH.isSubtypeOf(Exception, Throwable) should be(Yes)

        preInitCH.isSubtypeOf(Object, Throwable) should be(No)

        preInitCH.isSubtypeOf(AnUnknownType, Object) should be(Yes)
        preInitCH.isSubtypeOf(Object, AnUnknownType) should be(No)

    }

    behavior of "the ClassHierarchy's isSubtypeOf method w.r.t. Arrays"

    it should "correctly reflect the basic type hierarchy related to Arrays" in {
        preInitCH.isSubtypeOf(ObjectArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(CloneableArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(ObjectArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArray, SeriablizableArray) should be(Yes)
        preInitCH.isSubtypeOf(AnUnknownTypeArray, AnUnknownTypeArray) should be(Yes)

        preInitCH.isSubtypeOf(Object, ObjectArray) should be(No)
        preInitCH.isSubtypeOf(CloneableArray, SeriablizableArray) should be(No)

        preInitCH.isSubtypeOf(AnUnknownTypeArray, SeriablizableArray) should be(Unknown)

        preInitCH.isSubtypeOf(SeriablizableArray, AnUnknownTypeArray) should be(No)
    }

    it should "correctly reflect the type hierarchy related to Arrays of primitives" in {
        preInitCH.isSubtypeOf(intArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(intArray, Serializable) should be(Yes)
        preInitCH.isSubtypeOf(intArray, Cloneable) should be(Yes)
        preInitCH.isSubtypeOf(intArray, intArray) should be(Yes)

        preInitCH.isSubtypeOf(intArray, longArray) should be(No)
        preInitCH.isSubtypeOf(longArray, intArray) should be(No)

        preInitCH.isSubtypeOf(arrayOfIntArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(arrayOfIntArray, SeriablizableArray) should be(Yes)
    }

    it should "correctly reflect the type hierarchy related to Arrays of Arrays" in {
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArrayOfArray) should be(Yes)

        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, CloneableArray) should be(Yes)

        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, AnUnknownTypeArray) should be(No)
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTS IF WE HAVE AN INCOMPLETE CLASS HIERARCHY
    //
    // -----------------------------------------------------------------------------------

    val apacheANTCH =
        ClassHierarchy(
            Traversable.empty,
            List(() ⇒ getClass.getResourceAsStream("ApacheANT1.7.1.ClassHierarchy.ths"))
        )

    it should "be possible to get all supertypes, even if not all information is available" in {

        val mi = ObjectType("org/apache/tools/ant/taskdefs/MacroInstance")
        apacheANTCH.allSupertypes(mi) should be(Set(
            ObjectType("org/apache/tools/ant/Task"),
            ObjectType("org/apache/tools/ant/TaskContainer"),
            ObjectType("org/apache/tools/ant/DynamicAttribute")
        ))
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE TRAVERSAL OF THE CLASS HIERARCHY
    //
    // -----------------------------------------------------------------------------------

    val clusteringProject =
        IndexBasedProject[java.net.URL](
            ClassFiles(TestSupport.locateTestResources("classfiles/ClusteringTestProject.jar"))
        )

    behavior of "the ClassHierarchy's method to traverse the class hierarchy"

    it should "correctly find all suptyes of an interface" in {
        import clusteringProject.classHierarchy

        val window = ObjectType("pattern/decorator/example1/Window")
        val simpleWindow = ObjectType("pattern/decorator/example1/SimpleWindow")

        classHierarchy.isKnown(window) should be(true)
        classHierarchy.isKnown(simpleWindow) should be(true)

        classHierarchy.isSubtypeOf(window, simpleWindow) should be(No)
        classHierarchy.isSubtypeOf(simpleWindow, window) should be(Yes)

        // check if the SimpleWindow is in the Set of all subtypes of Window
        var subtypes = Set.empty[ObjectType]
        classHierarchy.foreachSubtype(window) { subtypes += _ }
        subtypes.contains(simpleWindow) should be(true)

        clusteringProject(simpleWindow).get.methods.find(method ⇒
            method.name == "draw" &&
                method.descriptor == MethodDescriptor.NoArgsAndReturnVoid
        ) should be('defined)

        classHierarchy.lookupImplementingMethods(
            window,
            "draw",
            MethodDescriptor.NoArgsAndReturnVoid,
            clusteringProject) should be('nonEmpty)
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF FIELD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    val fieldsProject =
        IndexBasedProject[java.net.URL](
            ClassFiles(TestSupport.locateTestResources("classfiles/Fields.jar"))
        )
    import fieldsProject.classFile

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
    val SubSubClass = classFile(SubSubType).get

    behavior of "the ClassHierarchy's method to resolve field references"

    import fieldsProject.classHierarchy.resolveFieldReference

    it should "correctly resolve a reference to a static field in a superclass" in {
        resolveFieldReference(SuperType, "x", IntegerType, fieldsProject) should be(
            Some(SuperSuperClass.fields(0))
        )
    }

    it should "correctly resolve a reference to a field defined in an interface" in {
        resolveFieldReference(SubIType, "THE_SUB_I", IntegerType, fieldsProject) should be(
            Some(SubIClass.fields(0))
        )
    }

    it should "correctly resolve a reference to a field defined in a superinterface of an interface" in {
        resolveFieldReference(SubIType, "THE_I", IntegerType, fieldsProject) should be(
            Some(SuperIClass.fields(0))
        )
    }

    it should "correctly resolve a reference to a field defined in a superinterface" in {
        resolveFieldReference(SubType, "THE_I", IntegerType, fieldsProject) should be(
            Some(SuperIClass.fields(0))
        )
    }

    it should "correctly resolve a reference to a field defined in a superclass" in {
        resolveFieldReference(SubSubType, "x", IntegerType, fieldsProject) should be(
            Some(SubClass.fields(0))
        )
    }

    it should "correctly resolve a reference to a private field defined in a superclass" in {
        resolveFieldReference(SubSubType, "y", IntegerType, fieldsProject) should be(
            Some(SuperClass.fields(0))
        )
    }

    it should "not fail (throw an exception) if the field cannot be found" in {
        resolveFieldReference(SubSubType, "NOT_DEFINED", IntegerType, fieldsProject) should be(
            None
        )
    }

    it should "not fail if the type cannot be found" in {
        resolveFieldReference(
            ObjectType("NOT/DEFINED"),
            "NOT_DEFINED",
            IntegerType,
            fieldsProject) should be(None)
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF METHOD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    val methodsProject =
        IndexBasedProject(
            ClassFiles(TestSupport.locateTestResources("classfiles/Methods.jar"))
        )

    val superI = ObjectType("methods/b/SuperI")
    val directSub = ObjectType("methods/b/DirectSub")
    val directSubClassFile = methodsProject(directSub).get

    behavior of "the ClassHierarchy's methods to resolve method references"

    it should "handle the case if an interface has no implementing class" in {
        val classFile = methodsProject(superI).get
        val result = methodsProject.classHierarchy.lookupImplementingMethods(
            superI,
            "someMethod",
            MethodDescriptor.NoArgsAndReturnVoid,
            methodsProject)

        result.size should be(0)
    }

    it should "find a method in a super class" in {
        val classType = ObjectType("methods/b/B")
        val classFile = methodsProject(classType).get
        val result = methodsProject.classHierarchy.lookupImplementingMethods(
            classType,
            "publicMethod",
            MethodDescriptor.NoArgsAndReturnVoid,
            methodsProject)

        result.size should be(1)
        result.head should have(
            'name("publicMethod"),
            'descriptor(MethodDescriptor.NoArgsAndReturnVoid)
        )
    }
}
