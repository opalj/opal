/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj

import org.opalj.br.cfg.{BasicBlock, CFG}
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.CFG
import org.opalj.graphs.Node

/**
 * Common definitions related to the definition and processing of three address code.
 *
 * @author Michael Eichberg
 */
package object tac {

    final val AllTACNaiveOptimizations: List[TACOptimization[IdBasedVar]] = {
        List(SimplePropagation)
    }

    def tacToGraph[V <: Var[V]](stmts: Array[Stmt[V]], cfg: CFG): Iterable[Node] = {
        cfg.toDot {
            case cn: CatchNode ⇒ cn.toString
            case bb: BasicBlock ⇒
                val bbStmts = stmts.slice(bb.startPC, bb.endPC + 1)
                val txtStmts = bbStmts.map { stmt ⇒ ToTxt.toTxtStmt[V](stmt) }
                txtStmts.mkString("", "\\l\\l", "\\l")
        }
    }

    def tacToDot[V <: Var[V]](stmts: Array[Stmt[V]], cfg: CFG): String = {
        org.opalj.graphs.toDot(tacToGraph(stmts, cfg))
    }

}
