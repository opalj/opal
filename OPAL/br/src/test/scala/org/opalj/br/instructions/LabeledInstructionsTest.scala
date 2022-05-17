/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.instructions

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.ArraySeq

/**
 * Tests instantiation and resolving of LabeledInstructions.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class LabeledInstructionsTest extends AnyFlatSpec with Matchers {
    behavior of "LabeledInstructionsTest"

    val label = InstructionLabel(Symbol("TestLabel"))
    val simpleBranchInstructionsMap: List[(LabeledSimpleConditionalBranchInstruction, InstructionLabel => LabeledSimpleConditionalBranchInstruction)] = {
        List(
            IFEQ(label) -> LabeledIFEQ,
            IFNE(label) -> LabeledIFNE,
            IFLT(label) -> LabeledIFLT,
            IFGE(label) -> LabeledIFGE,
            IFGT(label) -> LabeledIFGT,
            IFLE(label) -> LabeledIFLE,

            IF_ICMPEQ(label) -> LabeledIF_ICMPEQ,
            IF_ICMPNE(label) -> LabeledIF_ICMPNE,
            IF_ICMPLT(label) -> LabeledIF_ICMPLT,
            IF_ICMPGE(label) -> LabeledIF_ICMPGE,
            IF_ICMPGT(label) -> LabeledIF_ICMPGT,
            IF_ICMPLE(label) -> LabeledIF_ICMPLE,
            IF_ACMPEQ(label) -> LabeledIF_ACMPEQ,
            IF_ACMPNE(label) -> LabeledIF_ACMPNE,

            IFNULL(label) -> LabeledIFNULL,
            IFNONNULL(label) -> LabeledIFNONNULL
        )
    }

    val resolvedSimpleBranchInstructions = {
        simpleBranchInstructionsMap.zipWithIndex.map { instructionWithIndex =>
            val ((labeledInstruction, _), index) = instructionWithIndex
            labeledInstruction.resolveJumpTargets(0, Map(label -> index))
        }
    }

    "the convenience factories of SimpleConditionalBranchInstructions" should
        "return the correct type of LabeledBranchInstruction" in {
            simpleBranchInstructionsMap foreach { bi =>
                val (factoryMethodResult, constructorResult) = bi
                assert(factoryMethodResult == constructorResult(label))
            }
        }

    "resolving SimpleBranchInstructions" should "resolve to the correct branchoffset" in {
        for ((i, index) <- resolvedSimpleBranchInstructions.zipWithIndex) {
            assert(i.branchoffset == index)
        }
    }

    "the convenience factories of GotoInstructions" should
        "return the correct type of LabeledGotoInstruction" in {
            assert(GOTO(label) == LabeledGOTO(label))
            assert(GOTO_W(label) == LabeledGOTO_W(label))
        }

    "resolving GotoInstructions" should "resolve to the correct branchoffset" in {
        assert(GOTO(label).resolveJumpTargets(1, Map(label -> 43)).branchoffset == 42)
        assert(GOTO_W(label).resolveJumpTargets(2, Map(label -> 44)).branchoffset == 42)
    }

    "the convenience factories of JSRInstructions" should
        "return the correct type of LabeledJSRInstruction" in {
            assert(JSR(label) == LabeledJSR(label))
            assert(JSR_W(label) == LabeledJSR_W(label))
        }

    "LabeledBranchInstruction.resolve for JSRInstructions" should
        "resolve to the correct branchoffset" in {
            assert(JSR(label).resolveJumpTargets(1, Map(label -> 43)).branchoffset == 42)
            assert(JSR_W(label).resolveJumpTargets(2, Map(label -> 44)).branchoffset == 42)
        }

    val table = ArraySeq(InstructionLabel(Symbol("two")), InstructionLabel(Symbol("three")))
    val lookupTable = ArraySeq.from[(Int, InstructionLabel)]((2 to 3).iterator zip table.iterator).take(2)
    val labelsMap = Map[InstructionLabel, Int](
        label -> 43,
        InstructionLabel(Symbol("two")) -> 44,
        InstructionLabel(Symbol("three")) -> 45
    )

    "the convenience factories of CompoundConditionalBranchInstruction" should
        "return the correct type of LabeledCompoundConditionalBranchInstruction" in {
            assert(LOOKUPSWITCH(label, lookupTable) == LabeledLOOKUPSWITCH(label, lookupTable))
            assert(TABLESWITCH(label, 2, 3, table) ==
                LabeledTABLESWITCH(label, 2, 3, table))
        }

    "LabeledBranchInstruction.resolve for CompoundConditionalBranchInstruction" should
        "resolve to the correct branchoffset" in {
            val resolvedLOOKUPSWITCH = LOOKUPSWITCH(label, lookupTable).resolveJumpTargets(
                1,
                labelsMap
            )
            assert(resolvedLOOKUPSWITCH.defaultOffset == 42)
            assert(resolvedLOOKUPSWITCH.jumpOffsets == IndexedSeq(43, 44))
            assert(resolvedLOOKUPSWITCH.caseValues.toIndexedSeq == IndexedSeq(2, 3))

            val resolvedTABLESWITCH = TABLESWITCH(label, 2, 3, table).resolveJumpTargets(
                1,
                labelsMap
            )
            assert(resolvedTABLESWITCH.defaultOffset == 42)
            assert(resolvedTABLESWITCH.jumpOffsets == ArraySeq(43, 44))
        }

}
