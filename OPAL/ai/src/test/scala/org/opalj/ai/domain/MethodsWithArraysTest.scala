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
import l0._

/**
 * Basic tests of the abstract interpreter related to handling arrays.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithArraysTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import MethodsWithArraysTest._

    class TestDomain
            extends Domain
            with DefaultDomainValueBinding
            with DefaultReferenceValuesBinding
            with DefaultTypeLevelIntegerValues
            with DefaultTypeLevelLongValues
            with DefaultTypeLevelFloatValues
            with DefaultTypeLevelDoubleValues
            with TypeLevelFieldAccessInstructions
            with SimpleTypeLevelInvokeInstructions
            with ThrowAllPotentialExceptionsConfiguration
            with IgnoreSynchronization
            with DefaultHandlingOfMethodResults
            with RecordLastReturnedValues
            with PredefinedClassHierarchy {

        type Id = String
        def id = "MethodsWithArraysTestDomain"
    }

    private def evaluateMethod(name: String, f: TestDomain ⇒ Unit) {
        val domain = new TestDomain

        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(classFile, method, domain)

        org.opalj.ai.debug.XHTML.dumpOnFailureDuringValidation(
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
            domain.allReturnedValues should be(
                Map((15 -> AByteValue))
            )
        })
    }

    it should "be able to analyze a method that processes a boolean array" in {
        evaluateMethod("booleanArrays", domain ⇒ {
            import domain._
            domain.allReturnedValues should be(
                Map((14 -> ABooleanValue))
            )
        })
    }

    it should "be able to analyze a method that uses the Java feature that arrays are covariant" in {
        evaluateMethod("covariantArrays", domain ⇒ {
            import domain._
            domain.allReturnedValues.size should be(1)
            domain.isValueSubtypeOf(
                domain.allReturnedValues(24), ObjectType.Object) should be(Yes)
        })
    }

    it should "be able to analyze a method that does various (complex) type casts related to arrays" in {
        evaluateMethod("integerArraysFrenzy", domain ⇒ {
            import domain._
            domain.allReturnedValues.size should be(2)
            domain.isValueSubtypeOf(
                domain.allReturnedValues(78), ArrayType(IntegerType)) should be(Yes)
            domain.isValueSubtypeOf(
                domain.allReturnedValues(76), ArrayType(ByteType)) should be(Yes)
        })
    }
}
private object MethodsWithArraysTest {

    val classFiles = ClassFiles(TestSupport.locateTestResources("classfiles/ai.jar", "ai"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithArrays").get
}
