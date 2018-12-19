/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.util.{List ⇒ JList}
import java.util.{Collection ⇒ JCollection}
import java.util.{Set ⇒ JSet}
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import heros.InterproceduralCFG

import org.opalj.fpcf.analyses.Statement
import org.opalj.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectLike
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.MethodCall
import org.opalj.tac.FunctionCall
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DUVar
import org.opalj.tac.TACode

class OpalICFG(project: SomeProject) extends InterproceduralCFG[Statement, Method] {

    val tacai: Method ⇒ TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(DefaultTACAIKey)

    //    val cfgs: ConcurrentHashMap[Method, CFG] = {
    //        val cfgs = new ConcurrentHashMap[Method, CFG]
    //        cg.project.parForeachMethodWithBody() { methodInfo ⇒
    //            val m = methodInfo.method
    //            cfgs.put(m, CFGFactory(m.body.get, cg.project.classHierarchy))
    //        }
    //        cfgs
    //    }

    def getMethodOf(stmt: Statement): Method = {
        stmt.method
    }

    //    def getPredsOf(instr: MInstruction): JList[MInstruction] = {
    //        if (instr.pc == 0)
    //            return Collections.emptyList()
    //
    //        val bb = cfgs.get(instr.m).bb(instr.pc)
    //        if (bb.startPC == instr.pc) {
    //            val prevInstrPCs = bb.predecessors.flatMap {
    //                case cn: CatchNode  ⇒ cn.predecessors.map(_.asBasicBlock)
    //                case bb: BasicBlock ⇒ Seq(bb)
    //            }.map(_.endPC)
    //            prevInstrPCs.map { prevInstrPC ⇒
    //                MInstruction(instr.m.body.get.instructions(prevInstrPC), prevInstrPC, instr.m)
    //            }.toList.asJava
    //        } else {
    //            val prevInstrPC = instr.m.body.get.pcOfPreviousInstruction(instr.pc)
    //            Collections.singletonList(MInstruction(instr.m.body.get.instructions(prevInstrPC), prevInstrPC, instr.m))
    //        }
    //    }

    def getPredsOf(stmt: Statement): JList[Statement] = {
        stmt.cfg.predecessors(stmt.index).toChain.map { index ⇒
            Statement(stmt.method, stmt.code(index), index, stmt.code, stmt.cfg)
        }.toList.asJava
    }

    //    def getSuccsOf(instr: MInstruction): JList[MInstruction] = {
    //        val pcs = cfgs.get(instr.m).successors(instr.pc)
    //        pcs.map { succPc ⇒
    //            MInstruction(instr.m.body.get.instructions(succPc), succPc, instr.m)
    //        }.toList.asJava
    //    }

    def getSuccsOf(stmt: Statement): JList[Statement] = {
        stmt.cfg.successors(stmt.index).toChain.map { index ⇒
            Statement(stmt.method, stmt.code(index), index, stmt.code, stmt.cfg)
        }.toList.asJava
    }

    def getCalleesOfCallAt(callInstr: Statement): JCollection[Method] = {
        val stmt = callInstr.stmt
        val declClass = callInstr.method.classFile.thisType
        implicit val p: ProjectLike = project
        (stmt.astID match {
            case StaticMethodCall.ASTID ⇒
                stmt.asStaticMethodCall.resolveCallTarget.toSet.filter(_.body.isDefined)

            case NonVirtualMethodCall.ASTID ⇒
                stmt.asNonVirtualMethodCall.resolveCallTarget(declClass).toSet.filter(_.body.isDefined)

            case VirtualMethodCall.ASTID ⇒
                stmt.asVirtualMethodCall.resolveCallTargets(declClass).filter(_.body.isDefined)

            case Assignment.ASTID if expr(stmt).astID == StaticFunctionCall.ASTID ⇒
                stmt.asAssignment.expr.asStaticFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined)

            case Assignment.ASTID if expr(stmt).astID == NonVirtualFunctionCall.ASTID ⇒
                stmt.asAssignment.expr.asNonVirtualFunctionCall.resolveCallTarget(declClass).toSet.filter(_.body.isDefined)

            case Assignment.ASTID if expr(stmt).astID == VirtualFunctionCall.ASTID ⇒
                stmt.asAssignment.expr.asVirtualFunctionCall.resolveCallTargets(declClass).filter(_.body.isDefined)

            case ExprStmt.ASTID if expr(stmt).astID == StaticFunctionCall.ASTID ⇒
                stmt.asExprStmt.expr.asStaticFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined)

            case ExprStmt.ASTID if expr(stmt).astID == NonVirtualFunctionCall.ASTID ⇒
                stmt.asExprStmt.expr.asNonVirtualFunctionCall.resolveCallTarget(declClass).toSet.filter(_.body.isDefined)

            case ExprStmt.ASTID if expr(stmt).astID == VirtualFunctionCall.ASTID ⇒
                stmt.asExprStmt.expr.asVirtualFunctionCall.resolveCallTargets(declClass).filter(_.body.isDefined)

            case _ ⇒ throw new RuntimeException("Unexpected type")
        }).asJava
    }

    /** Gets the expression from an assingment/expr statement. */
    def expr(stmt: Stmt[V]): Expr[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
    }

    def getCallersOf(m: Method): JCollection[Statement] = ???

    def getCallsFromWithin(m: Method): JSet[Statement] = {
        val TACode(_, code, _, cfg, _, _) = tacai(m)
        code.zipWithIndex.collect {
            case (mc: MethodCall[V], index) ⇒ Statement(m, mc, index, code, cfg)
            case (as @ Assignment(_, _, _: FunctionCall[V]), index) ⇒
                Statement(m, as, index, code, cfg)
            case (ex @ ExprStmt(_, _: FunctionCall[V]), index) ⇒ Statement(m, ex, index, code, cfg)
        }.toSet.asJava
    }

    def getStartPointsOf(m: Method): JCollection[Statement] = {
        val TACode(_, code, _, cfg, _, _) = tacai(m)
        Collections.singletonList(Statement(m, code(0), 0, code, cfg))
    }

    def getReturnSitesOfCallAt(callInstr: Statement): JCollection[Statement] = {
        getSuccsOf(callInstr)
    }

    def isCallStmt(stmt: Statement): Boolean = {
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

    def isExitStmt(stmt: Statement): Boolean = {
        getSuccsOf(stmt).isEmpty
    }

    def isStartPoint(stmt: Statement): Boolean = {
        stmt.index == 0
    }

    def allNonCallStartNodes(): JSet[Statement] = {
        val res = new ConcurrentLinkedQueue[Statement]
        project.parForeachMethodWithBody() { mi ⇒
            val m = mi.method
            val TACode(_, code, _, cfg, _, _) = tacai(m)
            val endIndex = code.length
            var index = 1
            while (index < endIndex) {
                val stmt = code(index)
                val statement = Statement(m, stmt, index, code, cfg)
                if (!isCallStmt(statement))
                    res.add(statement)
                index += 1
            }
        }
        new java.util.HashSet(res)
        Collections.emptySet()
    }

    def isFallThroughSuccessor(stmt: Statement, succ: Statement): Boolean = ???

    def isBranchTarget(stmt: Statement, succ: Statement): Boolean = ???

}