package org.opalj.tactobc

import org.opalj.br.instructions.{GOTO, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ICMPLT, IF_ICMPNE, Instruction, LOOKUPSWITCH, TABLESWITCH}
import org.opalj.collection.immutable.{IntIntPair, IntTrieSet}
import org.opalj.tac.{DUVar, Stmt}
import org.opalj.value.ValueInformation

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ThirdPass {

  def updateTargetsOfJumpInstructions(tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)], generatedByteCodeWithPC: ArrayBuffer[(Int, Instruction)], tacTargetToByteCodePcs: ArrayBuffer[(Int, Int)], switchCases: ArrayBuffer[(Int, Int)]): ArrayBuffer[(Int, Instruction)] = {
    val result = ArrayBuffer[(Int, Instruction)]()
    // Index for TAC statements
    var tacTargetToByteCodePcsIndex = 0

    generatedByteCodeWithPC.zipWithIndex.foreach {
      case ((pc, instruction), _) =>
        // Match and update branch instructions
        val updatedInstruction = instruction match {
          case IF_ICMPEQ(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPEQ(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case IF_ICMPNE(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPNE(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case IF_ICMPLT(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPLT(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case IF_ICMPLE(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPLE(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case IF_ICMPGT(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPGT(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case IF_ICMPGE(-1) =>
            tacTargetToByteCodePcsIndex -= 1
            val instruction = IF_ICMPGE(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
            tacTargetToByteCodePcsIndex += 1
            instruction
          case GOTO(-1) =>
            GOTO(updateBranchTargets(tacTargetToByteCodePcs, tacTargetToByteCodePcsIndex, pc))
          case LOOKUPSWITCH(defaultOffset, matchOffsets) =>
            val updatedMatchOffsets = matchOffsets.map { case IntIntPair(caseValue, _) =>
              val tacTarget = findTacTarget(switchCases, caseValue)
              IntIntPair(caseValue, updateSwitchTargets(tacTargetToByteCodePcs, tacTarget))
            }
            val updatedDefaultOffset = updateSwitchTargets(tacTargetToByteCodePcs, defaultOffset)
            LOOKUPSWITCH(updatedDefaultOffset, updatedMatchOffsets)
          case TABLESWITCH(defaultOffset, low, high, jumpOffsets) =>
            val updatedJumpOffsets = jumpOffsets.zipWithIndex.map { case (_, index) =>
              val tacTarget = findTacTarget(switchCases, index)
              updateSwitchTargets(tacTargetToByteCodePcs, tacTarget)
            }
            val updatedDefaultOffset = updateSwitchTargets(tacTargetToByteCodePcs, defaultOffset)
            TABLESWITCH(updatedDefaultOffset, low, high, updatedJumpOffsets.to(ArraySeq))
          case _ =>
            instruction
        }
        result += ((pc, updatedInstruction))

        // Only increment tacIndex when the current instruction corresponds to a TAC statement
        if (tacTargetToByteCodePcsIndex < tacStmts.length && directAssociationExists(tacTargetToByteCodePcs, tacTargetToByteCodePcs(tacTargetToByteCodePcsIndex)._1, pc)) {
          tacTargetToByteCodePcsIndex += 1
        }
    }
    ExprProcessor.uVarToLVIndex = mutable.Map[IntTrieSet, Int]()
    ExprProcessor.nextLVIndex = 1
    result
  }

  def findTacTarget(npairs: ArrayBuffer[(Int, Int)], caseValue: Int): Int = {
    val tacTarget = npairs.find(_._1 == caseValue).map(_._2).get
    tacTarget
  }

  def updateBranchTargets(tacTargetToByteCodePcs: ArrayBuffer[(Int, Int)], tacTargetToByteCodePcsIndex: Int, currentPC: Int): Int = {
    val tacTarget = tacTargetToByteCodePcs(tacTargetToByteCodePcsIndex)._1
    val byteCodeTarget = tacTargetToByteCodePcs(tacTarget)._2 - currentPC
    byteCodeTarget
  }

  def updateSwitchTargets(tacTargetToByteCodePcs: ArrayBuffer[(Int, Int)], tacTarget: Int): Int = {
    val byteCodeTarget = tacTargetToByteCodePcs(tacTarget)._2
    byteCodeTarget
  }

  def directAssociationExists(tacTargetToByteCodePcs: ArrayBuffer[(Int, Int)], tacTarget: Int, bytecodePC: Int): Boolean = {
    tacTargetToByteCodePcs.exists { case (tacGoto, bcPC) => (tacGoto, bcPC) == (tacTarget, bytecodePC) }
  }

}
