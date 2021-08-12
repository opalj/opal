/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A method's line number table.
 *
 * @author Michael Eichberg
 */
trait LineNumberTable extends CodeAttribute {

    def lineNumbers: LineNumbers

    def lookupLineNumber(pc: PC): Option[Int]

    def firstLineNumber(): Option[Int]

    override def remapPCs(codeSize: PC, f: PC => PC): LineNumberTable = {
        UnpackedLineNumberTable(lineNumbers.flatMap[LineNumber](ln => ln.remapPCs(codeSize, f)))
    }

    override def kindId: Int = LineNumberTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: LineNumberTable => this.similar(that)
            case _                     => false
        }
    }

    def similar(other: LineNumberTable): Boolean = {
        val thisLineNumbers = this.lineNumbers
        val otherLineNumbers = other.lineNumbers
        // the order of two line number tables need to be identical
        thisLineNumbers.size == otherLineNumbers.size && thisLineNumbers == otherLineNumbers
    }

}

object LineNumberTable {

    final val KindId = 19

    def unapply(lnt: LineNumberTable): Option[LineNumbers] = Some(lnt.lineNumbers)

}
