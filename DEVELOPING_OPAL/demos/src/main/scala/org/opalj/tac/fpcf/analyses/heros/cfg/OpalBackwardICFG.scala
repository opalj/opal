/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.cfg

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.BasicBlock
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.NewJavaStatement

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.{Collections, Collection ⇒ JCollection, List ⇒ JList, Set ⇒ JSet}
import scala.collection.JavaConverters._

/**
 * A backward ICFG for Heros analyses.
 *
 * @author Mario Trageser
 */
class OpalBackwardICFG(project: SomeProject) extends OpalICFG(project) {

    override def getPredsOf(stmt: NewJavaStatement): JList[NewJavaStatement] = super.getSuccsOf(stmt)

    override def getSuccsOf(stmt: NewJavaStatement): JList[NewJavaStatement] = super.getPredsOf(stmt)

    override def getStartPointsOf(m: Method): JCollection[NewJavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(m)
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).map {
            case bb: BasicBlock ⇒
                val index = bb.endPC
                NewJavaStatement(m, index, code, cfg)
        }.asJava
    }

    override def isExitStmt(stmt: NewJavaStatement): Boolean = stmt.index == 0

    override def isStartPoint(stmt: NewJavaStatement): Boolean = {
        val cfg = stmt.cfg
        val index = stmt.index
        (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors).exists {
            case bb: BasicBlock ⇒ bb.endPC == index
        }
    }

    override def allNonCallStartNodes(): JSet[NewJavaStatement] = {
        val res = new ConcurrentLinkedQueue[NewJavaStatement]
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
                val statement = NewJavaStatement(m, index, code, cfg)
                if (!(isCallStmt(statement) || startIndices.contains(index)))
                    res.add(statement)
                index += 1
            }
        }
        new java.util.HashSet(res)
        Collections.emptySet()
    }

    def getExitStmt(method: Method): NewJavaStatement = {
        val tac = tacai(method)
        val cfg = tac.cfg
        val code = tac.stmts
        NewJavaStatement(method, 0, code, cfg)
    }
}
