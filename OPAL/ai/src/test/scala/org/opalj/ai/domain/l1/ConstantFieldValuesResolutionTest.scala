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
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project

/**
 * Tests the resolution of ConstantFieldValues.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantFieldValuesResolutionTest
        extends FunSpec
        with Matchers
        with ParallelTestExecution {

    class ConstantFieldValuesResolutionTestDomain(val project: Project[java.net.URL])
        extends Domain
        with DefaultDomainValueBinding
        with TheProject[java.net.URL]
        with ProjectBasedClassHierarchy
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.DefaultPrimitiveValuesConversions
        with l1.DefaultIntegerRangeValues
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with RecordLastReturnedValues

    describe("Using ConstantFieldValuesResolution") {

        val testJAR = "classfiles/ai.jar"
        val testFolder = org.opalj.bi.TestSupport.locateTestResources(testJAR, "ai")
        val testProject = org.opalj.br.analyses.Project(testFolder)
        val IntegerValues = testProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

        it("(Prerequisite) it should be possible to get the constant value of a field") {
            val theField = IntegerValues.fields.find(_.name == "theValue").get
            theField.constantFieldValue should be('defined)
            theField.constantFieldValue.get.toInt should be(42)
        }
    }
}
