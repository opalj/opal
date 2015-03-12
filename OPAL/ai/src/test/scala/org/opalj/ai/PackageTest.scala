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

import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.opalj.ai.domain.ValuesCoordinatingDomain
import org.opalj.ai.domain.l0.DefaultReferenceValuesBinding
import org.opalj.ai.domain.l0.DefaultTypeLevelIntegerValues
import org.opalj.ai.domain.l0.DefaultTypeLevelLongValues
import org.opalj.ai.domain.l0.DefaultTypeLevelFloatValues
import org.opalj.ai.domain.l0.DefaultTypeLevelDoubleValues
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.PredefinedClassHierarchy

/**
 * Tests the utility methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PackageTest extends FlatSpec with Matchers with ParallelTestExecution {

    behavior of "the mapOperands method"

    it should ("be able to map an empty list of operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands = List.empty[DomainValue]

        mapOperands(operands, SimpleCoordinatingTypeLevelDomain) should be(empty)
    }

    it should ("be able to map a list with one operand") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: List[DomainValue] = List(IntegerValue(valueOrigin = -1))

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result.size should be(1)
        result.head should be(IntegerValue(valueOrigin = -1))
    }

    it should ("be able to map a list with two different operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: List[DomainValue] =
            List(
                IntegerValue(valueOrigin = -1),
                FloatValue(valueOrigin = -2)
            )

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(FloatValue(valueOrigin = -2))
    }

    it should ("be able to map a list with three different operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: List[DomainValue] =
            List(
                IntegerValue(valueOrigin = -1),
                FloatValue(valueOrigin = -2),
                DoubleValue(valueOrigin = -3)
            )

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(FloatValue(valueOrigin = -2))
        result(2) should be(DoubleValue(valueOrigin = -3))
    }

    it should ("be able to map a list with three different operands, where the two first operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val firstOperand = IntegerValue(valueOrigin = -1)
        val operands: List[DomainValue] =
            List(
                firstOperand,
                firstOperand,
                DoubleValue(valueOrigin = -3)
            )

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(IntegerValue(valueOrigin = -1))
        result(0) should be theSameInstanceAs (result(1))
        result(2) should be(DoubleValue(valueOrigin = -3))
    }

    it should ("be able to map a list with three different operands, where the two last operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val lastOperand = IntegerValue(valueOrigin = -2)
        val operands: List[DomainValue] =
            List(
                DoubleValue(valueOrigin = -1),
                lastOperand,
                lastOperand
            )

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(DoubleValue(valueOrigin = -1))
        result(1) should be(IntegerValue(valueOrigin = -2))
        result(2) should be(IntegerValue(valueOrigin = -2))
        result(1) should be theSameInstanceAs (result(2))
    }

    it should ("be able to map a list with three different operands, where all three operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operand = IntegerValue(valueOrigin = -1)
        val operands: List[DomainValue] = List(operand, operand, operand)

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(IntegerValue(valueOrigin = -1))
        result(2) should be(IntegerValue(valueOrigin = -1))
        result(0) should be theSameInstanceAs (result(1))
        result(1) should be theSameInstanceAs (result(2))
    }

}

object SimpleCoordinatingTypeLevelDomain
    extends ValuesCoordinatingDomain
    with DefaultDomainValueBinding
    with DefaultReferenceValuesBinding
    with DefaultTypeLevelIntegerValues
    with DefaultTypeLevelLongValues
    with DefaultTypeLevelFloatValues
    with DefaultTypeLevelDoubleValues
    with PredefinedClassHierarchy

