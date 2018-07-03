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

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.da.ClassFile

/**
 * Groups test case features that perform a pre Java 8 polymorhpic method call.
 *
 * @note The features represent the __PolymorphicCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class PolymorphicCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "BPC1", /* 0 --- virtual call with single target */
            "BPC2", /* 1 --- virtual call with multiple possible targets */
            "BPC3", /* 2 --- interface call with single target */
            "BPC4" /* 3 --- interface call with multiple targets */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionsLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            callerType = classFile.thisType
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case iv: INVOKEVIRTUAL   ⇒ iv
                case ii: INVOKEINTERFACE ⇒ ii
            }
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind.opcode match {
                case INVOKEVIRTUAL.opcode ⇒ {
                    val targets = project.virtualCall(callerType.packageName, invokeKind.asInstanceOf[INVOKEVIRTUAL])
                    targets.size match {
                        case 0 ⇒ -1 /* boring call site */
                        case 1 ⇒ 0 /* single target cs */
                        case _ ⇒ 1 /* multiple target cs*/
                    }
                }
                case INVOKEINTERFACE.opcode ⇒ {
                    val targets = project.interfaceCall(invokeKind.asInstanceOf[INVOKEINTERFACE])
                    targets.size match {
                        case 0 ⇒ -1 /* boring call site */
                        case 1 ⇒ 2 /* single target cs */
                        case _ ⇒ 3 /* multiple target cs*/
                    }
                }
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        instructionsLocations;
    }
}