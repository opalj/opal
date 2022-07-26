/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.immutable.LongMap

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br._
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation

/**
 * Tests the `DefaultPerInstructionPostProcessing`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultPerInstructionPostProcessingTest extends AnyFlatSpec with Matchers {

    import MethodsWithExceptionsTest._

    class DefaultRecordingDomain(val id: String) extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with PredefinedClassHierarchy
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with RecordLastReturnedValues
        with RecordAllThrownExceptions
        with RecordVoidReturns
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultReferenceValuesBinding
        with l1.DefaultIntegerRangeValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators {

        override def maxCardinalityOfIntegerRanges: Long = 16L

        override def throwIllegalMonitorStateException: Boolean = false

        override def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
            ExceptionsRaisedByCalledMethods.AllExplicitlyHandled
        }
    }

    private def evaluateMethod(name: String)(f: DefaultRecordingDomain => Unit): Unit = {
        val domain = new DefaultRecordingDomain(name)
        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(method, domain)

        dumpOnFailureDuringValidation(Some(classFile), Some(method), method.body.get, result) {
            f(domain)
        }
    }

    behavior of "the DefaultPerInstructionPostProcessing trait"

    it should "be able to analyze a method that always throws an exception" in {
        evaluateMethod("alwaysThrows") { domain =>
            import domain._
            allThrownExceptions should be(
                Map((8 -> Set(ObjectValue(0, No, true, ObjectType.RuntimeException))))
            )
        }
    }

    it should "be able to analyze a method that catches everything" in {
        evaluateMethod("alwaysCatch") { domain =>
            import domain._
            allReturnVoidInstructions should be(IntTrieSet(7)) // <= void return
        }
    }

    it should "be able to identify all potentially thrown exceptions when different exceptions are stored in a variable which is then passed to a throw statement" in {
        evaluateMethod("throwsThisOrThatException") { domain =>
            import domain._
            allThrownExceptions should be(
                Map(
                    (19 -> Set(ObjectValue(12, No, true, ObjectType("java/lang/IllegalArgumentException")))), // <= finally
                    (11 -> Set(ObjectValue(4, No, true, ObjectType.NullPointerException)))
                ) // <= if t is null
            )
        }
    }

    it should "be able to analyze a method that catches the thrown exceptions" in {
        evaluateMethod("throwsNoException") { domain =>
            import domain._
            allThrownExceptions should be(LongMap.empty)
            allReturnVoidInstructions should be(IntTrieSet(39)) // <= void return
        }
    }

    it should "be able to handle the pattern where some (checked) exceptions are caught and then rethrown as an unchecked exception" in {
        evaluateMethod("leverageException") { domain =>
            import domain._
            allReturnVoidInstructions should be(IntTrieSet(38)) // <= void return
            allThrownExceptions should be(LongMap.empty)
            // Due to the simplicity of the domain I(the exceptions of called methods are
            // not yet analyze) we cannot determine that the following exception
            // (among others?) may also be thrown:
            // ("throws", SomeReferenceValue(...,ObjectType("java/lang/RuntimeException"),No))
        }
    }

    it should "be able to analyze a method that always throws an exception but also swallows several exceptions" in {
        evaluateMethod("withFinallyAndThrows") { domain =>
            import domain._
            allThrownExceptions should be(
                Map(
                    (19, Set(ObjectValue(ImmediateVMExceptionsOriginOffset - 19, No, true, ObjectType.NullPointerException))),
                    (23, Set(
                        ObjectValue(-1, No, false, ObjectType.Throwable),
                        ObjectValue(ImmediateVMExceptionsOriginOffset - 11, No, true, ObjectType.NullPointerException)
                    )),
                    (25, Set(ObjectValue(ImmediateVMExceptionsOriginOffset - 25, No, true, ObjectType.NullPointerException)))
                )
            )
        }
    }
}
