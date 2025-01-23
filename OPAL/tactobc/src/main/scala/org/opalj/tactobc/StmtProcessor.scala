/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import org.opalj.RelationalOperator
import org.opalj.RelationalOperators._
import org.opalj.ba.InstructionElement
import org.opalj.br.ArrayType
import org.opalj.br.BootstrapMethod
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DoubleType
import org.opalj.br.FieldType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.PCs
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.instructions.{AASTORE, ARETURN, ATHROW, BASTORE, CASTORE, CHECKCAST, DASTORE, DEFAULT_INVOKEDYNAMIC, DRETURN, FASTORE, FRETURN, IASTORE, INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IRETURN, LabeledGOTO, LabeledIFNONNULL, LabeledIFNULL, LabeledIF_ACMPEQ, LabeledIF_ACMPNE, LabeledIF_ICMPEQ, LabeledIF_ICMPGE, LabeledIF_ICMPGT, LabeledIF_ICMPLE, LabeledIF_ICMPLT, LabeledIF_ICMPNE, LabeledJSR, LabeledLOOKUPSWITCH, LabeledTABLESWITCH}
//import org.opalj.br.instructions.LabeledGOTO
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.LRETURN
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NOP
//import org.opalj.br.instructions.PCLabel
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.RewriteLabel
import org.opalj.br.instructions.SASTORE
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.Expr
import org.opalj.tac.UVar
import org.opalj.tac.Var
import org.opalj.tactobc.ExprProcessor.inferElementType

object StmtProcessor {

