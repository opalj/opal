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
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._
import org.opalj.collection.immutable.ConstArray
import org.opalj.br.instructions.LDCString
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.LDC_W

/**
 * The ''key'' object to get information about all string constants found in the project's code.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object StringConstantsInformationKey
    extends ProjectInformationKey[StringConstantsInformation, Nothing] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the field access information.
     *
     * @note  This analysis is internally parallelized. I.e., it is advantageous to run this
     *        analysis in isolation.
     */
    override protected def compute(project: SomeProject): Map[String, ConstArray[PCInMethod]] = {

        val estimatedSize = project.methodsCount
        val map = new ConcurrentHashMap[String, ConcurrentLinkedQueue[PCInMethod]](estimatedSize)

        project.parForeachMethodWithBody(defaultIsInterrupted) { methodInfo ⇒
            val method = methodInfo.method

            method.body.get foreach { i: PCAndInstruction ⇒
                val pc = i.pc
                val instruction = i.instruction
                if (instruction.opcode == LDC.opcode || instruction.opcode == LDC_W.opcode) {
                    instruction match {
                        case LDCString(value) ⇒
                            var list: ConcurrentLinkedQueue[PCInMethod] = map.get(value)
                            if (list eq null) {
                                list = new ConcurrentLinkedQueue[PCInMethod]()
                                val previousList = map.putIfAbsent(value, list)
                                if (previousList != null) list = previousList
                            }
                            list.add(PCInMethod(method, pc))
                        case _ ⇒ /*other type of constant*/
                    }
                }
            }
        }

        var result: Map[String, ConstArray[PCInMethod]] = Map.empty
        map.asScala foreach { kv ⇒
            val (name, locations) = kv
            result += ((name, ConstArray.from(locations.asScala.toArray)))
        }
        result
    }
}
