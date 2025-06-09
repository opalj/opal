/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package lcp_on_fields
package problem

import scala.collection
import scala.collection.immutable
import scala.collection.mutable

import org.opalj.ai.isImplicitOrExternalException
import org.opalj.br.Field
import org.opalj.br.IntegerType
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.problem.JavaIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement.StmtAsCall
import org.opalj.value.IsIntegerValue

/**
 * Definition of linear constant propagation on fields problem. This implementation detects basic cases of linear
 * constant propagation involving fields. It detects direct field assignments but fails to detect assignments done in a
 * method where the value is passed as an argument (e.g. a classical setter). Similar, array read accesses can only be
 * resolved if the index is a constant literal. There also is just minimal support for static fields.
 * This implementation is mainly intended to be an example of a cyclic IDE analysis.
 *
 * @author Robin Körkemeier
 */
class LCPOnFieldsProblem(
    project: SomeProject,
    icfg:    JavaICFG
) extends JavaIDEProblem[LCPOnFieldsFact, LCPOnFieldsValue] {
    private val declaredFields = project.get(DeclaredFieldsKey)

    override val nullFact: LCPOnFieldsFact =
        NullFact

    override val lattice: MeetLattice[LCPOnFieldsValue] =
        LCPOnFieldsLattice

    override def getAdditionalSeeds(stmt: JavaStatement, callee: Method)(
        implicit propertyStore: PropertyStore
    ): collection.Set[LCPOnFieldsFact] = {
        (if (callee.isStatic) {
             immutable.Set.empty
         } else {
             /* Add fact for `this` */
             immutable.Set(ObjectFact("param0", -1))
         }) ++
            /* Add facts for other parameters */
            callee.parameterTypes
                .zipWithIndex
                .filter { case (paramType, _) => paramType.isObjectType || paramType.isArrayType }
                .map { case (paramType, index) =>
                    if (paramType.isObjectType) {
                        ObjectFact(s"param${index + 1}", -(index + 2))
                    } else {
                        ArrayFact(s"param${index + 1}", -(index + 2))
                    }
                }
                .toSet ++
            /* Add facts for static fields of class */
            callee.classFile
                .fields
                .filter(_.isStatic)
                .map { field => StaticFieldFact(field.classFile.thisType, field.name) }
                .toSet
    }

    override def getAdditionalSeedsEdgeFunction(stmt: JavaStatement, fact: LCPOnFieldsFact, callee: Method)(
        implicit propertyStore: PropertyStore
    ): EdgeFunctionResult[LCPOnFieldsValue] = {
        fact match {
            case ObjectFact(_, _)          => UnknownValueEdgeFunction
            case ArrayFact(_, _)           => ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
            case f @ StaticFieldFact(_, _) => getEdgeFunctionForStaticFieldFactByImmutability(f)
            case _                         => super.getAdditionalSeedsEdgeFunction(stmt, fact, callee)
        }
    }

    private def getEdgeFunctionForStaticFieldFactByImmutability(staticFieldFact: AbstractStaticFieldFact)(
        implicit propertyStore: PropertyStore
    ): EdgeFunctionResult[LCPOnFieldsValue] = {
        val declaredField = declaredFields(staticFieldFact.objectType, staticFieldFact.fieldName, IntegerType)
        if (!declaredField.isDefinedField) {
            return PutStaticFieldEdgeFunction(linear_constant_propagation.problem.VariableValue)
        }
        val field = declaredField.definedField

        /* We enhance the analysis with immutability information. When a static field is immutable and we have knowledge
         * of an assignment site, then this will always be the value of the field. This way we can make this analysis
         * more precise without the need to add precise handling of static initializers. */
        val fieldAssignabilityEOptionP = propertyStore(field, FieldAssignability.key)

        fieldAssignabilityEOptionP match {
            case FinalP(fieldAssignability) =>
                fieldAssignability match {
                    case NonAssignable | EffectivelyNonAssignable | LazilyInitialized =>
                        val value = getValueForGetStaticExprByStaticInitializer(field)
                        FinalEdgeFunction(PutStaticFieldEdgeFunction(value))
                    case _ =>
                        FinalEdgeFunction(PutStaticFieldEdgeFunction(linear_constant_propagation.problem.VariableValue))
                }

            case InterimUBP(fieldAssignability) =>
                fieldAssignability match {
                    case NonAssignable | EffectivelyNonAssignable | LazilyInitialized =>
                        val value = getValueForGetStaticExprByStaticInitializer(field)
                        InterimEdgeFunction(PutStaticFieldEdgeFunction(value), immutable.Set(fieldAssignabilityEOptionP))
                    case _ =>
                        FinalEdgeFunction(PutStaticFieldEdgeFunction(linear_constant_propagation.problem.VariableValue))
                }

            case _ =>
                InterimEdgeFunction(
                    PutStaticFieldEdgeFunction(linear_constant_propagation.problem.UnknownValue),
                    immutable.Set(fieldAssignabilityEOptionP)
                )
        }
    }

    private def getValueForGetStaticExprByStaticInitializer(field: Field): LinearConstantPropagationValue = {
        var value: LinearConstantPropagationValue = linear_constant_propagation.problem.UnknownValue

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
                                if (field.classFile.thisType == putStatic.declaringClass &&
                                    field.name == putStatic.name
                                ) {
                                    stmt.stmt.asPutStatic.value.asVar.value match {
                                        case v: IsIntegerValue =>
                                            v.constantValue match {
                                                case Some(constantValue) =>
                                                    value = LinearConstantPropagationLattice.meet(
                                                        value,
                                                        linear_constant_propagation.problem.ConstantValue(constantValue)
                                                    )
                                                case _ => return linear_constant_propagation.problem.VariableValue
                                            }

                                        case _ =>
                                            return linear_constant_propagation.problem.VariableValue
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

        value match {
            case linear_constant_propagation.problem.UnknownValue =>
                if (field.isFinal) {
                    linear_constant_propagation.problem.VariableValue
                } else {
                    linear_constant_propagation.problem.ConstantValue(0)
                }
            case _ => value
        }
    }

    override def getNormalFlowFunction(
        source:     JavaStatement,
        sourceFact: LCPOnFieldsFact,
        target:     JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        new FlowFunction[LCPOnFieldsFact] {
            override def compute(): FactsAndDependees = {
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
                        immutable.Set(f.toObjectOrArrayFact)

                    /* Static fields are modeled such that statements that change their value can always originate from
                     * the null fact */
                    case (PutStatic.ASTID, NullFact) =>
                        val putStatic = source.stmt.asPutStatic

                        /* Only fields of type integer */
                        if (putStatic.declaredFieldType.isIntegerType) {
                            val declaredField = declaredFields(putStatic.declaringClass, putStatic.name, IntegerType)
                            if (!declaredField.isDefinedField) {
                                return immutable.Set(sourceFact)
                            }
                            val field = declaredField.definedField

                            /* Only private fields (as they cannot be influenced by other static initializers) */
                            if (field.isPrivate) {
                                immutable.Set(sourceFact, PutStaticFieldFact(putStatic.declaringClass, putStatic.name))
                            } else {
                                immutable.Set(sourceFact)
                            }
                        } else {
                            immutable.Set(sourceFact)
                        }

                    case (PutStatic.ASTID, f: AbstractStaticFieldFact) =>
                        val putStatic = source.stmt.asPutStatic

                        /* Drop existing fact if for the same static field */
                        if (f.objectType == putStatic.declaringClass && f.fieldName == putStatic.name) {
                            immutable.Set.empty
                        } else {
                            immutable.Set(f.toStaticFieldFact)
                        }

                    case (_, f: AbstractStaticFieldFact) =>
                        /* Specialized facts only live for one step and are turned back into basic ones afterwards */
                        immutable.Set(f.toStaticFieldFact)

                    case _ => immutable.Set(sourceFact)
                }
            }
        }
    }

    override def getCallFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LCPOnFieldsFact,
        calleeEntry:  JavaStatement,
        callee:       Method
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        new FlowFunction[LCPOnFieldsFact] {
            override def compute(): FactsAndDependees = {
                callSiteFact match {
                    case NullFact =>
                        /* Only propagate null fact if function returns an object or an array of integers */
                        if (callee.returnType.isObjectType) {
                            immutable.Set(callSiteFact)
                        } else if (callee.returnType.isArrayType &&
                                   callee.returnType.asArrayType.componentType.isIntegerType
                        ) {
                            immutable.Set(callSiteFact)
                        } else if (callee.classFile.fields.exists { field => field.isStatic } &&
                                   !callee.classFile.fqn.startsWith("java/") &&
                                   !callee.classFile.fqn.startsWith("sun/")
                        ) {
                            /* The null fact is needed for writing static fields */
                            immutable.Set(callSiteFact)
                        } else {
                            immutable.Set.empty
                        }

                    case f: AbstractEntityFact =>
                        val callStmt = callSite.stmt.asCall()

                        /* All parameters (including the implicit 'this' reference) */
                        val allParams = callStmt.allParams
                        val staticCallIndexOffset =
                            if (callStmt.receiverOption.isEmpty) { 1 }
                            else { 0 }

                        allParams
                            .zipWithIndex
                            .filter { case (param, _) =>
                                /* Only parameters where the variable represented by the source fact is one possible
                                 * initializer */
                                param.asVar.definedBy.contains(f.definedAtIndex)
                            }
                            .map { case (_, index) =>
                                val adjustedIndex = index + staticCallIndexOffset
                                f match {
                                    case _: AbstractObjectFact =>
                                        ObjectFact(s"param$adjustedIndex", -(adjustedIndex + 1))
                                    case _: AbstractArrayFact =>
                                        ArrayFact(s"param$adjustedIndex", -(adjustedIndex + 1))
                                }
                            }
                            .toSet

                    case f: AbstractStaticFieldFact =>
                        /* Only propagate fact if the callee can access the corresponding static field */
                        if (callee.classFile.thisType == f.objectType) {
                            immutable.Set(f.toStaticFieldFact)
                        } else {
                            immutable.Set.empty
                        }
                }
            }
        }
    }

    override def getReturnFlowFunction(
        calleeExit:     JavaStatement,
        calleeExitFact: LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        new FlowFunction[LCPOnFieldsFact] {
            override def compute(): FactsAndDependees = {
                calleeExitFact match {
                    case NullFact =>
                        /* Always propagate null fact */
                        immutable.Set(calleeExitFact)

                    case f: AbstractEntityFact =>
                        val definedAtIndex = f.definedAtIndex

                        if (isImplicitOrExternalException(definedAtIndex)) {
                            return immutable.Set.empty
                        }

                        val callStmt = returnSite.stmt.asCall()

                        val allParams = callStmt.allParams
                        val staticCallIndexOffset =
                            if (callStmt.receiverOption.isEmpty) { 1 }
                            else { 0 }

                        /* Distinguish parameters and local variables */
                        if (definedAtIndex < 0) {
                            val paramIndex = -definedAtIndex - 1 - staticCallIndexOffset
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

                    case f: AbstractStaticFieldFact =>
                        /* Only propagate fact if the caller can access the corresponding static field */
                        if (returnSite.method.classFile.thisType == f.objectType) {
                            immutable.Set(f.toStaticFieldFact)
                        } else {
                            immutable.Set.empty
                        }
                }
            }
        }
    }

    override def getCallToReturnFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LCPOnFieldsFact,
        callee:       Method,
        returnSite:   JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        new FlowFunction[LCPOnFieldsFact] {
            override def compute(): FactsAndDependees = {
                val callStmt = returnSite.stmt.asCall()

                callSiteFact match {
                    case NullFact =>
                        /* Always propagate null fact */
                        immutable.Set(callSiteFact)

                    case f: AbstractEntityFact =>
                        /* Only propagate if the variable represented by the source fact is no initializer of one of the
                         * parameters */
                        if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                            immutable.Set.empty
                        } else {
                            immutable.Set(f.toObjectOrArrayFact)
                        }

                    case f: AbstractStaticFieldFact =>
                        /* Propagate facts that are not propagated via the call flow */
                        if (callee.classFile.thisType == f.objectType) {
                            immutable.Set.empty
                        } else {
                            immutable.Set(f.toStaticFieldFact)
                        }
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

            case PutStaticFieldFact(_, _) =>
                val valueVar = source.stmt.asPutStatic.value.asVar

                val lcpEOptionP =
                    propertyStore((source.method, source), LinearConstantPropagationPropertyMetaInformation.key)

                /* Decide based on the current result of the linear constant propagation analysis */
                lcpEOptionP match {
                    case FinalP(property) =>
                        val value = getVariableFromProperty(valueVar)(property)
                        FinalEdgeFunction(PutStaticFieldEdgeFunction(value))

                    case InterimUBP(property) =>
                        val value = getVariableFromProperty(valueVar)(property)
                        value match {
                            case linear_constant_propagation.problem.UnknownValue =>
                                InterimEdgeFunction(PutStaticFieldEdgeFunction(value), immutable.Set(lcpEOptionP))
                            case linear_constant_propagation.problem.ConstantValue(_) =>
                                InterimEdgeFunction(PutStaticFieldEdgeFunction(value), immutable.Set(lcpEOptionP))
                            case linear_constant_propagation.problem.VariableValue =>
                                FinalEdgeFunction(PutStaticFieldEdgeFunction(value))
                        }

                    case _ =>
                        InterimEdgeFunction(
                            PutStaticFieldEdgeFunction(linear_constant_propagation.problem.UnknownValue),
                            immutable.Set(lcpEOptionP)
                        )
                }

            case _ => FinalEdgeFunction(identityEdgeFunction)
        }
    }

    private def getVariableFromProperty(var0: JavaStatement.V)(
        property: LinearConstantPropagationPropertyMetaInformation.Self
    ): LinearConstantPropagationValue = {
        property
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
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = {
        callSiteFact match {
            case NullFact => UnknownValueEdgeFunction
            case _        => identityEdgeFunction
        }
    }

    override def getReturnEdgeFunction(
        calleeExit:     JavaStatement,
        calleeExitFact: LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact,
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact
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
        if (callee.isNative || callee.body.isEmpty) {
            return true
        }

        super.hasPrecomputedFlowAndSummaryFunction(callSite, callSiteFact, callee)
    }

    override def getPrecomputedFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LCPOnFieldsFact,
        callee:       Method,
        returnSite:   JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        if (callee.isNative || callee.body.isEmpty) {
            return new FlowFunction[LCPOnFieldsFact] {
                override def compute(): FactsAndDependees = {
                    callSiteFact match {
                        case NullFact =>
                            returnSite.stmt.astID match {
                                case Assignment.ASTID =>
                                    val assignment = returnSite.stmt.asAssignment
                                    if (callee.returnType.isObjectType) {
                                        immutable.Set(NewObjectFact(assignment.targetVar.name, returnSite.pc))
                                    } else if (callee.returnType.isArrayType &&
                                               callee.returnType.asArrayType.componentType.isIntegerType
                                    ) {
                                        immutable.Set(NewArrayFact(assignment.targetVar.name, returnSite.pc))
                                    } else {
                                        immutable.Set.empty
                                    }

                                case _ => immutable.Set.empty
                            }

                        case f: AbstractEntityFact =>
                            val callStmt = callSite.stmt.asCall()

                            /* Check whether fact corresponds to one of the parameters */
                            if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                                immutable.Set(f.toObjectOrArrayFact)
                            } else {
                                immutable.Set.empty
                            }

                        case f: AbstractStaticFieldFact =>
                            if (callee.classFile.thisType == f.objectType) {
                                immutable.Set(f.toStaticFieldFact)
                            } else {
                                immutable.Set.empty
                            }
                    }
                }
            }
        }

        super.getPrecomputedFlowFunction(callSite, callSiteFact, callee, returnSite)
    }

    override def getPrecomputedSummaryFunction(
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact,
        callee:         Method,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = {
        if (callee.isNative || callee.body.isEmpty) {
            return returnSiteFact match {
                case NullFact =>
                    identityEdgeFunction

                case _: AbstractObjectFact =>
                    val callStmt = callSite.stmt.asCall()

                    if (callStmt.declaringClass.isObjectType &&
                        callStmt.declaringClass.asObjectType.fqn == "java/lang/Object" && callStmt.name == "<init>"
                    ) {
                        identityEdgeFunction
                    } else {
                        /* It is unknown what the callee does with the object */
                        VariableValueEdgeFunction
                    }

                case _: AbstractArrayFact =>
                    NewArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)

                case _: AbstractStaticFieldFact =>
                    PutStaticFieldEdgeFunction(linear_constant_propagation.problem.VariableValue)
            }
        }

        super.getPrecomputedSummaryFunction(callSite, callSiteFact, callee, returnSite, returnSiteFact)
    }

    override def getPrecomputedFlowFunction(
        callSite:     JavaStatement,
        callSiteFact: LCPOnFieldsFact,
        returnSite:   JavaStatement
    )(implicit propertyStore: PropertyStore): FlowFunction[LCPOnFieldsFact] = {
        new FlowFunction[LCPOnFieldsFact] {
            override def compute(): FactsAndDependees = {
                callSiteFact match {
                    case NullFact =>
                        returnSite.stmt.astID match {
                            case Assignment.ASTID =>
                                val callStmt = callSite.stmt.asCall()
                                val assignment = returnSite.stmt.asAssignment

                                if (callStmt.descriptor.returnType.isObjectType) {
                                    immutable.Set(callSiteFact, NewObjectFact(assignment.targetVar.name, returnSite.pc))
                                } else if (callStmt.descriptor.returnType.isArrayType &&
                                           callStmt.descriptor.returnType.asArrayType.componentType.isIntegerType
                                ) {
                                    immutable.Set(callSiteFact, NewArrayFact(assignment.targetVar.name, returnSite.pc))
                                } else {
                                    immutable.Set(callSiteFact)
                                }

                            case _ => immutable.Set(callSiteFact)
                        }

                    case f: AbstractEntityFact =>
                        immutable.Set(f.toObjectOrArrayFact)

                    case f: AbstractStaticFieldFact =>
                        immutable.Set(f.toStaticFieldFact)
                }
            }
        }
    }

    override def getPrecomputedSummaryFunction(
        callSite:       JavaStatement,
        callSiteFact:   LCPOnFieldsFact,
        returnSite:     JavaStatement,
        returnSiteFact: LCPOnFieldsFact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[LCPOnFieldsValue] = {
        val callStmt = callSite.stmt.asCall()

        (callSiteFact, returnSiteFact) match {
            case (NullFact, _: AbstractObjectFact) =>
                VariableValueEdgeFunction

            case (NullFact, _: AbstractArrayFact) =>
                NewArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)

            case (_: AbstractEntityFact, f: AbstractEntityFact) =>
                /* Constructor of object class doesn't modify the object */
                if (callStmt.declaringClass.isObjectType &&
                    callStmt.declaringClass.asObjectType.fqn == "java/lang/Object" && callStmt.name == "<init>"
                ) {
                    identityEdgeFunction
                }
                /* Check whether fact corresponds to one of the parameters */
                else if (callStmt.allParams.exists { param => param.asVar.definedBy.contains(f.definedAtIndex) }) {
                    f match {
                        case _: AbstractObjectFact => VariableValueEdgeFunction
                        case _: AbstractEntityFact =>
                            NewArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)
                    }
                } else {
                    identityEdgeFunction
                }

            case (_, f: AbstractStaticFieldFact) =>
                val declaredField = declaredFields(f.objectType, f.fieldName, IntegerType)
                if (!declaredField.isDefinedField) {
                    return PutStaticFieldEdgeFunction(linear_constant_propagation.problem.VariableValue)
                }
                val field = declaredField.definedField

                if (callStmt.declaringClass != f.objectType && field.isPrivate) {
                    identityEdgeFunction
                } else {
                    getEdgeFunctionForStaticFieldFactByImmutability(f)
                }

            case _ => identityEdgeFunction
        }
    }
}
