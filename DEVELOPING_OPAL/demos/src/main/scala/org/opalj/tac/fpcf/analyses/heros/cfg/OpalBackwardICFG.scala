/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.cfg

import java.util.{List ⇒ JList}
import java.util.{Collection ⇒ JCollection}
import java.util.{Set ⇒ JSet}
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.TACode

/**
 * A backward ICFG for Heros analyses.
 *
 * @author Mario Trageser
 */
class OpalBackwardICFG(project: SomeProject) extends OpalICFG(project) {

    override def getPredsOf(stmt: Statement): JList[Statement] = super.getSuccsOf(stmt)

    override def getSuccsOf(stmt: Statement): JList[Statement] = super.getPredsOf(stmt)

    override def getStartPointsOf(m: Method): JCollection[Statement] = {
        val TACode(_, code, _, cfg, _) = tacai(m)
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).map {
            case bb: BasicBlock ⇒
                val index = bb.endPC
                Statement(m, bb, code(index), index, code, cfg)
        }.asJava
    }

    override def isExitStmt(stmt: Statement): Boolean = stmt.index == 0

    override def isStartPoint(stmt: Statement): Boolean = {
        val cfg = stmt.cfg
        val index = stmt.index
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).exists {
            case bb: BasicBlock ⇒ bb.endPC == index
        }
    }

    override def allNonCallStartNodes(): JSet[Statement] = {
        val res = new ConcurrentLinkedQueue[Statement]
        project.parForeachMethodWithBody() { mi ⇒
            val m = mi.method
            val TACode(_, code, _, cfg, _) = tacai(m)
            val endIndex = code.length
            val startIndices =
                (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).map {
                    case bb: BasicBlock ⇒ bb.endPC
                }
            var index = 0
            while (index < endIndex) {
                val stmt = code(index)
                val statement = Statement(m, cfg.bb(index), stmt, index, code, cfg)
                if (!(isCallStmt(statement) || startIndices.contains(index)))
                    res.add(statement)
                index += 1
            }
        }
        new java.util.HashSet(res)
        Collections.emptySet()
    }

    def getExitStmt(method: Method): Statement = {
        val tac = tacai(method)
        val cfg = tac.cfg
        val code = tac.stmts
        Statement(method, cfg.startBlock, code(0), 0, code, cfg)
    }
}
