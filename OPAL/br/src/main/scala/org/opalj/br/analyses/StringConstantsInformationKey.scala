/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import org.opalj.br.instructions.LDCString
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.LDC_W

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

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
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the field access information.
     *
     * @note  This analysis is internally parallelized. I.e., it is advantageous to run this
     *        analysis in isolation.
     */
    override def compute(project: SomeProject): mutable.Map[String, ArraySeq[PCInMethod]] = {

        val estimatedSize = project.methodsCount
        val map = new ConcurrentHashMap[String, ConcurrentLinkedQueue[PCInMethod]](estimatedSize)

        project.parForeachMethodWithBody(defaultIsInterrupted) { methodInfo =>
            val method = methodInfo.method

            method.body.get foreach { i: PCAndInstruction =>
                val pc = i.pc
                val instruction = i.instruction
                if (instruction.opcode == LDC.opcode || instruction.opcode == LDC_W.opcode) {
                    instruction match {
                        case LDCString(value) =>
                            var list: ConcurrentLinkedQueue[PCInMethod] = map.get(value)
                            if (list eq null) {
                                list = new ConcurrentLinkedQueue[PCInMethod]()
                                val previousList = map.putIfAbsent(value, list)
                                if (previousList != null) list = previousList
                            }
                            list.add(PCInMethod(method, pc))
                        case _ => /*other type of constant*/
                    }
                }
            }
        }

        val result: mutable.Map[String, ArraySeq[PCInMethod]] = mutable.Map.empty
        map.asScala foreach { kv =>
            val (name, locations) = kv
            result += ((name, ArraySeq.unsafeWrapArray(locations.asScala.toArray)))
        }
        result
    }
}
