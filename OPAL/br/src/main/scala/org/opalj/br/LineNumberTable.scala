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
