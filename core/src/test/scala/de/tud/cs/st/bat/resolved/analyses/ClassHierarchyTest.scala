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

import reader.Java7Framework.ClassFiles

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

/**
 * Basic tests of the class hierarchy.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ClassHierarchyTest
        extends FlatSpec
        with ShouldMatchers /*with BeforeAndAfterAll */ {

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE SUBTYPE RELATION RELATED FUNCTIONALITY
    //
    // -----------------------------------------------------------------------------------

    //
    // Setup
    //
    val ch = ClassHierarchy.preInitializedClassHierarchy

    val Object = ObjectType("java/lang/Object")
    val Throwable = ObjectType("java/lang/Throwable")
    val Exception = ObjectType("java/lang/Exception")
    val Error = ObjectType("java/lang/Error")
    val RuntimeException = ObjectType("java/lang/RuntimeException")
    val Cloneable = ObjectType.Cloneable
    val Serializable = ObjectType.Serializable
    val SeriablizableArray = ArrayType(Serializable)
    val SeriablizableArrayOfArray = ArrayType(SeriablizableArray)
    val AnUnknownType = ObjectType("myTest/AnUnknownType")
    val AnUnknownTypeArray = ArrayType(AnUnknownType)
    val CloneableArray = ArrayType(Cloneable)
    val ObjectArray = ArrayType(Object)
    val intArray = ArrayType(IntegerType)
    val longArray = ArrayType(LongType)

    //
    // Verify
    //

    behavior of "the default ClassHierarchy's isSubtypeOf method w.r.t. Arrays"

    import ch.isSubtypeOf

    it should "correctly reflect the base exception hierarchy" in {

        isSubtypeOf(Throwable, Object) should be(Yes)
        isSubtypeOf(Error, Throwable) should be(Yes)
        isSubtypeOf(RuntimeException, Exception) should be(Yes)
        isSubtypeOf(Exception, Throwable) should be(Yes)

        isSubtypeOf(Object, Throwable) should be(No)

        isSubtypeOf(AnUnknownType, Object) should be(Yes)
        isSubtypeOf(Object, AnUnknownType) should be(No)

    }

    it should "correctly reflect the basic type hierarchy related to Arrays" in {
        isSubtypeOf(ObjectArray, Object) should be(Yes)
        isSubtypeOf(SeriablizableArray, ObjectArray) should be(Yes)
        isSubtypeOf(CloneableArray, ObjectArray) should be(Yes)
        isSubtypeOf(ObjectArray, ObjectArray) should be(Yes)
        isSubtypeOf(SeriablizableArray, SeriablizableArray) should be(Yes)
        isSubtypeOf(AnUnknownTypeArray, AnUnknownTypeArray) should be(Yes)

        isSubtypeOf(Object, ObjectArray) should be(No)
        isSubtypeOf(CloneableArray, SeriablizableArray) should be(No)

        isSubtypeOf(AnUnknownTypeArray, SeriablizableArray) should be(Unknown)

        isSubtypeOf(SeriablizableArray, AnUnknownTypeArray) should be(No)
    }

    it should "correctly reflect the type hierarchy related to Arrays of primitives" in {
        isSubtypeOf(intArray, Object) should be(Yes)
        isSubtypeOf(intArray, Serializable) should be(Yes)
        isSubtypeOf(intArray, Cloneable) should be(Yes)
        isSubtypeOf(intArray, intArray) should be(Yes)

        isSubtypeOf(intArray, longArray) should be(No)
        isSubtypeOf(longArray, intArray) should be(No)
    }

    it should "correctly reflect the type hierarchy related to Arrays of Arrays" in {
        isSubtypeOf(SeriablizableArrayOfArray, Object) should be(Yes)
        isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArrayOfArray) should be(Yes)

        isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArray) should be(Yes)
        isSubtypeOf(SeriablizableArrayOfArray, ObjectArray) should be(Yes)
        isSubtypeOf(SeriablizableArrayOfArray, CloneableArray) should be(Yes)

        isSubtypeOf(SeriablizableArrayOfArray, AnUnknownTypeArray) should be(No)
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE RESOLVING OF FIELD REFERENCES
    //
    // -----------------------------------------------------------------------------------

    val resources = TestSupport.locateTestResources("classfiles/Fields.jar")
    val project = new Project ++ ClassFiles(resources)
    import project.classes

    val SuperSuperType = ObjectType("fields/SuperSuper")
    val SuperSuperClass = classes(SuperSuperType)
    val SuperType = ObjectType("fields/Super")
    val SuperClass = classes(SuperType)

    val SuperIType = ObjectType("fields/SuperI")
    val SuperIClass = classes(SuperIType)
    val SubIType = ObjectType("fields/SubI")
    val SubIClass = classes(SubIType)

    val SubType = ObjectType("fields/Sub")
    val SubClass = classes(SubType)
    val SubSubType = ObjectType("fields/SubSub")
    val SubSubClass = classes(SubSubType)

    behavior of "the ClassHierarchy's method to resolve field references"

    import project.classHierarchy.resolveFieldReference

    it should "correctly resolve a reference to a static field in a superclass" in {
        resolveFieldReference(SuperType, "x", IntegerType, project) should be(
            Some((SuperSuperClass, SuperSuperClass.fields(0)))
        )
    }

    it should "correctly resolve a reference to a field defined in an interface" in {
        resolveFieldReference(SubIType, "THE_SUB_I", IntegerType, project) should be(
            Some((SubIClass, SubIClass.fields(0)))
        )
    }

    it should "correctly resolve a reference to a field defined in a superinterface of an interface" in {
        resolveFieldReference(SubIType, "THE_I", IntegerType, project) should be(
            Some((SuperIClass, SuperIClass.fields(0)))
        )
    }

    it should "correctly resolve a reference to a field defined in a superinterface" in {
        resolveFieldReference(SubType, "THE_I", IntegerType, project) should be(
            Some((SuperIClass, SuperIClass.fields(0)))
        )
    }

    it should "correctly resolve a reference to a field defined in a superclass" in {
        resolveFieldReference(SubSubType, "x", IntegerType, project) should be(
            Some((SubClass, SubClass.fields(0)))
        )
    }

    it should "correctly resolve a reference to a private field defined in a superclass" in {
        resolveFieldReference(SubSubType, "y", IntegerType, project) should be(
            Some((SuperClass, SuperClass.fields(0)))
        )
    }

    it should "not fail if the field cannot be found" in {
        resolveFieldReference(SubSubType, "NOT_DEFINED", IntegerType, project) should be(
            None
        )
    }

    it should "not fail if the type cannot be found" in {
        resolveFieldReference(
            ObjectType("NOT/DEFINED"),
            "NOT_DEFINED",
            IntegerType,
            project) should be(
                None
            )
    }
}
