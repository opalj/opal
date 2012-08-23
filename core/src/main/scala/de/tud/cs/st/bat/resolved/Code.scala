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
case class Code(maxStack: Int,
                maxLocals: Int,
                instructions: Array[Instruction],
                exceptionHandlers: ExceptionHandlers,
                attributes: Attributes)
        extends Attribute {

    def lineNumberTable: Option[LineNumbers] =
        attributes collectFirst { case LineNumberTable(lnt) ⇒ lnt }

    def localVariableTable: Option[LocalVariables] =
        attributes collectFirst { case LocalVariableTable(lvt) ⇒ lvt }

    def localVariableTypeTable: Option[LocalVariableTypes] =
        attributes collectFirst { case LocalVariableTypeTable(lvtt) ⇒ lvtt }

    def stackMapTable: Option[StackMapFrames] =
        attributes collectFirst { case StackMapTable(smf) ⇒ smf }

    def isModifiedByWide(pc: Int): Boolean = {
        pc > 0 && instructions(pc - 1) == WIDE
    }

    // TODO implement CFG algorithm
    case class BBInfo private[Code] (
        val bbID: Int,
        val firstInstructionPC: Int,
        val lastInstructionPC: Int,
        val succBBIDs: List[Int],
        val predBBIDs: List[Int],
        val handlesException: Option[ReferenceType] = None)

    /**
      * @param instrToBBID Associates each instruction (by means of its program counter) with the ID of its
      * associated basic block.
      */
    // TODO implement CFG algorithm
    case class CFG private[Code] (
            val instrToBBID: IndexedSeq[Int],
            val bbInfo: IndexedSeq[BBInfo],
            val exitBBIDs: Seq[Int]) {
    }

    /**
      * The CFG is calculated under a certain assumption.
      */
    def cfg = {
        //    /**
        //      * The indexes/program counters of instructions that are (potentially) executed after this
        //      * instruction at runtime. (I.e., this is (in case of control transfer instructions) not the
        //      * index of the next instruction in the code array).
        //      *
        //      * @param currentPC The current pc; i.e., the current index in the code array.
        //      * @param code This instruction's code block. This information is required to determine the next
        //      * instruction, e.g., in case of a throw instruction or a "wide instruction".
        //      */
        //    def successorPCs(currentPC: Int, code: Code): Traversable[Int]

        // ATHROW
        //        def successorPCs(currentPC: Int, code: Code): Traversable[Int] = {
        //            for (exceptionHandler ← code.exceptionHandlers if exceptionHandler.startPC <= currentPC and exceptionHandler.endPC > currentPC) {
        //
        //            }
        //        }

        // ConditionalBranchInstruction
        //        def successorPCs(currentPC: Int, code: Code): Traversable[Int] = {
        //            new Traversable[Int] {
        //                def foreach[U](f: Int ⇒ U) {
        //                    f(currentPC + 1)
        //                    f(currentPC + branchoffset)
        //                }
        //            }
        //        }

        // UnconditionalBranchInstruction
        // def successorPCs(currentPC: Int, code: Code): Traversable[Int] = List(currentPC + branchoffset)

        // LOOKUPSWITCH
        //        def successorPCs(currentPC: Int, code: Code): Traversable[Int] = {
        //            new Traversable[Int] {
        //                def foreach[U](f: Int ⇒ U) {
        //                    f(currentPC + defaultOffset)
        //                    npairs.foreach(kv ⇒ f(currentPC + kv._2))
        //                }
        //            }
        //        }

        // TABLESWITCH
        //        def successorPCs(currentPC: Int, code: Code): Traversable[Int] = {
        //        new Traversable[Int] {
        //            def foreach[U](f: Int ⇒ U) {
        //                f(currentPC + defaultOffset)
        //                jumpOffsets.foreach(offset ⇒ f(currentPC + offset))
        //            }
        //        }
        //    }
    }

    override def toString = {
        "Code_attribute(maxStack="+
            maxStack+", maxLocals="+
            maxLocals+","+
            (instructions.filter(_ ne null).deep.toString) +
            (exceptionHandlers.toString)+","+
            (attributes.toString)+
            ")"
    }

}
