/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.opalj.RelationalOperator
import org.opalj.RelationalOperators._
import org.opalj.ba.CodeElement
import org.opalj.br._
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
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
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
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.RewriteLabel
import org.opalj.br.instructions.SASTORE
import org.opalj.collection.immutable.IntIntPair
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac._
import org.opalj.tactobc.ExprProcessor.inferElementType
import org.opalj.tactobc.ExprProcessor.storeVariable
import org.opalj.value.ValueInformation

object StmtProcessor {

    // Assignment
    def processAssignment(
        targetVar:          Var[_],
        expr:               Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(expr, uVarToLVIndex, listedCodeElements)
        ExprProcessor.storeVariable(targetVar, uVarToLVIndex, listedCodeElements)
    }

    def processSwitch(
        defaultTarget:      RewriteLabel,
        index:              Expr[_],
        npairs:             ArraySeq[IntIntPair /*(Case Value, Jump Target)*/ ],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]],
        indexMap:           Array[RewriteLabel]
    ): Unit = {
        // Remove Dead Code (cases of the switch statement that are not reachable
        //                  contain the value -1 and must be removed from the npairs seq)
        val npairsWithValidJumpTargets: ArraySeq[IntIntPair] = npairs.filter(_.value >= 0)
        // Transform nparis to their Labels
        val labeledNpairs: ArraySeq[(Int, RewriteLabel)] = npairsWithValidJumpTargets.map({
            case IntIntPair(key, value) =>
                val label = indexMap(value)
                (key, label)
        })

        // Translate the index expression first
        ExprProcessor.processExpression(index, uVarToLVIndex, listedCodeElements)

        listedCodeElements += {
            if (isLookupSwitch(index)) LabeledLOOKUPSWITCH(defaultTarget, labeledNpairs)
            else {
                val minValue = npairs.minBy(_._1)._1
                val maxValue = npairs.maxBy(_._1)._1
                val jumpTable = ArrayBuffer.fill(maxValue - minValue + 1)(defaultTarget)
                // Set the case values in the jump table
                labeledNpairs.foreach { case (caseValue, target) =>
                    jumpTable(caseValue - minValue) = target
                }
                LabeledTABLESWITCH(defaultTarget, minValue, maxValue, jumpTable.to(ArraySeq))
            }
        }
    }

    private def isLookupSwitch(index: Expr[_]): Boolean = {
        // TODO: decide when to use lookup switch more efficiently
        index match {
            case variable: UVar[_] => variable.defSites.size == 1
            case _                 => false
        }
    }

    def processReturn(listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        listedCodeElements += RETURN
    }

    def processReturnValue(
        expr:               Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(expr, uVarToLVIndex, listedCodeElements)
        listedCodeElements += {
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

    def processVirtualMethodCall(
        declaringClass:     ReferenceType,
        isInterface:        Boolean,
        methodName:         String,
        methodDescriptor:   MethodDescriptor,
        receiver:           Expr[_],
        params:             Seq[Expr[_]],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process the receiver object (e.g., aload_0 for `this`)
        ExprProcessor.processExpression(receiver, uVarToLVIndex, listedCodeElements)
        for (param <- params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += {
            if (isInterface) INVOKEINTERFACE(declaringClass.asObjectType, methodName, methodDescriptor)
            else INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)
        }
    }

    def processArrayStore(
        arrayRef:           Expr[_],
        index:              Expr[_],
        value:              Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the arrayRef onto the stack
        ExprProcessor.processExpression(arrayRef, uVarToLVIndex, listedCodeElements)
        // Load the index onto the stack
        ExprProcessor.processExpression(index, uVarToLVIndex, listedCodeElements)
        // Load the value to be stored onto the stack
        ExprProcessor.processExpression(value, uVarToLVIndex, listedCodeElements)
        // Infer the element type from the array reference expression
        val elementType = inferElementType(arrayRef)
        listedCodeElements += {
            elementType match {
                case IntegerType      => IASTORE
                case LongType         => LASTORE
                case FloatType        => FASTORE
                case DoubleType       => DASTORE
                case ByteType         => BASTORE
                case CharType         => CASTORE
                case ShortType        => SASTORE
                case _: ReferenceType => AASTORE
                case _                => throw new IllegalArgumentException(s"Unsupported array store type $elementType")
            }
        }
    }

    def processNop(listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        listedCodeElements += NOP
    }

    def processInvokeDynamicMethodCall(
        bootstrapMethod:    BootstrapMethod,
        name:               String,
        descriptor:         MethodDescriptor,
        params:             Seq[Expr[_]],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        for (param <- params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += DEFAULT_INVOKEDYNAMIC(bootstrapMethod, name, descriptor)
    }

    def processCheckCast(
        value:              Expr[_],
        cmpTpe:             ReferenceType,
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(value, uVarToLVIndex, listedCodeElements)
        listedCodeElements += CHECKCAST(cmpTpe)
        value match {
            case variable: Var[_] => storeVariable(variable, uVarToLVIndex, listedCodeElements)
            case _                => throw new UnsupportedOperationException(s"Error with CheckCast. Expected a Var but got: $value")
        }
    }

    def processRet(returnAddresses: PCs, listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        // Ensure there is only one return address, as RET can only work with one local variable index
        if (returnAddresses.size != 1) throw new IllegalArgumentException(
            s"RET instruction expects exactly one return address, but got: ${returnAddresses.size}"
        )
        // The RET instruction requires the index of the local variable that holds the return address
        // Create the RET instruction with the correct local variable index
        listedCodeElements += RET(returnAddresses.head)
    }

    def processCaughtException(
        exceptionType:      Option[ObjectType],
        throwingStmts:      IntTrieSet,
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]],
        tacStmts:           Array[(Stmt[DUVar[ValueInformation]], Int)],
        indexMap:           Array[RewriteLabel],
        tacIndex:           Int
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
//            println(s"pc: $pc, tac: ${tacStmts(pc)} minpc:$minPC maxpc:$maxPC")
//        })
//        maxPC = maxPC + 1
//        val minPCLabel = indexMap(minPC)
//        val maxPCLabel = indexMap(maxPC)
//        println(s"$minPCLabel $maxPCLabel")
//
//        val minIndex = listedCodeElements.indexWhere {
//            case LabelElement(label: RewriteLabel) => label == minPCLabel
//            case _                                 => false
//        }
//        val maxIndex = listedCodeElements.indexWhere {
//            case LabelElement(label: RewriteLabel) => label == maxPCLabel
//            case _                                 => false
//        }
//        if (minIndex != -1 && maxIndex != -1) {
//            val preMinInstr = TRY(Symbol("test"))
//            val postMaxInstr = TRYEND(Symbol("test"))
//            listedCodeElements.insert(minIndex + 1, preMinInstr)
//            listedCodeElements += postMaxInstr
//            listedCodeElements += CATCH(Symbol("test"), 0, exceptionType)
//        } else {
//            println("ERROR: minPCLabel oder maxPCLabel nicht gefunden!")
//        }

    }

    def processThrow(
        exception:          Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(exception, uVarToLVIndex, listedCodeElements)
        listedCodeElements += ATHROW
    }

    def processPutStatic(
        declaringClass:     ObjectType,
        name:               String,
        declaredFieldType:  FieldType,
        value:              Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(value, uVarToLVIndex, listedCodeElements)
        listedCodeElements += PUTSTATIC(declaringClass, name, declaredFieldType)
    }

    def processPutField(
        declaringClass:     ObjectType,
        name:               String,
        declaredFieldType:  FieldType,
        objRef:             Expr[_],
        value:              Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, listedCodeElements)
        // Load the value to be stored onto the stack
        ExprProcessor.processExpression(value, uVarToLVIndex, listedCodeElements)
        listedCodeElements += PUTFIELD(declaringClass, name, declaredFieldType)
    }

    def processMonitorEnter(
        objRef:             Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, listedCodeElements)
        listedCodeElements += MONITORENTER
    }

    def processMonitorExit(
        objRef:             Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        ExprProcessor.processExpression(objRef, uVarToLVIndex, listedCodeElements)
        listedCodeElements += MONITOREXIT
    }

    def processJSR(target: RewriteLabel, listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        listedCodeElements += LabeledJSR(target)
    }

    def processNonVirtualMethodCall(
        declaringClass:     ObjectType,
        isInterface:        Boolean,
        methodName:         String,
        methodDescriptor:   MethodDescriptor,
        receiver:           Expr[_],
        params:             Seq[Expr[_]],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // process receiver
        ExprProcessor.processExpression(receiver, uVarToLVIndex, listedCodeElements)
        // process each parameter
        for (param <- params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += INVOKESPECIAL(declaringClass, isInterface, methodName, methodDescriptor)
    }

    def processStaticMethodCall(
        declaringClass:     ObjectType,
        isInterface:        Boolean,
        methodName:         String,
        methodDescriptor:   MethodDescriptor,
        params:             Seq[Expr[_]],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        for (param <- params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += INVOKESTATIC(declaringClass, isInterface, methodName, methodDescriptor)
    }

    def processGoto(target: RewriteLabel, listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]): Unit = {
        listedCodeElements += LabeledGOTO(target)
    }

    def processIf(
        left:               Expr[_],
        condition:          RelationalOperator,
        right:              Expr[_],
        target:             RewriteLabel,
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // process the left expr
        ExprProcessor.processExpression(left, uVarToLVIndex, listedCodeElements)
        // process the right expr
        ExprProcessor.processExpression(right, uVarToLVIndex, listedCodeElements)

        listedCodeElements += {
            (left.cTpe, right.cTpe) match {
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
                    throw new UnsupportedOperationException(
                        s"Unsupported types: left = ${left.cTpe}, right = ${right.cTpe}"
                    )
            }
        }
    }
}
