/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.br.TestSupport.biProject

/**
 * Tests the `ProjectIndex`.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ProjectIndexTest extends AnyFlatSpec with Matchers {

    val methodsProjectIndex = biProject("methods.jar").get(ProjectIndexKey)

    val fieldsProjectIndex = biProject("fields-g=none-5.jar").get(ProjectIndexKey)

    //
    //
    // Verify
    //
    //

    behavior of "A ProjectIndex w.r.t. finding methods"

    it should "find a unique method by its signature" in {
        val name = "someMethod"
        val descriptor = MethodDescriptor.NoArgsAndReturnVoid
        val methods = methodsProjectIndex.findMethods(name, descriptor)
        methods should have size (1)
        methods.forall(m => m.name == name && m.descriptor == descriptor) should be(true)
    }

    it should "find multiple methods with the same signature if the method is overridden or reimplemented" in {
        val name = "publicMethod"
        val descriptor = MethodDescriptor.NoArgsAndReturnVoid
        val methods = methodsProjectIndex.findMethods(name, descriptor)
        methods should have size (4)
        methods.forall(m => m.name == name && m.descriptor == descriptor) should be(true)
    }

    it should ("not find a method that does not exist") in {
        val methods = methodsProjectIndex.findMethods(
            "someRandomMethodNameOfANonExistingMethod",
            MethodDescriptor.NoArgsAndReturnVoid
        )
        methods should have size (0)
    }

    behavior of "A ProjectIndex w.r.t. finding fields"

    it should ("find all definitions of the field \"x : int\"") in {
        val name = "x"
        val fieldType = IntegerType
        val matches = fieldsProjectIndex.findFields(name, fieldType).toSet
        matches should have size (4)
        matches.forall(f => f.name == name && f.fieldType == fieldType) should be(true)
    }

    it should ("find all definitions of the field \"y : int\"") in {
        val name = "y"
        val fieldType = IntegerType
        val results = fieldsProjectIndex.findFields(name, fieldType).toSet
        results should have size (3)
        results.forall(f => f.name == name && f.fieldType == fieldType) should be(true)
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
