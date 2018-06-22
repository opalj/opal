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
package org.opalj
package hermes
package queries
package jcg

import java.util.concurrent.ConcurrentHashMap

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.da.ClassFile

/**
 * Groups test case features that perform a polymorhpic method calls that are related to Java 8
 * interfaces. I.e., method calls to an interface's default method.
 *
 * @note The features represent the __Java8PolymorphicCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class Java8PolymorphicCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    case class CacheKey(otID: Int, method: String, opcode: Int)

    val counterMap = new ConcurrentHashMap[Int, Int]()
    val relInvokeCache = new ConcurrentHashMap[CacheKey, Boolean]()

    override def featureIDs: Seq[String] = {
        Seq( /* IDM = interface default method */
            "J8PC1", /* 0 --- call on interface which must be resolved to an IDM */
            "J8PC2", /* 1 --- call on interface (with IDM) that must not be resolved to it */
            "J8PC3", /* 2 --- call on class which transitively calls an method that potentially could target an IDM */
            "J8PC4", /* 3 --- call on class type which must be resolved to an IDM */
            "J8PC5", /* 4 --- */
            "J8PC6" /* 5 --- call to static interface method */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        counterMap.clear()
        val instructionsLocations = Array.fill(6)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            callerType = classFile.thisType
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case iv: INVOKEVIRTUAL if isPotentialCallOnDefaultMethod(iv, project)   ⇒ iv
                case ii: INVOKEINTERFACE if isPotentialCallOnDefaultMethod(ii, project) ⇒ ii
                case is: INVOKESTATIC if is.isInterface == true                         ⇒ is
            }
        } {
            val pc = pcAndInvocation.pc
            //TODO rewrite to opcode
            val invokeKind = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind match {
                case ii @ INVOKEINTERFACE(dc, name, md) ⇒ {
                    val subtypes = project.classHierarchy.allSubtypes(dc.asObjectType, false)
                    val hasDefaultMethodTarget = subtypes.exists { ot ⇒
                        val target = project.instanceCall(callerType, ot, name, md)
                        if (target.hasValue) {
                            val definingClass = target.value.asVirtualMethod.classType.asObjectType
                            project.classFile(definingClass).map(_.isInterfaceDeclaration).getOrElse(false)
                        } else
                            false
                    }

                    if (hasDefaultMethodTarget)
                        0 /* a default method can be a target */
                    else {
                        1 /* interface has default method, but it's always overridden */
                    }
                }
                case iv @ INVOKEVIRTUAL(dc, name, md) ⇒ {
                    val subtypes = project.classHierarchy.allSubtypes(dc.asObjectType, true)
                    var subtypeWithMultipleInterfaces = false
                    val hasDefaultMethodTarget = subtypes.exists { ot ⇒
                        val target = project.instanceCall(callerType, ot, name, md)
                        if (target.hasValue) {
                            val definingClass = target.value.asVirtualMethod.classType.asObjectType
                            val isIDM = project.classFile(definingClass).map(_.isInterfaceDeclaration).getOrElse(false)
                            if (isIDM) {
                                // if the method is resolved to an IDM we have to check whether there are multiple options
                                // in order to check the linearization order
                                val value = counterMap.getOrDefault(ot, 0)
                                counterMap.put(ot.id, value + 1)
                                val typeInheritMultipleIntWithSameIDM = project.classHierarchy.allSuperinterfacetypes(ot, false).count { it ⇒
                                    val cf = project.classFile(it)
                                    if (cf.nonEmpty) {
                                        val method = cf.get.findMethod(name, md)
                                        method.map(_.body.nonEmpty).getOrElse(false)
                                    } else {
                                        false
                                    }
                                }
                                subtypeWithMultipleInterfaces |= typeInheritMultipleIntWithSameIDM > 1
                                if (typeInheritMultipleIntWithSameIDM > 2) println(typeInheritMultipleIntWithSameIDM)
                            }
                            isIDM
                        } else
                            false
                    }

                    if (hasDefaultMethodTarget) {
                        if (subtypeWithMultipleInterfaces)
                            4 /* the type inherits multiple interface with IDMs */
                        else
                            3 /* a default method can be a target */
                    } else {
                        2 /* interface has default method, but it's always overridden */
                    }

                }
                case _: INVOKESTATIC ⇒ 5 /* call of a static interface method */
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        val sb = new StringBuffer()

        val keys = counterMap.keys()
        while (keys.hasMoreElements) {
            val key = keys.nextElement()
            sb.append(s"${project.classHierarchy.getObjectType(key)}: ${counterMap.get(key)}\n")
        }

        println(sb.toString)

        instructionsLocations;
    }

    // This method determines whether the called interface method might be dispatched to a default method.
    private[this] def isPotentialCallOnDefaultMethod[S](
        mii: MethodInvocationInstruction, project: Project[S]
    ): Boolean = {

        val t = mii.declaringClass
        if (!t.isObjectType)
            return false;

        val ot = t.asObjectType
        val methodName = mii.name
        val methodDescriptor = mii.methodDescriptor

        val invokeID = CacheKey(ot.id, methodDescriptor.toJava(methodName), mii.opcode)
        relInvokeCache.containsKey(invokeID)
        if (relInvokeCache.containsKey(invokeID))
            return relInvokeCache.get(invokeID);

        val ch = project.classHierarchy
        var relevantInterfaces = ch.allSuperinterfacetypes(ot, true)
        ch.allSubclassTypes(ot, false).foreach { st ⇒
            relevantInterfaces = relevantInterfaces ++ ch.allSuperinterfacetypes(st, false)
        }

        val isRelevant = relevantInterfaces.exists { si ⇒
            val cf = project.classFile(si)
            if (cf.isDefined) {
                val method = cf.get.findMethod(methodName, methodDescriptor)
                method.isDefined && method.get.body.isDefined && method.get.isPublic
            } else {
                false
            }
        }

        relInvokeCache.put(invokeID, isRelevant)
        isRelevant
    }
}
