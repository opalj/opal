/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught
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
class PackageTest extends FlatSpec with Matchers {

    behavior of "the mapOperands method"

    it should ("be able to map an empty list of operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands = Chain.empty[DomainValue]

        mapOperands(operands, SimpleCoordinatingTypeLevelDomain) should be(empty)
    }

    it should ("be able to map a list with one operand") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: Chain[DomainValue] = Chain(IntegerValue(valueOrigin = -1))

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result.size should be(1)
        result.head should be(IntegerValue(valueOrigin = -1))
    }

    it should ("be able to map a list with two different operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: Chain[DomainValue] =
            IntegerValue(valueOrigin = -1) :&: FloatValue(valueOrigin = -2) :&: Naught

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(FloatValue(valueOrigin = -2))
    }

    it should ("be able to map a list with three different operands") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operands: Chain[DomainValue] =
            IntegerValue(valueOrigin = -1) :&:
                FloatValue(valueOrigin = -2) :&:
                DoubleValue(valueOrigin = -3) :&:
                Naught

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(FloatValue(valueOrigin = -2))
        result(2) should be(DoubleValue(valueOrigin = -3))
    }

    it should ("be able to map a list with three different operands, where the two first operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val firstOperand = IntegerValue(valueOrigin = -1)
        val operands: Chain[DomainValue] =
            firstOperand :&:
                firstOperand :&:
                DoubleValue(valueOrigin = -3) :&:
                Naught

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(IntegerValue(valueOrigin = -1))
        result(1) should be(IntegerValue(valueOrigin = -1))
        result(0) should be theSameInstanceAs (result(1))
        result(2) should be(DoubleValue(valueOrigin = -3))
    }

    it should ("be able to map a list with three different operands, where the two last operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val lastOperand = IntegerValue(valueOrigin = -2)
        val operands: Chain[DomainValue] =
            DoubleValue(valueOrigin = -1) :&:
                lastOperand :&:
                lastOperand :&:
                Naught

        val result = mapOperands(operands, SimpleCoordinatingTypeLevelDomain)
        result(0) should be(DoubleValue(valueOrigin = -1))
        result(1) should be(IntegerValue(valueOrigin = -2))
        result(2) should be(IntegerValue(valueOrigin = -2))
        result(1) should be theSameInstanceAs (result(2))
    }

    it should ("be able to map a list with three different operands, where all three operands are identical") in {
        import SimpleCoordinatingTypeLevelDomain._
        val operand = IntegerValue(valueOrigin = -1)
        val operands: Chain[DomainValue] = operand :&: operand :&: operand :&: Naught

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
