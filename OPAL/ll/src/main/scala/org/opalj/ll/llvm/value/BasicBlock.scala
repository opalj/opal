/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.opalj.graphs.Node

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMBasicBlockAsValue
import org.bytedeco.llvm.global.LLVM.LLVMGetBasicBlockName
import org.bytedeco.llvm.global.LLVM.LLVMGetBasicBlockParent
import org.bytedeco.llvm.global.LLVM.LLVMGetBasicBlockTerminator
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstInstruction
import org.bytedeco.llvm.global.LLVM.LLVMGetLastInstruction
import org.bytedeco.llvm.global.LLVM.LLVMGetNextInstruction

/**
 * Represents a LLVM basic block
 *
 * @param block_ref reference to a LLVM basic block
 *
 * @author Marc Clement
 */
case class BasicBlock(block_ref: LLVMBasicBlockRef)
    extends Value(LLVMBasicBlockAsValue(block_ref))
    with Node {
    def parent: Function = Function(LLVMGetBasicBlockParent(block_ref))
    def instructions: InstructionIterator = new InstructionIterator(LLVMGetFirstInstruction(block_ref))
    def firstInstruction: Instruction = Instruction(LLVMGetFirstInstruction(block_ref))
    def lastInstruction: Instruction = Instruction(LLVMGetLastInstruction(block_ref))

    lazy val predecessors: Set[BasicBlock] = {
        parent.basicBlocks
            .filter { bb =>
                bb.lastInstruction.isTerminator &&
                    bb.terminator.get.successors
                    .exists(_.parent.nodeId.equals(nodeId))
            }
            .toSet
    }

    private def terminator: Option[Instruction with Terminator] = {
        OptionalInstruction(LLVMGetBasicBlockTerminator(block_ref)) match {
            case Some(terminator) => {
                assert(terminator.isTerminator)
                Some(terminator.asInstanceOf[Instruction with Terminator])
            }
            case None => None
        }
    }

    def blockName(): String = LLVMGetBasicBlockName(block_ref).getString

    /**
     * Returns a human readable representation (HRR) of this node.
     */
    override def toHRR: Option[String] = {
        Some(name) // TODO: maybe add more info
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
        case Some(t) => t.hasSuccessors
        case None    => false
    }

    /**
     * Applies the given function for each successor node.
     */
    override def foreachSuccessor(f: Node => Unit): Unit = terminator match {
        case Some(t) => t.foreachSuccessor(f)
        case None    =>
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
