/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation
import org.opalj.ai.domain.l0._

/**
 * Basic tests of the abstract interpreter related to handling arrays.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithArraysTest extends AnyFlatSpec with Matchers {

    import MethodsWithArraysTest._

    class TestDomain
        extends Domain
        with DefaultSpecialDomainValuesBinding
        with DefaultReferenceValuesBinding
        with DefaultTypeLevelIntegerValues
        with DefaultTypeLevelLongValues
        with DefaultTypeLevelFloatValues
        with DefaultTypeLevelDoubleValues
        with TypeLevelPrimitiveValuesConversions
        with TypeLevelLongValuesShiftOperators
        with TypeLevelFieldAccessInstructions
        with SimpleTypeLevelInvokeInstructions
        with TypeLevelDynamicLoads
        with ThrowAllPotentialExceptionsConfiguration
        with IgnoreSynchronization
        with DefaultHandlingOfMethodResults
        with RecordLastReturnedValues
        with PredefinedClassHierarchy {

        type Id = String
        def id = "MethodsWithArraysTestDomain"
    }

    private def evaluateMethod(name: String, f: TestDomain => Unit): Unit = {
        val domain = new TestDomain

        val method = classFile.methods.find(_.name == name).get
        val code = method.body.get
        val result = BaseAI(method, domain)

        dumpOnFailureDuringValidation(Some(classFile), Some(method), code, result) { f(domain) }
    }

    behavior of "the abstract interpreter"

    it should "be able to analyze a method that processes a byte array" in {
        evaluateMethod("byteArrays", domain => {
            import domain._
            domain.allReturnedValues should be(
                Map((15 -> AByteValue))
            )
        })
    }

    it should "be able to analyze a method that processes a boolean array" in {
        evaluateMethod("booleanArrays", domain => {
            import domain._
            domain.allReturnedValues should be(
                Map((14 -> ABooleanValue))
            )
        })
    }

    it should "be able to analyze a method that uses the Java feature that arrays are covariant" in {
        evaluateMethod("covariantArrays", domain => {
            domain.allReturnedValues.size should be(1)
            domain.isValueASubtypeOf(
                domain.allReturnedValues(24), ObjectType.Object
            ) should be(Yes)
        })
    }

    it should "be able to analyze a method that does various (complex) type casts related to arrays" in {
        evaluateMethod("integerArraysFrenzy", domain => {
            domain.allReturnedValues.size should be(2)
            domain.isValueASubtypeOf(
                domain.allReturnedValues(78), ArrayType(IntegerType)
            ) should be(Yes)
            domain.isValueASubtypeOf(
                domain.allReturnedValues(76), ArrayType(ByteType)
            ) should be(Yes)
        })
    }
}
private object MethodsWithArraysTest {

    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithArrays").get
}
