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
import org.junit.Ignore

import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import org.opalj.util._

import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Unit tests for handling `StringValues`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class StringValuesTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import PlainClassesTest._

    object AnalysisDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultTypeLevelLongValues
            with l0.TypeLevelFieldAccessInstructions
            with l0.TypeLevelInvokeInstructions
            with l1.DefaultStringValuesBinding
            with l1.DefaultIntegerRangeValues {

        type Id = String
        def id = "Domain which tracks String Values"
    }

    import AnalysisDomain._

    val s1 = StringValue(-1, "test")
    val s1Alt = StringValue(-1, "alt")
    val s2 = StringValue(-2, "test")
    val oN = ObjectValue(-1, No, true, ObjectType.String)
    val oU = ObjectValue(-1, Unknown, true, ObjectType.String)
    val joinS1AndS2 = MultipleReferenceValues(Set(s1, s2))

    behavior of "joining two StringValues"

    it should ("result in the original value if both values are the same") in {
        s1.join(5, s1) should be(NoUpdate)
    }

    it should ("result in a MultipleReferenceValue if both values have different origins") in {
        s1.join(5, s2) should be(StructuralUpdate(joinS1AndS2))
    }

    it should ("result in some object value if both values have the same origin but different values") in {
        s1.join(1, s1Alt) should be(StructuralUpdate(oN))
    }

    it should ("result in some object value if the original value is some object value") in {
        oN.join(2, s1) should be(NoUpdate)
    }

    it should ("result in some object value if the new value is some object value") in {
        s1.join(2, oN) should be(StructuralUpdate(oN))
    }

    behavior of "summarization of StringValues"

    it should ("result in the original value") in {
        s1.summarize(-1) should be(s1)
    }

    it should (" – if the values are stored in a MultipleReferenceValues– in an ObjectValue") in {
        joinS1AndS2.summarize(-1) should be(oN)
    }

}
