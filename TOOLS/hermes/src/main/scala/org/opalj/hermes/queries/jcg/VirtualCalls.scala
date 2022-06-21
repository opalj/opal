/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq

import org.opalj.da.ClassFile
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.VirtualMethodInvocationInstruction

/**
 * Groups test case features that perform a pre Java 8 polymorhpic method call.
 *
 * @note The features represent the __PolymorphicCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class VirtualCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "VC1", /* 0 --- virtual call with single target */
            "VC2", /* 1 --- virtual call with multiple possible targets */
            "VC3", /* 2 --- interface call with single target */
            "VC4" /* 3 --- interface call with multiple targets */
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
            callerType = classFile.thisType
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case iv: INVOKEVIRTUAL   => iv
                case ii: INVOKEINTERFACE => ii
            }: PartialFunction[Instruction, VirtualMethodInvocationInstruction])
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind.opcode match {
                case INVOKEVIRTUAL.opcode => {
                    val targets =
                        project.virtualCall(callerType, invokeKind.asInstanceOf[INVOKEVIRTUAL])
                    targets.size match {
                        case 0 => -1 /* boring call site */
                        case 1 => 0 /* single target cs */
                        case _ => 1 /* multiple target cs*/
                    }
                }
                case INVOKEINTERFACE.opcode => {
                    val targets =
                        project.interfaceCall(callerType, invokeKind.asInstanceOf[INVOKEINTERFACE])
                    targets.size match {
                        case 0 => -1 /* boring call site */
                        case 1 => 2 /* single target cs */
                        case _ => 3 /* multiple target cs*/
                    }
                }
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }
}