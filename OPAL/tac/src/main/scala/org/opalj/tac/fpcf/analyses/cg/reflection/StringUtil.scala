/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

object StringUtil {

    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies local use of String constants and Strings loaded from Properties objects.
     */
    def getPossibleStrings(
        value: Expr[V],
        pc:    Option[Int],
        stmts: Array[Stmt[V]]
    ): Option[Set[String]] = {
        Some(value.asVar.definedBy.map[Set[String]] { index ⇒
            if (index >= 0) {
                val expr = stmts(index).asAssignment.expr
                expr match {
                    case StringConst(_, v) ⇒ Set(v)
                    case _ ⇒
                        return None;
                }
            } else {
                return None;
            }
        }.flatten)
    }
}
