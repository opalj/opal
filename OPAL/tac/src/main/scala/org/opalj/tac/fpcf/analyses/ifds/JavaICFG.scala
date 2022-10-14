/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.ifds.ICFG
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.{Assignment, Expr, ExprStmt, NonVirtualFunctionCall, NonVirtualMethodCall, StaticFunctionCall, StaticMethodCall, Stmt, VirtualFunctionCall, VirtualMethodCall}
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.properties.cg.Callees

abstract class JavaICFG(project: SomeProject)
    extends ICFG[Method, JavaStatement] {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: JavaStatement): Option[collection.Set[Method]] =
        statement.stmt.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID =>
                Some(getCallees(statement))
            case Assignment.ASTID | ExprStmt.ASTID =>
                getExpression(statement.stmt).astID match {
                    case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID =>
                        Some(getCallees(statement))
                    case _ => None
                }
            case _ => None
        }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param statement The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression.
     */
    private def getExpression(statement: Stmt[_]): Expr[_] = statement.astID match {
        case Assignment.ASTID => statement.asAssignment.expr
        case ExprStmt.ASTID => statement.asExprStmt.expr
        case _ => throw new UnknownError("Unexpected statement")
    }

    private def getCallees(statement: JavaStatement): collection.Set[Method] = {
        val pc = statement.stmt.pc
        val caller = declaredMethods(statement.callable)
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) => definedMethods(p.directCallees(typeIterator.newContext(caller), pc).map(_.method))
            case _ =>
                throw new IllegalStateException(
                    "call graph must be computed before the analysis starts"
                )
        }
    }

    /**
     * Maps some declared methods to their defined methods.
     *
     * @param declaredMethods Some declared methods.
     * @return All defined methods of `declaredMethods`.
     */
    private def definedMethods(declaredMethods: Iterator[DeclaredMethod]): collection.Set[Method] = {
        val result = scala.collection.mutable.Set.empty[Method]
        declaredMethods
            .filter(
                declaredMethod =>
                    declaredMethod.hasSingleDefinedMethod ||
                        declaredMethod.hasMultipleDefinedMethods
            )
            .foreach(
                declaredMethod =>
                    declaredMethod
                        .foreachDefinedMethod(defineMethod => result.add(defineMethod))
            )
        result
    }
}
