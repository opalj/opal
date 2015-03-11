package org.opalj.br.cfg

import scala.collection.immutable.HashSet
import org.opalj.br.Code

trait CFGBlock {

    var predecessors: List[CFGBlock] = Nil

    var successors: List[CFGBlock] = Nil

    var ID: String = _

    def addPred(block: CFGBlock): Unit = predecessors = predecessors :+ block

    def addSucc(block: CFGBlock): Unit = successors = successors :+ block

    def returnAllBlocks(visited: HashSet[CFGBlock]): HashSet[CFGBlock] = {
        var res: HashSet[CFGBlock] = visited + this
        for (block ← successors if (!visited.contains(block)))
            res = res ++ block.returnAllBlocks(res)
        res
    }

    def toDot(code: Code): String
}