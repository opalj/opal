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
package l1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.{ ObjectType, ArrayType, IntegerType }

/**
 * Tests the `ConstraintsBetweenIntegerValuesTest` Domain extension.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstraintsBetweenIntegerValuesTest extends FunSpec with Matchers with ParallelTestExecution {

    final val IrrelevantPC = Int.MinValue

    class IntegerRangesWithInterIntegerConstraintsTestDomain(
        override val maxCardinalityOfIntegerRanges: Long = -(Int.MinValue.toLong) + Int.MaxValue)
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultReferenceValuesBinding
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l1.DefaultIntegerRangeValues // <----- The one we are going to test
            with l1.ConstraintsBetweenIntegerValues // <----- The one we are going to test
            with l0.DefaultPrimitiveValuesConversions
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with PredefinedClassHierarchy
            with RecordLastReturnedValues

    describe("with constraint tracking between integer values") {

        val testJAR = "classfiles/ai.jar"
        val testFolder = org.opalj.bi.TestSupport.locateTestResources(testJAR, "ai")
        val testProject = org.opalj.br.analyses.Project(testFolder)
        val IntegerValues = testProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

        it("it should handle cases where we constrain and compare unknown values (without join)") {
            val domain = new IntegerRangesWithInterIntegerConstraintsTestDomain(4)
            val method = IntegerValues.findMethod("multipleConstraints1").get
            val result = BaseAI(IntegerValues, method, domain)

            result.operandsArray(29) should be(null)
        }

        it("it should handle cases where we constrain and compare unknown values (with join)") {
            val domain = new IntegerRangesWithInterIntegerConstraintsTestDomain(4)
            val method = IntegerValues.findMethod("multipleConstraints2").get
            val result = BaseAI(IntegerValues, method, domain)

            result.operandsArray(25) should be(null)
        }
    }

}
