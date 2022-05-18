/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import scala.language.reflectiveCalls

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport.biProject

/**
 * Tests that we can detect situations in which a method calls itself.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PerformInvocationsWithRecursionDetectionTest extends AnyFlatSpec with Matchers {

    import PerformInvocationsWithRecursionDetectionTestFixture._

    behavior of "PerformInvocationsWithRecursionDetection"

    it should ("be able to analyze a simple static, recursive method") in {
        val method = StaticCalls.findMethod("simpleRecursion").head
        val domain = new InvocationDomain(project, method)
        val result = BaseAI(method, domain)
        result.domain.returnedNormally should be(true)
    }

    it should ("be able to analyze a method that is self-recursive and which will never abort") in {
        val method = StaticCalls.findMethod("endless").head
        val domain = new InvocationDomain(project, method)
        BaseAI(method, domain)
        if (domain.allReturnedValues.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
        if (domain.allThrownExceptions.nonEmpty)
            fail("the method never returns, but the following exceptions were thrown: "+
                domain.allThrownExceptions)
    }

    it should ("be able to analyze a method that is self-recursive and which will never abort due to exception handling") in {
        val method = StaticCalls.findMethod("endlessDueToExceptionHandling").head
        val domain = new InvocationDomain(project, method)
        BaseAI(method, domain)
        if (domain.allReturnedValues.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
        if (domain.allThrownExceptions.nonEmpty)
            fail("the method never returns, but the following exceptions were thrown: "+
                domain.allThrownExceptions)
    }

    it should ("be able to analyze some methods with mutual recursion") in {
        val method = StaticCalls.findMethod("mutualRecursionA").head
        val domain = new InvocationDomain(project, method)
        BaseAI(method, domain)

        domain.returnedNormally should be(true) // because we work at the type level at some point..
    }

    it should ("be able to analyze a static method that uses recursion to calculate the factorial of a small concrete number") in {
        val method = StaticCalls.findMethod("fak").head
        val domain = new InvocationDomain(project, method)
        BaseAI.perform(method, domain)(Some(IndexedSeq(domain.IntegerValue(-1, 3))))
        domain.returnedNormally should be(true)
        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(6))
    }

    it should ("issue a warning if a method is called very often using different operands") in {
        val method = StaticCalls.findMethod("fak").head
        val domain = new InvocationDomain(project, method, 1)
        BaseAI.perform(method, domain)(Some(IndexedSeq(domain.IntegerValue(-1, 11))))
        val theCalledMethodsStore = domain.calledMethodsStore
        if (!domain.returnedNormally) fail("domain didn't return normally")

        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(39916800))

        theCalledMethodsStore.warningIssued should be(true)
    }

}

object PerformInvocationsWithRecursionDetectionTestFixture {

    abstract class BaseDomain(val project: Project[java.net.URL])
        extends CorrelationalDomain
        with ValuesDomain
        with DefaultSpecialDomainValuesBinding
        with TheProject
        with TypedValuesFactory
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelDynamicLoads
        with l1.DefaultReferenceValuesBinding
        with l1.DefaultIntegerRangeValues {
        domain: Configuration =>
    }

    abstract class SharedInvocationDomain(
            project:    Project[java.net.URL],
            val method: Method
    ) extends BaseDomain(project) with Domain
        with TheMethod
        with l0.TypeLevelInvokeInstructions
        with ThrowAllPotentialExceptionsConfiguration
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with IgnoreSynchronization
        //with DefaultHandlingOfMethodResults
        with l0.DefaultTypeLevelHandlingOfMethodResults
        with PerformInvocationsWithRecursionDetection
        with DefaultRecordMethodCallResults {

        override def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
            ExceptionsRaisedByCalledMethods.AllExplicitlyHandled
        }

        def shouldInvocationBePerformed(method: Method): Boolean = true

        type CalledMethodDomain = ChildInvocationDomain

        val coordinatingDomain = new BaseDomain(project) with ValuesCoordinatingDomain
    }

    class InvocationDomain(
            project:                            Project[java.net.URL],
            method:                             Method,
            val frequentEvaluationWarningLevel: Int                   = 10
    ) extends SharedInvocationDomain(project, method) {
        callingDomain =>

        lazy val calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type; def warningIssued: Boolean } = {
            val operands =
                mapOperands(
                    localsArray(0).foldLeft(List.empty[DomainValue])((l, n) =>
                        if (n ne null) n :: l else l),
                    coordinatingDomain
                )

            new CalledMethodsStore {
                implicit val logContext = project.logContext
                val domain: coordinatingDomain.type = callingDomain.coordinatingDomain
                val frequentEvaluationWarningLevel = callingDomain.frequentEvaluationWarningLevel
                val calledMethods = Map((method, List(operands)))

                var warningIssued = false

                override def frequentEvaluation(
                    method:      Method,
                    operandsSet: List[Array[domain.DomainValue]]
                ): Unit = {
                    //super.frequentEvalution(definingClass, method, operandsSet)
                    warningIssued = true
                }

            }
        }

        override def calledMethodDomain(method: Method) =
            new ChildInvocationDomain(project, method, this)

        def calledMethodAI = BaseAI

    }

    class ChildInvocationDomain(
            project:          Project[java.net.URL],
            method:           Method,
            val callerDomain: SharedInvocationDomain
    ) extends SharedInvocationDomain(project, method)
        with ChildPerformInvocationsWithRecursionDetection { callingDomain =>

        final def calledMethodAI: AI[_ >: CalledMethodDomain] = callerDomain.calledMethodAI

        def calledMethodDomain(method: Method) =
            new ChildInvocationDomain(project, method, callingDomain)

    }

    val project = biProject("ai.jar")
    val StaticCalls = project.classFile(ObjectType("ai/domain/StaticCalls")).get

}
