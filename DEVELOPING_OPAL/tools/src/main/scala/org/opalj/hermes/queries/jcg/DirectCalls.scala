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
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.da.ClassFile

/**
 * Groups test case features that perform a direct method call.
 *
 * @note The features represent the __DirectCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class DirectCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "DC1", /* 0 --- static method call */
            "DC2", /* 1 --- private method call */
            "DC3", /* 2 --- call on super */
            "DC4" /* 3 --- constructor call */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(4)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case spec: INVOKESPECIAL ⇒ spec
                case stat: INVOKESTATIC  ⇒ stat
            }
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value
            val declType = classFile.thisType

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind match {
                case _: INVOKESTATIC ⇒ 0
                case invSpec @ INVOKESPECIAL(declaringClass, _, name, _) ⇒ {
                    if (name != "<init>") {
                        if (declType eq declaringClass) {
                            if (project.specialCall(invSpec).value.isPrivate) {
                                1
                            } else {
                                -1
                            }
                        } else {
                            // call on super
                            2
                        }
                    } else {
                        3
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
