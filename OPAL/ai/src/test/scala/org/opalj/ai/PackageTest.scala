/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.ai.domain.ValuesCoordinatingDomain
import org.opalj.ai.domain.DefaultSpecialDomainValuesBinding
import org.opalj.ai.domain.PredefinedClassHierarchy
import org.opalj.ai.domain.l0.DefaultReferenceValuesBinding
import org.opalj.ai.domain.l0.DefaultTypeLevelIntegerValues
import org.opalj.ai.domain.l0.DefaultTypeLevelLongValues
import org.opalj.ai.domain.l0.DefaultTypeLevelFloatValues
import org.opalj.ai.domain.l0.DefaultTypeLevelDoubleValues
import org.opalj.ai.domain.l0.TypeLevelDynamicLoads

/**
 * Tests the utility methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PackageTest extends AnyFlatSpec with Matchers {

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
            IntegerValue(valueOrigin = -1) :: FloatValue(valueOrigin = -2) :: List.empty

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
                lastOperand,
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
    with DefaultSpecialDomainValuesBinding
    with DefaultReferenceValuesBinding
    with DefaultTypeLevelIntegerValues
    with DefaultTypeLevelLongValues
    with DefaultTypeLevelFloatValues
    with DefaultTypeLevelDoubleValues
    with TypeLevelDynamicLoads
    with PredefinedClassHierarchy
