package org.opalj.br.cfg

import scala.collection.immutable.HashSet
import scala.collection.immutable.TreeSet
import org.opalj.br.PC
import org.opalj.br.Code

case class BasicBlock(
        startPC: PC) extends CFGBlock {

    var catchBlockSuccessors: List[CatchBlock] = Nil

    var endPC: PC = startPC

    def addPC(pc: PC): Unit = {
        endPC = pc
    }

    override def addSucc(block: CFGBlock): Unit = block match {
        case cb: CatchBlock ⇒ { catchBlockSuccessors = catchBlockSuccessors :+ cb }
        case _              ⇒ super.addSucc(block)
    }

    def programCounters(code: Code): Seq[PC] = {
        var res: List[PC] = Nil
        var pc: PC = startPC

        while (pc <= endPC) {
            res = res :+ pc
            pc = code.pcOfNextInstruction(pc)
        }

        res
    }
    
    def indexOfPC(pc: PC, code: Code): Int = {
    	var res = 0
    	var currentPC = startPC
    	
    	while(currentPC < pc){
    		currentPC = code.pcOfNextInstruction(currentPC)
    		res += 1
    	}
    	
    	res
    }
    
    def split(block: BasicBlock, newBlockStartPC: PC, oldBlockEndPC: PC): BasicBlock = {

        val newBlock = new BasicBlock(newBlockStartPC)
        newBlock.endPC = endPC
        newBlock.successors = successors
        newBlock.catchBlockSuccessors = catchBlockSuccessors
        newBlock.addPred(this)

        for (successor ← newBlock.successors) {
            val oldBlock = this
            successor.predecessors = successor.predecessors.map { c: CFGBlock ⇒ c match { case `oldBlock` ⇒ newBlock; case _ ⇒ c } }
        }

        successors = List(newBlock)
        endPC = oldBlockEndPC

        newBlock
    }

    def foreach[U](f: (PC) ⇒ U)(implicit code: Code): Unit = {
        var pc = this.startPC

        while (pc <= endPC) {
            f(pc)
            pc = code.instructions(pc).indexOfNextInstruction(pc, code)
        }
    }

    override def returnAllBlocks(visited: HashSet[CFGBlock]): HashSet[CFGBlock] = {

        var res = visited + this
        val worklist = (successors ++ catchBlockSuccessors)
        for (block ← worklist if (!visited.contains(block)))
            res = res ++ block.returnAllBlocks(res)
        res
    }

    def toDot(code: Code): String = {

        var blockLabel: String = ID+"\n"+"_____________________"+"\n"

        if (startPC == endPC) { // Sonderfall : 1 Instruktion
            blockLabel = blockLabel + startPC+":\t"+code.instructions(startPC).toString(startPC).replaceAll("\"", "")+"\n"
        } else {
            val padding: String = if (code.instructions(startPC).indexOfNextInstruction(startPC, code) == endPC) { "" } else { "\t***\n" } // Sonderfall: 2 Instructions

            blockLabel = blockLabel + startPC+":\t"+code.instructions(startPC).toString(startPC).replaceAll("\"", "")+"\n"+
                padding + endPC+":\t"+code.instructions(endPC).toString(endPC).replaceAll("\"", "")+"\n"
        }

        var res = ID+" [shape=box, label=\""+blockLabel+"\"];\n"

        val worklist = (successors ++ catchBlockSuccessors)

        for (succ ← worklist) {
            succ match {
                case cb: CatchBlock ⇒ {
                    res = res + ID+" -> "+succ.ID+"[color=red];\n"
                }
                case _ ⇒ {
                    res = res + ID+" -> "+succ.ID+";\n"
                }
            }
        }
        res
    }

    override def toString: String = {
        "bb@"+startPC+"to"+endPC
    }
}