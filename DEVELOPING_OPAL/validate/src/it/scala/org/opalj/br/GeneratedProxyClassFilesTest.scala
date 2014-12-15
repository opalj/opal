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
package br

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l0.BaseDomain

/**
 * Checks that the ClassFileFactory produces valid proxy class files.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class GeneratedProxyClassFilesTest extends FunSpec with Matchers {

    describe("the generation of Proxy classes") {

        val testProject = Project(locateTestResources("classfiles", "br"))

        val proxies: Iterable[(ClassFile, java.net.URL)] = testProject.methods.map { m ⇒
            val t = testProject.classFile(m).thisType
            var proxy: ClassFile = null

            describe(s"generating a valid proxy for ${t.toJava} { ${m.toJava} }") {
                val definingType =
                    TypeDeclaration(
                        ObjectType("ProxyValidation$"+t.toJava+":"+m.toJava.replace(' ', '_')+"$"),
                        false,
                        Some(ObjectType.Object),
                        Set.empty
                    )
                val proxyMethodName = m.name+"$proxy"
                val invocationInstruction =
                    if (testProject.classFile(t).get.isInterfaceDeclaration) {
                        INVOKEINTERFACE.opcode
                    } else if (m.isStatic) {
                        INVOKESTATIC.opcode
                    } else if (m.isPrivate) {
                        INVOKESPECIAL.opcode
                    } else {
                        INVOKEVIRTUAL.opcode
                    }
                proxy =
                    ClassFileFactory.Proxy(
                        definingType,
                        proxyMethodName,
                        m.descriptor,
                        t,
                        m.name,
                        m.descriptor,
                        invocationInstruction)

                def verifyMethod(classFile: ClassFile, method: Method): Unit = {
                    val domain = new BaseDomain(testProject, classFile, method)
                    val result = BaseAI(classFile, method, domain)

                    // the abstract interpretation succeed
                    result should not be ('wasAborted)

                    // the method was non-empty
                    val instructions = method.body.get.instructions
                    instructions.count(_ != null) should be >= 2

                    // there is no dead-code
                    var nextPc = 0
                    while (nextPc < instructions.size) {
                        result.operandsArray(nextPc) should not be (null)
                        nextPc = instructions(nextPc).indexOfNextInstruction(nextPc, false)
                    }

                    // the layout of the instructions array is correct
                    for { pc ← 0 until instructions.size } {
                        if (instructions(pc) != null) {
                            val nextPc = instructions(pc).indexOfNextInstruction(pc, false)
                            instructions.slice(pc + 1, nextPc).foreach(_ should be(null))
                        }
                    }
                }

                val proxyMethod = proxy.findMethod(proxyMethodName).get
                it("should produce a correct forwarding method") {
                    verifyMethod(proxy, proxyMethod)
                }

                val constructor = proxy.findMethod("<init>").get
                it("should produce a correct constructor") {
                    verifyMethod(proxy, constructor)
                }

                val factoryMethod =
                    proxy.findMethod("$newInstance").getOrElse(
                        proxy.findMethod("$createInstance").get)
                it("should produce a correct factory method") {
                    verifyMethod(proxy, factoryMethod)
                }

            }
            proxy -> testProject.source(t).get
        }

        describe("the project should be extendable with the generated proxies") {
            val extendedProject = testProject.extend(proxies, Iterable.empty)
            it("should have the right amount of class files") {
                extendedProject.classFilesCount should be(
                    testProject.classFilesCount + proxies.size)
            }
            it("should have the right class files") {
                testProject.classFiles foreach { cf ⇒
                    extendedProject.classFile(cf.thisType) should be('defined)
                    if (testProject.source(cf.thisType).isDefined)
                        extendedProject.source(cf.thisType) should be('defined)
                }
                proxies foreach { p ⇒
                    val (proxy, _/* source*/) = p
                    extendedProject.classFile(proxy.thisType) should be('defined)
                    extendedProject.source(proxy.thisType) should be('defined)
                }
            }
        }
    }
}