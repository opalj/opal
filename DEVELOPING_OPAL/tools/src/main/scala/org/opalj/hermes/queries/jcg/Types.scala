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

import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.br.ObjectType
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.IF_ACMPEQ
import org.opalj.br.instructions.IF_ACMPNE
import org.opalj.da.ClassFile

/**
 * Groups features that somehow rely on Javas type cast API given by either jvm instructions or
 * ```java.lang.Class```.
 *
 * @note The features represent the __TYPES__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class Types(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    val Class = ObjectType("java/lang/Class")
    val Object = ObjectType("java/lang/Object")

    override def featureIDs: Seq[String] = {
        Seq(
            "SimpleCast", /* 0 --- checkcast (instr.) */
            "CastClassAPI", /* 1 --- virutal invocation of java.lang.Class.cast that's not followed by a checkcast instr. */
            "ClassEQ", /* 2 --- equality check between two objects of type java.lang.Class */
            "InstanceOf", /* 3 --- instanceof (instr.) */
            "InstanceOfClassAPI", /* 4 --- virutal invocation of java.lang.Class.isInstance */
            "IsAssignable" /* 5 --- virutal invocation of java.lang.Class.isAssignableFrom */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(6)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInstruction ← body
        } {
            val instruction = pcAndInstruction.instruction
            val pc = pcAndInstruction.pc

            instruction.opcode match {
                case CHECKCAST.opcode  ⇒ instructionsLocations(0) += InstructionLocation(methodLocation, pc)
                case INSTANCEOF.opcode ⇒ instructionsLocations(3) += InstructionLocation(methodLocation, pc)
                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode ⇒ {
                    val ai = BaseAI
                    val aiResult = ai.apply(method, BaseDomain(project, method))
                    val operands = aiResult.operandsArray.apply(pc)
                    if (operands ne null) {
                        val op1Type = operands(0).asDomainReferenceValue.valueType
                        val op2Type = operands(1).asDomainReferenceValue.valueType
                        val isClassRefCheck = op1Type.map {
                            op1 ⇒ (op1 eq Class) && (op1 eq op2Type.getOrElse(Object))
                        }.getOrElse(false)
                        if (isClassRefCheck) {
                            instructionsLocations(2) += InstructionLocation(methodLocation, pc)
                        }
                    }
                }
                case INVOKEVIRTUAL.opcode ⇒ {
                    val INVOKEVIRTUAL(declaringClass, name, _) = instruction.asInstanceOf[INVOKEVIRTUAL];
                    if (declaringClass.isObjectType && (declaringClass.asObjectType eq Class)) {
                        if (name eq "cast") {
                            val nextPC = body.pcOfNextInstruction(pc)
                            if (body.instructions(nextPC).opcode != CHECKCAST.opcode) {
                                instructionsLocations(1) += InstructionLocation(methodLocation, pc)
                            }
                        } else if (name eq "isInstance") {
                            instructionsLocations(4) += InstructionLocation(methodLocation, pc)
                        } else if (name eq "isAssignableFrom") {
                            instructionsLocations(5) += InstructionLocation(methodLocation, pc)
                        }
                    }
                }
                case _ ⇒
            }
        }

        instructionsLocations;
    }

}
