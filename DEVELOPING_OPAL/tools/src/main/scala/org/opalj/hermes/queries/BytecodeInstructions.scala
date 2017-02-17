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

import java.net.URL

import org.opalj.bytecode.JVMInstructions
import org.opalj.bytecode.JVMOpcodes
import org.opalj.br.analyses.Project
import org.opalj.br.MethodWithBody

/**
 * Counts the number of occurrences of each bytecode instruction.
 *
 * @author Michael Eichberg
 */
object BytecodeInstructions extends FeatureQuery {

    val jvmInstructions = JVMInstructions
    val jvmOpcodes = JVMOpcodes
    val opcodesToOrdinalNumbers = new Array[Int](256)

    override def htmlDescription: Either[String, URL] = {
        Right(new URL("http://www.opal-project.de/bi/JVMInstructions.xml"))
    }

    override def featureIDs: IndexedSeq[String] = {
        var ordinalNumber = 0
        jvmInstructions.map { i ⇒
            val (opcode, mnemonic) = i
            opcodesToOrdinalNumbers(opcode) = ordinalNumber
            ordinalNumber += 1
            s"$mnemonic (opcode:$opcode)"
        }.toIndexedSeq
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {
        val instructionsLocations = Array.fill(256)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            (pc, i) ← body
        } {
            instructionsLocations(i.opcode) += InstructionLocation(methodLocation, pc)
        }

        for { (locations, opcode) ← instructionsLocations.view.zipWithIndex } yield {
            Feature[S](featureIDs(opcodesToOrdinalNumbers(opcode)), locations)
        }
    }
}
