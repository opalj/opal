/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMFunction, LLVMStatement, NativeIFDSProblem}
import org.opalj.ll.llvm.value.{Add, Alloca, Call, Function, Load, PHI, Ret, Store}
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintProblem

abstract class NativeForwardTaintProblem(project: SomeProject) extends NativeIFDSProblem[NativeFact](project) with TaintProblem[Function, LLVMStatement, NativeFact] {
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
            case NativeVariable(value) if value == store.src ⇒ Set(in, NativeVariable(store.dst))
            case NativeVariable(value) if value == store.dst ⇒ Set()
            case _                                           ⇒ Set(in)
        }
        case load: Load ⇒ in match {
            case NativeVariable(value) if value == load.src ⇒ Set(in, NativeVariable(load))
            case _                                          ⇒ Set(in)
        }
        case add: Add ⇒ in match {
            case NativeVariable(value) if value == add.op1 || value == add.op2 ⇒ Set(in, NativeVariable(add))
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
    override def callFlow(call: LLVMStatement, callee: Function, in: NativeFact): Set[NativeFact] = in match {
        // Taint formal parameter if actual parameter is tainted
        case NativeVariable(value) ⇒ call.instruction.asInstanceOf[Call].indexOfArgument(value) match {
            case Some(index) ⇒ Set(NativeVariable(callee.argument(index)))
            case None        ⇒ Set()
        }
        // TODO pass other java taints
        case NativeNullFact ⇒ Set(in)
        case _              ⇒ Set() // Nothing to do

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
            case NativeFlowFact(flow) if !flow.contains(LLVMFunction(call.function)) ⇒
                Set(NativeFlowFact(LLVMFunction(call.function) +: flow))
            case _ ⇒ Set()
        }
        if (exit.callable.name == "source") in match {
            case NativeNullFact ⇒ flows += NativeVariable(call.instruction)
        }
        if (exit.callable.name == "sink") in match {
            case NativeVariable(value) if value == exit.callable.argument(0) ⇒
                flows += NativeFlowFact(Seq(LLVMFunction(call.callable), LLVMFunction(exit.callable)))
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