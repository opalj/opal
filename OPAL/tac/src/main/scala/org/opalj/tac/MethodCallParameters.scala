/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

object MethodCallParameters {

    def unapply[V <: Var[V]](astNode: ASTNode[V]): Option[Seq[Expr[V]]] = {
        astNode match {
            case c: Call[V @unchecked]                   => Some(c.params)
            case Assignment(_, _, c: Call[V @unchecked]) => Some(c.params)
            case _                                       => None
        }
    }

}
