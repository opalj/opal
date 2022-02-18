/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.cfg

import java.util.{List ⇒ JList}
import java.util.{Collection ⇒ JCollection}
import java.util.{Set ⇒ JSet}

import scala.collection.JavaConverters._

import heros.InterproceduralCFG

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.DefinedMethod
import org.opalj.br.MultipleDefinedMethods
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.FunctionCall
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.MethodCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

/**
 * The superclass of the forward and backward ICFG for Heros analyses.
 *
 * @param project The project, which is analyzed.
 * @author Mario Trageser
 */
abstract class OpalICFG(project: SomeProject) extends InterproceduralCFG[Statement, Method] {

    val tacai: Method ⇒ TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(LazyDetachedTACAIKey)
    implicit val ps: PropertyStore = project.get(PropertyStoreKey)
    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    override def getMethodOf(stmt: Statement): Method = stmt.method

    override def getPredsOf(stmt: Statement): JList[Statement] = {
        stmt.cfg.predecessors(stmt.index).toChain.map { index ⇒
            Statement(stmt.method, stmt.node, stmt.code(index), index, stmt.code, stmt.cfg)
        }.toList.asJava
    }

    override def getSuccsOf(stmt: Statement): JList[Statement] = {
        stmt.cfg.successors(stmt.index).toChain.map { index ⇒
            Statement(stmt.method, stmt.node, stmt.code(index), index, stmt.code, stmt.cfg)
        }.toList.asJava
    }

    override def getCalleesOfCallAt(callInstr: Statement): JCollection[Method] = {
        val FinalEP(_, callees) = ps(declaredMethods(callInstr.method), Callees.key)
        callees.directCallees(callInstr.stmt.pc).collect {
            case d: DefinedMethod           ⇒ List(d.definedMethod)
            case md: MultipleDefinedMethods ⇒ md.definedMethods
        }.flatten.filter(_.body.isDefined).toList.asJava
    }

    override def getCallersOf(m: Method): JCollection[Statement] = {
        val FinalEP(_, callers) = ps(declaredMethods(m), Callers.key)
        callers.callers.flatMap {
            case (method, pc, true) ⇒
                val TACode(_, code, pcToIndex, cfg, _) = tacai(method.definedMethod)
                val index = pcToIndex(pc)
                Some(Statement(method.definedMethod, cfg.bb(index), code(index), index, code, cfg))
            case _ ⇒ None
        }.toSet.asJava
    }

    override def getCallsFromWithin(m: Method): JSet[Statement] = {
        val TACode(_, code, _, cfg, _) = tacai(m)
        code.zipWithIndex.collect {
            case (mc: MethodCall[V], index) ⇒ Statement(m, cfg.bb(index), mc, index, code, cfg)
            case (as @ Assignment(_, _, _: FunctionCall[V]), index) ⇒
                Statement(m, cfg.bb(index), as, index, code, cfg)
            case (ex @ ExprStmt(_, _: FunctionCall[V]), index) ⇒ Statement(m, cfg.bb(index), ex, index, code, cfg)
        }.toSet.asJava
    }

    override def getReturnSitesOfCallAt(callInstr: Statement): JCollection[Statement] = getSuccsOf(callInstr)

    override def isCallStmt(stmt: Statement): Boolean = {
        def isCallExpr(expr: Expr[V]) = expr.astID match {
            case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                VirtualFunctionCall.ASTID ⇒ true
            case _ ⇒ false
        }

        stmt.stmt.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID ⇒
                true
            case Assignment.ASTID ⇒ isCallExpr(stmt.stmt.asAssignment.expr)
            case ExprStmt.ASTID   ⇒ isCallExpr(stmt.stmt.asExprStmt.expr)
            case _                ⇒ false
        }
    }

    override def isFallThroughSuccessor(stmt: Statement, succ: Statement): Boolean = ???

    override def isBranchTarget(stmt: Statement, succ: Statement): Boolean = ???

}
