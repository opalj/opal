/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

/**
 * Representation of a method's code attribute.
 *
 * @author Michael Eichberg
 */
case class Code(
    maxStack: Int,
    maxLocals: Int,
    instructions: Array[Instruction],
    exceptionHandlers: ExceptionHandlers,
    attributes: Attributes)
        extends Attribute {

    /**
     * Collects all line number tables.
     *
     * The JVM specification does not prescribe that there has to be at most one
     * line number table.
     */
    def lineNumberTables: Seq[LineNumbers] =
        attributes collect { case LineNumberTable(lnt) ⇒ lnt }

    /**
     * @param pc The program counter/the index of an instruction in the code array for
     *    which we want to determine the source line.
     */
    def lookupLineNumber(pc: Int): Option[Int] = {
        import scala.util.control.Breaks
        val breaks = new Breaks
        import breaks.{ break, breakable }

        // though the spec explicitly states that a class file can have multiple
        // line number table attributes, we have never seen this in practice...
        val mergedTables = lineNumberTables.flatten
        val sortedTable = mergedTables.sortWith((ltA, ltB) ⇒ ltA.startPC < ltB.startPC)
        val lnsIterator = sortedTable.iterator
        var lastLineNumber: LineNumber = null
        breakable {
            while (lnsIterator.hasNext) {
                var currentLineNumber = lnsIterator.next
                if (currentLineNumber.startPC <= pc) {
                    lastLineNumber = currentLineNumber
                } else {
                    break
                }
            }
        }

        if (lastLineNumber eq null)
            return None
        else
            return Some(lastLineNumber.lineNumber)
    }

    def localVariableTable: Option[LocalVariables] =
        attributes collectFirst { case LocalVariableTable(lvt) ⇒ lvt }

    def localVariableTypeTable: Option[LocalVariableTypes] =
        attributes collectFirst { case LocalVariableTypeTable(lvtt) ⇒ lvtt }

    def stackMapTable: Option[StackMapFrames] =
        attributes collectFirst { case StackMapTable(smf) ⇒ smf }

    /**
     * True if the instruction with the given program counter is modified by wide.
     *
     * @param pc A valid index in the code array.
     */
    def isModifiedByWide(pc: Int): Boolean = pc > 0 && instructions(pc - 1) == WIDE

    /**
     * Collects all instructions for which the given function is defined.
     *
     * ==Usage scenario==
     * Use this function if you want to search for and collect specific instructions, but where you do
     * not immediately require the program counter/index of the instruction in the instruction array.
     *
     * ==Examples==
     * Example usage to collect the declaring class of all get field accesses where the
     * field name is "last".
     * {{{
     * collect({
     *  case GETFIELD(declaringClass, "last", _) ⇒ declaringClass
     * })
     * }}}
     *
     * Example usage to collect all instances of a "DUP" instruction.
     * {{{
     * code.collect({ case dup @ DUP ⇒ dup })
     * }}}
     *
     * @return The result of applying the function f to all instructions for which f is defined combined with
     *  the index (program counter) of the instruction in the code array.
     */
    def collect[B](f: PartialFunction[Instruction, B]): Seq[(Int, B)] = {
        val max_pc = instructions.size
        var pc = 0
        var result: List[(Int, B)] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if (instruction ne null) {
                if (f.isDefinedAt(instruction)) {
                    result = (pc, f(instruction)) :: result
                }
            }
            pc += 1
        }
        result.reverse
    }

    /**
     * Applies the given function `f` to all instruction objects for which the function is
     * defined. The function is passed a tuple consisting of the current program counter/index
     * in the code array and the corresponding instruction.
     *
     * ==Example==
     * Example usage to collect the program counters (indexes) of all instructions that
     * are the target of a conditional branch instruction:
     * {{{
     * code.collectWithIndex({
     *  case (pc, cbi: ConditionalBranchInstruction) ⇒
     *      Seq(cbi.indexOfNextInstruction(pc, code), pc + cbi.branchoffset)
     *  }) // .flatten should equal (Seq(...))
     * }}}
     */
    def collectWithIndex[B](f: PartialFunction[(Int, Instruction), B]): Seq[B] = {
        val max_pc = instructions.size
        var pc = 0
        var result: List[B] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if (instruction ne null) {
                if (f.isDefinedAt((pc, instruction))) {
                    result = f((pc, instruction)) :: result
                }
            }
            pc += 1
        }
        result.reverse
    }

    /**
     * Applies the given function to the first instruction for which the given function is defined.
     */
    def collectFirstWithIndex[B](f: PartialFunction[(Int, Instruction), B]): Option[B] = {
        val max_pc = instructions.size
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if ((instruction ne null) && f.isDefinedAt((pc, instruction))) {
                return Some(f(pc, instruction))
            }
            pc += 1
        }
        return None
    }

    /**
     * Tests if an instruction matches the given filter. If so, the index of the first matching
     * instruction is returned.
     */
    def find(f: Instruction ⇒ Boolean): Option[Int] = {
        val max_pc = instructions.size
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if ((instruction ne null) && f(instruction)) {
                return Some(pc)
            }
            pc += 1
        }
        return None
    }

    /**
     * Returns a new sequence that pairs the program_counter of an instruction with the instruction.
     */
    def associateWithIndex(): Seq[(Int, Instruction)] = collect { case i ⇒ i }

    /**
     * Slides over the code array and tries to apply the given function to each sequence of instructions
     * consisting of `windowSize` elements.
     *
     * ==Scenario==
     * If you want to search for specific patterns of bytecode instructions. Some "bug patterns" are
     * directly related to specific bytecode sequences and these patterns can easily be identified
     * using this method.
     *
     * ==Example==
     * Search for sequences of the bytecode instructions PUTFIELD and ALOAD_O in the methods
     * body and return the list of program counters of the start of the identified sequences.
     * {{{
     * code.slidingCollect(2)({
     *  case (pc, Seq(PUTFIELD(_, _, _), ALOAD_0)) ⇒ (pc)
     * }) should be(Seq(...))
     * }}}
     *
     * @param windowSize The size of the sequence of instructions that is passed to the partial function.
     *      It must be larger than 0. **Do not use this method with windowSize "0"** as it is more efficient
     *      to use the `collect` or `collectWithIndex` methods instead.
     * @return The list of results of applying the function f for each matching sequence.
     */
    def slidingCollect[B](windowSize: Int)(f: PartialFunction[(Int, Seq[Instruction]), B]): Seq[B] = {
        require(windowSize > 0)

        import scala.collection.immutable.Queue

        val max_pc = instructions.size
        var instrs: Queue[Instruction] = Queue.empty
        var firstPC, lastPC = 0
        var elementsInQueue = 0

        @scala.annotation.tailrec def pcOfNextInstruction(currentPC: Int): Int = {
            val nextPC = currentPC + 1
            if (nextPC >= max_pc || (instructions(nextPC) ne null))
                nextPC
            else
                pcOfNextInstruction(nextPC)
        }

        //
        // INITIALIZATION
        //
        while (elementsInQueue < windowSize - 1 && lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))
            lastPC = pcOfNextInstruction(lastPC)
            elementsInQueue += 1
        }

        // 
        // SLIDING OVER THE CODE
        //
        var result: List[B] = List.empty
        while (lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))

            if (f.isDefinedAt((firstPC, instrs))) {
                result = f((firstPC, instrs)) :: result
            }

            firstPC = pcOfNextInstruction(firstPC)
            lastPC = pcOfNextInstruction(lastPC)
            instrs = instrs.tail
        }

        result.reverse
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
