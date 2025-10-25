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
import org.opalj.ba.{CATCH, CodeElement, LabelElement, TRY, TRYEND}
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
import org.opalj.br.ClassType
import org.opalj.br.PCs
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.analyses.SomeProject
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
import org.opalj.tac.{ArrayStore, Assignment, Call, CaughtException, Checkcast, Const, Expr, ExprStmt, Goto, If, InvokedynamicMethodCall, JSR, MonitorEnter, MonitorExit, Nop, PutField, PutStatic, Ret, Return, ReturnValue, Stmt, Switch, Throw, UVar, V, Var}

object StmtProcessor {

    private val ConfigKeyPrefix = "org.opalj.tac2bc"

    /**
     * Generates Java bytecode instructions for Stmt.
     *
     * @param stmt the Statement to be converted into InstructionElements
     * @param tacToLVIndex map that holds information for Local Variable Indices
     * @param labels array that maps tac indices to RewriteLabels as targets for control flow instructions
     * @param code list where bytecode instructions should be added
     */
    def processStmt(
        stmt:         Stmt[V],
        tacToLVIndex: Map[Int, Int],
        labels:       Array[RewriteLabel],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        stmtIndex:    Int,
        delayStmtVisit: Boolean = false
        )(implicit project: SomeProject): Unit = {
        stmt match {
            case Assignment(_, targetVar, expr) =>
                processAssignment(targetVar, expr, tacToLVIndex, code, tacContext, delayStmtVisit)
            case ArrayStore(_, arrayRef, index, value) =>
                processArrayStore(arrayRef, index, value, tacToLVIndex, code, tacContext)
            case CaughtException(_, exceptionType, throwingStmts) =>
                // TODO: handle CaughtExceptions
                processCaughtException(
                    exceptionType,
                    throwingStmts,
                    code,
                    labels
                )
            case ExprStmt(_, expr) =>
                processExprStmt(expr, tacToLVIndex, code, tacContext)
            case If(_, left, condition, right, target) =>
                processIf(left, condition, right, labels(target), tacToLVIndex, code, tacContext)
            case Goto(_, target) =>
                processGoto(labels(target), code)
            case Switch(_, defaultTarget, index, npairs) =>
                processSwitch(
                    labels(defaultTarget),
                    index,
                    npairs,
                    tacToLVIndex,
                    code,
                    labels,
                    tacContext
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
                    tacToLVIndex,
                    code,
                    tacContext
                )
            case InvokedynamicMethodCall(_, bootstrapMethod, name, descriptor, params) =>
                processInvokeDynamicMethodCall(
                    bootstrapMethod,
                    name,
                    descriptor,
                    params,
                    tacToLVIndex,
                    code,
                    tacContext
                )
            case MonitorEnter(_, objRef) =>
                processMonitorEnter(objRef, tacToLVIndex, code, tacContext)
            case MonitorExit(_, objRef) =>
                processMonitorExit(objRef, tacToLVIndex, code, tacContext)
            case PutField(_, declaringClass, name, declaredFieldType, objRef, value) =>
                processPutField(
                    declaringClass,
                    name,
                    declaredFieldType,
                    objRef,
                    value,
                    tacToLVIndex,
                    code,
                    tacContext
                )
            case PutStatic(_, declaringClass, name, declaredFieldType, value) =>
                processPutStatic(
                    declaringClass,
                    name,
                    declaredFieldType,
                    value,
                    tacToLVIndex,
                    code,
                    tacContext
                )
            case Checkcast(_, value, cmpTpe) =>
                processCheckCast(value, cmpTpe, tacToLVIndex, code, tacContext)
            case Ret(_, returnAddresses) =>
                processRet(returnAddresses, code)
            case ReturnValue(_, expr) =>
                processReturnValue(expr, tacToLVIndex, code, tacContext)
            case Return(_) =>
                processReturn(code)
            case Throw(_, exception) =>
                processThrow(exception, tacToLVIndex, code, tacContext)
            case Nop(_) =>
                processNop(code)
            case _ => throw new UnsupportedOperationException(s"Unsupported TAC-Stmt: $stmt")
        }
        if(!delayStmtVisit){
            if (!tacContext.isStmtVisited(stmt)) {
                code += LabelElement(labels(stmtIndex))
            }
            tacContext.visitedStmt += stmt
        } else {
            tacContext.delayedVisitStmt += stmt
        }
    }

