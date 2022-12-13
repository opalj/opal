/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.{Callable, IFDSFact}
import org.opalj.ll.fpcf.analyses.ifds.{JNICallUtil, JNIMethod, LLVMFunction, LLVMStatement, NativeBackwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.PointerType
import org.opalj.ll.llvm.value._
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.{ArrayLength, ArrayLoad, BinaryExpr, Compare, Expr, GetField, GetStatic, NewArray, PrefixExpr, PrimitiveTypecastExpr, ReturnValue, Var}
import org.opalj.tac.fpcf.analyses.ifds.{JavaBackwardICFG, JavaICFG, JavaIFDSProblem, JavaMethod, JavaStatement}
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
                            successor: Option[LLVMStatement], unbCallChain: Seq[Callable]): Set[NativeTaintFact] = {
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
                                       unbCallChain: Seq[Callable]): Option[NativeTaintFact] = None

    override def callToReturnFlow(call: LLVMStatement, in: NativeTaintFact, successor: Option[LLVMStatement],
                                  unbCallChain: Seq[Callable]): Set[NativeTaintFact] = {
        val flowFact = createFlowFactAtCall(call, in, unbCallChain)
        val result = collection.mutable.Set.empty[NativeTaintFact]
        if (!sanitizesParameter(call, in)) result.add(in)
        if (flowFact.isDefined) result.add(flowFact.get)
        result.toSet
    }

    override def outsideAnalysisContextUnbReturn(callee: NativeFunction): Option[OutsideAnalysisContextUnbReturnHandler] = {
        def handleJavaUnbalancedReturn(callee: NativeFunction, in: NativeTaintFact, callChain: Seq[Callable],
                                       dependeesGetter: Getter): Unit = {
            // find calls of native function in java code
            val llvmCallee = callee.asInstanceOf[LLVMFunction]
            val (fqn, javaMethodName) = JNICallUtil.resolveNativeMethodName(llvmCallee).get
            val javaCompanions = project.allProjectClassFiles
                .filter(classFile => classFile.thisType.fqn == fqn)
                .flatMap(_.methods)
                .filter(_.name == javaMethodName)
            val javaCalls = javaCompanions.flatMap(javaICFG.getCallers)

            for (callStmt <- javaCalls) {
                val unbalancedReturnFacts = javaUnbalancedReturnFlow(llvmCallee, callStmt, in)
                    .map(new IFDSFact(_, true, Some(callStmt), Some(callChain.prepended(callee))))

                // Add the caller with the unbalanced return facts as a dependency to start its analysis
                for (unbRetFact <- unbalancedReturnFacts) {
                    val newEntity = (callStmt.callable, unbRetFact)
                    dependeesGetter(newEntity, javaPropertyKey)
                }
            }
        }

        callee match {
            case f: LLVMFunction if JNICallUtil.resolveNativeMethodName(f).isDefined =>
                Some(handleJavaUnbalancedReturn _)
            case _ => None
        }
    }

    private def javaUnbalancedReturnFlow(callee: LLVMFunction, call: JavaStatement, in: NativeTaintFact): Set[TaintFact] = {
        if (sanitizesReturnValue(callee)) return Set.empty

        val callStatement = JavaIFDSProblem.asCall(call.stmt)

        def taintActualIfFormal(in: Value): Set[TaintFact] = {
            callee.function.arguments.find(_.address == in.address) match {
                // arg.index - 1 because JNIEnv is first argument in native function
                case Some(arg) if arg.index > 0 => callStatement.allParams(arg.index - 1).asVar.definedBy.map(Variable)
                case _ => Set.empty
            }
        }

        in match {
            // Taint actual parameter if formal parameter is tainted
            case NativeVariable(value) => taintActualIfFormal(value)
            case NativeArrayElement(base, _) => taintActualIfFormal(base)
            // keep static field taints
            case JavaStaticField(classType, fieldName) => Set(StaticField(classType, fieldName))
            // propagate flow facts
            case NativeFlowFact(flow) if !flow.contains(JavaMethod(call.method)) =>
                Set(FlowFact(JavaMethod(call.method) +: flow))
            case NativeTaintNullFact => Set(TaintNullFact)
            case _ => Set.empty
        }
    }

    override protected def javaCallFlow(start: JavaStatement, call: LLVMStatement, callee: Method,
                                        in: NativeTaintFact): Set[TaintFact] = {
        val flow = collection.mutable.Set.empty[TaintFact]
        val callInstr = call.instruction.asInstanceOf[Call]

        def createNewTaints(expression: Expr[V], statement: JavaStatement): Set[TaintFact] = {
            /* TODO alias references and nested objects are not correctly handled, same for forward analysis
             if new variable is tainted, check if it holds a reference type value, if yes:
              1. taint aliases of that variable
              2. taint variables holding references to inner objects/arrays
             repeat 1 and 2 for variables found in 1 and 2

             code example showcasing the problem:
             ============================================================
             def nested_ret_array():
                a = new int[4][4]               "
                b = a[3]                        "
                c = source()                    "
                b[0] = c                        "
                d = a[3]                        {d[2], a[3]}
                return d                        {arr[2] -> d[2]}

             arr = nested_ret_array()            {arr[2]}
             sink(arr[2])
             ============================================================
             */
            expression.astID match {
                case Var.ASTID => expression.asVar.definedBy.map(Variable)
                case ArrayLoad.ASTID =>
                    val arrayLoad = expression.asArrayLoad
                    val arrayIndex = TaintProblem.getIntConstant(arrayLoad.index, statement.code)
                    val arrayDefinedBy = arrayLoad.arrayRef.asVar.definedBy
                    if (arrayIndex.isDefined) arrayDefinedBy.map(ArrayElement(_, arrayIndex.get))
                    else arrayDefinedBy.map(Variable)
                case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID |
                     PrimitiveTypecastExpr.ASTID | NewArray.ASTID | ArrayLength.ASTID =>
                    (0 until expression.subExprCount).foldLeft(Set.empty[TaintFact])((acc, subExpr) =>
                        acc ++ createNewTaints(expression.subExpr(subExpr), statement))
                case GetField.ASTID =>
                    val getField = expression.asGetField
                    getField.objRef.asVar.definedBy
                        .map(InstanceField(_, getField.declaringClass, getField.name))
                case GetStatic.ASTID =>
                    val getStatic = expression.asGetStatic
                    Set(StaticField(getStatic.declaringClass, getStatic.name))
                case _ => Set.empty
            }
        }

        // taint return value in callee, if tainted in caller
        if (start.stmt.astID == ReturnValue.ASTID) in match {
            case NativeVariable(value) if value == callInstr => flow ++= createNewTaints(start.stmt.asReturnValue.expr, start)
            case NativeArrayElement(base, _) if base == callInstr => flow ++= createNewTaints(start.stmt.asReturnValue.expr, start)
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
                                          unbCallChain: Seq[Callable], successor: Option[LLVMStatement]): Set[NativeTaintFact] = {
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
            case FlowFact(flow) => Set(NativeFlowFact(unbCallChain.prepended(call.function)))
            case _ => Set.empty
        }
    }

}
