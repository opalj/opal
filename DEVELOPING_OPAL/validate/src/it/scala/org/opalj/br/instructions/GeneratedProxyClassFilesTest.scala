/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import java.net.URL
import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.TestSupport.biProject
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l0.BaseDomain

import scala.collection.immutable.ArraySeq

/**
 * Checks that the ClassFileFactory produces valid proxy class files.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class GeneratedProxyClassFilesTest extends AnyFunSpec with Matchers {

    describe("the generation of Proxy classes") {

        val testProject = biProject("proxy.jar")

        val proxies: Seq[(ClassFile, URL)] = testProject.allMethods.map { m =>
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
                        ArraySeq.empty
                    )

                def verifyMethod(method: Method): Unit = {
                    val domain = new BaseDomain(testProject, method)
                    val result = BaseAI(method, domain)

                    // the abstract interpretation succeeded
                    result should not be (Symbol("wasAborted"))

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
                    for {
                        pc <- instructions.indices
                        if instructions(pc) != null
                    } {
                        val nextPc = instructions(pc).indexOfNextInstruction(pc, false)
                        instructions.slice(pc + 1, nextPc).foreach(_ should be(null))
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
            proxy -> testProject.source(t).get
        }.toSeq

        describe("the project should be extendable with the generated proxies") {
            val extendedProject = testProject.extend(proxies)

            it("should have the right amount of class files") {
                extendedProject.classFilesCount should be(
                    testProject.classFilesCount + proxies.size
                )
            }

            it("should have the right class files") {
                testProject.allProjectClassFiles foreach { cf =>
                    extendedProject.classFile(cf.thisType) should be(Symbol("defined"))
                    if (testProject.source(cf.thisType).isDefined)
                        extendedProject.source(cf.thisType) should be(Symbol("defined"))
                }
                proxies foreach { p =>
                    val (proxy, _ /* source*/ ) = p
                    extendedProject.classFile(proxy.thisType) should be(Symbol("defined"))
                    extendedProject.source(proxy.thisType) should be(Symbol("defined"))
                }
            }
        }
    }
}
