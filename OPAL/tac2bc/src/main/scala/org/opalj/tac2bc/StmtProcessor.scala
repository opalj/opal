/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

import org.opalj.RelationalOperator
import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.GE
import org.opalj.RelationalOperators.GT
import org.opalj.RelationalOperators.LE
import org.opalj.RelationalOperators.LT
import org.opalj.RelationalOperators.NE
import org.opalj.ba.CodeElement
import org.opalj.br.BooleanType
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
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.BASTORE
import org.opalj.br.instructions.CASTORE
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.DASTORE
import org.opalj.br.instructions.DEFAULT_INVOKEDYNAMIC
import org.opalj.br.instructions.DRETURN
import org.opalj.br.instructions.FASTORE
import org.opalj.br.instructions.FRETURN
import org.opalj.br.instructions.IASTORE
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.LabeledGOTO
import org.opalj.br.instructions.LabeledIF_ACMPEQ
import org.opalj.br.instructions.LabeledIF_ACMPNE
import org.opalj.br.instructions.LabeledIF_ICMPEQ
import org.opalj.br.instructions.LabeledIF_ICMPGE
import org.opalj.br.instructions.LabeledIF_ICMPGT
import org.opalj.br.instructions.LabeledIF_ICMPLE
import org.opalj.br.instructions.LabeledIF_ICMPLT
import org.opalj.br.instructions.LabeledIF_ICMPNE
import org.opalj.br.instructions.LabeledIFNONNULL
import org.opalj.br.instructions.LabeledIFNULL
import org.opalj.br.instructions.LabeledJSR
import org.opalj.br.instructions.LabeledLOOKUPSWITCH
import org.opalj.br.instructions.LabeledTABLESWITCH
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.LRETURN
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NOP
import org.opalj.br.instructions.POP
import org.opalj.br.instructions.POP2
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.RewriteLabel
import org.opalj.br.instructions.SASTORE
import org.opalj.collection.immutable.IntIntPair
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.CaughtException
import org.opalj.tac.Checkcast
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.Goto
import org.opalj.tac.If
import org.opalj.tac.InvokedynamicMethodCall
import org.opalj.tac.JSR
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.Nop
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.Ret
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.Stmt
import org.opalj.tac.Switch
import org.opalj.tac.Throw
import org.opalj.tac.V
import org.opalj.tac.Var

object StmtProcessor {

