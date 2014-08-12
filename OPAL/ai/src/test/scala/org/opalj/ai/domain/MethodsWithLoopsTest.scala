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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers

import org.opalj.bi.TestSupport.locateTestResources

import br._
import br.reader.Java8Framework.ClassFiles

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithLoopsTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import MethodsWithLoopsTest._

    def findMethod(name: String): Method = {
        classFile.methods.find(_.name == name).get
    }

    //    import domain.l0.BaseDomain
    //    private def evaluateMethod(name: String, f: BaseDomain ⇒ Unit) {
    //        val domain = new BaseDomain()
    //        val method = classFile.methods.find(_.name == name).get
    //        val result = BaseAI(classFile, method, domain)
    //
    //        org.opalj.ai.debug.XHTML.dumpOnFailureDuringValidation(
    //            Some(classFile),
    //            Some(method),
    //            method.body.get,
    //            result) {
    //                f(domain)
    //            }
    //    }

    behavior of "the abstract interpreter when analyzing methods with loops"

    import org.opalj.ai.domain.l0._

    //
    // RETURNS
    it should "be able to analyze a method that never terminates" in {

        object MostBasicDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultReferenceValuesBinding
            with l0.DefaultTypeLevelIntegerValues
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultPrimitiveValuesConversions
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization

        val method = findMethod("endless")
        val result = BaseAI(classFile, method, MostBasicDomain)
    }

}
object MethodsWithLoopsTest {

    val classFiles = ClassFiles(locateTestResources("classfiles/ai.jar", "ai"))

    val classFile = classFiles.map(_._1).
        find(_.thisType.fqn == "ai/MethodsWithLoops").get
}