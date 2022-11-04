/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.ifds.ICFG
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.{Assignment, DUVar, Expr, ExprStmt, LazyDetachedTACAIKey, NonVirtualFunctionCall, NonVirtualMethodCall, StaticFunctionCall, StaticMethodCall, Stmt, TACMethodParameter, TACode, VirtualFunctionCall, VirtualMethodCall}
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.properties.cg.{Callees, Callers}
import org.opalj.value.ValueInformation

abstract class JavaICFG(project: SomeProject)
    extends ICFG[Method, JavaStatement] {

    val tacai: Method => TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(LazyDetachedTACAIKey)
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
        case ExprStmt.ASTID   => statement.asExprStmt.expr
        case _                => throw new UnknownError("Unexpected statement")
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

    /**
     * Get the method's statement with the given index.
     *
     * @param callable the method containing the statement.
     * @param index    the index of the statement.
     * @return the corresponding statement.
     */
    override def getStatement(callable: Method, index: Int): JavaStatement = {
        val TACode(_, code, _, cfg, _) = tacai(callable)
        JavaStatement(callable, index, code, cfg)
    }

    override def getCallers(callee: Method): Seq[(Method, Int)] = {
        val declaredCallee = declaredMethods(callee)
        propertyStore(declaredCallee, Callers.key) match {
            case FinalEP(_, p: Callers) =>
                p.callers(declaredCallee).iterator
                    // We do not handle indirect calls.
                    .filter(callersProperty => callersProperty._3)
                    .map {
                        case (caller, callPc, _) =>
                            (caller.definedMethod, tacai(caller.definedMethod).pcToIndex(callPc))
                    }
                    .toSeq
            case _ =>
                throw new IllegalStateException(
                    "call graph must be computed before the analysis starts"
                )
        }
    }
}
