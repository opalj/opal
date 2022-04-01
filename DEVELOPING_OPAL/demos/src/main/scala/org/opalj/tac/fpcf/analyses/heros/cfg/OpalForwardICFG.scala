/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.cfg

import java.util.{Collection ⇒ JCollection}
import java.util.{Set ⇒ JSet}
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement

/**
 * A forward ICFG for Heros analyses.
 *
 * @author Mario Trageser
 */
class OpalForwardICFG(project: SomeProject) extends OpalICFG(project) {

    override def getStartPointsOf(m: Method): JCollection[JavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(m)
        Collections.singletonList(JavaStatement(m, 0, code, cfg))
    }

    override def isExitStmt(stmt: JavaStatement): Boolean = stmt.cfg.bb(stmt.index).successors.exists(_.isExitNode)

    override def isStartPoint(stmt: JavaStatement): Boolean = stmt.index == 0

    override def allNonCallStartNodes(): JSet[JavaStatement] = {
        val res = new ConcurrentLinkedQueue[JavaStatement]
        project.parForeachMethodWithBody() { mi ⇒
            val m = mi.method
            val TACode(_, code, _, cfg, _) = tacai(m)
            val endIndex = code.length
            var index = 1
            while (index < endIndex) {
                val statement = JavaStatement(m, index, code, cfg)
                if (!isCallStmt(statement))
                    res.add(statement)
                index += 1
            }
        }
        new java.util.HashSet(res)
        Collections.emptySet()
    }

    def getExitStmts(method: Method): Iterator[JavaStatement] = {
        val tac = tacai(method)
        val cfg = tac.cfg
        val code = tac.stmts
        cfg.allNodes.filter(_.isExitNode).flatMap(_.predecessors).map { bb ⇒
            val endPc = bb.asBasicBlock.endPC
            JavaStatement(method, endPc, code, cfg)
        }
    }

}