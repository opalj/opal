/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.collection.immutable.:&:
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.ai.collectPCWithOperands
import org.opalj.ai.AIResult
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues
import org.opalj.br.instructions.IStoreInstruction
import org.opalj.br.instructions.LStoreInstruction
import org.opalj.ai.domain.TheCode
import org.opalj.ai.ValuesDomain
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.InstructionLocation
import org.opalj.issues.Operands
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.InstructionLocation

/**
 * Identifies computations of primitive values that lead to the same result as a
 * previous computation. Such computations (which could be a constant expression)
 * are generally useless and hinder code comprehension.
 *
 * @author Michael Eichberg
 */
object UselessReComputationsAnalysis {

    def apply(
        theProject: SomeProject, method: Method,
        result: AIResult { val domain: TheCode with ConcreteIntegerValues with ConcreteLongValues with ValuesDomain }
    ): Seq[Issue] = {

        import result.domain.ConcreteIntegerValue
        import result.domain.ConcreteLongValue
        import result.domain

        if (!domain.code.localVariableTable.isDefined)
            // This analysis requires debug information to increase the likelihood
            // that we identify the correct local variable re-assignments. Otherwise
            // we are not able to distinguish the reuse of a "register variable"/
            // local variable for a new/different purpose or the situation where
            // the same variable is updated the second time using the same
            // value.
            return Seq.empty;

        val operandsArray = result.operandsArray
        val localsArray = result.localsArray
        val code = domain.code

        val methodsWithValueReassignment =
            collectPCWithOperands(domain)(code, operandsArray) {
                case (
                    pc,
                    IStoreInstruction(index),
                    ConcreteIntegerValue(a) :&: _
                    ) if localsArray(pc) != null &&
                    domain.intValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                    code.localVariable(pc, index).map(lv => lv.startPC < pc).getOrElse(false) =>
                    (pc, index, a.toString)

                case (
                    pc,
                    LStoreInstruction(index),
                    ConcreteLongValue(a) :&: _
                    ) if localsArray(pc) != null &&
                    domain.longValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                    code.localVariable(pc, index).map(lv => lv.startPC < pc).getOrElse(false) =>
                    (pc, index, a.toString)
            }

        methodsWithValueReassignment.map { e =>
            val (pc, index, value) = e
            val lv = code.localVariable(pc, index).get
            val details = List(new Operands(code, pc, operandsArray(pc), localsArray(pc)))
            val location = new InstructionLocation(
                Some("useless (re-)assignment"), theProject, method, pc, details
            )

            Issue(
                "UselessReevaluation",
                Relevance.Low,
                s"(re-)assigned the same value ($value) to the same variable (${lv.name})",
                Set(IssueCategory.Comprehensibility),
                Set(IssueKind.ConstantComputation),
                List(location)
            )

        }
    }
}
