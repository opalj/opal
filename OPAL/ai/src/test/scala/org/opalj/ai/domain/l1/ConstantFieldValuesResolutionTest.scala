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
import org.opalj.br.analyses.Project

/**
 * Tests the resolution of ConstantFieldValues.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantFieldValuesResolutionTest extends AnyFunSpec with Matchers {

    class ConstantFieldValuesResolutionTestDomain(val project: Project[java.net.URL])
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with TheProject
        with ThrowAllPotentialExceptionsConfiguration
        with l1.DefaultIntegerRangeValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with RecordLastReturnedValues

    describe("Using ConstantFieldValuesResolution") {

        val testProject = org.opalj.br.TestSupport.biProject("ai.jar")
        val IntegerValues = testProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

        it("(Prerequisite) it should be possible to get the constant value of a field") {
            val theField = IntegerValues.fields.find(_.name == "theValue").get
            theField.constantFieldValue should be(Symbol("defined"))
            theField.constantFieldValue.get.toInt should be(42)
        }
    }
}
