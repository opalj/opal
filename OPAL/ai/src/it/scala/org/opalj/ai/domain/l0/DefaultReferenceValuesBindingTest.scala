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
package l0

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport
import org.opalj.ai.domain.ProjectBasedClassHierarchy
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.ValuesCoordinatingDomain

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
            with l0.DefaultPrimitiveTypeConversions
            with l0.DefaultReferenceValuesBinding
            with TheProject[java.net.URL]
            with ProjectBasedClassHierarchy {
        type Id = String
        def id = "Values Domain"

        def project: Project[java.net.URL] = TestSupport.JREProject
    }

    behavior of "instances of domains of type DomainReferenceValuesBinding"

    it should "be able to join a value with a single interface with one with multiple interfaces" in {
        // the operand stack value org.omg.CORBA.Object(origin=-1;maybeNull;isUpperBound) does not abstract over org.omg.CORBA.Object with java.rmi.Remote(origin=-1; isUpperBound)
        val t1 = ObjectType("org/omg/CORBA/Object")
        val t2 = ObjectType("java.rmi.Remote")
        val domain = ValuesDomain
        val stValue = domain.ReferenceValue(-1, t1)
        val mtValue = domain.ObjectValue(-1, UIDSet(t1, t2))

        stValue.abstractsOver(mtValue) should be(true)

    }

}
