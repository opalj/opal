/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{JNIMethod, LLVMFunction, LLVMStatement, NativeBackwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.PointerType
import org.opalj.ll.llvm.value._
import org.opalj.tac.ReturnValue
import org.opalj.tac.fpcf.analyses.ifds.{JavaBackwardICFG, JavaICFG, JavaIFDSProblem, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, FlowFact, InstanceField, StaticField, TaintFact, TaintNullFact, TaintProblem, Variable}

abstract class NativeBackwardTaintProblem(project: SomeProject)
    extends NativeBackwardIFDSProblem[NativeTaintFact, TaintFact](project)
        with TaintProblem[NativeFunction, LLVMStatement, NativeTaintFact] {
    override val javaICFG: JavaICFG = new JavaBackwardICFG(project)

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
                          callee: NativeFunction): Set[NativeTaintFact] = {
        val callInstr = call.instruction.asInstanceOf[Call]
        val flow = collection.mutable.Set.empty[NativeTaintFact]
        callee match {
            case LLVMFunction(callee) =>
                // taint return value in callee, if tainted in caller
                start.instruction match {
                    case ret: Ret if ret.value.isDefined => in match {
                        case NativeVariable(value) if value == call.instruction =>
                            flow += NativeVariable(ret.value.get)
                        case NativeArrayElement(base, indices) if base == call.instruction =>
                            flow += NativeArrayElement(ret.value.get, indices)
                        case _ =>
                    }
                    case _ =>
                }

                // check for tainted pass-by-reference parameters (pointer)
                // TODO handle `byval` attribute or not? nested objects?
                in match {
                    case NativeVariable(value) => callInstr.indexOfArgument(value) match {
                        case Some(index) => callInstr.argument(index).get.typ match {
                            case PointerType(_) => flow += NativeVariable(callee.argument(index))
                            case _ =>
                        }
                        case None =>
                    }
                    case NativeArrayElement(base, indices) => callInstr.indexOfArgument(base) match {
                        case Some(index) => callInstr.argument(index).get.typ match {
                            case PointerType(_) => flow += NativeArrayElement(callee.argument(index), indices)
                            case _ =>
                        }
                        case None =>
                    }
                    case NativeTaintNullFact => flow += in
                    case _ =>
                }
            case _ => throw new RuntimeException("this case should be handled by outsideAnalysisContext")
        }
        flow.toSet
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

    /**
     * Called in callToReturnFlow. Creates a fact if necessary.
     *
     * @param call The call.
     * @param in   The fact, which holds before the call.
     * @return Some fact, if necessary. Otherwise None.
     */
    protected def createFlowFactAtCall(call: LLVMStatement, in: NativeTaintFact,
                                       callChain: Seq[NativeFunction]): Option[NativeTaintFact] = None

    override def callToReturnFlow(call: LLVMStatement, in: NativeTaintFact, successor: Option[LLVMStatement],
                                  unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = {
        val flowFact = createFlowFactAtCall(call, in, unbCallChain)
        val result = collection.mutable.Set.empty[NativeTaintFact]
        if (!sanitizesParameter(call, in)) result.add(in)
        if (flowFact.isDefined) result.add(flowFact.get)
        result.toSet
    }

    override protected def javaCallFlow(start: JavaStatement, call: LLVMStatement, callee: Method,
                                        in: NativeTaintFact): Set[TaintFact] = {
        val flow = collection.mutable.Set.empty[TaintFact]
        val callInstr = call.instruction.asInstanceOf[Call]

        // taint return value in callee, if tainted in caller
        if (start.stmt.astID == ReturnValue.ASTID) in match {
            case NativeVariable(_) => flow += Variable(start.index)
            case NativeArrayElement(_, _) => flow += Variable(start.index)
            case _ =>
        }

        def taintRefParam(callInstr: Call, in: Value): Set[TaintFact] = callInstr.indexOfArgument(in) match {
            case Some(index) => callInstr.argument(index).get.typ match {
                case PointerType(_) =>
                    val tacIndex = JavaIFDSProblem.switchParamAndVariableIndex(index - 1, callee.isStatic) // -1 offset JNIEnv
                    Set(Variable(tacIndex))
                case _ => Set.empty
            }
            case None => Set.empty
        }

        // check for tainted pass-by-reference parameters (pointer)
        in match {
            case NativeVariable(value) => flow ++= taintRefParam(callInstr, value)
            case NativeArrayElement(base, _) => flow ++= taintRefParam(callInstr, base)
            case NativeTaintNullFact => flow += TaintNullFact
            case JavaStaticField(classType, fieldName) => flow += StaticField(classType, fieldName)
            case _ =>
        }
        flow.toSet
    }

    override protected def javaReturnFlow(exit: JavaStatement, in: TaintFact, call: LLVMStatement, callFact: NativeTaintFact,
                                          successor: Option[LLVMStatement]): Set[NativeTaintFact] = {
        val callee = exit.callable
        if (sanitizesReturnValue(JNIMethod(callee))) return Set.empty

        val callInstr = call.instruction.asInstanceOf[Call]
        val formalParameterIndices = (0 until callInstr.numArgOperands)
            .map(index => JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic))

        def taintActualIfFormal(index: Int): Set[NativeTaintFact] = {
            if (formalParameterIndices.contains(index)) {
                val nativeIndex = JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                Set(NativeVariable(callInstr.argument(nativeIndex + 1).get)) // +1 offset JNIEnv
            } else Set.empty
        }

        in match {
            // Taint actual parameter if formal parameter is tainted
            case Variable(index) => taintActualIfFormal(index)
            case ArrayElement(index, _) => taintActualIfFormal(index)
            case InstanceField(index, _, _) => taintActualIfFormal(index)
            // also propagate tainted static fields
            case StaticField(classType, fieldName) => Set(JavaStaticField(classType, fieldName))
            case TaintNullFact => Set(NativeTaintNullFact)
            // Track the call chain to the sink back
            case FlowFact(flow) if !flow.contains(call.function) => Set(NativeFlowFact(call.function +: flow))
            case _ => Set.empty
        }
    }

}
