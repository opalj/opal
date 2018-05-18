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
package hermes
package queries

import org.opalj.da.ClassFile
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse

/**
 * Counts (non-)trivial usages of "Class.forName(...)".
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
class TrivialReflectionUsage(implicit hermes: HermesConfig) extends FeatureQuery {

    final val TrivialForNameUsage = "Trivial Class.forName Usage"
    final val NonTrivialForNameUsage = "Nontrivial Class.forName Usage"

    override val featureIDs: List[String] = List(TrivialForNameUsage, NonTrivialForNameUsage)

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {
        val Class = ObjectType.Class
        val ForName1MD = MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Class;")
        val ForName3MD =
            MethodDescriptor("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")

        val trivialLocations = new LocationsContainer[S]
        val nontrivialLocations = new LocationsContainer[S]

        project.parForeachMethodWithBody(isInterrupted = this.isInterrupted _) { mi ⇒
            val MethodInfo(source, m @ MethodWithBody(code)) = mi
            val classForNameCalls = code collect {
                case i @ INVOKESTATIC(Class, false, "forName", ForName1MD | ForName3MD) ⇒ i
            }
            if (classForNameCalls.nonEmpty) {
                val aiResult = BaseAI(m, new DefaultDomainWithCFGAndDefUse(project, m))
                val methodLocation = MethodLocation(source, m)
                for {
                    pcAndInstruction ← classForNameCalls
                    instruction = pcAndInstruction.value
                    pc = pcAndInstruction.pc
                    classNameParameterIndex = instruction.parametersCount - 1
                    operands = aiResult.operandsArray(pc) // if i is dead... opeands is null
                    if operands ne null
                    classNameParameter = operands(classNameParameterIndex)
                } {
                    classNameParameter match {
                        case aiResult.domain.StringValue(className) ⇒
                            trivialLocations += InstructionLocation(methodLocation, pc)
                        case aiResult.domain.MultipleReferenceValues(classNameParameters) ⇒
                            val classNames = classNameParameters.collect {
                                case aiResult.domain.StringValue(className) ⇒ className
                            }
                            // check if we have a concrete string in all cases..
                            val locations = if (classNames.size == classNameParameters.size) {
                                trivialLocations
                            } else {
                                nontrivialLocations
                            }
                            locations += InstructionLocation(methodLocation, pc)
                        case _ ⇒
                            nontrivialLocations += InstructionLocation(methodLocation, pc)
                    }
                }
            }
        }

        List(
            Feature[S](TrivialForNameUsage, trivialLocations),
            Feature[S](NonTrivialForNameUsage, nontrivialLocations)
        )
    }
}