    def visitDelayedStmt(stmt: Stmt[V],
                         code: mutable.ListBuffer[CodeElement[Nothing]],
                         labels: Array[RewriteLabel],
                         stmtIndex: Int,
                         tacContext: Tac2BcContext): Unit = {
        if (!code.contains(LabelElement(labels(stmtIndex)))) {
            code += LabelElement(labels(stmtIndex))
        }
        tacContext.visitedStmt += stmt
    }

    def processAssignment(
        targetVar:    Var[V],
        expr:         Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        delayStmtVisit: Boolean = false,
        nestedStmt:     Boolean = false
    ): Unit = {
        if (expr.isConst || expr.isNewArray || expr.isNew) {
            tacContext.emitVarUse(targetVar)
        } else {
            // Special handling for ArrayLoad:
            // Each ArrayLoad consumes the array reference as many times
            // as the load result is used, so we need to adjust the array reference use count.
            if (expr.isArrayLoad)
                tacContext.increaseUseSitesForArrRef(
                    targetVar,
                    expr.asArrayLoad.arrayRef.asVar.definedBy.head
                )
            ExprProcessor.processExpression(expr, tacToLVIndex, code, tacContext)
        }
    }

    def processExprStmt(
        expr:         Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        code += (if (expr.cTpe.isCategory2) POP2 else POP)
        ExprProcessor.processExpression(expr, tacToLVIndex, code, tacContext)
    }

