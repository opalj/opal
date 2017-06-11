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
import org.opalj.br.cfg.CFG

/**
 * Representation of the 3-address code of a method.
 *
 * == Attributes ==
 * The following attributes are directly reused: LineNumberTableAttribute; the statements keep
 * the reference to the underlying/original instruction which is used to retrieve the respective
 * information.
 *
 * @author Michael Eichberg
 */
case class TACode[V <: Var[V]](
        stmts:             Array[Stmt[V]], // CONST!!!
        cfg:               CFG,
        exceptionHandlers: ExceptionHandlers,
        lineNumberTable:   Option[LineNumberTable]
// TODO Support the rewriting of TypeAnnotations etc.
) extends Attribute {

    def kindId: Int = TACode.KindId

    def similar(other: Attribute): Boolean = this equals other

    def firstLineNumber: Option[Int] = lineNumberTable.flatMap(_.firstLineNumber())

    def lineNumber(index: Int): Option[Int] = {
        lineNumberTable.flatMap(_.lookupLineNumber(stmts(index).pc))
    }

    override def toString: String = {
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
        s"TACode($txtStmts,$cfg$txtExceptionHandlers$txtLineNumbers)"
    }

}
object TACode {

    final val KindId = 1003

}