    /**
     * Generates Java bytecode instructions for Stmt.
     *
     * @param stmt the Statement to be converted into InstructionElements
     * @param uVarToLVIndex map that holds information for Local Variable Indices
     * @param labels array that maps tac indices to RewriteLabels as targets for control flow instructions
     * @param code list where bytecode instructions should be added
     */
    def processStmt(
        stmt:          Stmt[V],
        uVarToLVIndex: Map[Int, Int],
        labels:        Array[RewriteLabel],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        stmt match {
            case Assignment(_, targetVar, expr) =>
                processAssignment(targetVar, expr, uVarToLVIndex, code)
            case ArrayStore(_, arrayRef, index, value) =>
                processArrayStore(arrayRef, index, value, uVarToLVIndex, code)
            case CaughtException(_, exceptionType, throwingStmts) =>
                // TODO: handle CaughtExceptions
                processCaughtException(
                    exceptionType,
                    throwingStmts,
                    code,
                    labels
                )
            case ExprStmt(_, expr) =>
                processExprStmt(expr, uVarToLVIndex, code)
            case If(_, left, condition, right, target) =>
                processIf(left, condition, right, labels(target), uVarToLVIndex, code)
            case Goto(_, target) =>
                processGoto(labels(target), code)
            case Switch(_, defaultTarget, index, npairs) =>
                processSwitch(
                    labels(defaultTarget),
                    index,
                    npairs,
                    uVarToLVIndex,
                    code,
                    labels
                )
            case JSR(_, target) =>
                processJSR(labels(target), code)
            case callStmt: Call[V @unchecked] =>
                val call @ Call(declaringClass, isInterface, name, descriptor) = callStmt
                ExprProcessor.processCall(
                    call,
                    declaringClass,
                    isInterface,
                    name,
                    descriptor,
                    uVarToLVIndex,
                    code
                )
            case InvokedynamicMethodCall(_, bootstrapMethod, name, descriptor, params) =>
                processInvokeDynamicMethodCall(
                    bootstrapMethod,
                    name,
                    descriptor,
                    params,
                    uVarToLVIndex,
                    code
                )
            case MonitorEnter(_, objRef) =>
                processMonitorEnter(objRef, uVarToLVIndex, code)
            case MonitorExit(_, objRef) =>
                processMonitorExit(objRef, uVarToLVIndex, code)
            case PutField(_, declaringClass, name, declaredFieldType, objRef, value) =>
                processPutField(
                    declaringClass,
                    name,
                    declaredFieldType,
                    objRef,
                    value,
                    uVarToLVIndex,
                    code
                )
            case PutStatic(_, declaringClass, name, declaredFieldType, value) =>
                processPutStatic(
                    declaringClass,
                    name,
                    declaredFieldType,
                    value,
                    uVarToLVIndex,
                    code
                )
            case Checkcast(_, value, cmpTpe) =>
                processCheckCast(value, cmpTpe, uVarToLVIndex, code)
            case Ret(_, returnAddresses) =>
                processRet(returnAddresses, code)
            case ReturnValue(_, expr) =>
                processReturnValue(expr, uVarToLVIndex, code)
            case Return(_) =>
                processReturn(code)
            case Throw(_, exception) =>
                processThrow(exception, uVarToLVIndex, code)
            case Nop(_) =>
                processNop(code)
            case _ => throw new UnsupportedOperationException(s"Unsupported TAC-Stmt: $stmt")
        }
    }

    def processAssignment(
        targetVar:     Var[V],
        expr:          Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(expr, uVarToLVIndex, code)
        ExprProcessor.storeVariable(targetVar, uVarToLVIndex, code)
    }

    def processExprStmt(
        expr:          Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(expr, uVarToLVIndex, code)
        code += (if (expr.cTpe.isCategory2) POP2 else POP)
    }

