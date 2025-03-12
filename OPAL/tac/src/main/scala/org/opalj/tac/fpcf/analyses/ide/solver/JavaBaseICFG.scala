/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package solver

import scala.collection
import scala.collection.immutable
import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation

/**
 * Base interprocedural control flow graph for Java programs. This implementation is based on the
 * [[org.opalj.tac.fpcf.analyses.ifds.JavaICFG]] from IFDS.
 */
abstract class JavaBaseICFG(project: SomeProject) extends JavaICFG {
    private val lazyTacProvider: Method => AITACode[TACMethodParameter, ValueInformation] = {
        project.get(LazyDetachedTACAIKey)
    }

    protected implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    protected implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)
    protected val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    private val tacProviderCache = mutable.Map.empty[Method, AITACode[TACMethodParameter, ValueInformation]]

    def tacProvider(callable: Method): AITACode[TACMethodParameter, ValueInformation] = {
        tacProviderCache.getOrElseUpdate(callable, { lazyTacProvider(callable) })
    }

    override def isCallStatement(javaStmt: JavaStatement): Boolean = {
        if (javaStmt.isReturnNode) {
            return false
        }

        val stmt = javaStmt.stmt
        stmt.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID => true
            case Assignment.ASTID | ExprStmt.ASTID =>
                val expr = stmt.astID match {
                    case Assignment.ASTID => stmt.asAssignment.expr
                    case ExprStmt.ASTID   => stmt.asExprStmt.expr
                }
                expr.astID match {
                    case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID => true
                    case _                                                                                   => false
                }
            case _ => false
        }
    }

    override def getCallees(javaStmt: JavaStatement): collection.Set[Method] = {
        val caller = declaredMethods(javaStmt.method)
        if (caller == null) {
            return immutable.Set.empty
        }
        val calleesEOptionP = propertyStore(caller, Callees.key)
        calleesEOptionP match {
            case FinalP(callees) =>
                callees
                    .directCallees(contextProvider.newContext(caller), javaStmt.stmt.pc)
                    .map(_.method)
                    .flatMap { callee =>
                        if (callee.hasSingleDefinedMethod) {
                            Seq(callee.definedMethod)
                        } else if (callee.hasMultipleDefinedMethods) {
                            callee.definedMethods
                        } else {
                            Seq.empty
                        }
                    }
                    .toSet
            case _ =>
                throw new IllegalStateException("Call graph must be computed before the analysis starts!")
        }
    }

    override def getCallable(javaStmt: JavaStatement): Method = javaStmt.method

    override def stringifyStatement(javaStmt: JavaStatement, indent: String = "", short: Boolean = false): String = {
        val stringifiedStatement = javaStmt.toString
        if (short) {
            stringifiedStatement.substring(0, stringifiedStatement.indexOf("{"))
        } else {
            stringifiedStatement
        }
    }
}
