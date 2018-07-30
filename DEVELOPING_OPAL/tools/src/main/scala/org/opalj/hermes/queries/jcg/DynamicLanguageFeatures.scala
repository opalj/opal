/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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
package org.opalj.hermes.queries.jcg

import org.opalj.da
import org.opalj.br.MethodWithBody
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.InvokeInterfaceMethodHandle
import org.opalj.br.InvokeVirtualMethodHandle
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.MethodCallMethodHandle
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.hermes.HermesConfig
import org.opalj.hermes.InstructionLocation
import org.opalj.hermes.ProjectConfiguration
import org.opalj.hermes.LocationsContainer
import org.opalj.hermes.DefaultFeatureQuery

/**
 * This feature query corresponds to the LambdaAnsMethodReferences.md test cases from the JCG call
 * graph test suite.
 *
 * @author Michael Reif
 */
class DynamicLanguageFeatures(
        implicit
        val hermesConfig: HermesConfig
) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] =
        /* There are 16 test cases, 8 pertaining to method references and 8 pertaining to lambdas */
        (1 to 7).map(num ⇒ s"MR$num") ++ (1 to 8).map(num ⇒ s"Lambda$num")

    def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            m @ MethodWithBody(code) ← project.allMethodsWithBody
            pcAndInvocation ← code collect {
                case dynInv: INVOKEDYNAMIC   ⇒ dynInv
                case intInv: INVOKEINTERFACE ⇒ intInv
            }
        } yield {

            val pc = pcAndInvocation.pc
            val l = InstructionLocation(project.source(m.classFile).get, m, pc)

            val testCaseId = pcAndInvocation.value match {
                case invDyn: INVOKEDYNAMIC ⇒
                    {
                        val bm = invDyn.bootstrapMethod

                        assert(bm.arguments.size == 2)

                        val handle = bm.arguments(1).asInstanceOf[MethodCallMethodHandle]
                        handle match {
                            case InvokeStaticMethodHandle(_, _, name, descriptor) ⇒ {
                                // this just the called method is defined in the same class..
                                // if there is a method in the same class with the same name and descriptor,
                                // this check is tricked.
                                val localMethod = m.classFile.findMethod(name, descriptor)
                                val isLocal = localMethod.isDefined
                                if (isLocal) {
                                    val callee = localMethod.get
                                    if (callee.isStatic) {
                                        if (callee.isSynthetic) {
                                            7 /* Lambda1 */
                                        } else {
                                            if (callee.parameterTypes.size == 0) {
                                                3 /* MR4 */
                                            } else {
                                                4 /* MR 5 */
                                            }
                                        }
                                    } else {
                                        /* something unexpected */ 14
                                    }
                                } else {
                                    /* something unexpected */ 14
                                }
                            }
                            case InvokeSpecialMethodHandle(_, isInterface, name, methodDescriptor) ⇒ {
                                val localMethod = m.classFile.findMethod(name, methodDescriptor)
                                val isLocal = localMethod.isDefined
                                if (isLocal) {
                                    val callee = localMethod.get
                                    if (callee.isSynthetic) 2 /* MR3  */ else 1 /* MR2 */
                                } else /* something unexpected */ 14
                            }
                            case InvokeInterfaceMethodHandle(_, _, _)  ⇒ 0
                            case InvokeVirtualMethodHandle(_, _, _)    ⇒ 6
                            case NewInvokeSpecialMethodHandle(_, _, _) ⇒ 5
                            case _ ⇒
                                throw new RuntimeException("Unexpected handle Kind.")
                        }
                    }

                    //                case intInv: INVOKEINTERFACE ⇒ {
                    //                    val declCls = intInv.declaringClass
                    //                    val declCf = project.classFile(declCls)
                    //
                    //                    if (declCf.isDefined) {
                    //                        val cf = declCf.get
                    //
                    //                    }
                    //
                    //                    -1 /* We are not interested in this interface invocation */
                    //                }
                    -1
            }

            if (testCaseId > 0 && testCaseId < featureIDs.size) {
                locations(testCaseId) += l
            }
        }

        locations
    }
}

/*
=========
== MR1 ==
=========
NVOKEDYNAMIC get(Lmr1/Interface;)Lmr1/Class$FIBoolean; [
  // handle kind 0x6 : INVOKESTATIC
  java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  // arguments:
  ()Z,
  // handle kind 0x9 : INVOKEINTERFACE
  mr1/Interface.method()Z,
  ()Z
]

=========
== MR2 ==
=========

INVOKEDYNAMIC get(Lmr2/Class;)Ljava/util/function/Supplier; [
  // handle kind 0x6 : INVOKESTATIC
  java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  // arguments:
  ()Ljava/lang/Object;,
  // handle kind 0x7 : INVOKESPECIAL
  mr2/Class.getTypeName()Ljava/lang/String;,
  ()Ljava/lang/String;
]

=========
== MR3 ==
=========

INVOKEDYNAMIC get(Lmr3/Class;)Ljava/util/function/Supplier; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      ()Ljava/lang/Object;,
      // handle kind 0x7 : INVOKESPECIAL
      mr3/Class.lambda$callViaMethodReference$0()Ljava/lang/String;,
      ()Ljava/lang/String;
    ]

=========
== MR4 ==
=========INVOKEDYNAMIC get()Ljava/util/function/Supplier; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      ()Ljava/lang/Object;,
      // handle kind 0x6 : INVOKESTATIC
      mr4/Class.getTypeName()Ljava/lang/String;,
      ()Ljava/lang/String;
    ]

=========
== MR5 ==
=========
INVOKEDYNAMIC apply()Lmr5/Class$FIDoubleDouble; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      (DD)D,
      // handle kind 0x6 : INVOKESTATIC
      mr5/Class.sum(DD)D,
      (DD)D
    ]

=========
== MR6 ==
=========

INVOKEDYNAMIC get()Ljava/util/function/Supplier; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      ()Ljava/lang/Object;,
      // handle kind 0x8 : NEWINVOKESPECIAL
      mr6/Class.<init>()V,
      ()Lmr6/Class;
    ]
=========
== MR7 ==
=========

INVOKEDYNAMIC get(Lmr7/Class;)Ljava/util/function/Supplier; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      ()Ljava/lang/Object;,
      // handle kind 0x5 : INVOKEVIRTUAL
      mr7/SuperClass.version()Ljava/lang/String;,
      ()Ljava/lang/String;
    ]
=============
== Lambda1 ==
=============

*  INVOKEDYNAMIC apply()Ljava/util/function/Function; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments:
      (Ljava/lang/Object;)Ljava/lang/Object;,
      // handle kind 0x6 : INVOKESTATIC
      lambda1/Class.lambda$main$0(Ljava/lang/Integer;)Ljava/lang/Boolean;,
      (Ljava/lang/Integer;)Ljava/lang/Boolean;
    ]
*
* */ 