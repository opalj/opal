/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM._
import org.opalj.graphs.Node
import org.opalj.ll.llvm.value

case class BasicBlock(block_ref: LLVMBasicBlockRef)
    extends Value(LLVMBasicBlockAsValue(block_ref))
    with Node {
    def parent = Function(LLVMGetBasicBlockParent(block_ref))
    def instructions: InstructionIterator = new InstructionIterator(LLVMGetFirstInstruction(block_ref))
    def firstInstruction: Instruction = Instruction(LLVMGetFirstInstruction(block_ref))
    def lastInstruction: Instruction = Instruction(LLVMGetLastInstruction(block_ref))

    def terminator(): Option[Instruction with value.Terminator] = {
        OptionalInstruction(LLVMGetBasicBlockTerminator(block_ref)) match {
            case Some(terminator) ⇒ {
                assert(terminator.isTerminator)
                Some(terminator.asInstanceOf[Instruction with value.Terminator])
            }
            case None ⇒ None
        }
    }

    def blockName(): String = LLVMGetBasicBlockName(block_ref).getString

    /**
     * Returns a human readable representation (HRR) of this node.
     */
    override def toHRR: Option[String] = {
        Some(name) //TODO: maybe add more info
    }

    /**
     * An identifier that uniquely identifies this node in the graph to which this
     * node belongs. By default two nodes are considered equal if they have the same
     * unique id.
     */
    override def nodeId: Int = {
        block_ref.hashCode
    }

    /**
     * Returns `true` if this node has successor nodes.
     */
    override def hasSuccessors: Boolean = terminator match {
        case Some(t) ⇒ t.hasSuccessors
        case None    ⇒ false
    }

    /**
     * Applies the given function for each successor node.
     */
    override def foreachSuccessor(f: Node ⇒ Unit): Unit = terminator match {
        case Some(t) ⇒ t.foreachSuccessor(f)
        case None    ⇒
    }
}

class InstructionIterator(var ref: LLVMValueRef) extends Iterator[Instruction] {
    override def hasNext: Boolean = LLVMGetNextInstruction(ref) != null

    override def next(): Instruction = {
        val instruction = Instruction(ref)
        this.ref = LLVMGetNextInstruction(ref)
        instruction
    }
}
