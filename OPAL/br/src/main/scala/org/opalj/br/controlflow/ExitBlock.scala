package org.opalj.br.controlflow

import org.opalj.br.Code

case class ExitBlock() extends CFGBlock {

    def toDot(code: Code): String = {

        val res: String = ID+" [shape=box, label=\""+ID+"\"];\n"

        res
    }
}