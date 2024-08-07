/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.ConstantValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearCombinationEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.UnknownValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.UnknownValueEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.VariableValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.VariableValueEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Extended definition of the linear constant propagation problem, trying to resolve field accesses with the LCP on
 * fields analysis.
 */
class LinearConstantPropagationProblemExtended(project: SomeProject) extends LinearConstantPropagationProblem(project) {
    override def getEdgeFunctionForExpression(
        expr:   Expr[JavaStatement.V],
        source: JavaStatement
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        expr.astID match {
            case ArrayLength.ASTID =>
                UnknownValueEdgeFunction

            case ArrayLoad.ASTID =>
                UnknownValueEdgeFunction

            case GetField.ASTID =>
                val getFieldExpr = expr.asGetField
                val objectVar = getFieldExpr.objRef.asVar
                val fieldName = getFieldExpr.name

                val lcpOnFieldsEOptionP =
                    propertyStore((source.method, source), LCPOnFieldsPropertyMetaInformation.key)

                /* Decide based on the current result of the LCP on fields analysis */
                lcpOnFieldsEOptionP match {
                    case FinalP(property) =>
                        val value = getObjectFieldFromProperty(objectVar, fieldName)(property)
                        FinalEdgeFunction(value match {
                            case UnknownValue     => UnknownValueEdgeFunction
                            case ConstantValue(c) => LinearCombinationEdgeFunction(0, c, lattice.top)
                            case VariableValue    => VariableValueEdgeFunction
                        })

                    case InterimUBP(property) =>
                        val value = getObjectFieldFromProperty(objectVar, fieldName)(property)
                        value match {
                            case UnknownValue =>
                                InterimEdgeFunction(UnknownValueEdgeFunction, immutable.Set(lcpOnFieldsEOptionP))
                            case ConstantValue(c) =>
                                InterimEdgeFunction(
                                    LinearCombinationEdgeFunction(0, c, lattice.top),
                                    immutable.Set(lcpOnFieldsEOptionP)
                                )
                            case VariableValue =>
                                FinalEdgeFunction(VariableValueEdgeFunction)
                        }

                    case _ =>
                        InterimEdgeFunction(UnknownValueEdgeFunction, immutable.Set(lcpOnFieldsEOptionP))
                }

            case GetStatic.ASTID =>
                UnknownValueEdgeFunction

            /* Unchanged behavior for all other expressions */
            case _ => super.getEdgeFunctionForExpression(expr, source)
        }
    }

    private def getObjectFieldFromProperty(
        objectVar: JavaStatement.V,
        fieldName: String
    )(property: Property): LinearConstantPropagationValue = {
        property
            .asInstanceOf[LCPOnFieldsPropertyMetaInformation.Self]
            .results
            .filter {
                case (f: AbstractObjectFact, ObjectValue(values)) =>
                    objectVar.definedBy.contains(f.definedAtIndex) && values.contains(fieldName)
                case _ => false
            }
            .map(_._2)
            .foldLeft(UnknownValue: LinearConstantPropagationValue) {
                case (value, ObjectValue(values)) =>
                    lattice.meet(value, values(fieldName))
            }
    }
}
