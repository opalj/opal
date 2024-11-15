/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem

import scala.annotation.unused

import scala.collection.immutable

import org.opalj.BinaryArithmeticOperators
import org.opalj.ai.domain.l1.DefaultIntegerRangeValues
import org.opalj.br.Method
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.IdentityFlowFunction
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
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.StmtAsCall

/**
 * Definition of the linear constant propagation problem
 */
class LinearConstantPropagationProblem
    extends JavaIDEProblem[LinearConstantPropagationFact, LinearConstantPropagationValue] {
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
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        new FlowFunction[LinearConstantPropagationFact] {
            override def compute(): FactsAndDependees = {
                source.stmt.astID match {
                    case Assignment.ASTID =>
                        val assignment = source.stmt.asAssignment
                        if (isExpressionGeneratedByFact(assignment.expr)(source, sourceFact, target)) {
                            /* Generate fact for target of assignment if the expression is influenced by the source
                             * fact */
                            immutable.Set(sourceFact, VariableFact(assignment.targetVar.name, source.pc))
                        } else {
                            immutable.Set(sourceFact)
                        }

                    case _ => immutable.Set(sourceFact)
                }
            }
        }
    }

    private def isExpressionGeneratedByFact(
        expr: Expr[JavaStatement.V]
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement
    ): Boolean = {
        (expr.astID match {
            case IntConst.ASTID =>
                isIntConstExpressionGeneratedByFact(expr.asIntConst)(_, _, _)

            case BinaryExpr.ASTID =>
                isBinaryExpressionGeneratedByFact(expr.asBinaryExpr)(_, _, _)

            case Var.ASTID =>
                isVarExpressionGeneratedByFact(expr.asVar)(_, _, _)

            case ArrayLength.ASTID =>
                isArrayLengthExpressionGeneratedByFact(expr.asArrayLength)(_, _, _)

            case ArrayLoad.ASTID =>
                isArrayLoadExpressionGeneratedByFact(expr.asArrayLoad)(_, _, _)

            case GetField.ASTID =>
                isGetFieldExpressionGeneratedByFact(expr.asGetField)(_, _, _)

            case GetStatic.ASTID =>
                isGetStaticExpressionGeneratedByFact(expr.asGetStatic)(_, _, _)

            case _ => return false
        })(
            source,
            sourceFact,
            target
        )
    }

    protected def isIntConstExpressionGeneratedByFact(
        @unused intConstExpr: IntConst
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        /* Only generate fact for constants from null fact */
        sourceFact == nullFact
    }

    protected def isBinaryExpressionGeneratedByFact(
        binaryExpr: BinaryExpr[JavaStatement.V]
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        if (sourceFact == nullFact) {
            /* Only generate fact by null fact for binary expressions if both subexpressions are influenced.
             * This is needed for binary expressions with one constant and one variable. */
            (leftExpr.isConst || isVarExpressionGeneratedByFact(leftExpr.asVar)(source, sourceFact, target)) &&
            (rightExpr.isConst || isVarExpressionGeneratedByFact(rightExpr.asVar)(source, sourceFact, target))
        } else {
            /* If source fact is not null fact, generate new fact if one subexpression is influenced by the
             * source fact */
            leftExpr.isVar && isVarExpressionGeneratedByFact(leftExpr.asVar)(source, sourceFact, target) ||
            rightExpr.isVar && isVarExpressionGeneratedByFact(rightExpr.asVar)(source, sourceFact, target)
        }
    }

    protected def isVarExpressionGeneratedByFact(
        varExpr: JavaStatement.V
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
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
    }

    protected def isArrayLengthExpressionGeneratedByFact(
        @unused arrayLengthExpr: ArrayLength[JavaStatement.V]
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        /* Generate fact for array length expressions only by null fact */
        sourceFact == nullFact
    }

    protected def isArrayLoadExpressionGeneratedByFact(
        arrayLoadExpr: ArrayLoad[JavaStatement.V]
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        val arrayVar = arrayLoadExpr.arrayRef.asVar
        /* Generate fact for array access expressions only by null fact and if array stores integers */
        sourceFact == nullFact &&
            arrayVar.value.asReferenceValue.asReferenceType.asArrayType.componentType.isIntegerType
    }

    protected def isGetFieldExpressionGeneratedByFact(
        getFieldExpr: GetField[JavaStatement.V]
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        /* Generate fact for field access expressions only by null fact and if field is of type integer */
        sourceFact == nullFact && getFieldExpr.declaredFieldType.isIntegerType
    }

    protected def isGetStaticExpressionGeneratedByFact(
        getStaticExpr: GetStatic
    )(
        @unused source: JavaStatement,
        sourceFact:     LinearConstantPropagationFact,
        @unused target: JavaStatement
    ): Boolean = {
        /* Generate fact for field access expressions only by null fact and if field is of type integer */
        sourceFact == nullFact && getStaticExpr.declaredFieldType.isIntegerType
    }

    override def getCallFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LinearConstantPropagationFact,
        calleeEntry:  JavaStatement,
        callee:       Method
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        new FlowFunction[LinearConstantPropagationFact] {
            override def compute(): FactsAndDependees = {
                /* Only propagate to callees that return integers */
                if (!callee.returnType.isIntegerType) {
                    immutable.Set.empty
                } else {
                    callSiteFact match {
                        case NullFact =>
                            /* Always propagate null facts */
                            immutable.Set(callSiteFact)

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
    }

    override def getReturnFlowFunction(
        calleeExit:     JavaStatement,
        calleeExitFact: LinearConstantPropagationFact,
        callee:         Method,
        returnSite:     JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        new FlowFunction[LinearConstantPropagationFact] {
            override def compute(): FactsAndDependees = {
                /* Only propagate to return site if callee returns an integer */
                if (!callee.returnType.isIntegerType) {
                    immutable.Set.empty
                } else {
                    calleeExitFact match {
                        case NullFact =>
                            /* Always propagate null fact */
                            immutable.Set(calleeExitFact)

                        case VariableFact(_, definedAtIndex) =>
                            returnSite.stmt.astID match {
                                case Assignment.ASTID =>
                                    val assignment = returnSite.stmt.asAssignment

                                    val returnExpr = calleeExit.stmt.asReturnValue.expr
                                    /* Only propagate if the variable represented by the source fact is one possible
                                     * initializer of the variable at the return site */
                                    if (returnExpr.asVar.definedBy.contains(definedAtIndex)) {
                                        immutable.Set(VariableFact(assignment.targetVar.name, returnSite.pc))
                                    } else {
                                        immutable.Set.empty
                                    }

                                case _ => immutable.Set.empty
                            }
                    }
                }
            }
        }
    }

    override def getCallToReturnFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LinearConstantPropagationFact,
        callee:       Method,
        returnSite:   JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        IdentityFlowFunction[LinearConstantPropagationFact](callSiteFact)
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
                val expr = assignment.expr

                (expr.astID match {
                    case IntConst.ASTID =>
                        getNormalEdgeFunctionForIntConstExpression(expr.asIntConst)(_, _, _, _)

                    case BinaryExpr.ASTID =>
                        getNormalEdgeFunctionForBinaryExpression(expr.asBinaryExpr)(_, _, _, _)

                    case ArrayLength.ASTID =>
                        getNormalEdgeFunctionForArrayLength(expr.asArrayLength)(_, _, _, _)

                    case ArrayLoad.ASTID =>
                        getNormalEdgeFunctionForArrayLoad(expr.asArrayLoad)(_, _, _, _)

                    case GetField.ASTID =>
                        getNormalEdgeFunctionForGetField(expr.asGetField)(_, _, _, _)

                    case GetStatic.ASTID =>
                        getNormalEdgeFunctionForGetStatic(expr.asGetStatic)(_, _, _, _)

                    case _ =>
                        throw new IllegalArgumentException(s"Expression $expr should not occur here!")
                })(
                    source,
                    sourceFact,
                    target,
                    targetFact
                )

            case _ => identityEdgeFunction
        }
    }

    protected def getNormalEdgeFunctionForIntConstExpression(
        intConstExpr: IntConst
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        LinearCombinationEdgeFunction(0, intConstExpr.value)
    }

    protected def getNormalEdgeFunctionForBinaryExpression(
        binaryExpr: BinaryExpr[JavaStatement.V]
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
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

            case (_, _, BinaryArithmeticOperators.And) =>
                VariableValueEdgeFunction
            case (_, _, BinaryArithmeticOperators.Or) =>
                VariableValueEdgeFunction
            case (_, _, BinaryArithmeticOperators.XOr) =>
                VariableValueEdgeFunction

            case (_, _, op) =>
                throw new UnsupportedOperationException(s"Operator $op is not implemented!")
        }
    }

    protected def getNormalEdgeFunctionForArrayLength(
        @unused arrayLengthExpr: ArrayLength[JavaStatement.V]
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        VariableValueEdgeFunction
    }

    protected def getNormalEdgeFunctionForArrayLoad(
        @unused arrayLoadExpr: ArrayLoad[JavaStatement.V]
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        VariableValueEdgeFunction
    }

    protected def getNormalEdgeFunctionForGetField(
        @unused getFieldExpr: GetField[JavaStatement.V]
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        VariableValueEdgeFunction
    }

    protected def getNormalEdgeFunctionForGetStatic(
        @unused getStaticExpr: GetStatic
    )(
        @unused source:     JavaStatement,
        @unused sourceFact: LinearConstantPropagationFact,
        @unused target:     JavaStatement,
        @unused targetFact: LinearConstantPropagationFact
    )(implicit @unused propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        VariableValueEdgeFunction
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
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = identityEdgeFunction

    override def hasPrecomputedFlowAndSummaryFunction(
        callSite:     JavaStatement,
        callSiteFact: LinearConstantPropagationFact,
        callee:       Method
    )(implicit propertyStore: PropertyStore): Boolean = {
        if (callee.isNative) {
            return true
        }

        super.hasPrecomputedFlowAndSummaryFunction(callSite, callSiteFact, callee)
    }

    override def getPrecomputedFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LinearConstantPropagationFact,
        callee:       Method,
        returnSite:   JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LinearConstantPropagationFact] = {
        if (callee.isNative) {
            return emptyFlowFunction
        }

        super.getPrecomputedFlowFunction(callSite, callSiteFact, callee, returnSite)
    }

    override def getPrecomputedSummaryFunction(
        callSite:       JavaStatement,
        callSiteFact:   LinearConstantPropagationFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunction[LinearConstantPropagationValue] = {
        if (callee.isNative) {
            return identityEdgeFunction
        }

        super.getPrecomputedSummaryFunction(callSite, callSiteFact, callee, returnSite, returnSiteFact)
    }
}
