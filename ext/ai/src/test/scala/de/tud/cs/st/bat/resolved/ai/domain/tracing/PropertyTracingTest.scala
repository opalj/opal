/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package tracing

import reader.Java7Framework
import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL

/**
 * This test(suite) basically tests BATAIs support for tracing a property.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyTracingTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    import util.XHTML.dumpOnFailureDuringValidation
    import PropertyTracingTest._

    class AnalysisDomain(val identifier: String)
            extends l1.PreciseDomain[String]
            with RecordReturnValues[String]
            with IgnoreSynchronization
            with SimpleBooleanPropertyTracing[String] {

        override def propertyName = "isSanitized"

        override def invokestatic(
            pc: Int,
            declaringClass: ObjectType,
            name: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]): MethodCallResult = {

            // let's check if the first parameter (_ == -2) passed to a method is 
            // passed to a method called sanitize...
            if (name == "sanitize" && origin(operands.head).exists(_ == -2)) {
                updateProperty(pc, true)
            }
            super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
        }

        def isSanitized(): Boolean = hasPropertyOnExit(allReturnedValues)
    }

    private def evaluateMethod(name: String)(f: AnalysisDomain ⇒ Unit) {
        /**
         * In this case we want to make sure that a specific value (given as a
         * parameter to a method) is always sanitized (within the method.) I.e.,
         * that the value is passed to a function called sanitizer.
         */
        val method = classFile.methods.find(_.name == name).get
        val domain = new AnalysisDomain(name)
        val result = BaseTracingAI(classFile, method, domain)
        dumpOnFailureDuringValidation(
            Some(classFile),
            Some(method),
            method.body.get,
            result) {
                f(domain)
            }
    }

    behavior of "an abstract interpreter that enables the tracing of control-flow dependent properties"

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

    val classFiles = Java7Framework.ClassFiles(
        TestSupport.locateTestResources("classfiles/ai.jar", "ext/ai"))

    val classFile = classFiles.map(_._1).
        find(_.thisType.fqn == "ai/domain/Sanitization").get
}