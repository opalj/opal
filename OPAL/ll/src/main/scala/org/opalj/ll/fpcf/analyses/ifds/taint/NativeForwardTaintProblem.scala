/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMFunction, LLVMStatement, NativeFunction, NativeIFDSProblem}
import org.opalj.ll.llvm.value.{Add, Alloca, BitCast, Call, GetElementPtr, Load, PHI, Ret, Store, Sub}
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintProblem

abstract class NativeForwardTaintProblem(project: SomeProject) extends NativeIFDSProblem[NativeFact](project) with TaintProblem[LLVMFunction, LLVMStatement, NativeFact] {
    override def nullFact: NativeFact = NativeNullFact

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
    override def normalFlow(statement: LLVMStatement, in: NativeFact, predecessor: Option[LLVMStatement]): Set[NativeFact] = statement.instruction match {
        case _: Alloca ⇒ Set(in)
        case store: Store ⇒ in match {
            case NativeVariable(value) if value == store.src ⇒ store.dst match {
                case dst: Alloca                          ⇒ Set(in, NativeVariable(dst))
                case gep: GetElementPtr if gep.isConstant ⇒ Set(in, NativeArrayElement(gep.base, gep.constants))
            }
            case NativeArrayElement(base, indices) if store.src == base ⇒ Set(in, NativeArrayElement(store.dst, indices))
            case NativeVariable(value) if value == store.dst ⇒ Set()
            case _ ⇒ Set(in)
        }
        case load: Load ⇒ in match {
            case NativeVariable(value) if value == load.src ⇒ Set(in, NativeVariable(load))
            case NativeArrayElement(base, indices) ⇒ load.src match {
                case gep: GetElementPtr if gep.isConstant && gep.base == base && gep.constants == indices ⇒ Set(in, NativeVariable(load))
                case _ ⇒ Set(in, NativeArrayElement(load, indices))
            }
            case _ ⇒ Set(in)
        }
        case add: Add ⇒ in match {
            case NativeVariable(value) if value == add.op1 || value == add.op2 ⇒ Set(in, NativeVariable(add))
            case _ ⇒ Set(in)
        }
        case sub: Sub ⇒ in match {
            case NativeVariable(value) if value == sub.op1 || value == sub.op2 ⇒ Set(in, NativeVariable(sub))
            case _ ⇒ Set(in)
        }
        case gep: GetElementPtr ⇒ in match {
            case NativeVariable(value) if value == gep.base ⇒ Set(in, NativeVariable(gep))
            case NativeArrayElement(base, indices) if base == gep.base && gep.isZero ⇒ Set(in, NativeArrayElement(gep, indices))
            case _ ⇒ Set(in)
        }
        case bitcast: BitCast ⇒ in match {
            case NativeVariable(value) if value == bitcast.operand(0) ⇒ Set(in, NativeVariable(bitcast))
            case _ ⇒ Set(in)
        }
        case _ ⇒ Set(in)
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
    override def callFlow(call: LLVMStatement, callee: NativeFunction, in: NativeFact): Set[NativeFact] = callee match {
        case LLVMFunction(callee) ⇒
            in match {
                // Taint formal parameter if actual parameter is tainted
                case NativeVariable(value) ⇒ call.instruction.asInstanceOf[Call].indexOfArgument(value) match {
                    case Some(index) ⇒ Set(NativeVariable(callee.argument(index)))
                    case None        ⇒ Set()
                }
                // TODO pass other java taints
                case NativeNullFact ⇒ Set(in)
                case NativeArrayElement(base, indices) ⇒ call.instruction.asInstanceOf[Call].indexOfArgument(base) match {
                    case Some(index) ⇒ Set(NativeArrayElement(callee.argument(index), indices))
                    case None        ⇒ Set()
                }
                case _ ⇒ Set() // Nothing to do
            }
        case _ ⇒ throw new RuntimeException("this case should be handled by outsideAnalysisContext")
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
    override def returnFlow(exit: LLVMStatement, in: NativeFact, call: LLVMStatement, callFact: NativeFact, successor: LLVMStatement): Set[NativeFact] = {
        val callee = exit.callable()
        var flows: Set[NativeFact] = if (sanitizesReturnValue(callee)) Set.empty else in match {
            case NativeVariable(value) ⇒ exit.instruction match {
                case ret: Ret if ret.value == value ⇒ Set(NativeVariable(call.instruction))
                case _: Ret                         ⇒ Set()
                case _                              ⇒ Set()
            }
            case NativeNullFact ⇒ Set(NativeNullFact)
            case NativeFlowFact(flow) if !flow.contains(call.function) ⇒
                Set(NativeFlowFact(call.function +: flow))
            case _ ⇒ Set()
        }
        if (exit.callable.name == "source") in match {
            case NativeNullFact ⇒ flows += NativeVariable(call.instruction)
        }
        if (exit.callable.name == "sink") in match {
            case NativeVariable(value) if value == exit.callable.function.argument(0) ⇒
                flows += NativeFlowFact(Seq(call.callable, exit.callable))
            case _ ⇒
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
    override def callToReturnFlow(call: LLVMStatement, in: NativeFact, successor: LLVMStatement): Set[NativeFact] = Set(in)

    override def needsPredecessor(statement: LLVMStatement): Boolean = statement.instruction match {
        case PHI(_) ⇒ true
        case _      ⇒ false
    }
}