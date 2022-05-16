/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.reflect.ClassTag
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import org.opalj.collection.mutable.Locals
import org.opalj.br.Code
import org.opalj.br.NoExceptionHandlers
import org.opalj.br.FieldType
import org.opalj.br.LocalVariable
import org.opalj.br.LocalVariableTable
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.DADD
import org.opalj.br.instructions.DUP
import org.opalj.br.instructions.DUP2
import org.opalj.br.instructions.IF_ICMPEQ
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.LOOKUPSWITCH
import org.opalj.br.instructions.NOP
import org.opalj.collection.immutable.IntIntPair

import scala.collection.immutable.ArraySeq

/**
 * Tests the toIDL method of IssueDetails
 *
 * @author Lukas Berg
 */
@RunWith(classOf[JUnitRunner])
class IssueDetailsIDLTest extends AnyFlatSpec with Matchers {

    import IDLTestsFixtures._

    behavior of "the toIDL method"

    it should "return a valid issue description if there are no LocalVariables" in {
        simpleLocalVariables.toIDL should be(simpleLocalVariablesIDL)
    }

    it should "return a valid issue description if we have a single int typed LocalVariable" in {
        val localVariable = LocalVariable(0, 1, "foo", FieldType("I"), 0)
        val code = Code(0, 0, null, NoExceptionHandlers, ArraySeq(LocalVariableTable(ArraySeq(localVariable))))
        val localVariables = new LocalVariables(code, 0, Locals(IndexedSeq(ClassTag.Int)))

        localVariables.toIDL should be(Json.obj(
            "type" -> "LocalVariables",
            "values" -> Json.arr(
                Json.obj(
                    "name" -> "foo",
                    "value" -> "Int"
                )
            )
        ))
    }

    it should "return a valid issue description if we have an int and double LocalVariable" in {
        val localVariable = LocalVariable(0, 1, "foo", FieldType("I"), 0)
        val localVariable2 = LocalVariable(0, 1, "bar", FieldType("I"), 1)
        val arrLocalVariable = ArraySeq(localVariable2, localVariable)
        val code = Code(0, 0, null, NoExceptionHandlers, ArraySeq(LocalVariableTable(arrLocalVariable)))
        val localVariables = new LocalVariables(code, 0, Locals(IndexedSeq(ClassTag.Int, ClassTag.Double)))

        localVariables.toIDL should be(Json.obj(
            "type" -> "LocalVariables",
            "values" -> Json.arr(
                Json.obj(
                    "name" -> "foo",
                    "value" -> "Int"
                ), Json.obj(
                    "name" -> "bar",
                    "value" -> "Double"
                )
            )
        ))
    }

    it should "return a valid issue description for the Operand of a SimpleConditionalBranchInstruction with one operand" in {
        simpleOperands.toIDL should be(simpleOperandsIDL)
    }

    it should "return a valid issue description for the Operands of a SimpleConditionalBranchInstruction with two operands" in {
        val instruction = new IF_ICMPEQ(0)
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("1", "2"), null)

        operands.toIDL should be(Json.obj(
            "type" -> "SimpleConditionalBranchInstruction",
            "operator" -> "==",
            "value" -> "2",
            "value2" -> "1"
        ))
    }

    it should "return a valid issue description for the Operands of a CompoundConditionalBranchInstruction with a single case" in {
        val instruction = LOOKUPSWITCH(0, ArraySeq(IntIntPair(0, 1)))
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo"), null)

        operands.toIDL should be(Json.obj(
            "type" -> "CompoundConditionalBranchInstruction",
            "value" -> "foo",
            "caseValues" -> "0"
        ))
    }

    it should "return a valid issue description for the Operands of a CompoundConditionalBranchInstruction with two cases" in {
        val instruction = LOOKUPSWITCH(0, ArraySeq(IntIntPair(0, 1), IntIntPair(2, 3)))
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo", "bar"), null)

        operands.toIDL should be(Json.obj(
            "type" -> "CompoundConditionalBranchInstruction",
            "value" -> "foo",
            "caseValues" -> "0, 2"
        ))
    }

    it should "return a valid issue description for the Operands of a StackManagementInstruction with a single operand" in {
        val instruction = DUP
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo"), null)

        operands.toIDL should be(Json.obj(
            "type" -> "StackManagementInstruction",
            "mnemonic" -> DUP.mnemonic,
            "values" -> Json.arr("foo")
        ))
    }

    it should "return a valid issue description for the Operands of a StackManagementInstruction with 2 operands" in {
        val instruction = DUP2
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo", "bar"), null)

        operands.toIDL should be(Json.obj(
            "type" -> "StackManagementInstruction",
            "mnemonic" -> DUP2.mnemonic,
            "values" -> Json.arr("foo", "bar")
        ))
    }

    it should "return a valid issue description for the Locals accessed by an IINC(0)" in {
        val instruction = IINC(0, 1)
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, null, Locals(IndexedSeq(ClassTag.Int)))

        operands.toIDL should be(Json.obj(
            "type" -> "IINC",
            "value" -> ClassTag.Int.toString,
            "constValue" -> 1
        ))
    }

    it should "return a valid issue description for the Locals accessed by an IINC(1)" in {
        val instruction = IINC(1, 0)
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, null, Locals(IndexedSeq(ClassTag.Short, ClassTag.Int)))

        operands.toIDL should be(Json.obj(
            "type" -> "IINC",
            "value" -> ClassTag.Int.toString,
            "constValue" -> 0
        ))
    }

    it should "return a valid issue description for an instruction without operands" in {
        val code = Code(0, 0, Array(NOP))
        val operands = new Operands(code, 0, List("foo"), null)

        operands.toIDL should be(Json.obj(
            "type" -> NOP.getClass.getSimpleName,
            "mnemonic" -> NOP.mnemonic,
            "parameters" -> Json.arr()
        ))
    }

    it should "return a valid issue description for the operands of an athrow instruction" in {
        val instruction = ATHROW
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo", "bar"), null)

        operands.toIDL should be(Json.obj(
            "type" -> ATHROW.getClass.getSimpleName,
            "mnemonic" -> ATHROW.mnemonic,
            "parameters" -> Json.arr("foo")
        ))
    }

    it should "return a valid issue description for the operands of some arithmetic instruction with 2 operands" in {
        val instruction = DADD
        val code = Code(0, 0, Array(instruction))
        val operands = new Operands(code, 0, List("foo", "bar", "baz"), null)

        operands.toIDL should be(Json.obj(
            "type" -> DADD.getClass.getSimpleName,
            "mnemonic" -> DADD.mnemonic,
            "parameters" -> Json.arr("bar", "foo")
        ))
    }

    //TODO implement tests for FieldValues

    //TODO implement tests for MethodReturnValues
}

