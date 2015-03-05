package org.opalj.br.controlflow

import org.opalj.br.Code
import org.opalj.br.PC
import org.opalj.br.ExceptionHandler

case class CatchBlock(handler: ExceptionHandler) extends CFGBlock {

    final def startPC: PC = handler.startPC
    final def endPC: PC = handler.endPC
    final def handlerPC: PC = handler.handlerPC

    def toDot(code: Code): String = {
        var res: String = ID+" [shape=box, label=\""+ID+"\"];\n"

        for (succ ← successors) {
            res = res + ID+" -> "+succ.ID+";\n"
        }

        res
    }
}