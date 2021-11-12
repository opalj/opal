/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{
  LLVMBasicBlockAsValue,
  LLVMGetBasicBlockName,
  LLVMGetBasicBlockTerminator,
  LLVMGetFirstInstruction,
  LLVMGetNextInstruction
}
import org.opalj.graphs.Node

case class BasicBlock(block_ref: LLVMBasicBlockRef)
    extends Value(LLVMBasicBlockAsValue(block_ref))
    with Node {
  def instructions(): InstructionIterator = {
    new InstructionIterator(LLVMGetFirstInstruction(block_ref))
  }

  def terminator(): Terminator = {
    val instruction = Instruction(LLVMGetBasicBlockTerminator(block_ref))
    assert(instruction.is_terminator())
    instruction.asInstanceOf[Terminator]
  }

  def blockName(): String = LLVMGetBasicBlockName(block_ref).getString

  /**
   * Returns a human readable representation (HRR) of this node.
   */
  override def toHRR: Option[String] = {
    Some(name()) //TODO: maybe add more info
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
  override def hasSuccessors: Boolean = {
    terminator.hasSuccessors()
  }

  /**
   * Applies the given function for each successor node.
   */
  override def foreachSuccessor(f: Node => Unit): Unit = {
    terminator.foreachSuccessor(f)
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
