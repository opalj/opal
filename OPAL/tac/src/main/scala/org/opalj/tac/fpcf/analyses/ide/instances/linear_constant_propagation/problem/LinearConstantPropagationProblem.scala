/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem

import scala.annotation.unused

import scala.collection.immutable

import org.opalj.BinaryArithmeticOperators
import org.opalj.ai.domain.l1.DefaultIntegerRangeValues
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.IntConst
import org.opalj.tac.Var
import org.opalj.tac.fpcf.analyses.ide.problem.JavaIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.StmtAsCall

/**
 * Definition of the linear constant propagation problem
 */
class LinearConstantPropagationProblem(project: SomeProject)
    extends JavaIDEProblem[LinearConstantPropagationFact, LinearConstantPropagationValue](
        new JavaICFG(project)
    ) {
    override val nullFact: LinearConstantPropagationFact =
        NullFact

    override val lattice: MeetLattice[LinearConstantPropagationValue] =
        LinearConstantPropagationLattice

    override def getAdditionalSeeds(stmt: JavaStatement, callee: Method)(
        implicit propertyStore: PropertyStore
    ): collection.Set[LinearConstantPropagationFact] = {
        callee.parameterTypes
            .zipWithIndex
            .filter { case (paramType, _) => paramType.isIntegerType }
            .map { case (_, index) => VariableFact(s"param${index + 1}", -(index + 2)) }
            .toSet
    }

    override def getNormalFlowFunction(
        source: JavaStatement,
        target: JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        (sourceFact: LinearConstantPropagationFact) =>
            {
                source.stmt.astID match {
                    case Assignment.ASTID =>
                        val assignment = source.stmt.asAssignment
                        if (isExpressionInfluencedByFact(assignment.expr, sourceFact)) {
                            /* Generate fact for target of assignment if the expression is influenced by the source
                             * fact */
                            immutable.Set(sourceFact, VariableFact(assignment.targetVar.name, source.index))
                        } else {
                            immutable.Set(sourceFact)
                        }

                    case _ => immutable.Set(sourceFact)
                }
            }
    }

    protected def isExpressionInfluencedByFact(
        expr:       Expr[JavaStatement.V],
        sourceFact: LinearConstantPropagationFact
    ): Boolean = {
        expr.astID match {
            case IntConst.ASTID =>
                /* Only generate fact for constants from null fact */
                sourceFact == nullFact

            case BinaryExpr.ASTID =>
                val binaryExpr = expr.asBinaryExpr
                val leftExpr = binaryExpr.left
                val rightExpr = binaryExpr.right

                if (sourceFact == nullFact) {
                    /* Only generate fact by null fact for binary expressions if both subexpressions are influenced.
                     * This is needed for binary expressions with one constant and one variable. */
                    isExpressionInfluencedByFact(leftExpr, sourceFact)
                    && isExpressionInfluencedByFact (rightExpr, sourceFact)
                } else {
                    /* If source fact is not null fact, generate new fact if one subexpression is influenced by the
                     * source fact */
                    isExpressionInfluencedByFact(leftExpr, sourceFact)
                    || isExpressionInfluencedByFact (rightExpr, sourceFact)
                }

            case Var.ASTID =>
                val varExpr = expr.asVar

                val hasConstantValue =
                    varExpr.value match {
                        case intRange: DefaultIntegerRangeValues#IntegerRange =>
                            intRange.lowerBound == intRange.upperBound
                        case _ => false
                    }

                sourceFact match {
                    case NullFact =>
                        /* Generate fact by null fact for variables if it is definitely a constant */
                        hasConstantValue
                    case VariableFact(_, definedAtIndex) =>
                        /* Generate fact only if it is not detected as constant (by the value analysis) and the variable
                         * represented by the source fact is one possible initializer of the target variable */
                        !hasConstantValue && varExpr.definedBy.contains(definedAtIndex)
                }

            case ArrayLength.ASTID =>
                /* Generate for array length expressions only by null fact */
                sourceFact == nullFact

            case ArrayLoad.ASTID =>
                val arrayLoadExpr = expr.asArrayLoad
                val arrayVar = arrayLoadExpr.arrayRef.asVar
                /* Generate for array access expressions only by null fact and if array stores integers */
                sourceFact == nullFact &&
                    arrayVar.value.asReferenceValue.asReferenceType.asArrayType.componentType.isIntegerType

            case GetField.ASTID =>
                val getFieldExpr = expr.asGetField
                /* Generate for field access expressions only by null fact and if field is of type integer */
                sourceFact == nullFact && getFieldExpr.declaredFieldType.isIntegerType

            case GetStatic.ASTID =>
                val getStaticExpr = expr.asGetStatic
                /* Generate for field access expressions only by null fact and if field is of type integer */
                sourceFact == nullFact && getStaticExpr.declaredFieldType.isIntegerType

            case _ => false
        }
    }

    override def getCallFlowFunction(
        callSite:    JavaStatement,
        calleeEntry: JavaStatement,
        callee:      Method
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        (sourceFact: LinearConstantPropagationFact) =>
            {
                /* Only propagate to callees that return integers */
                if (!callee.returnType.isIntegerType) {
                    immutable.Set.empty
                } else {
                    sourceFact match {
                        case NullFact =>
                            /* Always propagate null facts */
                            immutable.Set(sourceFact)

                        case VariableFact(_, definedAtIndex) =>
                            val callStmt = callSite.stmt.asCall()

                            /* Parameters and their types (excluding the implicit 'this' reference) */
                            val params = callStmt.params
                            val paramTypes = callee.parameterTypes

                            params
                                .zipWithIndex
                                .filter { case (param, index) =>
                                    /* Only parameters that are of type integer and where the variable represented by
                                     * the source fact is one possible initializer */
                                    paramTypes(index).isIntegerType && param.asVar.definedBy.contains(definedAtIndex)
                                }
                                .map { case (_, index) =>
                                    VariableFact(s"param${index + 1}", -(index + 2))
                                }
                                .toSet
                    }
                }
            }
    }

    override def getReturnFlowFunction(
        calleeExit: JavaStatement,
        callee:     Method,
        returnSite: JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        (sourceFact: LinearConstantPropagationFact) =>
            {
                /* Only propagate to return site if callee returns an integer */
                if (!callee.returnType.isIntegerType) {
                    immutable.Set.empty
                } else {
                    sourceFact match {
                        case NullFact =>
                            /* Always propagate null fact */
                            immutable.Set(sourceFact)

                        case VariableFact(_, definedAtIndex) =>
                            returnSite.stmt.astID match {
                                case Assignment.ASTID =>
                                    val assignment = returnSite.stmt.asAssignment

                                    val returnExpr = calleeExit.stmt.asReturnValue.expr
                                    /* Only propagate if the variable represented by the source fact is one possible
                                     * initializer of the variable at the return site */
                                    if (returnExpr.asVar.definedBy.contains(definedAtIndex)) {
                                        immutable.Set(VariableFact(assignment.targetVar.name, returnSite.index))
                                    } else {
                                        immutable.Set.empty
                                    }

                                case _ => immutable.Set.empty
                            }
                    }
                }
            }
    }

    override def getCallToReturnFlowFunction(
        callSite:   JavaStatement,
        callee:     Method,
        returnSite: JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        identityFlowFunction
    }

    override def getNormalEdgeFunction(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement,
        targetFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        if (sourceFact == targetFact) {
            /* Simply propagates a fact through the method */
            return identityEdgeFunction
        }

        source.stmt.astID match {
            case Assignment.ASTID =>
                val assignment = source.stmt.asAssignment
                getEdgeFunctionForExpression(assignment.expr, source)
            case _ => identityEdgeFunction
        }
    }

    protected def getEdgeFunctionForExpression(
        expr:   Expr[JavaStatement.V],
        source: JavaStatement
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        expr.astID match {
            case IntConst.ASTID =>
                LinearCombinationEdgeFunction(0, expr.asIntConst.value)

            case BinaryExpr.ASTID =>
                val binaryExpr = expr.asBinaryExpr
                val leftExpr = binaryExpr.left
                val rightExpr = binaryExpr.right

                if (leftExpr.astID != Var.ASTID && leftExpr.astID != IntConst.ASTID
                    || rightExpr.astID != Var.ASTID && rightExpr.astID != IntConst.ASTID
                ) {
                    throw new IllegalArgumentException(s"Combination ($leftExpr, $rightExpr) should not occur here!")
                }

                /* Try to resolve an constant or variable expression to a constant value */
                val getValueForExpr: Expr[JavaStatement.V] => Option[Int] = expr => {
                    expr.astID match {
                        case Var.ASTID =>
                            val var0 = expr.asVar
                            var0.value match {
                                case intRange: DefaultIntegerRangeValues#IntegerRange =>
                                    if (intRange.lowerBound == intRange.upperBound) {
                                        /* If boundaries are equal, the value is constant */
                                        Some(intRange.lowerBound)
                                    } else if (var0.definedBy.size > 1) {
                                        // TODO (IDE) ADD TESTS FOR THIS (CAN WE REFACTOR THIS/DOES THIS REDUCE THE
                                        //  ACCURACY)
                                        return VariableValueEdgeFunction
                                    } else {
                                        None
                                    }
                                case _ =>
                                    None
                            }

                        case IntConst.ASTID => Some(expr.asIntConst.value)
                    }
                }

                val leftValue = getValueForExpr(leftExpr)
                val rightValue = getValueForExpr(rightExpr)

                (leftValue, rightValue, binaryExpr.op) match {
                    case (Some(l), Some(r), BinaryArithmeticOperators.Add) =>
                        LinearCombinationEdgeFunction(0, l + r)
                    case (Some(l), None, BinaryArithmeticOperators.Add) =>
                        LinearCombinationEdgeFunction(1, l)
                    case (None, Some(r), BinaryArithmeticOperators.Add) =>
                        LinearCombinationEdgeFunction(1, r)

                    case (Some(l), Some(r), BinaryArithmeticOperators.Subtract) =>
                        LinearCombinationEdgeFunction(0, l - r)
                    case (Some(l), None, BinaryArithmeticOperators.Subtract) =>
                        LinearCombinationEdgeFunction(-1, l)
                    case (None, Some(r), BinaryArithmeticOperators.Subtract) =>
                        LinearCombinationEdgeFunction(1, -r)

                    case (Some(l), Some(r), BinaryArithmeticOperators.Multiply) =>
                        LinearCombinationEdgeFunction(0, l * r)
                    case (Some(l), None, BinaryArithmeticOperators.Multiply) =>
                        LinearCombinationEdgeFunction(l, 0)
                    case (None, Some(r), BinaryArithmeticOperators.Multiply) =>
                        LinearCombinationEdgeFunction(r, 0)

                    case (None, None, _) =>
                        VariableValueEdgeFunction

                    case (_, _, op) =>
                        throw new UnsupportedOperationException(s"Operator $op is not implemented!")
                }

            case ArrayLength.ASTID =>
                VariableValueEdgeFunction

            case ArrayLoad.ASTID =>
                VariableValueEdgeFunction

            case GetField.ASTID =>
                VariableValueEdgeFunction

            case GetStatic.ASTID =>
                VariableValueEdgeFunction

            case _ =>
                throw new IllegalArgumentException(s"Expression $expr should not occur here!")
        }
    }

    override def getCallEdgeFunction(
        callSite:        JavaStatement,
        callSiteFact:    LinearConstantPropagationFact,
        calleeEntry:     JavaStatement,
        calleeEntryFact: LinearConstantPropagationFact,
        callee:          Method
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = identityEdgeFunction

    override def getReturnEdgeFunction(
        calleeExit:     JavaStatement,
        calleeExitFact: LinearConstantPropagationFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = identityEdgeFunction

    override def getCallToReturnEdgeFunction(
        callSite:       JavaStatement,
        callSiteFact:   LinearConstantPropagationFact,
        returnSite:     JavaStatement,
        returnSiteFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = identityEdgeFunction
}
