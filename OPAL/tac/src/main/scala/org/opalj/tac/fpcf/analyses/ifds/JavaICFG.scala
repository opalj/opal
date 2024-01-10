/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.ICFG
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.ValueInformation

/**
 * Interprocedural control flow graph for Java projects used in IFDS Analysis
 *
 * @author Marc Clement
 */
abstract class JavaICFG(project: SomeProject)
    extends ICFG[Method, JavaStatement] {

    val tacai: Method => TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(LazyDetachedTACAIKey)
    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: Method): Set[JavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(callable)
        Set(JavaStatement(callable, 0, code, cfg))
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: JavaStatement): Set[JavaStatement] = {
        statement.cfg
            .successors(statement.index)
            .map { index => JavaStatement(statement, index) }
        // TODO improve performance
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: JavaStatement): Option[Set[Method]] =
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

    private def getCallees(statement: JavaStatement): Set[Method] = {
        val pc = statement.stmt.pc
        val caller = declaredMethods(statement.callable)
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) => definedMethods(p.directCallees(contextProvider.newContext(caller), pc).map(_.method))
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
    private def definedMethods(declaredMethods: Iterator[DeclaredMethod]): Set[Method] = {
        declaredMethods
            .flatMap(declaredMethod =>
                if (declaredMethod.hasSingleDefinedMethod)
                    Seq(declaredMethod.definedMethod)
                else if (declaredMethod.hasMultipleDefinedMethods)
                    declaredMethod.definedMethods
                else
                    Seq.empty
            ).toSet
    }

    override def getCallers(callee: Method): Set[JavaStatement] = {
        val declaredCallee = declaredMethods(callee)
        propertyStore(declaredCallee, Callers.key) match {
            case FinalEP(_, p: Callers) =>
                p.callers(declaredCallee).iterator
                    // We do not handle indirect calls.
                    .filter(callersProperty => callersProperty._3)
                    .map {
                        case (caller, callPc, _) =>
                            val TACode(_, code, _, cfg, _) = tacai(caller.definedMethod)
                            val callIndex = tacai(caller.definedMethod).pcToIndex(callPc)
                            JavaStatement(caller.definedMethod, callIndex, code, cfg)
                    }.toSet
            case _ =>
                throw new IllegalStateException(
                    "call graph must be computed before the analysis starts"
                )
        }
    }

    /**
     * Returns all methods, that can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @return All methods, that can be called from outside the library.
     */
    def methodsCallableFromOutside: Set[DeclaredMethod] = {
        declaredMethods.declaredMethods.filter(canBeCalledFromOutside).toSet
    }

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    def canBeCalledFromOutside(method: DeclaredMethod): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }

    def canBeCalledFromOutside(method: Method): Boolean =
        canBeCalledFromOutside(declaredMethods(method))

}
