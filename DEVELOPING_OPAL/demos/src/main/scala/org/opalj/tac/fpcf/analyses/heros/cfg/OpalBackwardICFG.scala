/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.cfg

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.BasicBlock
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.{Collections, Collection => JCollection, List => JList, Set => JSet}
import scala.jdk.CollectionConverters._

/**
 * A backward ICFG for Heros analyses.
 *
 * @author Mario Trageser
 */
class OpalBackwardICFG(project: SomeProject) extends OpalICFG(project) {

    override def getPredsOf(stmt: JavaStatement): JList[JavaStatement] = super.getSuccsOf(stmt)

    override def getSuccsOf(stmt: JavaStatement): JList[JavaStatement] = super.getPredsOf(stmt)

    override def getStartPointsOf(m: Method): JCollection[JavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(m)
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).map {
            case bb: BasicBlock =>
                val index = bb.endPC
                JavaStatement(m, index, code, cfg)
        }.asJava
    }

    override def isExitStmt(stmt: JavaStatement): Boolean = stmt.index == 0

    override def isStartPoint(stmt: JavaStatement): Boolean = {
        val cfg = stmt.cfg
        val index = stmt.index
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).exists {
            case bb: BasicBlock => bb.endPC == index
        }
    }

    override def allNonCallStartNodes(): JSet[JavaStatement] = {
        val res = new ConcurrentLinkedQueue[JavaStatement]
        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            val TACode(_, code, _, cfg, _) = tacai(m)
            val endIndex = code.length
            val startIndices =
                (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).map {
                    case bb: BasicBlock => bb.endPC
                }
            var index = 0
            while (index < endIndex) {
                val statement = JavaStatement(m, index, code, cfg)
                if (!(isCallStmt(statement) || startIndices.contains(index)))
                    res.add(statement)
                index += 1
            }
        }
        new java.util.HashSet(res)
        Collections.emptySet()
    }

    def getExitStmt(method: Method): JavaStatement = {
        val tac = tacai(method)
        val cfg = tac.cfg
        val code = tac.stmts
        JavaStatement(method, 0, code, cfg)
    }
}
