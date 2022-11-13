/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{JNIMethod, LLVMFunction, LLVMStatement, NativeForwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.value.{Add, Alloca, BitCast, Call, GetElementPtr, Load, PHI, Ret, Store, Sub}
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{TaintFact, TaintProblem}
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.StaticField
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.ReturnValue

abstract class NativeForwardTaintProblem(project: SomeProject)
    extends NativeForwardIFDSProblem[NativeTaintFact, TaintFact](project)
        with TaintProblem[NativeFunction, LLVMStatement, NativeTaintFact] {
    override def nullFact: NativeTaintFact = NativeTaintNullFact

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement   The analyzed statement.
     * @param in          The fact which holds before the execution of the `statement`.
     * @param predecessor The predecessor of the analyzed `statement`, for which the data flow shall be
     *                    computed. Used for phi statements to distinguish the flow.
     * @return The facts, which hold after the execution of `statement` under the assumption
     *         that the facts in `in` held before `statement` and `successor` will be
     *         executed next.
     */
    override def normalFlow(statement: LLVMStatement, in: NativeTaintFact, predecessor: Option[LLVMStatement]): Set[NativeTaintFact] = statement.instruction match {
        case _: Alloca => Set(in)
        case store: Store => in match {
            case NativeVariable(value) if value == store.src => store.dst match {
                case dst: Alloca                          => Set(in, NativeVariable(dst))
                case gep: GetElementPtr if gep.isConstant => Set(in, NativeArrayElement(gep.base, gep.constants))
            }
            case NativeArrayElement(base, indices) if store.src == base => Set(in, NativeArrayElement(store.dst, indices))
            case NativeVariable(value) if value == store.dst => Set()
            case _ => Set(in)
        }
        case load: Load => in match {
            case NativeVariable(value) if value == load.src => Set(in, NativeVariable(load))
            case NativeArrayElement(base, indices) => load.src match {
                case gep: GetElementPtr if gep.isConstant && gep.base == base && gep.constants == indices => Set(in, NativeVariable(load))
                case _ => Set(in, NativeArrayElement(load, indices))
            }
            case _ => Set(in)
        }
        case add: Add => in match {
            case NativeVariable(value) if value == add.op1 || value == add.op2 => Set(in, NativeVariable(add))
            case _ => Set(in)
        }
        case sub: Sub => in match {
            case NativeVariable(value) if value == sub.op1 || value == sub.op2 => Set(in, NativeVariable(sub))
            case _ => Set(in)
        }
        case gep: GetElementPtr => in match {
            case NativeVariable(value) if value == gep.base => Set(in, NativeVariable(gep))
            case NativeArrayElement(base, indices) if base == gep.base && gep.isZero => Set(in, NativeArrayElement(gep, indices))
            case _ => Set(in)
        }
        case bitcast: BitCast => in match {
            case NativeVariable(value) if value == bitcast.operand(0) => Set(in, NativeVariable(bitcast))
            case _ => Set(in)
        }
        case _ => Set(in)
    }

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call   The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in     The fact which holds before the execution of the `call`.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    override def callFlow(start: LLVMStatement, in: NativeTaintFact, call: LLVMStatement, callee: NativeFunction): Set[NativeTaintFact] = callee match {
        case LLVMFunction(callee) =>
            in match {
                // Taint formal parameter if actual parameter is tainted
                case NativeVariable(value) => call.instruction.asInstanceOf[Call].indexOfArgument(value) match {
                    case Some(index) => Set(NativeVariable(callee.argument(index)))
                    case None        => Set()
                }
                // TODO pass other java taints
                case NativeTaintNullFact => Set(in)
                case NativeArrayElement(base, indices) => call.instruction.asInstanceOf[Call].indexOfArgument(base) match {
                    case Some(index) => Set(NativeArrayElement(callee.argument(index), indices))
                    case None        => Set()
                }
                case _ => Set() // Nothing to do
            }
        case _ => throw new RuntimeException("this case should be handled by outsideAnalysisContext")
    }

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param exit The statement, which terminated the `callee`.
     * @param in   The fact which holds before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    override def returnFlow(exit: LLVMStatement, in: NativeTaintFact, call: LLVMStatement, successor: Option[LLVMStatement], unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = {
        val callee = exit.callable
        var flows: Set[NativeTaintFact] = if (sanitizesReturnValue(callee)) Set.empty else in match {
            case NativeVariable(value) => exit.instruction match {
                case ret: Ret if ret.value.contains(value) => Set(NativeVariable(call.instruction))
                case _: Ret                                => Set()
                case _                                     => Set()
            }
            case NativeTaintNullFact => Set(NativeTaintNullFact)
            case NativeFlowFact(flow) if !flow.contains(call.function) =>
                Set(NativeFlowFact(call.function +: flow))
            case _ => Set()
        }
        if (exit.callable.name == "source") in match {
            case NativeTaintNullFact => flows += NativeVariable(call.instruction)
        }
        if (exit.callable.name == "sink") in match {
            case NativeVariable(value) if value == exit.callable.function.argument(0) =>
                flows += NativeFlowFact(Seq(call.callable, exit.callable))
            case _ =>
        }
        flows
    }

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param in   The facts, which hold before the `call`.
     * @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
    override def callToReturnFlow(call: LLVMStatement, in: NativeTaintFact, successor: Option[LLVMStatement],
                                  unbCallChain: Seq[NativeFunction]): Set[NativeTaintFact] = Set(in)

    override def needsPredecessor(statement: LLVMStatement): Boolean = statement.instruction match {
        case PHI(_) => true
        case _      => false
    }

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The fact which holds before the execution of the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    override protected def javaCallFlow(
        call:   LLVMStatement,
        callee: Method,
        in:     NativeTaintFact
    ): Set[TaintFact] =
        in match {
            // Taint formal parameter if actual parameter is tainted
            case NativeVariable(value) => call.instruction.asInstanceOf[Call].indexOfArgument(value) match {
                case Some(index) => Set(Variable(JavaIFDSProblem.switchParamAndVariableIndex(
                    index - 2,
                    callee.isStatic
                )))
                case None => Set()
            }
            // TODO pass other java taints
            case NativeTaintNullFact => Set(TaintNullFact)
            case _                   => Set() // Nothing to do
        }

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param exit The statement, which terminated the `callee`.
     * @param in The fact which holds before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    override protected def javaReturnFlow(
        exit:      JavaStatement,
        in:        TaintFact,
        call:      LLVMStatement,
        callFact:  NativeTaintFact,
        successor: Option[LLVMStatement]
    ): Set[NativeTaintFact] = {
        val callee = exit.callable
        if (sanitizesReturnValue(JNIMethod(callee))) return Set.empty
        var flows: Set[NativeTaintFact] = Set.empty
        in match {
            case StaticField(classType, fieldName) => flows += JavaStaticField(classType, fieldName)

            // Track the call chain to the sink back
            case FlowFact(flow) if !flow.contains(call.function) =>
                flows += NativeFlowFact(call.function +: flow)
            case _ =>
        }

        // Propagate taints of the return value
        if (exit.stmt.astID == ReturnValue.ASTID) {
            val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
            in match {
                case Variable(index) if returnValueDefinedBy.contains(index) =>
                    flows += NativeVariable(call.instruction)
                case _ => // Nothing to do
            }
        }
        flows
    }
}