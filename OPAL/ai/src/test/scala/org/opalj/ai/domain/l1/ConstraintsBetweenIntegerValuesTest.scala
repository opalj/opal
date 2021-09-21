/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.br.ObjectType

/**
 * Tests the `ConstraintsBetweenIntegerValuesTest` Domain extension.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstraintsBetweenIntegerValuesTest extends AnyFunSpec with Matchers {

    final val IrrelevantPC = Int.MinValue

    class IntegerRangesWithInterIntegerConstraintsTestDomain(
            override val maxCardinalityOfIntegerRanges: Long = -(Int.MinValue.toLong) + Int.MaxValue
    ) extends CorrelationalDomain
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultIntegerRangeValues // <----- The one we are going to test
        with l1.ConstraintsBetweenIntegerValues // <----- The one we are going to test
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with PredefinedClassHierarchy
        with RecordLastReturnedValues

    describe("with constraint tracking between integer values") {

        val testProject = org.opalj.br.TestSupport.biProject("ai.jar")
        val IntegerValues = testProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

        it("it should handle cases where we constrain and compare unknown values (without join)") {
            val domain = new IntegerRangesWithInterIntegerConstraintsTestDomain(4)
            val method = IntegerValues.findMethod("multipleConstraints1").head
            /*val result =*/ BaseAI(method, domain)

            // TODO result.operandsArray(29) should be(null)
        }

        it("it should handle cases where we constrain and compare unknown values (with join)") {
            val domain = new IntegerRangesWithInterIntegerConstraintsTestDomain(4)
            val method = IntegerValues.findMethod("multipleConstraints2").head
            /*val result =*/ BaseAI(method, domain)

            // TODO result.operandsArray(25) should be(null)
        }
    }

}
