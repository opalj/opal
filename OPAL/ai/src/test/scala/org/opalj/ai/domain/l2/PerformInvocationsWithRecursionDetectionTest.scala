/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package domain
package l2

import scala.language.reflectiveCalls

import org.junit.runner.RunWith
import org.junit.Ignore
import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.opalj.util._
import br._
import br.analyses.{ SomeProject, Project }
import reader.Java8Framework.ClassFiles
import l1._
import org.opalj.ai.domain.l0.RecordMethodCallResults

/**
 * Tests that we can detect situations in which a method calls itself.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PerformInvocationsWithRecursionDetectionTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import PerformInvocationsWithRecursionDetectionTestFixture._

    behavior of "PerformInvocationsWithRecursionDetection"

    it should ("be able to analyze a simple static, recursive method") in {
        val domain = new InvocationDomain(project) {
            val calledMethodsStore = createCalledMethodsStore(project)
        }
        val result = BaseAI(StaticCalls, StaticCalls.findMethod("simpleRecursion").get, domain)
        result.domain.returnedNormally should be(true)
    }

    it should ("be able to analyze a method that is self-recursive and which will never abort") in {
        val theCalledMethodsStore = createCalledMethodsStore(project)
        val domain = new InvocationDomain(project) {
            val calledMethodsStore = theCalledMethodsStore
        }
        BaseAI(StaticCalls, StaticCalls.findMethod("endless").get, domain)
        if (domain.allReturnedValues.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
        if (domain.allThrownExceptions.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
    }

    it should ("be able to analyze a method that is self-recursive and which will never abort due to exception handling") in {
        val theCalledMethodsStore = createCalledMethodsStore(project)
        val domain = new InvocationDomain(project) {
            val calledMethodsStore = theCalledMethodsStore
        }
        BaseAI(StaticCalls, StaticCalls.findMethod("endlessDueToExceptionHandling").get, domain)
        if (domain.allReturnedValues.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
        if (domain.allThrownExceptions.nonEmpty)
            fail("the method never returns, but the following result was produced: "+
                domain.allReturnedValues)
    }

    it should ("be able to analyze some methods with mutual recursion") in {
        val theCalledMethodsStore = createCalledMethodsStore(project)
        val domain = new InvocationDomain(project) {
            val calledMethodsStore = theCalledMethodsStore
        }
        BaseAI(StaticCalls, StaticCalls.findMethod("mutualRecursionA").get, domain)

        domain.returnedNormally should be(true) // because we work at the type level at some point..
    }

    it should ("be able to analyze a static method that uses recursion to calculate the factorial of a small concrete number") in {
        val domain = new InvocationDomain(project) {
            val calledMethodsStore = createCalledMethodsStore(project)
        }
        BaseAI.perform(StaticCalls, StaticCalls.findMethod("fak").get, domain)(
            Some(IndexedSeq(domain.IntegerValue(-1, 3)))
        )
        domain.returnedNormally should be(true)
        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(6))
    }

    it should ("issue a warning if a method is called very often using different operands") in {
        val theCalledMethodsStore = createCalledMethodsStore(project)
        val domain = new InvocationDomain(project) { val calledMethodsStore = theCalledMethodsStore }
        BaseAI.perform(StaticCalls, StaticCalls.findMethod("fak").get, domain)(
            Some(IndexedSeq(domain.IntegerValue(-1, 11)))
        )
        if (!domain.returnedNormally) fail("domain didn't return normally")

        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(39916800))

        theCalledMethodsStore.warningIssued should be(true)
    }

}

object PerformInvocationsWithRecursionDetectionTestFixture {

    abstract class BaseDomain(override val project: Project[java.net.URL]) extends Domain
            with DefaultDomainValueBinding
            with TheProject[java.net.URL]
            with ThrowAllPotentialExceptionsConfiguration
            with l0.TypeLevelFieldAccessInstructions
            with l0.DefaultTypeLevelLongValues
            with l0.TypeLevelInvokeInstructions
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l1.DefaultReferenceValuesBinding
            with li.DefaultPreciseIntegerValues
            with ProjectBasedClassHierarchy
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization {
        override def maxUpdatesForIntegerValues: Long = Int.MaxValue.toLong * 2
    }

    def createCalledMethodsStore(theProject: Project[java.net.URL]): CalledMethodsStore { def warningIssued: Boolean } =
        new CalledMethodsStore(
            new BaseDomain(theProject) {
                type Id = String
                override def id = "Called Methods Store Domain"
            }) {
            var warningIssued = false
            override def frequentEvalution(
                definingClass: ClassFile,
                method: Method,
                operandsSet: List[domain.Operands]): Unit = {
                //super.frequentEvalution(definingClass, method, operandsSet)
                warningIssued = true
            }
        }

    abstract class InvocationDomain(project: Project[java.net.URL])
            extends BaseDomain(project)
            with PerformInvocationsWithRecursionDetection
            with RecordMethodCallResults {

        type Id = Project[java.net.URL]

        override def id = project

        /*ABSTRACT*/ val calledMethodsStore: CalledMethodsStore

        def invokeExecutionHandler(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            operands: Operands): InvokeExecutionHandler =
            new InvokeExecutionHandler {

                override val domain = new InvocationDomain(project) {
                    val calledMethodsStore: InvocationDomain.this.calledMethodsStore.type =
                        InvocationDomain.this.calledMethodsStore
                }

                def ai: AI[_ >: domain.type] = BaseAI
            }
    }

    val testClassFileName = "classfiles/performInvocations.jar"
    val testClassFile = TestSupport.locateTestResources(testClassFileName, "ai")
    val project = Project(testClassFile)
    val StaticCalls = project.classFile(ObjectType("performInvocations/StaticCalls")).get

}