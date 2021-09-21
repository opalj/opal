/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests OPAL's support w.r.t. inner classes.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IsInnerClassPropertyTest extends AnyFlatSpec with Matchers {

    val project = TestSupport.biProject("innerclasses-1.8-g-parameters-genericsignature")

    val myRootClass$Formatter = ObjectType("innerclasses/MyRootClass$Formatter")
    val myRootClass = ObjectType("innerclasses/MyRootClass")
    val myRootClass$1 = ObjectType("innerclasses/MyRootClass$1")
    val myRootClass$1$1 = ObjectType("innerclasses/MyRootClass$1$1")

    behavior of "a named inner class"

    it should "should return true if isInnerClass is called" in {
        project.classFile(myRootClass$Formatter).get.isInnerClass should be(true)
    }

    it should "correctly identify its outer class" in {
        project.classFile(myRootClass$Formatter).get.outerType.get._1 should be(myRootClass)
    }

    behavior of "an anonymous inner class"

    it should "should return true when isInnerClass is called" in {
        project.classFile(myRootClass$1).get.isInnerClass should be(true)
    }

    it should "should return true when isInnerClass is called even if it is an inner class of another anonymous inner class" in {
        project.classFile(myRootClass$Formatter).get.isInnerClass should be(true)
    }

    behavior of "an outer class that is not an inner class"

    it should "return false if isInnerClass is called" in {
        project.classFile(myRootClass).get.isInnerClass should be(false)
    }

}
