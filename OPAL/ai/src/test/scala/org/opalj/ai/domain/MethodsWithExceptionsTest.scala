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

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._
import br.reader.Java8Framework.ClassFiles

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithExceptionsTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import debug.XHTML.dumpOnFailureDuringValidation
    import domain.l1
    import MethodsWithExceptionsTest._
    import org.opalj.collection.mutable.UShortSet

    private def evaluateMethod(name: String)(f: l1.DefaultRecordingDomain[String] ⇒ Unit) {
        val domain = new l1.DefaultRecordingDomain(name)
        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(classFile, method, domain)

        dumpOnFailureDuringValidation(Some(classFile), Some(method), method.body.get, result) {
            f(domain)
        }
    }

    behavior of "the abstract interpreter"

    it should "be able to analyze a method that always throws an exception" in {
        evaluateMethod("alwaysThrows") { domain ⇒
            import domain._
            allThrownExceptions should be(
                Map((8 -> Set(ObjectValue(0, No, true, ObjectType.RuntimeException))))
            )
        }
    }

    it should "be able to analyze a method that catches everything" in {
        evaluateMethod("alwaysCatch") { domain ⇒
            import domain._
            allReturnVoidInstructions should be(UShortSet(7)) // <= void return
        }
    }

    it should "be able to identify all potentially thrown exceptions when different exceptions are stored in a variable which is then passed to a throw statement" in {
        evaluateMethod("throwsThisOrThatException") { domain ⇒
            import domain._
            allThrownExceptions should be(
                Map((19 -> Set(ObjectValue(12, No, true, ObjectType("java/lang/IllegalArgumentException")))), // <= finally
                    (11 -> Set(ObjectValue(4, No, true, ObjectType.NullPointerException)))) // <= if t is null
            )
        }
    }

    it should "be able to analyze a method that catches the thrown exceptions" in {
        evaluateMethod("throwsNoException") { domain ⇒
            import domain._
            allThrownExceptions should be(Map.empty)
            allReturnVoidInstructions should be(UShortSet(39)) // <= void return
        }
    }

    it should "be able to handle the pattern where some (checked) exceptions are caught and then rethrown as an unchecked exception" in {
        evaluateMethod("leverageException") { domain ⇒
            import domain._
            allReturnVoidInstructions should be(UShortSet(38)) // <= void return
            allThrownExceptions should be(Map.empty)
            // Due to the simplicity of the domain (the exceptions of called methods are 
            // not yet analyze) we cannot determine that the following exception 
            // (among others?) may also be thrown:
            // ("throws", SomeReferenceValue(...,ObjectType("java/lang/RuntimeException"),No)) 
        }
    }

    it should "be able to analyze a method that always throws an exception (from the code/generated by the JVM)" in {
        evaluateMethod("withFinallyAndThrows") { domain ⇒
            import domain._
            allThrownExceptions should be(
                Map((23, Set(ObjectValue(-1, No, false, ObjectType.Throwable))),
                    (19, Set(ObjectValue(19, No, true, ObjectType.NullPointerException))),
                    (23, Set(
                        ObjectValue(-1, No, false, ObjectType.Throwable),
                        MultipleReferenceValues(Set(
                            ObjectValue(-1, No, false, ObjectType.Throwable),
                            ObjectValue(11, No, true, ObjectType.NullPointerException))))),
                    (25, Set(ObjectValue(25, No, true, ObjectType.NullPointerException)))
                )
            )
        }
    }

}
private object MethodsWithExceptionsTest {

    val classFiles = ClassFiles(TestSupport.locateTestResources("classfiles/ai.jar", "ai"))

    val classFile = classFiles.map(_._1).
        find(_.thisType.fqn == "ai/MethodsWithExceptions").get
}

