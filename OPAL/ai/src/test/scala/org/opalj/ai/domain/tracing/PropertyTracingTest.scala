/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package tracing

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

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
class PropertyTracingTest extends FlatSpec with Matchers {

    import PropertyTracingTest._

    class AnalysisDomain(val method: Method)
        extends CorrelationalDomain
        with DefaultDomainValueBinding
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

    private def evaluateMethod(name: String)(f: AnalysisDomain ⇒ Unit): Unit = {
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
        evaluateMethod("notSanitized1") { domain ⇒
            domain.isSanitized() should be(false)
        }
    }

    it should "be able to correctly identify that the method notSanitized2 does not sanitize the value" in {
        evaluateMethod("notSanitized2") { domain ⇒
            domain.isSanitized() should be(false)
        }
    }

    it should "be able to correctly identify that the method sanitized1 does sanitize the value" in {
        evaluateMethod("sanitized1") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }
    it should "be able to correctly identify that the method sanitized2 does sanitize the value" in {
        evaluateMethod("sanitized2") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized3 does sanitize the value" in {
        evaluateMethod("sanitized3") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized4 does sanitize the value" in {
        evaluateMethod("sanitized4") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized5 does sanitize the value" in {
        evaluateMethod("sanitized5") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }

    it should "be able to correctly identify that the method sanitized6 does sanitize the value" in {
        evaluateMethod("sanitized6") { domain ⇒
            domain.isSanitized() should be(true)
        }
    }

    // We can not yet identify that the value is definitively sanitized.
    //    it should "be able to correctly identify that the method sanitized7 does sanitize the value" in {
    //        evaluateMethod("sanitized7", domain ⇒ {
    //            domain.isSanitized() should be(true)
    //        })
    //    }
}

private object PropertyTracingTest {

    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/domain/Sanitization").get
}
