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
package ai
package common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY

import scala.collection.JavaConverters._

class DefinitionSites(val project: SomeProject) {
    val definitionSites = new ConcurrentHashMap[DefinitionSite, DefinitionSite]()
    private[this] val aiResult = project.get(SimpleAIKey)

    def apply(m: Method, pc: Int): DefinitionSite = {
        val uses = aiResult(m).domain.safeUsedBy(pc)
        val defSite = new DefinitionSite(m, pc, uses)
        val prev = definitionSites.putIfAbsent(defSite, defSite)
        if (prev == null) defSite else prev
    }

    def getAllocationSites: Seq[DefinitionSite] = {
        val allocationSites = new ConcurrentLinkedQueue[DefinitionSite]()

        project.parForeachMethodWithBody() { methodInfo ⇒
            val m = methodInfo.method
            val code = m.body.get.instructions
            var pc = 0
            while (pc < code.length) {
                val instr = code(pc)
                if (instr != null) {
                    instr.opcode match {
                        case NEW.opcode | NEWARRAY.opcode | ANEWARRAY.opcode | MULTIANEWARRAY.opcode ⇒
                            val defSite: DefinitionSite = apply(m, pc)
                            allocationSites.add(defSite)
                        case _ ⇒
                    }
                }
                pc += 1
            }
        }
        allocationSites.asScala.toSeq
    }

}
