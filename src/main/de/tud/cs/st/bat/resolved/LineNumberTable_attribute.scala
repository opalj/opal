/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved

/**
 * A method's line number table.
 *
 * @author Michael Eichberg
 */
case class LineNumberTable_attribute(val lineNumberTable: LineNumberTable)
        extends Attribute {

    def toXML =
        <line_number_table>
			{ for (entry ← lineNumberTable) yield entry.toXML }
		</line_number_table>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A], declaringEntityKey: A): List[F] =
        sys.error("Not supported; use toProlog(PrologTermFactory,Atom,Array[Int]) instead.")

    def toProlog[F, T, A <: T](
        factory: PrologTermFactory[F, T, A],
        declaringEntityKey: A,
        pc_to_seqNo: Array[Int]): F = {

        import factory._

        Fact(
            "method_line_number_table",
            declaringEntityKey,
            Terms(lineNumberTable, (_: LineNumberTableEntry).toProlog(factory, pc_to_seqNo))
        )
    }
}

case class LineNumberTableEntry(val startPC: Int,
                                val lineNumber: Int) {

    def toXML = <entry start_pc={ startPC.toString } lineNumber={ lineNumber.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A], pc_to_seqNo: Array[Int]): T = {
        import factory._
        Term("kv", IntegerAtom(pc_to_seqNo(startPC)), Term("ln", IntegerAtom(lineNumber)))
    }
}