/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

import scala.collection.immutable.ArraySeq

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
            "TC1", /* 0 --- checkcast (instr.) */
            "TC2", /* 1 --- virutal invocation of java.lang.Class.cast that's not followed by a checkcast instr. */
            "TC3", /* 2 --- equality check between two objects of type java.lang.Class */
            "TC4", /* 3 --- instanceof (instr.) */
            "TC5", /* 4 --- virutal invocation of java.lang.Class.isInstance */
            "TC6" /* 5 --- virutal invocation of java.lang.Class.isAssignableFrom */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInstruction <- body
        } {
            val instruction = pcAndInstruction.instruction
            val pc = pcAndInstruction.pc

            instruction.opcode match {
                case CHECKCAST.opcode  => instructionsLocations(0) += InstructionLocation(methodLocation, pc)
                case INSTANCEOF.opcode => instructionsLocations(3) += InstructionLocation(methodLocation, pc)
                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode =>
                    val ai = BaseAI
                    val aiResult = ai.apply(method, BaseDomain(project, method))
                    val operands = aiResult.operandsArray.apply(pc)
                    if (operands ne null) {
                        val op1Type = operands(0).asDomainReferenceValue.leastUpperType
                        val op2Type = operands(1).asDomainReferenceValue.leastUpperType
                        val isClassRefCheck = op1Type.exists {
                            op1 => (op1 eq Class) && (op1 eq op2Type.getOrElse(Object))
                        }
                        if (isClassRefCheck) {
                            instructionsLocations(2) += InstructionLocation(methodLocation, pc)
                        }
                    }
                case INVOKEVIRTUAL.opcode =>
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
                case _ =>
            }
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }

}
