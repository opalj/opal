/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package instructions

import java.net.URL

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.TestSupport.biProject
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

        val testProject = biProject("proxy.jar")

        val proxies: Seq[(ClassFile, URL)] = testProject.allMethods.map { m ⇒
            val classFile = m.classFile
            val t = classFile.thisType
            var proxy: ClassFile = null

            describe(s"generating a valid proxy for ${m.toJava}") {
                val typeName = "ProxyValidation$"+t.fqn+":"+m.name + m.descriptor.toJVMDescriptor+"$"
                val definingType =
                    TypeDeclaration(
                        ObjectType(typeName),
                        false,
                        Some(ObjectType.Object),
                        UIDSet.empty
                    )
                val proxyMethodName = m.name + '$'+"proxy"
                val tIsInterface = testProject.classFile(t).get.isInterfaceDeclaration
                val (invocationInstruction: Opcode, methodHandle: MethodCallMethodHandle) =
                    if (tIsInterface) {
                        (
                            INVOKEINTERFACE.opcode,
                            InvokeInterfaceMethodHandle(
                                t,
                                m.name,
                                m.descriptor
                            )
                        )
                    } else if (m.isStatic) {
                        (
                            INVOKESTATIC.opcode,
                            InvokeStaticMethodHandle(
                                t,
                                tIsInterface,
                                m.name,
                                m.descriptor
                            )
                        )
                    } else if (m.isPrivate) {
                        (
                            INVOKESPECIAL.opcode,
                            InvokeSpecialMethodHandle(
                                t,
                                tIsInterface,
                                m.name,
                                m.descriptor
                            )
                        )
                    } else {
                        (
                            INVOKEVIRTUAL.opcode,
                            InvokeVirtualMethodHandle(
                                t,
                                m.name,
                                m.descriptor
                            )
                        )
                    }
                proxy =
                    ClassFileFactory.Proxy(
                        classFile.thisType,
                        classFile.isInterfaceDeclaration,
                        definingType,
                        proxyMethodName,
                        m.descriptor,
                        t, tIsInterface,
                        methodHandle,
                        invocationInstruction,
                        MethodDescriptor.NoArgsAndReturnVoid, // <= not tested...
                        IndexedSeq.empty
                    )

                def verifyMethod(method: Method): Unit = {
                    val domain = new BaseDomain(testProject, method)
                    val result = BaseAI(method, domain)

                    // the abstract interpretation succeeded
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

                val proxyMethod = proxy.findMethod(proxyMethodName).head
                it("should produce a correct forwarding method") {
                    verifyMethod(proxyMethod)
                }

                val constructor = proxy.findMethod("<init>").head
                it("should produce a correct constructor") {
                    verifyMethod(constructor)
                }

                val factoryMethod =
                    proxy.findMethod("$newInstance").headOption.getOrElse(
                        proxy.findMethod("$createInstance").head
                    )
                it("should produce a correct factory method") {
                    verifyMethod(factoryMethod)
                }

            }
            proxy → testProject.source(t).get
        }.toSeq

        describe("the project should be extendable with the generated proxies") {
            val extendedProject = testProject.extend(proxies)

            it("should have the right amount of class files") {
                extendedProject.classFilesCount should be(
                    testProject.classFilesCount + proxies.size
                )
            }

            it("should have the right class files") {
                testProject.allProjectClassFiles foreach { cf ⇒
                    extendedProject.classFile(cf.thisType) should be('defined)
                    if (testProject.source(cf.thisType).isDefined)
                        extendedProject.source(cf.thisType) should be('defined)
                }
                proxies foreach { p ⇒
                    val (proxy, _ /* source*/ ) = p
                    extendedProject.classFile(proxy.thisType) should be('defined)
                    extendedProject.source(proxy.thisType) should be('defined)
                }
            }
        }
    }
}
