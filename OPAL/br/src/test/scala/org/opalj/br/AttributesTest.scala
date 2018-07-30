/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.FunSuite

import org.opalj.bi.TestResources.locateTestResources

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AttributesTest extends FunSuite {

    val attributesJARFile = locateTestResources("deprecated.jar", "bi")

    import reader.Java8Framework.ClassFile

    test("test that the deprecated attribute is present") {
        val cf1 = ClassFile(attributesJARFile, "deprecated/DeprecatedByAnnotation.class").head
        assert(cf1.isDeprecated)
        assert(
            cf1.runtimeVisibleAnnotations.exists {
                case Annotation(ObjectType("java/lang/Deprecated"), _) ⇒ true
                case _                                                 ⇒ false
            }
        )

        val cf2 = ClassFile(attributesJARFile, "deprecated/DeprecatedByJavaDoc.class").head
        assert(cf2.isDeprecated)
    }

    test("test that the source file attribute is present") {
        val cf1 = ClassFile(attributesJARFile, "deprecated/DeprecatedByAnnotation.class").head
        assert(cf1.sourceFile != None)
    }

}
