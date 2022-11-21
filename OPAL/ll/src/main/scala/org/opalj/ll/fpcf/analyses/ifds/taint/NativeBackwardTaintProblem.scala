/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{JNIMethod, LLVMFunction, LLVMStatement, NativeBackwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.value._
import org.opalj.tac.{ReturnValue, TACode}
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, FlowFact, InstanceField, StaticField, TaintFact, TaintNullFact, TaintProblem, Variable}

abstract class NativeBackwardTaintProblem(project: SomeProject)
    extends NativeBackwardIFDSProblem[NativeTaintFact, TaintFact](project)
        with TaintProblem[NativeFunction, LLVMStatement, NativeTaintFact] {

    override def nullFact: NativeTaintFact = NativeTaintNullFact

    override def enableUnbalancedReturns: Boolean = true

    override def needsPredecessor(statement: LLVMStatement): Boolean = false

    override def normalFlow(statement: LLVMStatement, in: NativeTaintFact,
                            predecessor: Option[LLVMStatement]): Set[NativeTaintFact] = statement.instruction match {
        case store: Store => in match {
            case NativeVariable(value) if value == store.dst => Set(in, NativeVariable(store.src))
            case NativeArrayElement(base, indices) => store.dst match { // dst is pointer type
                // value is array and is stored to tainted alloca
                case dst: Alloca if dst == base => Set(in, NativeArrayElement(store.src, indices))
                // value is stored into tainted array element
                case gep: GetElementPtr if gep.base == base =>
                    // if indices are not constant, assume the tainted element is written
                    if ((gep.isConstant && gep.constants.exists(indices.toSeq.contains(_))) || !gep.isConstant)
                        Set(in, NativeVariable(store.src))
                    else Set(in)
            }
            case _ => Set(in)
        }
        case load: Load => in match {
            case NativeVariable(value) if value == load => Set(in, NativeVariable(load.src))
            case NativeArrayElement(base, indices) if base == load => load.src match { // src is pointer type
                // array loaded from alloca
                case src: Alloca => Set(in, NativeArrayElement(src, indices))
                // array loaded from array element -> nested arrays
                case gep: GetElementPtr =>
                    if (gep.isConstant) Set(in, NativeArrayElement(gep.base, gep.constants))
                    else Set(in, NativeVariable(gep.base)) // taint whole array if indices are not constant
                case _ => Set(in)
            }
            case _ => Set(in)
        }
        case gep: GetElementPtr => in match {
            case NativeVariable(value) if value == gep => Set(in, NativeVariable(gep.base))
            case NativeArrayElement(base, indices) if base == gep && gep.isZero => Set(in, NativeArrayElement(gep.base, indices))
            case _ => Set(in)
        }
        case fneg: FNeg => in match {
            case NativeVariable(value) if value == fneg => Set(in, NativeVariable(fneg.operand(0)))
            case _ => Set(in)
        }
        case binOp: BinaryOperation => in match {
            case NativeVariable(value) if value == binOp => Set(in, NativeVariable(binOp.op1), NativeVariable(binOp.op2))
            case _ => Set(in)
        }
        case convOp: ConversionOperation => in match {
            case NativeVariable(value) if value == convOp => Set(in, NativeVariable(convOp.value))
            case _ => Set(in)
        }
        case extrElem: ExtractElement =>
            def taintExtElemVec: Set[NativeTaintFact] = {
                if (extrElem.isConstant) Set(in, NativeArrayElement(extrElem.vec, Seq(extrElem.constant)))
                else Set(in, NativeVariable(extrElem.vec)) // taint whole array if index not constant
            }
            in match {
                case NativeVariable(value) if value == extrElem => taintExtElemVec
                case NativeArrayElement(base, _) if base == extrElem => taintExtElemVec
                case _ => Set(in)
            }
        case insElem: InsertElement => in match {
            case NativeVariable(value) if insElem.vec == value => Set(in, NativeVariable(insElem.value))
            case NativeArrayElement(base, indices) if insElem.vec == base =>
                // check if tainted element is written. if index is not constant, assume tainted element is written.
                if ((insElem.isConstant && indices.exists(_ == insElem.constant)) || !insElem.isConstant)
                    Set(in, NativeVariable(insElem.value))
                else Set(in)
            case _ => Set(in)
        }
        case shuffleVec: ShuffleVector => in match {
            // simplification: taint both input arrays as a whole
            case NativeVariable(value) if value == shuffleVec =>
                Set(in, NativeVariable(shuffleVec.vec1), NativeVariable(shuffleVec.vec2))
            case NativeArrayElement(base, _) if base == shuffleVec =>
                Set(in, NativeVariable(shuffleVec.vec1), NativeVariable(shuffleVec.vec2))
            case _ => Set(in)
        }
        case extrValue: ExtractValue => in match {
            case NativeVariable(value) if extrValue == value =>
                Set(in, NativeArrayElement(extrValue.aggregVal, extrValue.constants))
            case NativeArrayElement(base, _) if base == extrValue =>
                // array loaded from array element -> nested arrays
                Set(in, NativeArrayElement(extrValue.aggregVal, extrValue.constants))
            case _ => Set(in)
        }
        case insValue: InsertValue => in match {
            case NativeVariable(value) if insValue.aggregVal == value => Set(in, NativeVariable(insValue.value))
            // check if tainted element is written
            case NativeArrayElement(base, indices) if insValue.aggregVal == base &&
                insValue.constants.exists(indices.toSeq.contains(_)) => Set(in, NativeVariable(insValue.value))
            case _ => Set(in)
        }
        case _ => Set(in)
    }

    override def callFlow(start: LLVMStatement, in: NativeTaintFact, call: LLVMStatement,
                          callee: NativeFunction): Set[NativeTaintFact] = callee match {
        // taint return value in callee, if tainted in caller
        case LLVMFunction(_) => in match {
            case NativeVariable(value) if value == call.instruction => start.instruction match {
                case ret: Ret if ret.value.isDefined => Set(NativeVariable(ret.value.get))
                case _ => Set.empty
            }
            case NativeArrayElement(base, indices) if base == call.instruction => start.instruction match {
                case ret: Ret if ret.value.isDefined => Set(NativeArrayElement(ret.value.get, indices))
                case _ => Set.empty
            }
            case NativeTaintNullFact => Set(in)
            case _ => Set.empty
        }
        case _ => throw new RuntimeException("this case should be handled by outsideAnalysisContext")
    }

    override def returnFlow(exit: LLVMStatement, in: NativeTaintFact, call: LLVMStatement,
                            successor: Option[LLVMStatement], unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = {
        val callee = exit.callable;
        if (sanitizesReturnValue(callee)) return Set.empty

        val callInstr = call.instruction.asInstanceOf[Call]
        // taint parameters in caller context if they were tainted in the callee context
        in match {
            case NativeVariable(value) => callee.function.arguments.find(_.address == value.address) match {
                case Some(arg) => Set(NativeVariable(callInstr.argument(arg.index).get))
                case None => Set.empty
            }
            case NativeArrayElement(base, indices) => callee.function.arguments.find(_.address == base.address) match {
                case Some(arg) => Set(NativeArrayElement(callInstr.argument(arg.index).get, indices))
                case None => Set.empty
            }
            case NativeTaintNullFact => Set(in)
            case NativeFlowFact(flow) if !flow.contains(call.function) =>
                Set(NativeFlowFact(call.function +: flow))
            case _ => Set.empty
        }
    }

    override def callToReturnFlow(call: LLVMStatement, in: NativeTaintFact, successor: Option[LLVMStatement],
                                  unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = {
        // create flow facts if callee is source or sink
        val callInstr = call.instruction.asInstanceOf[Call]
        val callees = icfg.resolveCallee(callInstr)
        if (callees.exists(_.name == "sink")) in match {
            // taint variable that is put into sink
            case NativeTaintNullFact => Set(NativeVariable(callInstr.argument(0).get))
            case _ => Set.empty
        } else if (callees.exists(_.name == "source")) in match {
            // create flow fact if source is reached with tainted value
            case NativeVariable(value) if value == call.instruction && !unbCallChain.contains(call.callable) =>
                Set(NativeFlowFact(unbCallChain.prepended(call.callable)))
            case _ => Set.empty
        } else Set.empty
    }

    override def javaStartStatements(callable: Method): Set[JavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(callable)
        val exitStatements = cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors
        exitStatements.map(s => JavaStatement(callable, s.asBasicBlock.endPC, code, cfg))
    }

    override protected def javaCallFlow(start: JavaStatement, call: LLVMStatement, callee: Method,
                                        in: NativeTaintFact): Set[TaintFact] = in match {
        // taint return value in callee, if tainted in caller
        case NativeVariable(_) if start.stmt.astID == ReturnValue.ASTID =>
            Set(Variable(start.index))
        case NativeArrayElement(_, _) if start.stmt.astID == ReturnValue.ASTID =>
            Set(Variable(start.index))
        case NativeTaintNullFact => Set(TaintNullFact)
        case JavaStaticField(classType, fieldName) => Set(StaticField(classType, fieldName))
        case _ => Set.empty
    }

    override protected def javaReturnFlow(exit: JavaStatement, in: TaintFact, call: LLVMStatement, callFact: NativeTaintFact,
                                          successor: Option[LLVMStatement]): Set[NativeTaintFact] = {
        val callee = exit.callable
        if (sanitizesReturnValue(JNIMethod(callee))) return Set.empty

        // Track the call chain to the sink back
        val callInstr = call.instruction.asInstanceOf[Call]
        val formalParameterIndices = (0 until callInstr.numArgOperands)
            .map(index => JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic))

        in match {
            // Taint formal parameter if actual parameter is tainted
            case Variable(index) if formalParameterIndices.contains(index) =>
                val nativeIndex = JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                Set(NativeVariable(callInstr.argument(nativeIndex).get))
            case ArrayElement(index, _) if formalParameterIndices.contains(index) =>
                val nativeIndex = JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                Set(NativeVariable(callInstr.argument(nativeIndex).get))
            case InstanceField(index, _, _) if formalParameterIndices.contains(index) =>
                val nativeIndex = JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                Set(NativeVariable(callInstr.argument(nativeIndex).get))
            // also propagate tainted static fields
            case StaticField(classType, fieldName) => Set(JavaStaticField(classType, fieldName))
            case TaintNullFact => Set(NativeTaintNullFact)
            case FlowFact(flow) if !flow.contains(call.function) => Set(NativeFlowFact(call.function +: flow))
            case _ => Set.empty
        }
    }

}
