/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMFunction, LLVMStatement, NativeBackwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.value._
import org.opalj.tac.fpcf.analyses.ifds.taint.{TaintFact, TaintProblem}

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
        var flows: Set[NativeTaintFact] = in match {
            case NativeTaintNullFact => Set(in)
            case NativeVariable(value) => callee.function.arguments.find(_.ref == value.ref) match {
                case Some(arg) => Set(NativeVariable(callInstr.argument(arg.index).get))
                case None => Set.empty
            }
            case NativeArrayElement(base, indices) => callee.function.arguments.find(_.ref == base.ref) match {
                case Some(arg) => Set(NativeArrayElement(callInstr.argument(arg.index).get, indices))
                case None => Set.empty
            }
            case NativeFlowFact(flow) if !flow.contains(call.function) =>
                Set(NativeFlowFact(call.function +: flow))
            case _ => Set.empty
        }

        flows
    }

    override def callToReturnFlow(call: LLVMStatement, in: NativeTaintFact, successor: Option[LLVMStatement],
                                  unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = {
        // create flow facts if callee is source or sink
        val callInstr = call.instruction.asInstanceOf[Call]
        val callees = icfg.resolveCallee(callInstr)
        val sourceCallee = callees.find(_.name == "source")
        if (callees.exists(_.name == "sink")) in match {
            // taint variable that is put into sink
            case NativeTaintNullFact => Set(NativeVariable(callInstr.argument(0).get))
            case _ => Set.empty
        } else if (sourceCallee.isDefined) in match {
            // create flow fact if source is reached with tainted value
            case NativeVariable(value) if value == call.instruction && !unbCallChain.contains(call.callable) =>
                Set(NativeFlowFact(unbCallChain.prepended(call.callable)))
            case _ => Set.empty
        } else Set.empty
    }

}
