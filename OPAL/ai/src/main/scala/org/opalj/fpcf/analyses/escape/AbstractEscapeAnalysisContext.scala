/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.cfg.CFG
import org.opalj.ai.ValueOrigin
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.UVar
import org.opalj.tac.TACStmts

/**
 * Provides the basic information corresponding to an entity to determine its escape information.
 * Furthermore, it has helper functions to check whether the entity might be used in expressions.
 *
 * @see [[AbstractEscapeAnalysis]]
 *
 * @author Florian Kuebler
 */
trait AbstractEscapeAnalysisContext {

    val entity: Entity
    val uses: IntTrieSet
    val defSite: ValueOrigin
    val targetMethod: Method
    val code: Array[Stmt[V]]

    def targetMethodDeclaringClassType: ObjectType = targetMethod.classFile.thisType

    /**
     * Checks whether the expression is a use of the defSite.
     * This method is called on expressions within tac statements. We assume a flat hierarchy, so
     * the expression is expected to be a [[org.opalj.tac.Var]].
     */
    private[escape] final def usesDefSite(expr: Expr[V]): Boolean = {
        assert(expr.isVar)
        expr.asVar.definedBy.contains(defSite)
    }

    /**
     * If there exists a [[org.opalj.tac.UVar]] in the params of a method call that is a use of the
     * current entity's def-site return true.
     */
    private[escape] final def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        assert(params.forall(_.isVar))
        params.exists { case UVar(_, defSites) ⇒ defSites.contains(defSite) }
    }
}

trait CFGContainer {
    val cfg: CFG[Stmt[V], TACStmts[V]]
}

trait PropertyStoreContainer {
    val propertyStore: PropertyStore
}

trait DeclaredMethodsContainer {
    val declaredMethods: DeclaredMethods
}

trait VirtualFormalParametersContainer {
    val virtualFormalParameters: VirtualFormalParameters
}

trait IsMethodOverridableContainer {
    val isMethodOverridable: Method ⇒ Answer
}
