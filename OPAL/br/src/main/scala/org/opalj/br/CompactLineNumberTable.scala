/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.util.{Arrays => JArrays}
import scala.collection.immutable.ArraySeq

/**
 * A method's line number table.
 *
 * @param   rawLineNumbers
 *          <pre>
 *          LineNumberTable_attribute {
 *              DATA:
 *              {   u2 start_pc;
 *                  u2 line_number;
 *              }
 *          }
 *          </pre>
 *          The array must not be mutated by callers!
 *
 * @author Michael Eichberg
 */
case class CompactLineNumberTable(rawLineNumbers: Array[Byte]) extends LineNumberTable {

    def lineNumbers: LineNumbers = {
        val lineNumbersBuilder = ArraySeq.newBuilder[LineNumber]
        var e = 0
        val entries = rawLineNumbers.length / 4
        while (e < entries) {
            val index = e * 4
            val startPC = asUnsignedShort(rawLineNumbers(index), rawLineNumbers(index + 1))
            val lineNumber = asUnsignedShort(rawLineNumbers(index + 2), rawLineNumbers(index + 3))
            lineNumbersBuilder += LineNumber(startPC, lineNumber)
            e += 1
        }
        lineNumbersBuilder.result()
    }

    def asUnsignedShort(hb: Byte, lb: Byte): Int = {
        ((hb & 0xFF) << 8) | (lb & 0xFF) // cf. DataInput.readUnsignedShort
    }

    def lookupLineNumber(pc: PC): Option[Int] = {
        var lastLineNumber = -1
        var e = 0
        val entries = rawLineNumbers.length / 4
        while (e < entries) {
            val index = e * 4
            val startPC = asUnsignedShort(rawLineNumbers(index), rawLineNumbers(index + 1))
            if (startPC <= pc) {
                lastLineNumber = asUnsignedShort(rawLineNumbers(index + 2), rawLineNumbers(index + 3))
            } else if (lastLineNumber == -1) {
                return None;
            } else {
                return Some(lastLineNumber);
            }
            e += 1
        }
        if (lastLineNumber == -1) None else Some(lastLineNumber)
    }

    def firstLineNumber(): Option[Int] = {
        if (rawLineNumbers.length == 0)
            return None;

        val raw_pc_lns = rawLineNumbers.grouped(2).zipWithIndex.filter(_._2 % 2 == 1)
        val raw_lns = raw_pc_lns.map(_._1)
        val lns =
            raw_lns map { bytes =>
                val hb = bytes(0); val lb = bytes(1); asUnsignedShort(hb, lb)
            }
        Some(lns.min)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: CompactLineNumberTable =>
                JArrays.equals(this.rawLineNumbers, that.rawLineNumbers)
            case _ =>
                false
        }
    }

    override def hashCode(): Opcode = JArrays.hashCode(rawLineNumbers)

    override def toString: String = lineNumbers.mkString("CompactLineNumber({ ", ", ", " })")

}
