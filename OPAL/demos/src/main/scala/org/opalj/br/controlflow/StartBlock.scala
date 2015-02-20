package org.opalj.br.controlflow

import org.opalj.br.Code

case class StartBlock() extends CFGBlock {

    def toDot(code: Code): String = {

        var res: String = ID+" [shape=box, label=\""+ID+"\"];\n"

        for (succ ← successors) {
            res = res + ID+" -> "+succ.ID+";\n"
        }
        res
    }
}