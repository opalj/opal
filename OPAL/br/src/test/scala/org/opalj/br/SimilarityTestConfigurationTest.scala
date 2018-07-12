/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PROTECTED
import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.opalj.br.instructions.IADD
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.IRETURN

/**
 * Tests the configuration of the similarity test.
 *
 * The default functionality is also tested in ClassFileTest.
 *
 * @author Timothy Earley
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SimilarityTestConfigurationTest extends FunSuite with Matchers {

    //
    // Test Fixtures
    // (The following classes do NOT represent valid class files!)
    //
    def simpleFieldAttributes = List(Synthetic, Deprecated)
    def simpleField = Field(ACC_PUBLIC.mask, "test", ByteType, simpleFieldAttributes)
    def simpleFields = Vector(simpleField, Field(ACC_PROTECTED.mask, "field 2", BooleanType, simpleFieldAttributes))
    def simpleCode = Code(2, 0, Array(ICONST_1, ICONST_1, IADD, IRETURN))
    def simpleMethod = Method(
        ACC_PUBLIC.mask,
        "simple_method",
        MethodDescriptor.JustReturnsBoolean,
        Vector(simpleCode, Deprecated)
    )
    def simpleMethod2 = Method(
        ACC_STATIC.mask | ACC_PRIVATE.mask,
        "simple_method_2",
        MethodDescriptor.NoArgsAndReturnVoid,
        Vector(Code(0, 0, Array()))
    )
    def simpleMethods = Vector(simpleMethod, simpleMethod2)
    def simpleClass = ClassFile(
        minorVersion = 1,
        majorVersion = 2,
        accessFlags = ACC_PUBLIC.mask,
        thisType = ObjectType.Boolean,
        superclassType = Some(ObjectType.Object),
        interfaceTypes = List(ObjectType.Byte, ObjectType.Float),
        fields = simpleFields,
        methods = simpleMethods,
        attributes = List(SourceFile("abc"), Deprecated)
    )

    //
    // TESTS
    //

    test("two identical class files are similar when all elements are compared") {
        assert(simpleClass.findDissimilarity(simpleClass, CompareAllConfiguration).isEmpty)
    }

    test("two identical class files are similar when only hardcoded comparisons are done") {
        object NoTestsConfiguration extends SimilarityTestConfiguration {

            def compareFields(
                leftContext: ClassFile,
                left:        Seq[JVMField],
                right:       Seq[JVMField]
            ): (Seq[JVMField], Seq[JVMField]) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareMethods(
                leftContext: ClassFile,
                left:        Seq[JVMMethod],
                right:       Seq[JVMMethod]
            ): (Seq[JVMMethod], Seq[JVMMethod]) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareCode(
                leftContext: JVMMethod,
                left:        Option[Code],
                right:       Option[Code]
            ): (Option[Code], Option[Code]) = {
                (None, None)
            }
        }
        assert(simpleClass.findDissimilarity(simpleClass, NoTestsConfiguration).isEmpty)
    }

    test("tests the comparison of two identical class files if only one attribute is compared") {
        object NotDeprecatedFilter extends CompareAllConfiguration {
            override def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                val (newLeft, newRight) = super.compareAttributes(leftContext, left, right)
                (
                    newLeft.filter(a ⇒ a.isInstanceOf[SourceFile]),
                    newRight.filter(a ⇒ a.isInstanceOf[SourceFile])
                )
            }
        }
        // the following class has less attributes
        val noLongerDeprecatedClass = simpleClass.copy(attributes = Vector(SourceFile("abc")))

        // normal test fails
        assert(!simpleClass.similar(noLongerDeprecatedClass))

        // ignoring certain attributes works both ways
        assert(
            simpleClass.similar(noLongerDeprecatedClass, NotDeprecatedFilter),
            simpleClass.findDissimilarity(noLongerDeprecatedClass, NotDeprecatedFilter).toString
        )
        assert(
            noLongerDeprecatedClass.similar(simpleClass, NotDeprecatedFilter)
        )
    }

    test("tests the comparison of two identical class files if only one attribute is selected") {
        object NotDeprecatedFilter extends CompareAllConfiguration {
            override def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                val (superNewLeft, superNewRight) = super.compareAttributes(leftContext, left, right)
                val (newLeft, newRight) = (
                    superNewLeft.filter(a ⇒ a != Deprecated),
                    superNewRight.filter(a ⇒ a != Deprecated)
                )
                (newLeft, newRight)
            }
        }
        // the following class has less attributes
        val noLongerDeprecatedClass = simpleClass.copy(attributes = Vector(SourceFile("abc")))

        // normal test fails
        assert(!simpleClass.similar(noLongerDeprecatedClass))

        // ignoring certain attributes works both ways
        assert(
            simpleClass.similar(noLongerDeprecatedClass, NotDeprecatedFilter),
            simpleClass.findDissimilarity(noLongerDeprecatedClass, NotDeprecatedFilter).toString
        )
        assert(
            noLongerDeprecatedClass.similar(simpleClass, NotDeprecatedFilter)
        )
    }

    test("tests the comparison of two identical class files if some field is not defined") {
        object FieldsWithAccessFlagsEquals1 extends CompareAllConfiguration {
            override def compareFields(
                leftContext: ClassFile,
                left:        Seq[JVMField],
                right:       Seq[JVMField]
            ): (Seq[JVMField], Seq[JVMField]) = {
                (
                    left.filter(a ⇒ a.accessFlags == 1),
                    right.filter(a ⇒ a.accessFlags == 1)
                )
            }
        }
        // the following class has less fields
        val classWithLessFields = simpleClass.copy(fields = Vector(simpleField))

        // normal test fails
        assert(!simpleClass.similar(classWithLessFields))

        // ignoring certain fields works both ways
        assert(
            simpleClass.similar(classWithLessFields, FieldsWithAccessFlagsEquals1),
            simpleClass.attributes+" vs. "+classWithLessFields.attributes
        )
        assert(
            classWithLessFields.similar(simpleClass, FieldsWithAccessFlagsEquals1)
        )
    }

}
