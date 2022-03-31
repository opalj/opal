/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.ifds.{AbstractIFDSFact, ICFG}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.{Assignment, DUVar, Expr, ExprStmt, LazyDetachedTACAIKey, NonVirtualFunctionCall, NonVirtualMethodCall, StaticFunctionCall, StaticMethodCall, Stmt, TACMethodParameter, TACode, VirtualFunctionCall, VirtualMethodCall}
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.value.ValueInformation

class NewForwardICFG[IFDSFact <: AbstractIFDSFact](implicit project: SomeProject)
    extends ICFG[IFDSFact, Method, NewJavaStatement] {
    val tacai: Method ⇒ TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(LazyDetachedTACAIKey)
    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: Method): Set[NewJavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(callable)
        Set(NewJavaStatement(callable, 0, code, cfg))
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: NewJavaStatement): Set[NewJavaStatement] = {
        statement.cfg
            .successors(statement.index)
            .toChain
            .map { index ⇒ NewJavaStatement(statement, index) }
            .toSet
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: NewJavaStatement): Option[collection.Set[Method]] =
        statement.stmt.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID ⇒
                Some(getCallees(statement))
            case Assignment.ASTID | ExprStmt.ASTID ⇒
                getExpression(statement.stmt).astID match {
                    case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                        Some(getCallees(statement))
                    case _ ⇒ None
                }
            case _ ⇒ None
        }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param statement The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression.
     */
    private def getExpression(statement: Stmt[_]): Expr[_] = statement.astID match {
        case Assignment.ASTID ⇒ statement.asAssignment.expr
        case ExprStmt.ASTID   ⇒ statement.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
    }

    private def getCallees(statement: NewJavaStatement): collection.Set[Method] = {
        val pc = statement.code(statement.index).pc
        val caller = declaredMethods(statement.callable)
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) ⇒ definedMethods(p.directCallees(typeProvider.newContext(caller), pc).map(_.method))
            case _ ⇒
                throw new IllegalStateException(
                    "call graph must be computed before the analysis starts"
                )
        }
    }

    override def isExitStatement(statement: NewJavaStatement): Boolean = {
        statement.index == statement.node.asBasicBlock.endPC &&
            statement.node.successors.exists(_.isExitNode)
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
                declaredMethod ⇒
                    declaredMethod.hasSingleDefinedMethod ||
                        declaredMethod.hasMultipleDefinedMethods
            )
            .foreach(
                declaredMethod ⇒
                    declaredMethod
                        .foreachDefinedMethod(defineMethod ⇒ result.add(defineMethod))
            )
        result
    }
}
