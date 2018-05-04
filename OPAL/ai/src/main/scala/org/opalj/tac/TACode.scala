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
package tac

import org.opalj.br.Attribute
import org.opalj.br.ExceptionHandlers
import org.opalj.br.LineNumberTable
import org.opalj.br.SimilarityTestConfiguration
import org.opalj.br.cfg.CFG

/**
 * Contains the 3-address code of a method.
 *
 * == Attributes ==
 * The following code attributes are `directly` reused (i.e., the PCs are not transformed):
 * - LineNumberTableAttribute; the statements keep the reference to the underlying/original
 *   instruction which is used to retrieve the respective information.
 *
 * @param params The variables which store the method's explicit and implicit (`this` in case
 *               of an instance method) parameters.
 *               In case of the ai-based representation (TACAI - default representation),
 *               the variables are returned which store (the initial) parameters. If these variables
 *               are written and we have a loop which includes the very first instruction, the
 *               value will reflect this usage.
 *               In case of the naive representation it "just" contains the names of the
 *               registers which store the parameters.
 * @author Michael Eichberg
 */
case class TACode[P <: AnyRef, V <: Var[V]](
        params:            Parameters[P],
        stmts:             Array[Stmt[V]], // CONST
        cfg:               CFG[Stmt[V], TACStmts[V]],
        exceptionHandlers: ExceptionHandlers,
        lineNumberTable:   Option[LineNumberTable]
// TODO Support the rewriting of TypeAnnotations etc.
) extends Attribute {

    override def kindId: Int = TACode.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this equals other
    }

    def firstLineNumber: Option[Int] = lineNumberTable.flatMap(_.firstLineNumber()) // IMPROVE Use IntOption

    def lineNumber(index: Int): Option[Int] = { // IMPROVE Use IntOption
        lineNumberTable.flatMap(_.lookupLineNumber(stmts(index).pc))
    }

    override def toString: String = {
        val txtParams = s"params=($params)"
        val stmtsWithIndex = stmts.iterator.zipWithIndex.map { e ⇒ val (s, i) = e; s"$i: $s" }
        val txtStmts = stmtsWithIndex.mkString("stmts=(\n\t", ",\n\t", "\n)")
        val txtExceptionHandlers =
            if (exceptionHandlers.nonEmpty)
                exceptionHandlers.mkString(",exceptionHandlers=(\n\t", ",\n\t", "\n)")
            else
                ""
        val txtLineNumbers =
            lineNumberTable match {
                case Some(lnt) ⇒ lnt.lineNumbers.mkString(",lineNumberTable=(\n\t", ",\n\t", "\n)")
                case None      ⇒ ""
            }
        s"TACode($txtParams,$txtStmts,cfg=$cfg$txtExceptionHandlers$txtLineNumbers)"
    }

}
object TACode {

    final val KindId = 1003

}
