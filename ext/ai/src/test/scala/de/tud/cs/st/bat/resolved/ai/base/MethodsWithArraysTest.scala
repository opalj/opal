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
import domain.BaseRecordingDomain

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.ShouldMatchers

/**
 * Basic tests of the abstract interpreter related to handling arrays.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithArraysTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    import MethodsWithArraysTest._

    private def evaluateMethod(name: String, f: BaseRecordingDomain[String] ⇒ Unit) {
        val domain = new BaseRecordingDomain(name)

        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(classFile, method, domain)

        util.XHTML.dumpOnFailureDuringValidation(
            Some(classFile),
            Some(method),
            method.body.get,
            result) {
                f(domain)
            }
    }

    behavior of "the abstract interpreter"

    it should "be able to analyze a method that processes a byte array" in {
        evaluateMethod("byteArrays", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("ireturn", 15, newByteValue))
            )
        })
    }

    it should "be able to analyze a method that processes a boolean array" in {
        evaluateMethod("booleanArrays", domain ⇒ {
            import domain._
            domain.returnedValues should be(
                Set(("ireturn", 14, newBooleanValue))
            )
        })
    }
}
private object MethodsWithArraysTest {

    val classFiles = Java7Framework.ClassFiles(
        TestSupport.locateTestResources("classfiles/ai.jar", "ext/ai"))

    val classFile = classFiles.map(_._1).
        find(_.thisClass.className == "ai/MethodsWithArrays").get
}
