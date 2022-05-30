/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY

/**
 * Holds a mutable map of [[DefinitionSite]] objects to ensure unique identities.
 * The map is filled on-the-fly while querying.
 *
 * ==Thread Safety==
 * This class is thread-safe.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
class DefinitionSites(val project: SomeProject) {

    val definitionSites = new ConcurrentHashMap[DefinitionSite, DefinitionSite]()

    /**
     * Returns the [[DefinitionSite]] instance for the given program counter and given method.
     * The definition site is either retrieved from the map (if present) or a new one is created
     * and stored into the map.
     */
    def apply(m: Method, pc: Int): DefinitionSite = {
        val defSite = DefinitionSite(m, pc)
        val prev = definitionSites.putIfAbsent(defSite, defSite)
        if (prev == null) defSite else prev
    }

    /**
     * Computes all [[DefinitionSite]]s that correspond to an allocation in the project.
     * I.e. all definition sites corresponding to [[org.opalj.br.instructions.NEW]],
     * [[org.opalj.br.instructions.NEWARRAY]], [[org.opalj.br.instructions.ANEWARRAY]] or
     * [[org.opalj.br.instructions.MULTIANEWARRAY]] instructions.
     */
    def getAllocationSites: Seq[DefinitionSite] = {
        val allocationSites = new ConcurrentLinkedQueue[DefinitionSite]()

        project.parForeachMethodWithBody() { methodInfo =>
            val m = methodInfo.method
            val code = m.body.get.instructions
            var pc = 0
            while (pc < code.length) {
                val instr = code(pc)
                if (instr != null) {
                    instr.opcode match {
                        case NEW.opcode | NEWARRAY.opcode | ANEWARRAY.opcode | MULTIANEWARRAY.opcode =>
                            val defSite: DefinitionSite = apply(m, pc)
                            allocationSites.add(defSite)
                        case _ =>
                    }
                }
                pc += 1
            }
        }
        allocationSites.asScala.toSeq
    }

}
