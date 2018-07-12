/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest.FlatSpec
import org.scalatest.Matchers
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
class DefaultReferenceValuesBindingTest extends FlatSpec with Matchers {

    private object ValuesDomain extends {
        final val project: Project[java.net.URL] = TestSupport.createJREProject
    } with ValuesCoordinatingDomain
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.DefaultReferenceValuesBinding
        with TheProject

    behavior of "instances of domains of type l0.DomainReferenceValuesBinding"

    it should "be able to join a value with a single interface with one with multiple interfaces" in {
        // the operand stack value org.omg.CORBA.Object(origin=-1;maybeNull;isUpperBound)
        // does not abstract over org.omg.CORBA.Object with java.rmi.Remote(origin=-1; isUpperBound)
        val t1 = ObjectType("org/omg/CORBA/Object")
        val t2 = ObjectType("java/rmi/Remote")
        val domain = ValuesDomain
        val stValue = domain.ReferenceValue(-1, t1)
        val mtValue = domain.ObjectValue(-1, UIDSet2(t1, t2))

        stValue.abstractsOver(mtValue) should be(true)

    }

}
