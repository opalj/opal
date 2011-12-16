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
package de.tud.cs.st.bat.resolved

/**
 * Representation of a method's code attribute.
 *
 * @author Michael Eichberg
 */
case class CodeAttribute(maxStack: Int,
                         maxLocals: Int,
                         code: Array[Instruction],
                         exceptionTable: ExceptionTable,
                         attributes: Attributes)
        extends Attribute {

    def lineNumberTable: Option[LineNumberTable] =
        attributes collectFirst { case LineNumberTableAttribute(lnt) ⇒ lnt }

    def localVariableTable: Option[LocalVariableTable] =
        attributes collectFirst { case LocalVariableTableAttribute(lvt) ⇒ lvt }

    def localVariableTypeTable: Option[LocalVariableTypeTable] =
        attributes collectFirst { case LocalVariableTypeTableAttribute(lvtt) ⇒ lvtt }

    def stackMapTable: Option[StackMapFrames] =
        attributes collectFirst { case StackMapTableAttribute(smf) ⇒ smf }

    override def toString = {
        "Code_attribute(maxStack="+
            maxStack+", maxLocals="+
            maxLocals+","+
            (code.filter(_ ne null).deep.toString) +
            (exceptionTable.toString)+","+
            (attributes.toString)+
            ")"
    }

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

	def toXML =
		<code
			max_stack={ maxStack.toString }
			max_locals={ maxLocals.toString } >
			<attributes>{ for (attribute <- attributes) yield attribute.toXML }</attributes>
			<exception_handlers>{ for (entry <- exceptionTable) yield entry.toXML }</exception_handlers>
			<instructions>
			{
				var instrs : List[scala.xml.Node] = Nil
				var i = 0
				while (i < code.length){
					if (code(i) != null) {
						instrs = code(i).toXML(i) :: instrs
					}
					i += 1
				}
				instrs.reverse
			}
			</instructions>
		</code>


	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A], declaringEntityKey : A) : List[F] = {

		import factory._

		// 1. create a "map" that associates each instruction's PC with the
		// 	a logical number (ignoring wide)
		val pc_to_seqNo : Array[Int] = new Array[Int](code.size)

		{	// the following two variables have only "local" scope
			var pc = 0
			var index = 0
			while (pc < code.length){
				pc_to_seqNo(pc) = index
				if (pc == 0) {
					index += 1
				}
				else if (code(pc) != null) {
					if (code(pc-1) == null || code(pc-1).opcode != WIDE.opcode) {
				 		// if an instruction is modified by "wide" then the jump target must be the wide instruction!
						index += 1
					}
				}
				pc += 1
			}
		}

		var facts : List[F] = Nil

		// 2. get the prolog representation of all relevant instructions
		{	// the following two variables have only "local" scope
			var pc = code.length-1
			while (pc >= 0 ){
				if (code(pc) != null && code(pc).opcode != WIDE.opcode) {
					facts = code(pc).toProlog(
						factory,
						declaringEntityKey,
						pc,
						pc_to_seqNo
					) :: facts
				}
				pc -= 1
			}
		}

		// 3. get the prolog representation of all relevant attributes
		for (attribute <- attributes) {
			 attribute match {
				case lnta : LineNumberTableAttribute =>
				 	facts = lnta.toProlog(factory,declaringEntityKey,pc_to_seqNo) :: facts
				case lvta : LocalVariableTableAttribute =>
				 	facts = lvta.toProlog(factory,declaringEntityKey,pc_to_seqNo) :: facts
				case lvtta : LocalVariableTypeTableAttribute =>
				 	facts = lvtta.toProlog(factory,declaringEntityKey,pc_to_seqNo) :: facts
				case _ => Nil
			}
		}

		// 4. represent the "MethodExceptionsTable"
		if (exceptionTable != null && exceptionTable.size > 0){
			facts = Fact(
				"method_exceptions_table",
				declaringEntityKey,
				Terms(
					exceptionTable,
					(_:ExceptionHandler).toProlog(factory,pc_to_seqNo)
				)
			) :: facts
		}

		facts
	}



}
