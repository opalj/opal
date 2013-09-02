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
package de.tud.cs.st.bat
package resolved
package ai
package base

import reader.Java7Framework
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.ShouldMatchers
import de.tud.cs.st.bat.resolved.ai.domain.DoNothingOnReturnFromMethod

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
        with ShouldMatchers
        with ParallelTestExecution {

    import util.Util.dumpOnFailureDuringValidation

    class RecordingDomain
            extends domain.DefaultDomain[None.type] {
        val identifier = None

        var returnedValues: Set[(String, Value)] = Set()
        override def areturn(pc: Int, value: Value) { returnedValues += (("areturn", value)) }
        override def dreturn(pc: Int, value: Value) { returnedValues += (("dreturn", value)) }
        override def freturn(pc: Int, value: Value) { returnedValues += (("freturn", value)) }
        override def ireturn(pc: Int, value: Value) { returnedValues += (("ireturn", value)) }
        override def lreturn(pc: Int, value: Value) { returnedValues += (("lreturn", value)) }
        override def returnVoid(pc: Int) { returnedValues += (("return", null)) }
        override def abnormalReturn(pc: Int, exception: Value) {
            returnedValues += (("throws", exception))
        }
    }

    val classFiles = Java7Framework.ClassFiles(TestSupport.locateTestResources("classfiles/ai.jar", "ext/ai"))
    val classFile = classFiles.map(_._1).find(_.thisClass.className == "ai/MethodsWithExceptions").get

    private def evaluateMethod(name: String, f: RecordingDomain ⇒ Unit) {
        val domain = new RecordingDomain; import domain._
        val method = classFile.methods.find(_.name == name).get
        val result = AI(classFile, method, domain)

        dumpOnFailureDuringValidation(Some(classFile), Some(method), method.body.get, result) {
            f(domain)
        }
    }

    behavior of "the abstract interpreter"

    it should "be able to analyze a method that always throws an exception" in {
        evaluateMethod("alwaysThrows", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("throws", SomeReferenceValue(0, ObjectType.RuntimeException, No)))
            )
        })
    }

    it should "be able to analyze a method that catches everything" in {
        evaluateMethod("alwaysCatch", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("return", null),
                    ("throws", SomeReferenceValue(4, ObjectType.NullPointerException, No)),
                    ("throws", SomeReferenceValue(1, ObjectType.NullPointerException, No)))
            )
        })
    }

    it should "be able to analyze a method that may return normally or throw an exception" in {
        evaluateMethod("withFinallyAndThrows", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("return", null), // <= void return 
                    //("throws", SomeReferenceValue(1, ObjectType.NullPointerException, No))) // <= when the invoke fails... but we are not good enough
                    ("throws", SomeReferenceValue(4, ObjectType.Throwable, No))) // <= if t is null
            )
        })
    }
    //
    //    it should "be able to identify all potentially thrown exceptions" in {
    //        evaluateMethod("throwsThisOrThatException", domain ⇒ {
    //            import domain._
    //            domain.returnedValues should be(
    //                Set(("throws", AReferenceValue(ObjectType("java/lang/IllegalArgumentException"))), // <= finally
    //                    ("throws", AReferenceValue(ObjectType.NullPointerException))) // <= if t is null
    //            )
    //        })
    //    }

    it should "be able to identify all potentially thrown exceptions if an exception is caught and rethrown" in {
        evaluateMethod("leverageException", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("return", null)) // <= void return
            // Due to the simplicity of the domain we cannot determine that the following two exceptions may also be thrown:
            // ("throws", AReferenceValue(ObjectType("java/lang/RuntimeException"))) 
            // ("throws", AReferenceValue(ObjectType.NullPointerException))) 
            )
        })
    }

    //    it should "be able to analyze a method that catches the thrown exceptions" in {
    //        evaluateMethod("throwsNoException", domain ⇒ {
    //            import domain._
    //            domain.returnedValues should be(
    //                Set(("return", null),
    //                    ("throws", SomeReferenceValue) // <= the default domain is too simple to infer that we did catch all types of exceptions
    //                )
    //            )
    //        })
    //    }
}
