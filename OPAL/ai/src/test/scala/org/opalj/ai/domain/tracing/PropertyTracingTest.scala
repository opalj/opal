/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package tracing

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * This test(suite) basically tests OPAL's support for tracing a property.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyTracingTest extends AnyFlatSpec with Matchers {

    import PropertyTracingTest._

    class AnalysisDomain(val method: Method)
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with PredefinedClassHierarchy
        with SimpleBooleanPropertyTracing
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l1.DefaultIntegerRangeValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l1.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with TheMethod {

        override def maxCardinalityOfIntegerRanges: Long = 16L

        override def throwIllegalMonitorStateException: Boolean = false

        override def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
            ExceptionsRaisedByCalledMethods.AllExplicitlyHandled
        }

        override def propertyName = "isSanitized"

        override def invokestatic(
            pc:             Int,
            declaringClass: ObjectType,
            isInterface:    Boolean,
            name:           String,
            descriptor:     MethodDescriptor,
            operands:       Operands
        ): MethodCallResult = {

            // let's check if the first parameter (_ == -2) passed to a method is
            // passed to a method called sanitize...
            if (name == "sanitize" && originsIterator(operands.head).exists(_ == -2)) {
                updateProperty(pc, true)
            }
            super.invokestatic(pc, declaringClass, isInterface, name, descriptor, operands)
        }

        def isSanitized(): Boolean = hasPropertyOnExit
    }

    private def evaluateMethod(name: String)(f: AnalysisDomain => Unit): Unit = {
        /*
         * In this case we want to make sure that a specific value (given as a
         * parameter to a method) is always sanitized (within the method.) I.e.,
         * that the value is passed to a function called sanitizer.
         */
        val method = classFile.findMethod(name).head
        val domain = new AnalysisDomain(method)
        val code = method.body.get
        val result = BaseAI(method, domain)
        dumpOnFailureDuringValidation(Some(classFile), Some(method), code, result) { f(domain) }
    }

    behavior of "a domain that traces control-flow dependent properties"

    //
    // TESTS
    //

    it should "be able to correctly identify that the method notSanitized1 does not sanitize the value" in {
        evaluateMethod("notSanitized1") { domain =>
            domain.isSanitized() should be(false)
        }
    }

    it should "be able to correctly identify that the method notSanitized2 does not sanitize the value" in {
        evaluateMethod("notSanitized2") { domain =>
            domain.isSanitized() should be(false)
        }
    }

    it should "be able to correctly identify that the method sanitized1 does sanitize the value" in {
        evaluateMethod("sanitized1") { domain =>
            domain.isSanitized() should be(true)
        }
    }
    it should "be able to correctly identify that the method sanitized2 does sanitize the value" in {
        evaluateMethod("sanitized2") { domain =>
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized3 does sanitize the value" in {
        evaluateMethod("sanitized3") { domain =>
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized4 does sanitize the value" in {
        evaluateMethod("sanitized4") { domain =>
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized5 does sanitize the value" in {
        evaluateMethod("sanitized5") { domain =>
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized6 does sanitize the value" in {
        evaluateMethod("sanitized6") { domain =>
            domain.isSanitized() should be(true)
        }
    }

    // We can not yet identify that the value is definitively sanitized.
    //    it should "be able to correctly identify that the method sanitized7 does sanitize the value" in {
    //        evaluateMethod("sanitized7", domain => {
    //            domain.isSanitized() should be(true)
    //        })
    //    }
}

private object PropertyTracingTest {

    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/domain/Sanitization").get
}