    def processSwitch(
        defaultTarget: RewriteLabel,
        index:         Expr[V],
        npairs:        ArraySeq[IntIntPair /*(Case Value, Jump Target)*/ ],
        tacToLVIndex:  Map[Int, Int],
        code:          mutable.ListBuffer[CodeElement[Nothing]],
        labels:        Array[RewriteLabel],
        tacContext:    Tac2BcContext
    )(implicit project: SomeProject): Unit = {
        // Transform nparis to their Labels
        // Cases that are not reachable contain the value -1 and must be removed from the npairs
        val labeledNpairs: ArraySeq[(Int, RewriteLabel)] = npairs.collect({
            case IntIntPair(key, value) if value >= 0 =>
                val label = labels(value)
                (key, label)
        })

        // Translate the index expression first
        ExprProcessor.processExpression(index, tacToLVIndex, code, tacContext)

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

    private def isTableSwitch(numLabels: Int, minValue: Int, maxValue: Int)(implicit project: SomeProject): Boolean = {
        // This uses similar logic to javac:
        // https://github.com/openjdk/jdk/blob/a6ebcf61eb522a1bcfc9f2169d42974af3883b00/src/jdk.compiler/share/classes/com/sun/tools/javac/jvm/Gen.java#L1344
        val config = project.config
        val tableSwitchFixedCost = config.getInt(s"$ConfigKeyPrefix.switch.tableSwitchFixedCost")
        val tableSwitchCostPerValue = config.getInt(s"$ConfigKeyPrefix.switch.tableSwitchCostPerValue")
        val lookupSwitchFixedCost = config.getInt(s"$ConfigKeyPrefix.switch.lookupSwitchFixedCost")
        val lookupSwitchCostPerValue = config.getInt(s"$ConfigKeyPrefix.switch.lookupSwitchCostPerValue")
        val tableCost = (maxValue - minValue + 1) * tableSwitchCostPerValue + tableSwitchFixedCost
        val lookupCost = numLabels * lookupSwitchCostPerValue + lookupSwitchFixedCost
        numLabels > 0 && tableCost <= lookupCost
    }

    def processReturn(code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += RETURN
    }

    def processReturnValue(
        expr:         Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
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
        if (expr.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(expr.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(expr.asVar.definedBy.head)
    }

    def processArrayStore(
        arrayRef:     Expr[V],
        index:        Expr[V],
        value:        Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
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

        // Load the value to be stored onto the stack
        tacContext.emitStmt(value.asVar.definedBy.head)
        // Load the index onto the stack
        tacContext.emitStmt(index.asVar.definedBy.head)
        // Load the arrayRef onto the stack
        tacContext.emitStmt(arrayRef.asVar.definedBy.head)
    }

    def processNop(code: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        code += NOP
    }

    def processInvokeDynamicMethodCall(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr[V]],
        tacToLVIndex:    Map[Int, Int],
        code:            mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:             Tac2BcContext
    ): Unit = {
        for (param <- params) ExprProcessor.processExpression(param, tacToLVIndex, code, tacContext)
        code += DEFAULT_INVOKEDYNAMIC(bootstrapMethod, name, descriptor)
    }

    def processCheckCast(
        value:        Expr[V],
        cmpTpe:       ReferenceType,
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        code += CHECKCAST(cmpTpe)
        tacContext.emitStmt(value.asVar.definedBy.head)
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
        exceptionType: Option[ClassType],
        throwingStmts: IntTrieSet,
        code:          mutable.ListBuffer[CodeElement[Nothing]],
        labels:        Array[RewriteLabel]
    ): Unit = {
        // TODO: handle CaughtExceptions correctly
        // below is an idea on how to handle caught exceptions - but its not working yet:
        // somethings wrong with the stack map frames (for the exception tests only)
        //throw new UnsupportedOperationException("Caught Exception not yet supported")
        println("DEBUG")
        var minPC = Int.MaxValue
        var maxPC = Int.MinValue
        var pc = 0
        throwingStmts.foreach(stmt => {
            if (ai.isImmediateVMException(stmt)) {
                pc = ai.pcOfImmediateVMException(stmt)
                println("ImmediateVMException")
            } else if (ai.isMethodExternalExceptionOrigin(stmt)) {
                pc = ai.pcOfMethodExternalException(stmt)
                println("MethodExternalException")
            } else {
                pc = stmt
                println("throw")
            }
            if (pc > maxPC) maxPC = pc
            if (pc < minPC) minPC = pc
        })
        maxPC = maxPC + 1
        val minPCLabel = labels(minPC)
        val maxPCLabel = labels(maxPC)
        println(s"$minPCLabel $maxPCLabel")

        val minIndex = code.indexWhere {
            case LabelElement(label: RewriteLabel) => label == minPCLabel
            case _                                 => false
        }
        val maxIndex = code.indexWhere {
            case LabelElement(label: RewriteLabel) => label == maxPCLabel
            case _                                 => false
        }
        if (minIndex != -1 && maxIndex != -1) {
            val preMinInstr = TRY(Symbol("test"))
            val postMaxInstr = TRYEND(Symbol("test"))
            code.insert(minIndex + 1, preMinInstr)
            code += postMaxInstr
            code += CATCH(Symbol("test"), 0, exceptionType)
        } else {
            println("ERROR: minPCLabel oder maxPCLabel nicht gefunden!")
        }

    }

    def processThrow(
        exception:    Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        ExprProcessor.processExpression(exception, tacToLVIndex, code, tacContext)
        code += ATHROW
    }

    def processPutStatic(
        declaringClass:    ClassType,
        name:              String,
        declaredFieldType: FieldType,
        value:             Expr[V],
        tacToLVIndex:      Map[Int, Int],
        code:              mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:        Tac2BcContext
    ): Unit = {
        code += PUTSTATIC(declaringClass, name, declaredFieldType)
        if (value.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(value.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(value.asVar.definedBy.head)
    }

    def processPutField(
        declaringClass:    ClassType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[V],
        value:             Expr[V],
        tacToLVIndex:      Map[Int, Int],
        code:              mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:        Tac2BcContext
    ): Unit = {
        code += PUTFIELD(declaringClass, name, declaredFieldType)
        // Load the value to be stored onto the stack
        if (value.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(value.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(value.asVar.definedBy.head)

        // Load the object reference onto the stack
        if (objRef.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(objRef.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(objRef.asVar.definedBy.head)

    }

    def processMonitorEnter(
        objRef:       Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, tacToLVIndex, code, tacContext)
        code += MONITORENTER
    }

    def processMonitorExit(
        objRef:       Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, tacToLVIndex, code, tacContext)
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
        left:         Expr[V],
        condition:    RelationalOperator,
        right:        Expr[V],
        target:       RewriteLabel,
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
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

        // process the right expr
        right match {
            case const: Const => ExprProcessor.loadConstant(const, code)
            case uvar: UVar[_] =>
                if (uvar.definedBy.head < 0)
                    ExprProcessor.loadVariable(uvar, tacToLVIndex, code)
                else
                    tacContext.emitStmt(uvar.definedBy.head, delayStmtVisit = true)
        }

        // process the left expr
        if (left.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(left.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(left.asVar.definedBy.head)
    }
}