    def processSwitch(
        defaultTarget: RewriteLabel,
        index:         Expr[V],
        npairs:        ArraySeq[IntIntPair /*(Case Value, Jump Target)*/ ],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]],
        labels:        Array[RewriteLabel]
    ): Unit = {
        // Transform nparis to their Labels
        // Cases that are not reachable contain the value -1 and must be removed from the npairs
        val labeledNpairs: ArraySeq[(Int, RewriteLabel)] = npairs.collect({
            case IntIntPair(key, value) if value >= 0 =>
                val label = labels(value)
                (key, label)
        })

        // Translate the index expression first
        ExprProcessor.processExpression(index, uVarToLVIndex, code)

        val minValue = npairs.minBy(_._1)._1
        val maxValue = npairs.maxBy(_._1)._1

        code += {
            if (isTableSwitch(npairs.size, minValue, maxValue)) {
                val jumpTable = mutable.ArrayBuffer.fill(maxValue - minValue + 1)(defaultTarget)
                // Set the case values in the jump table
                labeledNpairs.foreach { case (caseValue, target) =>
                    jumpTable(caseValue - minValue) = target
                }
                LabeledTABLESWITCH(defaultTarget, minValue, maxValue, jumpTable.to(ArraySeq))
            } else LabeledLOOKUPSWITCH(defaultTarget, labeledNpairs)
        }
    }

    private def isTableSwitch(numLabels: Int, minValue: Int, maxValue: Int): Boolean = {
        // This uses similar logic to javac:
        // https://github.com/openjdk/jdk/blob/a6ebcf61eb522a1bcfc9f2169d42974af3883b00/src/jdk.compiler/share/classes/com/sun/tools/javac/jvm/Gen.java#L1344
        val tableCost = maxValue - minValue + 11
        val lookupCost = 5 * numLabels
        numLabels > 0 && tableCost <= lookupCost
    }

    def processReturn(code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += RETURN
    }

    def processReturnValue(
        expr:          Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(expr, uVarToLVIndex, code)
        code += {
            expr.cTpe match {
                case ComputationalTypeInt       => IRETURN
                case ComputationalTypeLong      => LRETURN
                case ComputationalTypeFloat     => FRETURN
                case ComputationalTypeDouble    => DRETURN
                case ComputationalTypeReference => ARETURN
                case _                          => throw new UnsupportedOperationException("Unsupported computational type:" + expr.cTpe)
            }
        }
    }

    def processArrayStore(
        arrayRef:      Expr[V],
        index:         Expr[V],
        value:         Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the arrayRef onto the stack
        ExprProcessor.processExpression(arrayRef, uVarToLVIndex, code)
        // Load the index onto the stack
        ExprProcessor.processExpression(index, uVarToLVIndex, code)
        // Load the value to be stored onto the stack
        ExprProcessor.processExpression(value, uVarToLVIndex, code)
        // Infer the element type from the array reference expression
        val elementType = ExprProcessor.inferElementType(arrayRef)
        code += {
            elementType match {
                case IntegerType      => IASTORE
                case LongType         => LASTORE
                case FloatType        => FASTORE
                case DoubleType       => DASTORE
                case ByteType         => BASTORE
                case BooleanType      => BASTORE // Boolean arrays are also accessed with BALOAD (see JVM Spec. newarray / bastore)
                case CharType         => CASTORE
                case ShortType        => SASTORE
                case _: ReferenceType => AASTORE
            }
        }
    }

    def processNop(code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += NOP
    }

    def processInvokeDynamicMethodCall(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr[V]],
        uVarToLVIndex:   Map[Int, Int],
        code:            mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        for (param <- params) ExprProcessor.processExpression(param, uVarToLVIndex, code)
        code += DEFAULT_INVOKEDYNAMIC(bootstrapMethod, name, descriptor)
    }

    def processCheckCast(
        value:         Expr[V],
        cmpTpe:        ReferenceType,
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(value, uVarToLVIndex, code)
        code += CHECKCAST(cmpTpe)
        value match {
            case variable: Var[V] => ExprProcessor.storeVariable(variable, uVarToLVIndex, code)
            case _                => throw new UnsupportedOperationException(s"Error with CheckCast. Expected a Var but got: $value")
        }
    }

    def processRet(returnAddresses: PCs, code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        // Ensure there is only one return address, as RET can only work with one local variable index
        if (returnAddresses.size != 1) throw new IllegalArgumentException(
            s"RET instruction expects exactly one return address, but got: ${returnAddresses.size}"
        )
        // The RET instruction requires the index of the local variable that holds the return address
        // Create the RET instruction with the correct local variable index
        code += RET(returnAddresses.head) // FIXME This can't be correct, returnAddresses contains PCs, not local variables
    }

    def processCaughtException(
        exceptionType: Option[ObjectType],
        throwingStmts: IntTrieSet,
        code:          mutable.ListBuffer[CodeElement[Nothing]],
        labels:        Array[RewriteLabel]
    ): Unit = {
        // TODO: handle CaughtExceptions correctly
        // below is an idea on how to handle caught exceptions - but its not working yet:
        // somethings wrong with the stack map frames (for the exception tests only)
        throw new UnsupportedOperationException("Caught Exception not yet supported")
//        println("DEBUG")
//        var minPC = Int.MaxValue
//        var maxPC = Int.MinValue
//        var pc = 0
//        throwingStmts.foreach(stmt => {
//            if (ai.isImmediateVMException(stmt)) {
//                pc = ai.pcOfImmediateVMException(stmt)
//                println("ImmediateVMException")
//            } else if (ai.isMethodExternalExceptionOrigin(stmt)) {
//                pc = ai.pcOfMethodExternalException(stmt)
//                println("MethodExternalException")
//            } else {
//                pc = stmt
//                println("throw")
//            }
//            if (pc > maxPC) maxPC = pc
//            if (pc < minPC) minPC = pc
//        })
//        maxPC = maxPC + 1
//        val minPCLabel = labels(minPC)
//        val maxPCLabel = labels(maxPC)
//        println(s"$minPCLabel $maxPCLabel")
//
//        val minIndex = code.indexWhere {
//            case LabelElement(label: RewriteLabel) => label == minPCLabel
//            case _                                 => false
//        }
//        val maxIndex = code.indexWhere {
//            case LabelElement(label: RewriteLabel) => label == maxPCLabel
//            case _                                 => false
//        }
//        if (minIndex != -1 && maxIndex != -1) {
//            val preMinInstr = TRY(Symbol("test"))
//            val postMaxInstr = TRYEND(Symbol("test"))
//            code.insert(minIndex + 1, preMinInstr)
//            code += postMaxInstr
//            code += CATCH(Symbol("test"), 0, exceptionType)
//        } else {
//            println("ERROR: minPCLabel oder maxPCLabel nicht gefunden!")
//        }

    }

    def processThrow(
        exception:     Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(exception, uVarToLVIndex, code)
        code += ATHROW
    }

    def processPutStatic(
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        value:             Expr[V],
        uVarToLVIndex:     Map[Int, Int],
        code:              mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(value, uVarToLVIndex, code)
        code += PUTSTATIC(declaringClass, name, declaredFieldType)
    }

    def processPutField(
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[V],
        value:             Expr[V],
        uVarToLVIndex:     Map[Int, Int],
        code:              mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, code)
        // Load the value to be stored onto the stack
        ExprProcessor.processExpression(value, uVarToLVIndex, code)
        code += PUTFIELD(declaringClass, name, declaredFieldType)
    }

    def processMonitorEnter(
        objRef:        Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, code)
        code += MONITORENTER
    }

    def processMonitorExit(
        objRef:        Expr[V],
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, code)
        code += MONITOREXIT
    }

    def processJSR(target: RewriteLabel, code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += LabeledJSR(target)
        // FIXME This instruction produces a value of computational type returnAddress on the stack that must be handled
    }

    def processGoto(target: RewriteLabel, code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += LabeledGOTO(target)
    }

    def processIf(
        left:          Expr[V],
        condition:     RelationalOperator,
        right:         Expr[V],
        target:        RewriteLabel,
        uVarToLVIndex: Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // process the left expr
        ExprProcessor.processExpression(left, uVarToLVIndex, code)
        // process the right expr
        ExprProcessor.processExpression(right, uVarToLVIndex, code)

        code += {
            (left.cTpe, right.cTpe, condition) match {
                // Handle null comparisons
                case (_, _, EQ) if right.isNullExpr || left.isNullExpr => LabeledIFNULL(target)
                case (_, _, NE) if right.isNullExpr || left.isNullExpr => LabeledIFNONNULL(target)
                // Handle reference comparisons (object references)
                case (ComputationalTypeReference, ComputationalTypeReference, EQ) => LabeledIF_ACMPEQ(target)
                case (ComputationalTypeReference, ComputationalTypeReference, NE) => LabeledIF_ACMPNE(target)
                // Handle integer comparisons
                case (ComputationalTypeInt, ComputationalTypeInt, EQ) => LabeledIF_ICMPEQ(target)
                case (ComputationalTypeInt, ComputationalTypeInt, NE) => LabeledIF_ICMPNE(target)
                case (ComputationalTypeInt, ComputationalTypeInt, LT) => LabeledIF_ICMPLT(target)
                case (ComputationalTypeInt, ComputationalTypeInt, LE) => LabeledIF_ICMPLE(target)
                case (ComputationalTypeInt, ComputationalTypeInt, GT) => LabeledIF_ICMPGT(target)
                case (ComputationalTypeInt, ComputationalTypeInt, GE) => LabeledIF_ICMPGE(target)
                // Handle unsupported types
                case _ =>
                    throw new UnsupportedOperationException(
                        s"Unsupported types: left = ${left.cTpe}, right = ${right.cTpe}"
                    )
            }
        }
    }
}
