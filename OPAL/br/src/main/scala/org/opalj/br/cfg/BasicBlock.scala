/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.cfg

import org.opalj.br.Code

/**
 * Represents a basic block of a method's control flow graph (CFG). The basic block
 * is identified by referring to the first and last instruction belonging to the
 * basic block.
 *
 * @param startPC The pc of the first instruction belonging to the `BasicBlock`.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
// IMPROVE create specialized implementations for the most common cases: (1) just one instruction and just on predecessor, (2) just one predecessor, (3) just one successor
final class BasicBlock(
        val startPC:             Int, // <= determines this basic blocks' hash value!
        private[cfg] var _endPC: Int = Int.MinValue
) extends CFGNode {

    def this(startPC: Int, successors: Set[CFGNode]) = {
        this(startPC, Int.MinValue)
        this.setSuccessors(successors)
    }

    final override def nodeId: Int = startPC

    def copy(
        startPC:      Int          = this.startPC,
        endPC:        Int          = this.endPC,
        predecessors: Set[CFGNode] = this.predecessors,
        successors:   Set[CFGNode] = this.successors
    ): BasicBlock = {
        val newBB = new BasicBlock(startPC, endPC)
        newBB.setPredecessors(predecessors)
        newBB.setSuccessors(successors)
        newBB
    }

    final override def isCatchNode: Boolean = false
    final override def isExitNode: Boolean = false
    final override def isAbnormalReturnExitNode: Boolean = false
    final override def isNormalReturnExitNode: Boolean = false
    final override def isBasicBlock: Boolean = true
    final override def asBasicBlock: this.type = this

    def endPC_=(pc: Int): Unit = {
        _endPC = pc
    }
    /**
     * The pc of the last instruction belonging to this basic block.
     */
    def endPC: Int = _endPC

    private[this] var _isStartOfSubroutine: Boolean = false // will be initialized at construction time
    def setIsStartOfSubroutine(): Unit = {
        _isStartOfSubroutine = true
    }

    def isStartOfSubroutine: Boolean = _isStartOfSubroutine

    /**
     * Returns the index of an instruction – identified by its program counter (pc) –
     * in a basic block.
     *
     * ==Example==
     * Given a basic block which has five instructions which have the following
     * program counters: {0,1,3,5,6}. In this case the index of the instruction with
     * program counter 3 will be 2 and in case of the instruction with pc 6 the index
     * will be 4.
     *
     * @param pc The program counter of the instruction for which the index is needed.
     *     `pc` has to satisfy: `startPC <= pc <= endPC`.
     * @param code The code to which this basic block belongs.
     */
    def index(pc: Int)(implicit code: Code): Int = {
        assert(pc >= startPC && pc <= endPC)

        var index = 0
        var currentPC = startPC
        while (currentPC < pc) {
            currentPC = code.pcOfNextInstruction(currentPC)
            index += 1
        }
        index
    }

    /**
     * Calls the function `f` for all instructions - identified by their respective
     * pcs - of a basic block.
     *
     * @param     f The function that will be called.
     * @param     code The [[org.opalj.br.Code]]` object to which this `BasicBlock` implicitly
     *             belongs.
     */
    def foreach[U](f: Int => U)(implicit code: Code): Unit = {
        val instructions = code.instructions

        var pc = this.startPC
        val endPC = this.endPC
        while (pc <= endPC) {
            f(pc)
            pc = instructions(pc).indexOfNextInstruction(pc)
        }
    }

    /**
     * Counts the instructions of this basic block.
     */
    def countInstructions(implicit code: Code): Int = {
        val instructions = code.instructions
        var count = 1
        val startPC = this.startPC
        var pc = instructions(startPC).indexOfNextInstruction(startPC)
        val endPC = this.endPC
        while (pc <= endPC) {
            count += 1
            pc = instructions(pc).indexOfNextInstruction(pc)
        }
        count
    }

    //
    // FOR DEBUGGING/VISUALIZATION PURPOSES
    //

    override def toString: String = s"BasicBlock(start=$startPC,end=$endPC)"

    override def toHRR: Option[String] = Some(s"[$startPC,$endPC]#=${endPC - startPC + 1}")

    override def visualProperties: Map[String, String] = {
        var visualProperties = Map("shape" -> "box", "labelloc" -> "l")

        if (startPC == 0) {
            visualProperties += "fillcolor" -> "green"
            visualProperties += "style" -> "filled"
        }

        if (!hasSuccessors) { // in this case something is very broken (internally)...
            visualProperties += "shape" -> "octagon"
            visualProperties += "fillcolor" -> "gray"
            visualProperties += "style" -> "filled"
        }

        visualProperties
    }
}
