/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.MethodWithBody

/**
 * Counts the number of occurrences of each bytecode instruction.
 *
 * @author Michael Eichberg
 */
class BytecodeInstructions(implicit hermes: HermesConfig) extends FeatureQuery {

    // Let's do some caching...
    final val JVMInstructions: List[(Int, String)] = bytecode.JVMInstructions
    private[this] final val OpcodesToOrdinalNumbers = new Array[Int](256)

    override val htmlDescription: Either[String, URL] = {
        Right(new URL("https://www.opal-project.de/bi/JVMInstructions.xml"))
    }

    override def featureIDs: IndexedSeq[String] = {
        var ordinalNumber = 0
        JVMInstructions.map { i =>
            val (opcode, mnemonic) = i
            OpcodesToOrdinalNumbers(opcode) = ordinalNumber
            ordinalNumber += 1
            s"$mnemonic (opcode:$opcode)"
        }.toIndexedSeq
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        val instructionsLocations = Array.fill(256)(new LocationsContainer[S])

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
            instructionsLocations(instruction.opcode) += InstructionLocation(methodLocation, pc)
        }

        for { (locations, opcode) <- instructionsLocations.iterator.zipWithIndex } yield {
            Feature[S](featureIDs(OpcodesToOrdinalNumbers(opcode)), locations)
        }
    }
}
