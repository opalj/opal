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
import org.opalj.br.Method
import org.scalatest.FunSpec

/**
 * This integration test tests if the collected def/use information make sense.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RecordDefUseTest extends FunSpec with Matchers {

    class DefUseDomain[I](val method: Method, val project: Project[java.net.URL])
        extends CorrelationalDomain
        with TheProject
        with TheMethod
        with ProjectBasedClassHierarchy
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l1.DefaultReferenceValuesBinding
        with l1.NullPropertyRefinement
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelLongValuesShiftOperators
        with RecordDefUse

    describe("getting def/use information") {

        it("should be possible to calculate the def/use information for all methods of the JDK") {

            val comparisonCount = new java.util.concurrent.atomic.AtomicLong(0)
            val project = org.opalj.br.TestSupport.createJREProject

            info(s"the loaded project contains ${project.methodsCount} methods")
            val failures = new java.util.concurrent.atomic.AtomicInteger(0)

            project.parForeachMethodWithBody() { m ⇒
                val (_, classFile, method) = m
                val domain = new DefUseDomain(method, project)
                val body = method.body.get
                val r = BaseAI(classFile, method, domain)
                try {
                    r.operandsArray.zipWithIndex.foreach { opsPC ⇒
                        val (ops, pc) = opsPC
                        if ((ops ne null) &&
                            // Note:
                            // In case of handlers, the def/use information
                            // is slightly different when compared with the
                            // information recorded by the reference values domain.
                            !body.exceptionHandlers.exists(_.handlerPC == pc)) {
                            ops.zipWithIndex.foreach { opValueIndex ⇒

                                val (op, valueIndex) = opValueIndex
                                val domainOrigins = domain.origin(op).toSet

                                val defUseOrigins = try {
                                    domain.operandOrigin(pc, valueIndex)
                                } catch {
                                    case t: Throwable ⇒
                                        fail(s"${method.toJava(classFile)} no def/use information avaiable for pc=$pc and stack index=$valueIndex", t)
                                }
                                def haveSameOrigins: Boolean = {
                                    domainOrigins forall { o ⇒
                                        defUseOrigins.contains(o) ||
                                            defUseOrigins.exists(duo ⇒
                                                body.exceptionHandlers.exists(_.handlerPC == duo)
                                            )
                                    }
                                }
                                if (!haveSameOrigins) {
                                    val message =
                                        s"{pc=$pc: operands[$valueIndex] == domain: $domainOrigins vs defUse: $defUseOrigins}"
                                    fail(s"${method.toJava(classFile)}$message")
                                }
                                comparisonCount.incrementAndGet
                            }
                        }
                    }
                } catch {
                    case t: Throwable ⇒
                        t.printStackTrace()
                        failures.incrementAndGet()
                }
            }

            val baseMessage = s"compared origin information of ${comparisonCount.get} values"
            if (failures.get > 0)
                fail(failures.get + s" exceptions occured ($baseMessage)")
            else
                info(baseMessage)
        }
    }
}