    // Assignment
    def processAssignment(
        targetVar:     Var[_],
        expr:          Expr[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(expr, uVarToLVIndex)
        instructions += ExprProcessor.storeVariable(targetVar, uVarToLVIndex)
        instructions.toList
    }

    def processSwitch(
        defaultTarget: RewriteLabel,
        index:         Expr[_],
        npairs:        ArraySeq[(Int, RewriteLabel) /*(Case Value, Jump Target)*/ ],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Translate the index expression first
        instructions ++= ExprProcessor.processExpression(index, uVarToLVIndex)

        if (isLookupSwitch(index)) {
            // Add LOOKUPSWITCH instruction with placeholders for targets
            val lookupswitchInstruction = LabeledLOOKUPSWITCH(defaultTarget, npairs)
            instructions += lookupswitchInstruction
        } else {
            // Add TABLESWITCH instruction with placeholders for targets
            val minValue = npairs.minBy(_._1)._1
            val maxValue = npairs.maxBy(_._1)._1
            val jumpTable = ArrayBuffer.fill(maxValue - minValue + 1)(defaultTarget)

            // Set the case values in the jump table
            npairs.foreach { case (caseValue, target) =>
                jumpTable(caseValue - minValue) = target
            }
            instructions += LabeledTABLESWITCH(defaultTarget, minValue, maxValue, jumpTable.to(ArraySeq))
        }
        instructions.toList
    }

    def isLookupSwitch(index: Expr[_]): Boolean = {
        // todo: decision of when to use lookupswtich/tableswitch
        index match {
            case variable: UVar[_] => variable.defSites.size == 1
            case _                 => false
        }
    }

    def processReturn(): List[InstructionElement] = {
        List(RETURN)
    }

    def processReturnValue(expr: Expr[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(expr, uVarToLVIndex)
        val instruction = expr.cTpe match {
            case ComputationalTypeInt       => IRETURN
            case ComputationalTypeLong      => LRETURN
            case ComputationalTypeFloat     => FRETURN
            case ComputationalTypeDouble    => DRETURN
            case ComputationalTypeReference => ARETURN
            case _                          => throw new UnsupportedOperationException("Unsupported computational type:" + expr.cTpe)
        }
        instructions += instruction
        instructions.toList
    }

    def processVirtualMethodCall(
        declaringClass:   ReferenceType,
        isInterface:      Boolean,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        receiver:         Expr[_],
        params:           Seq[Expr[_]],
        uVarToLVIndex:    mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Process the receiver object (e.g., aload_0 for `this`)
        instructions ++= ExprProcessor.processExpression(receiver, uVarToLVIndex)

        // Process each parameter and update the PC accordingly
        for (param <- params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }

        val instruction = {
            if (isInterface) {
                INVOKEINTERFACE(declaringClass.asObjectType, methodName, methodDescriptor)
            } else {
                INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)
            }
        }
        instructions += instruction
        instructions.toList
    }

    def processArrayStore(
        arrayRef:      Expr[_],
        index:         Expr[_],
        value:         Expr[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Load the arrayRef onto the stack
        instructions ++= ExprProcessor.processExpression(arrayRef, uVarToLVIndex)

        // Load the index onto the stack
        instructions ++= ExprProcessor.processExpression(index, uVarToLVIndex)

        // Load the value to be stored onto the stack
        instructions ++= ExprProcessor.processExpression(value, uVarToLVIndex)

        // Infer the element type from the array reference expression
        val elementType = inferElementType(arrayRef)

        val instruction = elementType match {
            case IntegerType   => IASTORE
            case LongType      => LASTORE
            case FloatType     => FASTORE
            case DoubleType    => DASTORE
            case ByteType      => BASTORE
            case CharType      => CASTORE
            case ShortType     => SASTORE
            case _: ObjectType => AASTORE
            case ArrayType(componentType) => componentType match {
                    case _: ReferenceType => AASTORE
                    case _: CharType      => CASTORE
                    case _: FloatType     => FASTORE
                    case _: DoubleType    => DASTORE
                    case _: ByteType      => BASTORE
                    case _: ShortType     => SASTORE
                    case _: IntegerType   => IASTORE
                    case _: LongType      => LASTORE
                    case _                => throw new IllegalArgumentException(s"Unsupported array store type $componentType")
                }
            case _ => throw new IllegalArgumentException(s"Unsupported array store type $elementType")
        }
        // Add the store instruction
        instructions += instruction
        instructions.toList
    }

    def processNop(): List[InstructionElement] = {
        List(NOP)
    }

    def processInvokeDynamicMethodCall(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr[_]],
        uVarToLVIndex:   mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        for (param <- params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        instructions += DEFAULT_INVOKEDYNAMIC(bootstrapMethod, name, descriptor)
        instructions.toList
    }

    def processCheckCast(
        value:         Expr[_],
        cmpTpe:        ReferenceType,
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(value, uVarToLVIndex)
        instructions += CHECKCAST(cmpTpe)
        instructions.toList
    }

    def processRet(returnAddresses: PCs): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Ensure there is only one return address, as RET can only work with one local variable index
        if (returnAddresses.size != 1) {
            throw new IllegalArgumentException(
                s"RET instruction expects exactly one return address, but got: ${returnAddresses.size}"
            )
        }

        // The RET instruction requires the index of the local variable that holds the return address
        val localVarIndex = returnAddresses.head

        // Create the RET instruction with the correct local variable index
        instructions += RET(localVarIndex)
        instructions.toList

    }

    def processCaughtException(
        exceptionType: Option[ObjectType],
        throwingStmts: IntTrieSet
    ): List[InstructionElement] = {
        // todo: handle this correctly
        List[InstructionElement]()
    }

    def processThrow(exception: Expr[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(exception, uVarToLVIndex)
        instructions += ATHROW
        instructions.toList
    }

    def processPutStatic(
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        value:             Expr[_],
        uVarToLVIndex:     mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(value, uVarToLVIndex)
        instructions += PUTSTATIC(declaringClass, name, declaredFieldType)
        instructions.toList
    }

    def processPutField(
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[_],
        value:             Expr[_],
        uVarToLVIndex:     mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Load the object reference onto the stack
        instructions ++= ExprProcessor.processExpression(objRef, uVarToLVIndex)
        // Load the value to be stored onto the stack
        instructions ++= ExprProcessor.processExpression(value, uVarToLVIndex)
        instructions += PUTFIELD(declaringClass, name, declaredFieldType)
        instructions.toList
    }

    def processMonitorEnter(
        objRef:        Expr[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Load the object reference onto the stack
        instructions ++= ExprProcessor.processExpression(objRef, uVarToLVIndex)
        instructions += MONITORENTER
        instructions.toList
    }

    def processMonitorExit(objRef: Expr[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Load the object reference onto the stack
        instructions ++= ExprProcessor.processExpression(objRef, uVarToLVIndex)
        instructions += MONITOREXIT
        instructions.toList
    }

    def processJSR(target: RewriteLabel): List[InstructionElement] = {
        List(LabeledJSR(target))
    }

    def processNonVirtualMethodCall(
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        receiver:         Expr[_],
        params:           Seq[Expr[_]],
        uVarToLVIndex:    mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        instructions ++= ExprProcessor.processExpression(receiver, uVarToLVIndex)

        // Process each parameter and update the PC accordingly
        for (param <- params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        instructions += INVOKESPECIAL(declaringClass, isInterface, methodName, methodDescriptor)
        instructions.toList
    }

    def processStaticMethodCall(
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        params:           Seq[Expr[_]],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Process each parameter and update the PC accordingly
        for (param <- params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        instructions += INVOKESTATIC(declaringClass, isInterface, methodName, methodDescriptor)
        instructions.toList
    }

    def processGoto(target: RewriteLabel): List[InstructionElement] = {
        List(LabeledGOTO(target))
    }

    def processIf(
        left:      Expr[_],
        condition: RelationalOperator,
        right:     Expr[_],
        target:    RewriteLabel,
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {

        val instructions = mutable.ListBuffer[InstructionElement]()

        // process the left expr and save the pc to give in the right expr processing
        instructions ++= ExprProcessor.processExpression(left, uVarToLVIndex)
        // process the right expr
        instructions ++= ExprProcessor.processExpression(right, uVarToLVIndex)

        val instruction = (left.cTpe, right.cTpe) match {
            // Handle null comparisons
            case (_, _) if right.isNullExpr || left.isNullExpr =>
                condition match {
                    case EQ => LabeledIFNULL(target)
                    case NE => LabeledIFNONNULL(target)
                    case _  => throw new UnsupportedOperationException(s"Unsupported condition: $condition")
                }

            // Handle reference comparisons (object references)
            case (ComputationalTypeReference, ComputationalTypeReference) =>
                condition match {
                    case EQ => LabeledIF_ACMPEQ(target)
                    case NE => LabeledIF_ACMPNE(target)
                    case _  => throw new UnsupportedOperationException(s"Unsupported condition: $condition")
                }

            // Handle integer comparisons
            case (ComputationalTypeInt, ComputationalTypeInt) =>
                condition match {
                    case EQ => LabeledIF_ICMPEQ(target)
                    case NE => LabeledIF_ICMPNE(target)
                    case LT => LabeledIF_ICMPLT(target)
                    case LE => LabeledIF_ICMPLE(target)
                    case GT => LabeledIF_ICMPGT(target)
                    case GE => LabeledIF_ICMPGE(target)
                    case _  => throw new UnsupportedOperationException(s"Unsupported condition: $condition")
                }
            // Handle unsupported types
            case _ =>
                throw new UnsupportedOperationException(s"Unsupported types: left = ${left.cTpe}, right = ${right.cTpe}")
        }
        instructions += instruction
        instructions.toList
    }
}
