/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

import org.opalj.bi.TestResources.locateTestResources

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class JavaProjectTest extends AnyFlatSpec with Matchers {

    behavior of "A Java Project"

    it should "be able to query the sub classes" in {
        val methodsProjectArchive = locateTestResources("methods.jar", "bi")
        val deprecatedProjectArchive = locateTestResources("deprecated.jar", "bi")

        val cp = new java.util.ArrayList[java.io.File]();
        cp.add(methodsProjectArchive)
        cp.add(deprecatedProjectArchive)

        val jp = new JavaProject(cp)

        jp.getAllSubclassesOfObjectType("methods/a/Super").asScala should contain("methods.a.DirectSub")
    }

}
