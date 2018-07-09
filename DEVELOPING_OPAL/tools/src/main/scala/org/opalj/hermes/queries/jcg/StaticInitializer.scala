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
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.ObjectType
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.LDC_W
import org.opalj.da.ClassFile

/**
 * Groups test case features that test the correct recognition of static initializers.
 *
 * @note The features represent the __StaticInitializer__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class StaticInitializer(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    val Object = ObjectType.Object
    val String = ObjectType.String

    override def featureIDs: Seq[String] = {
        Seq(
            "SI1", /* 0 --- reference of a non-constant field  within an interface. */
            "SI2", /* 1  --- invocation of a static interface method. */
            "SI3", /* 2  --- class creation when class impl. Interface with default method and static fields. */
            "SI4", /* 3 ---  reference of a final non-primitive and non-String field within an interface. */
            "SI5", /* 4 --- class creation should trigger the static initializer */
            "SI6", /* 5 --- call of a static method */
            "SI7" /* 6 --- assignment to a static field */ ,
            "SI8" /* 7 --- initialization of a class should cause initialization of super classes */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val classLocations = Array.fill(8)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if classFile.staticInitializer.isDefined
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
        } {
            val hasStaticField = classFile.fields.exists(_.isStatic)
            val hasStaticMethod = classFile.methods.exists(m ⇒ m.isStatic && !m.isStaticInitializer)

            if (classFile.isInterfaceDeclaration) { // index 0 - 3

                if (hasStaticMethod) {
                    classLocations(1) += classFileLocation
                }
                val hasDefaultMethod = classFile.instanceMethods.exists { m ⇒
                    m.body.nonEmpty && m.isPublic
                }

                if (hasStaticField) {
                    if (hasDefaultMethod) {
                        classLocations(2) += classFileLocation
                    }

                    val si = classFile.staticInitializer.get
                    val putStaticPCs = si.body.get.collectInstructionsWithPC {
                        case pci @ PCAndInstruction(_, PUTSTATIC(_, _, _)) ⇒ pci
                    }

                    val domain = new DefaultDomainWithCFGAndDefUse(project, si)

                    putStaticPCs.foreach { pci ⇒
                        val pc = pci.pc

                        val ai = BaseAI
                        val aiResult = ai.apply(si, domain)
                        val vo = aiResult.domain.operandOrigin(pc, 0).head
                        val inst = aiResult.code.instructions(vo)

                        if (inst.isInvocationInstruction) {
                            classLocations(3) += classFileLocation
                        } else if (inst.opcode != LDC.opcode && inst.opcode != LDC_W.opcode) {
                            classLocations(0) += classFileLocation
                        }
                    }
                }
            } else { // index 4 - 7
                val hasNonPrivateConstructor = classFile.constructors.exists { !_.isPrivate }

                if (hasNonPrivateConstructor) {
                    classLocations(4) += classFileLocation
                }

                if (hasStaticMethod) {
                    classLocations(5) += classFileLocation
                }

                if (hasStaticField) {
                    classLocations(6) += classFileLocation
                }

                val superclassType = classFile.superclassType
                if (superclassType.nonEmpty && superclassType.get != Object) {
                    classLocations(7) += classFileLocation
                }
            }
        }

        classLocations;
    }
}
