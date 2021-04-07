/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

import org.opalj.bi.TestResources.locateTestResources

/**
 * Tests the reading of class files.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class ClassFileReaderTest extends AnyFlatSpec with Matchers {

    import Java8Framework.ClassFiles

    behavior of "ClassFile reader"

    it should "be able to read class files stored in jar files stored within jar files (nested jar files)" in {
        val classFiles = ClassFiles(locateTestResources("classfiles/JarsInAJar.jar", "bi"))
        if (!classFiles.exists(_._1.fqn == "attributes/DeprecatedByAnnotation"))
            fail("could not find the class attributes.DeprecatedByAnnotation")

        if (!classFiles.exists(_._1.fqn == "code/BoundedBuffer"))
            fail("could not find the class code.BoundedBuffer")
    }

    it should "not crash when trying to read an empty (0-byte) .jar" in {
        val emptyJARFile = locateTestResources("classfiles/Empty.jar", "bi")
        emptyJARFile.length() should be(0)
        ClassFiles(emptyJARFile) should be(empty)
    }

}
