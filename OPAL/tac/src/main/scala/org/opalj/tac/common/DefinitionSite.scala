/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package common

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import org.opalj.br.Method

/**
 * Identifies a definition site object in a method in the bytecode using its program counter and
 * the corresponding use-sites.
 * It acts as entity for the [[org.opalj.br.fpcf.properties.EscapeProperty]] and the computing
 * analyses.
 * A definition-site can be for example an allocation, the result of a function call, or the result
 * of a field-retrieval.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
case class DefinitionSite(method: Method, pc: Int) extends DefinitionSiteLike {

    override def usedBy[V <: ValueInformation](
        tacode: TACode[TACMethodParameter, DUVar[V]]
    ): IntTrieSet = {
        val defSite = tacode.properStmtIndexForPC(pc)
        if (defSite == -1) {
            // the code is dead
            IntTrieSet.empty
        } else {
            tacode.stmts(defSite) match {
                case Assignment(_, dvar, _) => dvar.usedBy
                case _: ExprStmt[_]         => IntTrieSet.empty
                case stmt =>
                    throw new RuntimeException(s"unexpected stmt ($stmt) at definition site $this")
            }
        }
    }

    override def toString: String = s"DefinitionSite(m=${method.toJava}, pc=$pc)"

    override def hashCode(): Int = method.hashCode() ^ pc
}
