/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet2

/**
 * Unit tests for handling `StringValues`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class StringValuesTest extends AnyFlatSpec with Matchers {

    object AnalysisDomain
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with PredefinedClassHierarchy
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultStringValuesBinding
        with l1.DefaultIntegerRangeValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators

    import AnalysisDomain._

    val s1t0 = StringValue(-1, "test")
    val s1t1 = StringValue(-1, "test")
    val s1Alt = StringValue(-1, "alt")
    val s2 = StringValue(-2, "test")

    val oN = ObjectValue(-1, No, true, ObjectType.String)
    val oU = ObjectValue(-1, Unknown, true, ObjectType.String)

    val msS1t0AndS2 = MultipleReferenceValues(UIDSet2[DomainSingleOriginReferenceValue](s1t0, s2))
    assert(msS1t0AndS2.upperTypeBound == UIDSet(ObjectType.String))

    behavior of "joining two StringValues"

    it should ("result in a new instance if both values have the same properties but represent different instances") in {
        val joinResult = s1t0.join(5, s1t1)
        joinResult.value should not(be theSameInstanceAs s1t0)
        joinResult.value should be theSameInstanceAs (s1t1)
        joinResult should be(MetaInformationUpdate(s1t1))
    }

    it should ("result in some object value if both values have the same origin but different values") in {
        // we are now referring to all "Strings"..
        val joinResult = s1t0.join(1, s1Alt)
        joinResult.value should not(be theSameInstanceAs s1t0)
        joinResult.value should not(be theSameInstanceAs s1Alt)
        joinResult should be(StructuralUpdate(oN))
    }

    it should ("result in a new object value if the original value is some object value") in {
        val joinResult = oN.join(2, s1t0)
        joinResult.value should not(be theSameInstanceAs oN)
        joinResult should be(MetaInformationUpdate(oN))
    }

    it should ("result in some object value if the new value is some object value") in {
        s1t0.join(2, oN) should be(StructuralUpdate(oN))
    }

    it should ("result in a MultipleReferenceValue if both values have different origins") in {
        s1t0.join(5, s2) should be(StructuralUpdate(msS1t0AndS2))
    }

    behavior of "summarization of StringValues"

    it should ("result in the original value") in {
        s1t0.summarize(-1) should be(s1t0)
    }

    it should ("result – if the values are stored in a MultipleReferenceValues – in an ObjectValue") in {
        msS1t0AndS2.summarize(-1) should be(oN)
    }

}
