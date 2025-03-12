/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package lcp_on_fields
package problem

import scala.collection.immutable

import org.opalj.ai.domain.l1.DefaultIntegerRangeValues
import org.opalj.br.ObjectType
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.{VariableValue => LCPVariableValue}
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.ConstantValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearCombinationEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.NullFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.UnknownValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.UnknownValueEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.VariableFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.VariableValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.VariableValueEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.V

/**
 * Extended definition of the linear constant propagation problem, trying to resolve field accesses with the LCP on
 * fields analysis.
 *
 * @author Robin Körkemeier
 */
class LinearConstantPropagationProblemExtended extends LinearConstantPropagationProblem {
    override def isArrayLoadExpressionGeneratedByFact(
        arrayLoadExpr: ArrayLoad[V]
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement
    ): Boolean = {
        val arrayVar = arrayLoadExpr.arrayRef.asVar

        /* Generate fact only if the variable represented by the source fact is one possible initializer of the index
         * variable */
        sourceFact match {
            case NullFact =>
                arrayVar.value.asReferenceValue.asReferenceType.asArrayType.componentType.isIntegerType

            case VariableFact(_, definedAtIndex) =>
                val indexVar = arrayLoadExpr.index.asVar
                indexVar.definedBy.contains(definedAtIndex) &&
                    arrayVar.value.asReferenceValue.asReferenceType.asArrayType.componentType.isIntegerType
        }
    }

    override def getNormalEdgeFunctionForArrayLoad(
        arrayLoadExpr: ArrayLoad[JavaStatement.V]
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement,
        targetFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        if (sourceFact == nullFact) {
            return UnknownValueEdgeFunction
        }

        val arrayVar = arrayLoadExpr.arrayRef.asVar

        val index = arrayLoadExpr.index.asVar.value match {
            case intRange: DefaultIntegerRangeValues#IntegerRange =>
                if (intRange.lowerBound == intRange.upperBound) {
                    Some(intRange.lowerBound)
                } else {
                    None
                }
            case _ =>
                None
        }

        val lcpOnFieldsEOptionP =
            propertyStore((source.method, source), LCPOnFieldsPropertyMetaInformation.key)

        lcpOnFieldsEOptionP match {
            case FinalP(property) =>
                val value = getArrayElementFromProperty(arrayVar, index)(property)
                FinalEdgeFunction(value match {
                    case UnknownValue     => UnknownValueEdgeFunction
                    case ConstantValue(c) => LinearCombinationEdgeFunction(0, c, lattice.top)
                    case VariableValue    => VariableValueEdgeFunction
                })

            case InterimUBP(property) =>
                val value = getArrayElementFromProperty(arrayVar, index)(property)
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
    }

    private def getArrayElementFromProperty(
        arrayVar: JavaStatement.V,
        index:    Option[Int]
    )(property: LCPOnFieldsPropertyMetaInformation.Self): LinearConstantPropagationValue = {
        property
            .results
            .filter {
                case (f: AbstractArrayFact, ArrayValue(_, _)) =>
                    arrayVar.definedBy.contains(f.definedAtIndex)
                case (f: AbstractArrayFact, LCPVariableValue) =>
                    arrayVar.definedBy.contains(f.definedAtIndex)
                case _ => false
            }
            .map(_._2)
            .foldLeft(UnknownValue: LinearConstantPropagationValue) {
                case (value, ArrayValue(initValue, values)) =>
                    val arrayValue = index match {
                        case Some(i) => values.getOrElse(i, initValue)
                        case None =>
                            if (values.values.forall { v => v == initValue }) {
                                initValue
                            } else {
                                VariableValue
                            }
                    }
                    lattice.meet(value, arrayValue)

                case (_, LCPVariableValue) =>
                    VariableValue
            }
    }

    override def getNormalEdgeFunctionForGetField(
        getFieldExpr: GetField[JavaStatement.V]
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement,
        targetFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
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
    }

    private def getObjectFieldFromProperty(
        objectVar: JavaStatement.V,
        fieldName: String
    )(property: LCPOnFieldsPropertyMetaInformation.Self): LinearConstantPropagationValue = {
        property
            .results
            .filter {
                case (f: AbstractObjectFact, ObjectValue(values)) =>
                    objectVar.definedBy.contains(f.definedAtIndex) && values.contains(fieldName)
                case (f: AbstractObjectFact, LCPVariableValue) =>
                    objectVar.definedBy.contains(f.definedAtIndex)
                case _ => false
            }
            .map(_._2)
            .foldLeft(UnknownValue: LinearConstantPropagationValue) {
                case (value, ObjectValue(values)) =>
                    lattice.meet(value, values(fieldName))
                case (_, LCPVariableValue) =>
                    VariableValue
            }
    }

    override def getNormalEdgeFunctionForGetStatic(
        getStaticExpr: GetStatic
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement,
        targetFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        val objectType = getStaticExpr.declaringClass
        val fieldName = getStaticExpr.name

        val lcpOnFieldsEOptionP = propertyStore((source.method, source), LCPOnFieldsPropertyMetaInformation.key)

        /* Decide based on the current result of the LCP on fields analysis */
        lcpOnFieldsEOptionP match {
            case FinalP(property) =>
                FinalEdgeFunction(getStaticFieldFromProperty(objectType, fieldName)(property) match {
                    case UnknownValue     => UnknownValueEdgeFunction
                    case ConstantValue(c) => LinearCombinationEdgeFunction(0, c, lattice.top)
                    case VariableValue    => VariableValueEdgeFunction
                })

            case InterimUBP(property) =>
                getStaticFieldFromProperty(objectType, fieldName)(property) match {
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
    }

    private def getStaticFieldFromProperty(
        objectType: ObjectType,
        fieldName:  String
    )(property: LCPOnFieldsPropertyMetaInformation.Self): LinearConstantPropagationValue = {
        property
            .results
            .filter {
                case (f: AbstractStaticFieldFact, StaticFieldValue(_)) =>
                    f.objectType == objectType && f.fieldName == fieldName
                case (f: AbstractStaticFieldFact, LCPVariableValue) =>
                    f.objectType == objectType && f.fieldName == fieldName
                case _ => false
            }
            .map(_._2)
            .foldLeft(UnknownValue: LinearConstantPropagationValue) {
                case (value, StaticFieldValue(v)) =>
                    lattice.meet(value, v)
                case (_, LCPVariableValue) =>
                    VariableValue
            }
    }
}
