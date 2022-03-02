/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.{DeclaredMethod}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.{Assignment, Call, ExprStmt, Stmt}
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.cg.{Callees, Callers}

abstract class JavaIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, DeclaredMethod, JavaStatement](project) {
    /**
     * All declared methods in the project.
     */
    implicit final protected val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    implicit final protected val typeProvider: TypeProvider = project.get(TypeProviderKey)

    /**
     * Checks, if a callee is inside this analysis' context.
     * If not, `callOutsideOfAnalysisContext` is called instead of analyzing the callee.
     * By default, native methods are not inside the analysis context.
     *
     * @param callee The callee.
     * @return True, if the callee is inside the analysis context.
     */
    override def insideAnalysisContext(callee: DeclaredMethod): Boolean =
        callee.definedMethod.body.isDefined

    override def getCallees(
        statement: JavaStatement,
        caller:    DeclaredMethod
    ): Iterator[DeclaredMethod] = {
        val pc = statement.code(statement.index).pc
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) ⇒
                p.directCallees(typeProvider.newContext(caller), pc).map(_.method)
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }
    }

    /**
     * Returns all methods, that can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @return All methods, that can be called from outside the library.
     */
    protected def methodsCallableFromOutside: Set[DeclaredMethod] =
        declaredMethods.declaredMethods.filter(canBeCalledFromOutside).toSet

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    protected def canBeCalledFromOutside(method: DeclaredMethod): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }

    /**
     * Gets the call object for a statement that contains a call.
     *
     * @param call The call statement.
     * @return The call object for `call`.
     */
    protected def asCall(call: Stmt[V]): Call[V] = call.astID match {
        case Assignment.ASTID ⇒ call.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ call.asExprStmt.expr.asFunctionCall
        case _                ⇒ call.asMethodCall
    }
}

abstract class JavaBackwardIFDSProblem[IFDSFact <: AbstractIFDSFact, UnbalancedIFDSFact <: IFDSFact with UnbalancedReturnFact[IFDSFact]](project: SomeProject) extends JavaIFDSProblem[IFDSFact](project) with BackwardIFDSProblem[IFDSFact, UnbalancedIFDSFact, DeclaredMethod, JavaStatement] {
    /**
     * Checks for the analyzed entity, if an unbalanced return should be performed.
     *
     * @param source The analyzed entity.
     * @return False, if no unbalanced return should be performed.
     */
    def shouldPerformUnbalancedReturn(source: (DeclaredMethod, IFDSFact)): Boolean =
        source._2.isInstanceOf[UnbalancedReturnFact[IFDSFact]] || entryPoints.contains(source)
}
