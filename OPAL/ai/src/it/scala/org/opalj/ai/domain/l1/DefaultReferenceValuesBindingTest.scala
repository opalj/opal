/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet2
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.ValuesCoordinatingDomain

/**
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultReferenceValuesBindingTest extends AnyFlatSpec with Matchers {

    private object ValuesDomain extends ValuesCoordinatingDomain
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l1.DefaultReferenceValuesBinding
        with l0.TypeLevelDynamicLoads
        with TheProject {
        override final val project: Project[java.net.URL] = TestSupport.createJREProject()
    }

    behavior of "instances of domains of type DomainReferenceValuesBinding"

    it should "determine that a value with a single interface as its upper bound abstracts over "+
        "a value that implements multiple interfaces that includes the previous one" in {
            val t1 = ObjectType("org/omg/CORBA/Object")
            val t2 = ObjectType("java/rmi/Remote")
            val domain = ValuesDomain
            val stValue = domain.ReferenceValue(-1, t1)
            val mtValue = domain.ObjectValue(-1, UIDSet(t1, t2))

            stValue.abstractsOver(mtValue) should be(true)

        }

    it should "determine that a value with a single interface as its upper bound abstracts over "+
        "a value that is non-null and that implements multiple interfaces that includes the previous one" in {
            val t1 = ObjectType("org/omg/CORBA/Object")
            val t2 = ObjectType("java/rmi/Remote")
            val domain = ValuesDomain
            val stValue = domain.ReferenceValue(-1, Unknown, false, t1)
            val mtValue = domain.ObjectValue(-1, No, UIDSet(t1, t2))

            if (!stValue.abstractsOver(mtValue))
                fail(s"$stValue does not abstract over $mtValue (Result of the join was: ${stValue.join(-1, mtValue)})")

        }

    it should "correctly join a value which implements multiple interfaces with a value that implementes just one interface that is a subtype of one of the previous interfaces" in {
        val l1 = ObjectType("java/io/Serializable")
        val l2 = ObjectType("java/util/RandomAccess")
        val r = ObjectType("java/io/Externalizable")
        val domain = ValuesDomain
        val lValue = domain.ObjectValue(-1, No, UIDSet(l1, l2))
        val rValue = domain.ReferenceValue(-1, r)

        val expectedValue = domain.ReferenceValue(-1, l1)
        val joinedValue = lValue.join(-1, rValue).value
        if (joinedValue != expectedValue)
            fail(s"$lValue join $rValue was $joinedValue expected $expectedValue")
    }

    it should "calculate the correct least upper type bound if one of the types of a MultipleReferenceValues already defines that bound" in {
        val l = ObjectType("java/util/AbstractCollection")
        val r = ObjectType("java/util/ArrayList")
        val lValue = ValuesDomain.ObjectValue(-1, l)
        val rValue = ValuesDomain.ObjectValue(-2, r)
        val value = ValuesDomain.MultipleReferenceValues(UIDSet2(lValue, rValue))
        if (value.upperTypeBound.head != l)
            fail(s"unexpected upper type bound:${value.upperTypeBound} expected ${l.toJava}")
    }

}
