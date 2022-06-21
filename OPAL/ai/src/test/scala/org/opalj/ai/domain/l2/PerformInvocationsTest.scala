/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport.biProject

/**
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PerformInvocationsTest extends AnyFlatSpec with Matchers {

    import PerformInvocationsTestFixture._

    behavior of "PerformInvocations"

    // This primarily tests that mixing in the PerformInvocations trait does
    // not cause any immediate harm.
    it should ("be able to analyze a simple static method that does nothing") in {
        val method = StaticCalls.findMethod("doNothing").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        result.domain.returnedNormally should be(true)
    }

    // This primarily tests that mixing in the PerformInvocations trait does
    // not cause any immediate harm.
    it should ("be able to analyze a simple static method that always throws an exception") in {
        val method = StaticCalls.findMethod("throwException").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(false)

        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 1) fail(exs.mkString("expected one exception: ", ", ", "."))
        exs forall {
            case domain.SObjectValueLike(ObjectType("java/lang/UnsupportedOperationException")) => true
            case _ => false
        } should be(true)
    }

    it should ("be able to analyze a static method that calls another static method that my fail") in {
        val method = StaticCalls.findMethod("mayFail").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(true)

        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 1) fail(exs.mkString("expected one exception: ", ", ", "."))
        exs forall {
            case domain.SObjectValueLike(ObjectType("java/lang/UnsupportedOperationException")) => true
            case _ => false
        } should be(true)
    }

    it should ("be able to analyze a static method that calls another static method") in {
        val method = StaticCalls.findMethod("performCalculation").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.thrownExceptions(result.domain, -1).size should be(0)
    }

    it should ("be able to analyze a static method that calls multiple other static methods") in {
        val method = StaticCalls.findMethod("doStuff").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.thrownExceptions(result.domain, -1).size should be(0)
    }

    it should ("be able to analyze a static method that processes the results of other static methods") in {
        val method = StaticCalls.findMethod("callComplexMult").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        /*val result =*/ BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(110))
    }

    it should ("be able to analyze a static method that throws different exceptions using the same throws statement") in {
        val method = StaticCalls.findMethod("throwMultipleExceptions").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(false)
        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 4) fail("too many exceptions: "+exs)
        var foundUnknownError = false
        var foundUnsupportedOperationException = false
        var foundNullPointerException = false
        var foundIllegalArgumentException = false

        exs forall { ex =>
            ex match {
                case domain.SObjectValueLike(ObjectType("java/lang/UnsupportedOperationException")) =>
                    foundUnsupportedOperationException = true
                    true
                case domain.SObjectValueLike(ObjectType.NullPointerException) =>
                    foundNullPointerException = true
                    true
                case domain.SObjectValueLike(ObjectType("java/lang/UnknownError")) =>
                    foundUnknownError = true
                    true
                case domain.SObjectValueLike(ObjectType("java/lang/IllegalArgumentException")) =>
                    foundIllegalArgumentException = true
                    true
                case _ =>
                    fail("unexpected exception: "+ex)
            }
        } should be(true)
        if (!(foundUnknownError &&
            foundUnsupportedOperationException &&
            foundIllegalArgumentException &&
            foundNullPointerException)) fail("Not all expected exceptions were thrown")
    }

    it should ("be able to analyze a static method that calls another static method that calls ...") in {
        val method = StaticCalls.findMethod("aLongerCallChain").head
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(true)
        val exs = domain.thrownExceptions(result.domain, -1)
        exs.size should be(0)

        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(175))
    }

    it should ("be able to analyze a method that analyzes the correlation between values") in {
        val method = StaticCalls.findMethod("callAreEqual").head
        val domain = new L1InvocationDomain(PerformInvocationsTestFixture.project, method)
        /*val result =*/ BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions.size should be(2) // the ArithmeticExceptions due to "%"

        domain.allReturnedValues.size should be(2)
        if (!domain.allReturnedValues.forall {
            e => domain.intValueOption(e._2).map(_ == 1).getOrElse(false)
        }) fail("unexpected result: "+domain.allReturnedValues)
    }

    it should ("be able to identify the situation where a passed value is returned as is") in {
        val method = StaticCalls.findMethod("uselessReferenceTest").head
        val domain = new L1InvocationDomain(PerformInvocationsTestFixture.project, method)
        /*val result =*/ BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions.size should be(0)

        domain.allReturnedValues.size should be(1)
        domain.allReturnedValues.head should be((17, domain.IntegerRange(1)))
    }

    it should ("be able to return the information that a parameter or some other value is returned") in {
        val method = StaticCalls.findMethod("advancedUselessReferenceTest").head
        val domain = new L1InvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions.size should be(0)

        domain.allReturnedValues.size should be(2)
        assert(
            domain.allReturnedValues(21) == domain.NullValue(20)
        )
        assert(
            domain.allReturnedValues(19) == domain.InitializedObjectValue(0, ObjectType.Object)
        )
        println(result)
    }

}

object PerformInvocationsTestFixture {

    trait L1Domain
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with TheProject
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l1.DefaultReferenceValuesBinding
        with l1.DefaultIntegerRangeValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with TheMethod

    trait LiDomain
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with TheProject
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l1.DefaultReferenceValuesBinding
        with l1.DefaultIntegerRangeValues
        with l1.DefaultLongSetValues
        with l1.ConcretePrimitiveValuesConversions
        with l1.LongValuesShiftOperators
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with TheMethod

    abstract class InvocationDomain(
            val project: Project[java.net.URL],
            val method:  Method
    ) extends Domain
        with l0.TypeLevelInvokeInstructions
        with PerformInvocations
        with ThrowAllPotentialExceptionsConfiguration
        with IgnoreSynchronization
        with l0.DefaultTypeLevelHandlingOfMethodResults
        with DefaultRecordMethodCallResults {
        domain: ValuesFactory with Configuration with TheProject with TheMethod =>

        override def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
            ExceptionsRaisedByCalledMethods.AllExplicitlyHandled
        }

        override def throwIllegalMonitorStateException: Boolean = false

        def isRecursive(
            definingClass: ClassFile,
            method:        Method,
            operands:      Operands
        ): Boolean = false

        def shouldInvocationBePerformed(method: Method): Boolean = true

        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method:  Method
        ): InvocationDomain

        override val useExceptionsThrownByCalledMethod = true

        type CalledMethodDomain = Domain with MethodCallResults

        def calledMethodDomain(method: Method): Domain with MethodCallResults =
            createInvocationDomain(project, method)

        def calledMethodAI = BaseAI

    }

    class LiInvocationDomain(
            project: Project[java.net.URL],
            method:  Method
    ) extends InvocationDomain(project, method) with LiDomain {

        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method:  Method
        ): InvocationDomain = new LiInvocationDomain(project, method)
    }

    class L1InvocationDomain(project: Project[java.net.URL], method: Method)
        extends InvocationDomain(project, method) with L1Domain {

        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method:  Method
        ): InvocationDomain = new L1InvocationDomain(project, method)
    }

    val project = biProject("ai.jar")
    val StaticCalls = project.classFile(ObjectType("ai/domain/StaticCalls")).get

}
