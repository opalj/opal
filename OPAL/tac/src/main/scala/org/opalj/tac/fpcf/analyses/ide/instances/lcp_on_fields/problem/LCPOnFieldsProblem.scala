/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.PutField
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.problem.JavaForwardIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.StmtAsCall

/**
 * Definition of linear constant propagation on fields problem. This implementation detects basic cases of linear
 * constant propagation involving fields. It detects direct field assignments but fails to detect assignments done in a
 * method where the value is passed as an argument (e.g. a classical setter). Similar, array read accesses can only be
 * resolved if the index is a constant literal.
 * This implementation is mainly intended to be an example of a cyclic IDE analysis.
 */
class LCPOnFieldsProblem(project: SomeProject)
    extends JavaForwardIDEProblem[LCPOnFieldsFact, LCPOnFieldsValue](project) {
    override val nullFact: LCPOnFieldsFact =
        NullFact

    override val lattice: MeetLattice[LCPOnFieldsValue] =
        LCPOnFieldsLattice

    override def getNormalFlowFunction(source: JavaStatement, target: JavaStatement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                (source.stmt.astID, sourceFact) match {
                    case (Assignment.ASTID, NullFact) =>
                        val assignment = source.stmt.asAssignment
                        assignment.expr.astID match {
                            case New.ASTID =>
                                /* Generate new object fact from null fact if assignment is a 'new' expression */
                                immutable.Set(sourceFact, NewObjectFact(assignment.targetVar.name, source.pc))

                            case NewArray.ASTID =>
                                /* Generate new array fact from null fact if assignment is a 'new array' expression for
                                 * an integer array */
                                if (assignment.expr.asNewArray.tpe.componentType.isIntegerType) {
                                    immutable.Set(sourceFact, NewArrayFact(assignment.targetVar.name, source.pc))
                                } else {
                                    immutable.Set(sourceFact)
                                }

                            case _ => immutable.Set(sourceFact)
                        }

                    case (PutField.ASTID, f: AbstractObjectFact) =>
                        val putField = source.stmt.asPutField
                        /* Only consider field assignments for integers */
                        if (putField.declaredFieldType.isIntegerType) {
                            val targetObject = putField.objRef.asVar
                            if (targetObject.definedBy.contains(f.definedAtIndex)) {
                                /* Generate new (short-lived) fact to handle field assignment */
                                immutable.Set(PutFieldFact(f.name, f.definedAtIndex, putField.name))
                            } else {
                                immutable.Set(f.toObjectFact)
                            }
                        } else {
                            immutable.Set(f.toObjectFact)
                        }

                    case (ArrayStore.ASTID, f: AbstractArrayFact) =>
                        val arrayStore = source.stmt.asArrayStore
                        val arrayVar = arrayStore.arrayRef.asVar
                        if (arrayVar.definedBy.contains(f.definedAtIndex)) {
                            immutable.Set(PutElementFact(f.name, f.definedAtIndex))
                        } else {
                            immutable.Set(f.toArrayFact)
                        }

                    case (_, f: AbstractEntityFact) =>
                        /* Specialized facts only live for one step and are turned back into basic ones afterwards */
                        immutable.Set(f match {
                            case f: AbstractObjectFact => f.toObjectFact
                            case f: AbstractArrayFact  => f.toArrayFact
                        })

                    case _ => immutable.Set(sourceFact)
                }
            }
    }

    override def getCallFlowFunction(
        callSite:    JavaStatement,
        calleeEntry: JavaStatement,
        callee:      Method
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                sourceFact match {
                    case NullFact =>
                        /* Only propagate null fact if function returns an object or an array of integers */
                        if (callee.returnType.isObjectType) {
                            immutable.Set(sourceFact)
                        } else if (callee.returnType.isArrayType &&
                                   callee.returnType.asArrayType.componentType.isIntegerType
                        ) {
                            immutable.Set(sourceFact)
                        } else {
                            immutable.Set.empty
                        }

                    case f: AbstractEntityFact =>
                        val callStmt = callSite.stmt.asCall()

                        /* All parameters (including the implicit 'this' reference) */
                        val allParams = callStmt.allParams

                        allParams
                            .zipWithIndex
                            .filter { case (param, _) =>
                                /* Only parameters where the variable represented by the source fact is one possible
                                 * initializer */
                                param.asVar.definedBy.contains(f.definedAtIndex)
                            }
                            .map { case (_, index) =>
                                f match {
                                    case _: AbstractObjectFact => ObjectFact(s"param$index", -(index + 1))
                                    case _: AbstractArrayFact  => ArrayFact(s"param$index", -(index + 1))
                                }
                            }
                            .toSet
                }
            }
    }

    override def getReturnFlowFunction(
        calleeExit: JavaStatement,
        callee:     Method,
        returnSite: JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                sourceFact match {
                    case NullFact =>
                        /* Always propagate null fact */
                        immutable.Set(sourceFact)

                    case f: AbstractEntityFact =>
                        val definedAtIndex = f.definedAtIndex

                        val callStmt = returnSite.stmt.asCall()

                        val allParams = callStmt.allParams

                        /* Distinguish parameters and local variables */
                        if (definedAtIndex < 0) {
                            val paramIndex = -(definedAtIndex + 1)
                            val param = allParams(paramIndex)
                            val paramName = param.asVar.name.substring(1, param.asVar.name.length - 1)
                            param.asVar.definedBy.map { dAI =>
                                f match {
                                    case _: AbstractObjectFact => ObjectFact(paramName, dAI)
                                    case _: AbstractArrayFact  => ArrayFact(paramName, dAI)
                                }
                            }.toSet
                        } else {
                            returnSite.stmt.astID match {
                                case Assignment.ASTID =>
                                    val assignment = returnSite.stmt.asAssignment

                                    val returnExpr = calleeExit.stmt.asReturnValue.expr
                                    /* Only propagate if the variable represented by the source fact is one possible
                                     * initializer of the variable at the return site */
                                    if (returnExpr.asVar.definedBy.contains(f.definedAtIndex)) {
                                        immutable.Set(f match {
                                            case _: AbstractObjectFact =>
                                                ObjectFact(assignment.targetVar.name, returnSite.pc)
                                            case _: AbstractArrayFact =>
                                                ArrayFact(assignment.targetVar.name, returnSite.pc)
                                        })
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
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                val callStmt = returnSite.stmt.asCall()

                sourceFact match {
                    case NullFact =>
                        /* Always propagate null fact */
                        immutable.Set(sourceFact)

                    case f: AbstractEntityFact =>
                        /* Only propagate if the variable represented by the source fact is no initializer of one of the
                         * parameters */
                        if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                            immutable.Set.empty
                        } else {
                            immutable.Set(f match {
                                case f: AbstractObjectFact => f.toObjectFact
                                case f: AbstractArrayFact  => f.toArrayFact
                            })
                        }
                }
            }
    }

    override def getNormalEdgeFunction(
        source:     JavaStatement,
        sourceFact: LCPOnFieldsFact,
        target:     JavaStatement,
        targetFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = {
        if (sourceFact == targetFact) {
            return FinalEdgeFunction(identityEdgeFunction)
        }

        targetFact match {
            case NewObjectFact(_, _) =>
                FinalEdgeFunction(NewObjectEdgeFunction)

            case PutFieldFact(_, _, fieldName) =>
                val valueVar = source.stmt.asPutField.value.asVar

                val lcpEOptionP =
                    propertyStore((source.method, source), LinearConstantPropagationPropertyMetaInformation.key)

                /* Decide based on the current result of the linear constant propagation analysis */
                lcpEOptionP match {
                    case FinalP(property) =>
                        val value = getVariableFromProperty(valueVar)(property)
                        FinalEdgeFunction(PutFieldEdgeFunction(fieldName, value))

                    case InterimUBP(property) =>
                        val value = getVariableFromProperty(valueVar)(property)
                        value match {
                            case linear_constant_propagation.problem.UnknownValue =>
                                InterimEdgeFunction(
                                    PutFieldEdgeFunction(fieldName, value),
                                    immutable.Set(lcpEOptionP)
                                )
                            case linear_constant_propagation.problem.ConstantValue(_) =>
                                InterimEdgeFunction(
                                    PutFieldEdgeFunction(fieldName, value),
                                    immutable.Set(lcpEOptionP)
                                )
                            case linear_constant_propagation.problem.VariableValue =>
                                FinalEdgeFunction(PutFieldEdgeFunction(fieldName, value))
                        }

                    case _ =>
                        InterimEdgeFunction(
                            PutFieldEdgeFunction(
                                fieldName,
                                linear_constant_propagation.problem.UnknownValue
                            ),
                            immutable.Set(lcpEOptionP)
                        )
                }

            case NewArrayFact(_, _) =>
                FinalEdgeFunction(NewArrayEdgeFunction())

            case PutElementFact(_, _) =>
                val arrayStore = source.stmt.asArrayStore
                val indexVar = arrayStore.index.asVar
                val valueVar = arrayStore.value.asVar

                val lcpEOptionP =
                    propertyStore((source.method, source), LinearConstantPropagationPropertyMetaInformation.key)

                /* Decide based on the current result of the linear constant propagation analysis */
                lcpEOptionP match {
                    case FinalP(property) =>
                        val index = getVariableFromProperty(indexVar)(property)
                        val value = getVariableFromProperty(valueVar)(property)
                        FinalEdgeFunction(PutElementEdgeFunction(index, value))

                    case InterimUBP(property) =>
                        val index = getVariableFromProperty(indexVar)(property)
                        val value = getVariableFromProperty(valueVar)(property)
                        InterimEdgeFunction(PutElementEdgeFunction(index, value), immutable.Set(lcpEOptionP))

                    case _ =>
                        InterimEdgeFunction(
                            PutElementEdgeFunction(
                                linear_constant_propagation.problem.UnknownValue,
                                linear_constant_propagation.problem.UnknownValue
                            ),
                            immutable.Set(lcpEOptionP)
                        )
                }

            case _ => FinalEdgeFunction(identityEdgeFunction)
        }
    }

    private def getVariableFromProperty(var0: JavaStatement.V)(property: Property): LinearConstantPropagationValue = {
        property
            .asInstanceOf[LinearConstantPropagationPropertyMetaInformation.Self]
            .results
            .filter {
                case (linear_constant_propagation.problem.VariableFact(_, definedAtIndex), _) =>
                    var0.definedBy.contains(definedAtIndex)
                case _ => false
            }
            .map(_._2)
            .foldLeft(
                linear_constant_propagation.problem.UnknownValue: LinearConstantPropagationValue
            ) {
                case (value1, value2) =>
                    LinearConstantPropagationLattice.meet(value1, value2)
            }
    }

    override def getCallEdgeFunction(
        callSite:        JavaStatement,
        callSiteFact:    LCPOnFieldsFact,
        calleeEntry:     JavaStatement,
        calleeEntryFact: LCPOnFieldsFact,
        callee:          Method
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = identityEdgeFunction

    override def getReturnEdgeFunction(
        calleeExit:     JavaStatement,
        calleeExitFact: LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = identityEdgeFunction

    override def getCallToReturnEdgeFunction(
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = identityEdgeFunction

    override def hasPrecomputedFlowAndSummaryFunction(
        callSite:     JavaStatement,
        callSiteFact: LCPOnFieldsFact,
        callee:       Method
    )(implicit propertyStore: PropertyStore): Boolean = {
        if (callee.isNative) {
            return true
        }

        super.hasPrecomputedFlowAndSummaryFunction(callSite, callSiteFact, callee)
    }

    override def getPrecomputedFlowFunction(callSite: JavaStatement, callee: Method, returnSite: JavaStatement)(implicit
        propertyStore: PropertyStore
    ): FlowFunction[LCPOnFieldsFact] = {
        if (callee.isNative) {
            return (sourceFact: LCPOnFieldsFact) => {
                val callStmt = callSite.stmt.asCall()

                sourceFact match {
                    case NullFact =>
                        returnSite.stmt.astID match {
                            case Assignment.ASTID =>
                                val assignment = returnSite.stmt.asAssignment
                                immutable.Set(NewObjectFact(assignment.targetVar.name, returnSite.pc))

                            case _ => immutable.Set.empty
                        }

                    case f: AbstractEntityFact =>
                        /* Check whether fact corresponds to one of the parameters */
                        if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                            immutable.Set(f match {
                                case f: AbstractObjectFact => f.toObjectFact
                                case f: AbstractArrayFact  => f.toArrayFact
                            })
                        } else {
                            immutable.Set.empty
                        }
                }
            }
        }

        super.getPrecomputedFlowFunction(callSite, callee, returnSite)
    }

    override def getPrecomputedSummaryFunction(
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunction[LCPOnFieldsValue] = {
        if (callee.isNative) {
            return returnSiteFact match {
                case _: AbstractObjectFact =>
                    VariableValueEdgeFunction

                case _: AbstractArrayFact =>
                    NewArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)

                case _ => identityEdgeFunction
            }
        }

        super.getPrecomputedSummaryFunction(callSite, callSiteFact, callee, returnSite, returnSiteFact)
    }
}
