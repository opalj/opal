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
package domain

import reader.Java7Framework

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.ShouldMatchers

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithBranchesTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    import MethodsWithBranchesTest._

    import domain.RecordConstraints
    import domain.l0.BaseRecordingDomain

    type TestDomain = BaseRecordingDomain[String] with RecordConstraints[String]

    private def evaluateMethod(name: String)(f: TestDomain ⇒ Unit) {
        val domain = new BaseRecordingDomain(name) with RecordConstraints[String]
        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(classFile, method, domain)

        util.XHTML.dumpOnFailureDuringValidation(
            Some(classFile),
            Some(method),
            method.body.get,
            result) { f(domain) }
    }

    behavior of "the abstract interpreter"

    //
    // RETURNS
    it should "be able to analyze a method that performs a comparison with \"nonnull\"" in {
        evaluateMethod("nullComp") { domain ⇒
            //    0  aload_0 [o]
            //    1  ifnonnull 6
            //    4  iconst_1
            //    5  ireturn
            //    6  iconst_0
            //    7  ireturn 
            import domain._
            domain.allReturnedValues should be(
                Map((5 -> AnIntegerValue), (7 -> AnIntegerValue)))

            domain.allConstraints exists { constraint ⇒
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 4 &&
                    domain.isValueSubtypeOf(value, ObjectType.Object).yes &&
                    kind == "is null"
            } should be(true)

            domain.allConstraints exists { constraint ⇒
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 6 &&
                    domain.isValueSubtypeOf(value, ObjectType.Object).yes &&
                    kind == "is not null"
            } should be(true)
        }
    }

    it should "be able to analyze a method that performs a comparison with \"null\"" in {
        evaluateMethod("nonnullComp") { domain ⇒
            //    0  aload_0 [o]
            //    1  ifnull 6
            //    4  iconst_1
            //    5  ireturn
            //    6  iconst_0
            //    7  ireturn
            import domain._
            domain.allReturnedValues should be(
                Map((5 -> AnIntegerValue), (7 -> AnIntegerValue)))

            domain.allConstraints exists { constraint ⇒
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 6 &&
                    domain.isValueSubtypeOf(value, ObjectType.Object).yes &&
                    kind == "is null"
            } should be(true)

            domain.allConstraints exists { constraint ⇒
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 4 &&
                    domain.isValueSubtypeOf(value, ObjectType.Object).yes &&
                    kind == "is not null"
            } should be(true)
        }
    }

    it should "be able to analyze methods that perform multiple comparisons" in {
        evaluateMethod("multipleComp") { domain ⇒
            //     0  aload_0 [a]
            //     1  ifnull 17
            //     4  aload_1 [b]
            //     5  ifnull 17
            //     8  aload_0 [a]
            //     9  aload_1 [b]
            //    10  if_acmpne 15
            //    13  iconst_1
            //    14  ireturn
            //    15  iconst_0
            //    16  ireturn
            //    17  iconst_0
            //    18  ireturn
            import domain._
            allReturnedValues should be(Map(
                (14 -> AnIntegerValue),
                (16 -> AnIntegerValue),
                (18 -> AnIntegerValue)
            ))
        }
    }
}
private object MethodsWithBranchesTest {

    val classFiles = Java7Framework.ClassFiles(
        TestSupport.locateTestResources("classfiles/ai.jar", "ext/ai"))

    val classFile = classFiles.map(_._1).
        find(_.thisType.fqn == "ai/MethodsWithBranches").get
}