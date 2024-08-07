/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.PutField
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.problem.JavaIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.StmtAsCall

/**
 * Definition of linear constant propagation on fields problem. This implementation detects basic cases of linear
 * constant propagation involving fields. It detects direct field assignments but fails to detect assignments done in a
 * method where the value is passed as an argument (e.g. a classical setter).
 * This implementation is mainly intended to be an example of a cyclic IDE analysis.
 */
class LCPOnFieldsProblem(project: SomeProject)
    extends JavaIDEProblem[LCPOnFieldsFact, LCPOnFieldsValue](project) {
    override val nullFact: LCPOnFieldsFact =
        NullFact

    override val lattice: MeetLattice[LCPOnFieldsValue] =
        LCPOnFieldsLattice

    override def getNormalFlowFunction(source: JavaStatement, target: JavaStatement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[LCPOnFieldsFact] =
        (sourceFact: LCPOnFieldsFact) => {
            (source.stmt.astID, sourceFact) match {
                case (Assignment.ASTID, NullFact) =>
                    val assignment = source.stmt.asAssignment
                    assignment.expr.astID match {
                        case New.ASTID =>
                            /* Generate new object fact from null fact if assignment is a 'new' expression */
                            immutable.Set(sourceFact, NewObjectFact(assignment.targetVar.name, source.index))

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

                case (_, f: AbstractObjectFact) =>
                    /* Specialized facts only live for one step and are turned back into object facts afterwards */
                    immutable.Set(f.toObjectFact)

                case _ => immutable.Set(sourceFact)
            }
        }

    override def getCallFlowFunction(
        callSite:    JavaStatement,
        calleeEntry: JavaStatement,
        callee:      Method
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                // TODO (IDE) REMOVE ONCE PRECOMPUTED SUMMARIES ARE IMPLEMENTED
                if (callee.classFile.thisType.fqn.startsWith("java/") && callee.name != "<init>") {
                    immutable.Set.empty
                } else {
                    sourceFact match {
                        case NullFact =>
                            /* Only propagate null fact if function returns an object */
                            if (callee.returnType.isObjectType) {
                                immutable.Set(sourceFact)
                            } else {
                                immutable.Set.empty
                            }

                        case f: AbstractObjectFact =>
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
                                    ObjectFact(s"param$index", -(index + 1))
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
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        (sourceFact: LCPOnFieldsFact) =>
            {
                sourceFact match {
                    case NullFact =>
                        /* Always propagate null fact */
                        immutable.Set(sourceFact)

                    case f: AbstractObjectFact =>
                        val definedAtIndex = f.definedAtIndex

                        val callStmt = returnSite.stmt.asCall()

                        val allParams = callStmt.allParams

                        /* Distinguish parameters and local variables */
                        if (definedAtIndex < 0) {
                            val paramIndex = -(definedAtIndex + 1)
                            val param = allParams(paramIndex)
                            val paramName = param.asVar.name.substring(1, param.asVar.name.length - 1)
                            param.asVar.definedBy.map { dAI => ObjectFact(paramName, dAI) }.toSet
                        } else {
                            returnSite.stmt.astID match {
                                case Assignment.ASTID =>
                                    val assignment = returnSite.stmt.asAssignment

                                    val returnExpr = calleeExit.stmt.asReturnValue.expr
                                    /* Only propagate if the variable represented by the source fact is one possible
                                     * initializer of the variable at the return site */
                                    if (returnExpr.asVar.definedBy.contains(f.definedAtIndex)) {
                                        immutable.Set(ObjectFact(assignment.targetVar.name, returnSite.index))
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

                    case f: AbstractObjectFact =>
                        /* Only propagate if the variable represented by the source fact is no initializer of one of the
                         * parameters */
                        if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                            immutable.Set.empty
                        } else {
                            immutable.Set(f.toObjectFact)
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
}
