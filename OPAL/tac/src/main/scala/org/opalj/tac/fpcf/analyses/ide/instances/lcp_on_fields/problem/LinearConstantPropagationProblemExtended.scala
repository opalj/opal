/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable
import scala.collection.mutable

import org.opalj.ai.domain.l1.DefaultIntegerRangeValues
import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.tac.ArrayLoad
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.PutStatic
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsPropertyMetaInformation
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
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.V

/**
 * Extended definition of the linear constant propagation problem, trying to resolve field accesses with the LCP on
 * fields analysis.
 */
class LinearConstantPropagationProblemExtended(
    project: SomeProject,
    icfg:    JavaICFG
) extends LinearConstantPropagationProblem {
    override def isArrayLoadExpressionGeneratedByFact(
        arrayLoadExpr: ArrayLoad[V]
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement
    ): Boolean = {
        /* Generate fact only if the variable represented by the source fact is one possible initializer of the index
         * variable */
        sourceFact match {
            case NullFact => false
            case VariableFact(_, definedAtIndex) =>
                val arrayVar = arrayLoadExpr.arrayRef.asVar
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
        val arrayVar = arrayLoadExpr.arrayRef.asVar

        val index = arrayLoadExpr.index.asVar.value match {
            case intRange: DefaultIntegerRangeValues#IntegerRange =>
                if (intRange.lowerBound == intRange.upperBound) {
                    Some(intRange.lowerBound)
                } else {
                    None
                }
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
    )(property: Property): LinearConstantPropagationValue = {
        property
            .asInstanceOf[LCPOnFieldsPropertyMetaInformation.Self]
            .results
            .filter {
                case (f: AbstractArrayFact, ArrayValue(_, _)) =>
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

    override def getNormalEdgeFunctionForGetStatic(
        getStaticExpr: GetStatic
    )(
        source:     JavaStatement,
        sourceFact: LinearConstantPropagationFact,
        target:     JavaStatement,
        targetFact: LinearConstantPropagationFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LinearConstantPropagationValue] = {
        val field =
            project.get(DeclaredFieldsKey)(
                getStaticExpr.declaringClass,
                getStaticExpr.name,
                getStaticExpr.declaredFieldType
            ).definedField

        /* We enhance the analysis with immutability information. When a static field is immutable and we have knowledge
         * of an assignment site, then this will always be the value of the field. This way we can make this analysis
         * more precise without the need to add precise handling of static initializers. */
        val fieldImmutabilityEOptionP = propertyStore(field, FieldImmutability.key)

        fieldImmutabilityEOptionP match {
            case FinalP(fieldImmutability) =>
                fieldImmutability match {
                    case TransitivelyImmutableField =>
                        getValueForGetStaticExpr(getStaticExpr, field) match {
                            case ConstantValue(c) => FinalEdgeFunction(LinearCombinationEdgeFunction(0, c, lattice.top))
                            case _                => FinalEdgeFunction(VariableValueEdgeFunction)
                        }
                    case _ => FinalEdgeFunction(VariableValueEdgeFunction)
                }

            case InterimUBP(fieldImmutability) =>
                fieldImmutability match {
                    case TransitivelyImmutableField =>
                        getValueForGetStaticExpr(getStaticExpr, field) match {
                            case ConstantValue(c) =>
                                InterimEdgeFunction(
                                    LinearCombinationEdgeFunction(0, c, lattice.top),
                                    immutable.Set(fieldImmutabilityEOptionP)
                                )
                            case _ => FinalEdgeFunction(VariableValueEdgeFunction)
                        }
                    case _ => FinalEdgeFunction(VariableValueEdgeFunction)
                }

            case _ =>
                InterimEdgeFunction(UnknownValueEdgeFunction, immutable.Set(fieldImmutabilityEOptionP))
        }
    }

    private def getValueForGetStaticExpr(getStaticExpr: GetStatic, field: Field): LinearConstantPropagationValue = {
        var value: LinearConstantPropagationValue = UnknownValue

        /* Search for statements that write the field in static initializer of the class belonging to the field. */
        field.classFile.staticInitializer match {
            case Some(method) =>
                var workingStmts: mutable.Set[JavaStatement] = mutable.Set.from(icfg.getStartStatements(method))
                val seenStmts = mutable.Set.empty[JavaStatement]

                while (workingStmts.nonEmpty) {
                    workingStmts.foreach { stmt =>
                        stmt.stmt.astID match {
                            case PutStatic.ASTID =>
                                val putStatic = stmt.stmt.asPutStatic
                                if (getStaticExpr.declaringClass == putStatic.declaringClass &&
                                    getStaticExpr.name == putStatic.name
                                ) {
                                    stmt.stmt.asPutStatic.value.asVar.value match {
                                        case intRange: DefaultIntegerRangeValues#IntegerRange =>
                                            if (intRange.lowerBound == intRange.upperBound) {
                                                value = lattice.meet(value, ConstantValue(intRange.upperBound))
                                            } else {
                                                return VariableValue
                                            }

                                        case _ =>
                                            return VariableValue
                                    }
                                }

                            case _ =>
                        }
                    }

                    seenStmts.addAll(workingStmts)
                    workingStmts = workingStmts.foldLeft(mutable.Set.empty[JavaStatement]) { (nextStmts, stmt) =>
                        nextStmts.addAll(icfg.getNextStatements(stmt))
                    }.diff(seenStmts)
                }

            case _ =>
        }

        value
    }
}
