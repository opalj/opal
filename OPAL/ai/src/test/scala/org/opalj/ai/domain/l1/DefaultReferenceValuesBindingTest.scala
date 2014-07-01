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
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport
import org.opalj.util.{ No, Unknown }

/**
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultReferenceValuesBindingTest extends FlatSpec with Matchers {

    private object ValuesDomain
            extends ValuesCoordinatingDomain
            with l0.DefaultTypeLevelIntegerValues
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l1.DefaultReferenceValuesBinding
            with TheProject[java.net.URL]
            with ProjectBasedClassHierarchy {
        type Id = String
        def id = "Values Domain"

        def project: Project[java.net.URL] = TestSupport.JREProject
    }

    behavior of "instances of domains of type DomainReferenceValuesBinding"

    it should "be able to determine that a value with a single interface as its upper bound abstracts over a value that implements multiple interfaces that includes the previous one" in {
        val t1 = ObjectType("org/omg/CORBA/Object")
        val t2 = ObjectType("java.rmi.Remote")
        val domain = ValuesDomain
        val stValue = domain.ReferenceValue(-1, t1)
        val mtValue = domain.ObjectValue(-1, UIDSet(t1, t2))

        stValue.abstractsOver(mtValue) should be(true)

    }

    it should "be able to determine that a value with a single interface as its upper bound abstracts over a value that is non-null and that implements multiple interfaces that includes the previous one" in {
        val t1 = ObjectType("org/omg/CORBA/Object")
        val t2 = ObjectType("java.rmi.Remote")
        val domain = ValuesDomain
        val stValue = domain.ReferenceValue(-1, Unknown, false, t1)
        val mtValue = domain.ObjectValue(-1, No, UIDSet(t1, t2))

        if (!stValue.abstractsOver(mtValue))
            fail(stValue+" does not abstract over "+mtValue+" (Result of the join was: "+stValue.join(-1, mtValue)+")")

    }

    it should "be able to join a value which implements multiple interfaces with a value that implementes just one interface that is a subtype of one of the previous interfaces" in {
        val l1 = ObjectType("com/sun/org/apache/xml/internal/utils/PrefixResolver")
        val l2 = ObjectType("org/w3c/dom/xpath/XPathNSResolver")
        val r = ObjectType("com/sun/org/apache/xpath/internal/domapi/XPathEvaluatorImpl$DummyPrefixResolver")
        val domain = ValuesDomain
        val lValue = domain.ObjectValue(-1, No, UIDSet(l1, l2))
        val rValue = domain.ReferenceValue(-1, r)

        val expectedValue = domain.ReferenceValue(-1, l1)
        val joinedValue = lValue.join(-1, rValue).value
        if (joinedValue != expectedValue)
            fail(lValue+" join "+rValue+" was "+joinedValue+" expected "+expectedValue)
    }

    it should "be able to calculate the correct least upper type bound if one the types of a MultipleReferenceValues already defines that bound" in {
        val l = ObjectType("javafx/embed/swt/FXCanvas")
        val r = ObjectType("org/eclipse/swt/widgets/Composite")
        val lValue = ValuesDomain.ObjectValue(-1, l)
        val rValue = ValuesDomain.ObjectValue(-2, r)
        val value = ValuesDomain.MultipleReferenceValues(scala.collection.SortedSet(lValue, rValue))
        if (value.upperTypeBound.first != r)
            fail("unexpected upper type bound:"+value.upperTypeBound+" expected org.eclipse.swt.widgets.Composite")
    }

}
