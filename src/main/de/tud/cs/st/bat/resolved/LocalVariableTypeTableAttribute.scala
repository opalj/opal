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
 * Representation of the local variable type table.
 *
 * @author Michael Eichberg
 */
case class LocalVariableTypeTableAttribute(val localVariableTypeTable: LocalVariableTypeTable)
        extends Attribute {

    def toXML =
        <local_variable_type_table>
			{ for (entry ← localVariableTypeTable) yield entry.toXML }
		</local_variable_type_table>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A], declaringEntityKey: A): List[F] =
        sys.error("Not supported; use toProlog(PrologTermFactory,Atom,Array[Int]) instead.")

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A], declaringEntityKey: A, pc_to_seqNo: Array[Int]): F = {
    	factory.Fact("method_local_variable_type_table") // TODO complete support
    }
}

case class LocalVariableTypeTableEntry(
        val startPC: Int,
        val length: Int,
        val name: String,
        val signature: FieldTypeSignature,
        val index: Int) {

    def toXML =
        <entry
			signature={ signature.toXML }
			start_pc={ startPC.toString }
			length={ length.toString }
			name={ name }
			index={ index.toString }/>

}

