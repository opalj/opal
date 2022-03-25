/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMStatement, NativeIFDSProblem}
import org.opalj.ll.llvm.{Function, PHI}
import org.opalj.tac.fpcf.analyses.ifds.taint.NewTaintProblem

 abstract class NativeForwardTaintProblem(project: SomeProject) extends NativeIFDSProblem[NativeFact](project) with NewTaintProblem[Function, LLVMStatement, NativeFact] {
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
     override def normalFlow(statement: LLVMStatement, in: NativeFact, predecessor: Option[LLVMStatement]): Set[NativeFact] = ???

     /**
      * Computes the data flow for a call to start edge.
      *
      * @param call   The analyzed call statement.
      * @param callee The called method, for which the data flow shall be computed.
      * @param in     The fact which holds before the execution of the `call`.
      * @param source The entity, which is analyzed.
      * @return The facts, which hold after the execution of `statement` under the assumption that
      *         the facts in `in` held before `statement` and `statement` calls `callee`.
      */
     override def callFlow(call: LLVMStatement, callee: Function, in: NativeFact): Set[NativeFact] = ???

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
     override def returnFlow(exit: LLVMStatement, in: NativeFact, call: LLVMStatement, callFact: NativeFact): Set[NativeFact] = ???

     /**
      * Computes the data flow for a call to return edge.
      *
      * @param call The statement, which invoked the call.
      * @param in   The facts, which hold before the `call`.
      * @return The facts, which hold after the call independently of what happens in the callee
      *         under the assumption that `in` held before `call`.
      */
     override def callToReturnFlow(call: LLVMStatement, in: NativeFact): Set[NativeFact] = ???

     override def needsPredecessor(statement: LLVMStatement): Boolean = statement.instruction match {
       case PHI(_) => true
       case _ => false
     }
 }