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
import org.opalj.br.analyses.Project
import org.opalj.br.MethodWithBody
import org.opalj.br.Code
import org.scalatest.FunSpec

/**
 * This integration test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods to test if a domain that uses more
 * precise partial domains leads to a more precise result.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PrecisionOfDomainsTest extends FunSpec with Matchers {

    describe("a more precise domain") {
        it("should return a more precise result") {
            val project = org.opalj.br.TestSupport.JREProject
            // The following three domains are very basic domains that – given that the
            // same partial domains are used – should compute the same results.

            // We use this domain for the comparison of the values; it has the same
            // expressive power as the other domains.
            class TheValuesDomain(val project: Project[java.net.URL])
                extends ValuesCoordinatingDomain
                with l0.DefaultTypeLevelLongValues
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l1.DefaultReferenceValuesBinding
                with l1.DefaultIntegerRangeValues
                with l0.DefaultPrimitiveValuesConversions
                with ProjectBasedClassHierarchy
                with TheProject[java.net.URL]

            class TypeLevelDomain(val code: Code, val project: Project[java.net.URL])
                extends Domain
                with TheProject[java.net.URL]
                with TheCode
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
                with ProjectBasedClassHierarchy

            class L1Domain[I](val code: Code, val project: Project[java.net.URL])
                extends Domain
                with ThrowAllPotentialExceptionsConfiguration
                with DefaultHandlingOfMethodResults
                with IgnoreSynchronization
                with l1.DefaultReferenceValuesBinding
                with l1.DefaultIntegerRangeValues
                with l0.DefaultTypeLevelLongValues
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l0.DefaultPrimitiveValuesConversions
                with l0.TypeLevelInvokeInstructions
                with l0.TypeLevelFieldAccessInstructions
                with ProjectBasedClassHierarchy
                with TheProject[java.net.URL]
                with TheCode

            val ValuesDomain = new TheValuesDomain(project)

            def abstractsOver(r1: AIResult, r2: AIResult): Option[String] = {
                val codeSize = r1.operandsArray.length

                r1.operandsArray.corresponds(r2.operandsArray) { (lOperands, rOperands) ⇒
                    (lOperands == null && rOperands == null) ||
                        (lOperands != null && (rOperands == null ||
                            lOperands.corresponds(rOperands) { (lValue, rValue) ⇒
                                val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                if (!lVD.abstractsOver(rVD)) {
                                    return Some("the operand stack value "+lValue+
                                        " does not abstract over "+rValue+
                                        " the result of the join was: "+lVD.join(-1, rVD))
                                }
                                true
                            }
                        ))
                }
                None
            }

            val failed = new java.util.concurrent.atomic.AtomicBoolean(false)
            val comparisonCount = new java.util.concurrent.atomic.AtomicInteger(0)
            val jreClassFiles = project.classFiles
            val classFilesPerPackage = jreClassFiles.groupBy(cf ⇒ cf.thisType.packageName)
            val classFilesPerPackageSorted = classFilesPerPackage.toSeq.sortWith(
                (v1, v2) ⇒ v1._1 < v2._1
            )
            for ((packageName, classFiles) ← classFilesPerPackageSorted) {
                info("processing: "+(if (packageName.length == 0) "<DEFAULT>" else packageName))
                for {
                    classFile ← classFiles.par
                    method @ MethodWithBody(body) ← classFile.methods
                } {
                    val a1 = BaseAI
                    val r1 = a1(classFile, method, new TypeLevelDomain(body, project))
                    val a2 = BaseAI
                    val r2 = a2(classFile, method, new L1Domain(body, project))

                    abstractsOver(r1, r2).foreach { m ⇒
                        failed.set(true)
                        println(
                            classFile.thisType.toJava+"{ "+
                                method.toJava+"(Instructions "+method.body.get.instructions.size+")\n"+
                                "\t// the less precise domain did not abstract over the state of the more precise domain\n"+
                                "\t// "+Console.BOLD + m + Console.RESET+"\n"
                        )
                    }

                    comparisonCount.incrementAndGet()
                }
            }

            if (comparisonCount.get() < 2)
                fail("didn't find any class files/methods to analyze")
            if (failed.get()) {
                fail("the less precise domain did not abstract over the more precise domain")
            }
            info(
                "successfully compared the results of "+
                    comparisonCount.get+
                    " abstract interpretations")
        }
    }
}
