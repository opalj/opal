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
import org.scalatest.Matchers

import org.opalj.br.Code
import org.opalj.br.MethodWithBody

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods using different domain configurations.
 *
 * This test suite has the following goals:
 *  - Test if seemingly independent (partial-) domain implementations are really
 *    independent by using different mixin-composition orders and comparing the
 *    results.
 *  - Test if several different domain configurations are actually working.
 *  - (Test if we can load and process a large number of different classes
 *    without exceptions.)
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DomainIndependenceTest extends FlatSpec with Matchers {

    // We use this domain for the comparison of the values; it has the same
    // expressive power as the other domains.
    private object ValuesDomain
        extends ValuesCoordinatingDomain
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultPrimitiveValuesConversions
        with l0.DefaultReferenceValuesBinding
        with PredefinedClassHierarchy

    //
    // The following three domains are very basic domains that – given that the
    // same partial domains are used – should compute the same results.
    // 

    private class Domain1(val code: Code)
        extends Domain
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultReferenceValuesBinding
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultPrimitiveValuesConversions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with PredefinedClassHierarchy
        with TheCode

    private class Domain2(val code: Code)
        extends Domain
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with ThrowAllPotentialExceptionsConfiguration
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with PredefinedClassHierarchy
        with DefaultDomainValueBinding
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultReferenceValuesBinding
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelLongValues
        with TheCode
        with l0.DefaultPrimitiveValuesConversions

    private class Domain3(val code: Code)
        extends Domain
        with l0.DefaultPrimitiveValuesConversions
        with l0.DefaultReferenceValuesBinding
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with PredefinedClassHierarchy
        with IgnoreSynchronization
        with DefaultHandlingOfMethodResults
        with ThrowAllPotentialExceptionsConfiguration
        with TheCode

    behavior of "a final domain composed of \"independent\" partial domains"

    it should "always calculate the same result" in {

        def corresponds(r1: AIResult, r2: AIResult): Option[String] = {
            val codeSize = r1.operandsArray.length

            r1.operandsArray.corresponds(r2.operandsArray) { (lOperands, rOperands) ⇒
                (lOperands == null && rOperands == null) ||
                    (lOperands != null && rOperands != null &&
                        lOperands.corresponds(rOperands) { (lValue, rValue) ⇒
                            val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            if (!(lVD.abstractsOver(rVD) && rVD.abstractsOver(lVD)))
                                return Some(Console.RED_B+"the operand stack value "+lVD+" and "+rVD+" do not correspond ")
                            else
                                true
                        }
                    )
            }

            r1.localsArray.corresponds(r2.localsArray) { (lLocals, rLocals) ⇒
                (lLocals == null && rLocals == null) ||
                    (lLocals != null && rLocals != null &&
                        lLocals.corresponds(rLocals) { (lValue, rValue) ⇒
                            (lValue == null && rValue == null) || (
                                lValue != null && rValue != null && {
                                    val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                    val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                    if (!(lVD.abstractsOver(rVD) && rVD.abstractsOver(lVD)))
                                        return Some(Console.YELLOW_B+"the register value "+lVD+" does not correspond with "+rVD)
                                    else
                                        true
                                }
                            )
                        }
                    )
            }

            None
        }

        val failed = new java.util.concurrent.atomic.AtomicInteger(0)
        val aiCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val comparisonCount = new java.util.concurrent.atomic.AtomicInteger(0)

        for {
            (classFile, source) ← org.opalj.br.TestSupport.readJREClassFiles().par
            method @ MethodWithBody(body) ← classFile.methods
        } {
            def TheAI() = new InstructionCountBoundedAI[Domain](body)

            val a1 = TheAI()
            val r1 = a1(classFile, method, new Domain1(body))
            aiCount.incrementAndGet()
            val a2 = TheAI()
            val r2 = a2(classFile, method, new Domain2(body))
            aiCount.incrementAndGet()
            val a3 = TheAI()
            val r3 = a3(classFile, method, new Domain3(body))
            aiCount.incrementAndGet()

            def abort(ai: InstructionCountBoundedAI[_], r: AIResult) {
                fail("the abstract interpretation of "+
                    classFile.thisType.toJava+
                    "{ "+method.toJava+" } was aborted after evaluating "+
                    ai.currentEvaluationCount+" instructions.\n "+
                    r.stateToString)
            }

            if (r1.wasAborted) abort(a1, r1)
            if (r2.wasAborted) abort(a2, r1)
            if (r3.wasAborted) abort(a3, r1)

            corresponds(r1, r2).foreach { m ⇒
                failed.incrementAndGet()
                // let's test if r1 is stable....
                val a1_2 = TheAI()
                val r1_2 = a1_2(classFile, method, new Domain1(body))
                if (corresponds(r1, r1_2).nonEmpty) {
                    failed.incrementAndGet()
                    println(
                        classFile.thisType.toJava+"{ "+
                            method.toJava+"(Instructions "+method.body.get.instructions.size+")} \n"+
                            Console.BLUE+"\t// the domain r1 is not deterministic (concurrency bug?)\n"+
                            Console.RESET
                    )
                } else
                    println(
                        classFile.thisType.toJava+"{ "+
                            method.toJava+"(Instructions "+method.body.get.instructions.size+")} \n"+
                            "\t// the results of r1 and r2 do not correspond\n"+
                            "\t// "+Console.BOLD + m + Console.RESET+"\n"
                    )
                comparisonCount.incrementAndGet()
            }
            comparisonCount.incrementAndGet()

            corresponds(r2, r3).foreach { m ⇒
                failed.incrementAndGet()
                println(
                    classFile.thisType.toJava+"{ "+
                        method.toJava+"(Instructions "+method.body.get.instructions.size+")} \n"+
                        "\t// the results of r2 and r3 do not correspond\n"+
                        "\t// "+Console.BOLD + m + Console.RESET+"\n"
                )
            }
            comparisonCount.incrementAndGet()
        }

        if (comparisonCount.get() < 2)
            fail("did not find any class files/method to analyze")
        if (failed.get() > 0) {
            fail("the domains computed different results in "+
                failed.get()+" cases out of "+comparisonCount.get)
        }
        info(
            s"successfully compared (${comparisonCount.get} comparisons) the results of "+
                s" ${aiCount.get} abstract interpretations")
    }
}
