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
package br

/**
 * A method's line number table.
 *
 * @author Michael Eichberg
 */
trait LineNumberTable extends CodeAttribute {

    def lineNumbers: LineNumbers

    def lookupLineNumber(pc: PC): Option[Int] // IMPROVE [L2] Define and use IntOption

    def firstLineNumber(): Option[Int] // IMPROVE [L2] Define and use IntOption

    override def kindId: Int = LineNumberTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: LineNumberTable ⇒ this.similar(that)
            case _                     ⇒ false
        }
    }

    def similar(other: LineNumberTable): Boolean = {
        val thisLineNumbers = this.lineNumbers
        val otherLineNumbers = other.lineNumbers
        // the order of two line number tables need to be identical
        thisLineNumbers.size == otherLineNumbers.size &&
            thisLineNumbers == otherLineNumbers
    }

    override def remapPCs(codeSize: PC, f: PC ⇒ PC): LineNumberTable = {
        val newLineNumbers = List.newBuilder[LineNumber]
        lineNumbers.foreach { ln ⇒
            val newLNOption = ln.remapPCs(codeSize, f)
            if (newLNOption.isDefined) newLineNumbers += newLNOption.get
        }
        UnpackedLineNumberTable(newLineNumbers.result())
    }
}

object LineNumberTable {

    final val KindId = 19

    def unapply(lnt: LineNumberTable): Option[LineNumbers] = Some(lnt.lineNumbers)

}
