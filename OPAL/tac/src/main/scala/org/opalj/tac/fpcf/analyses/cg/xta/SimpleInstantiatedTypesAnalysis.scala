/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.instructions.NEW
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.CreateNewArrayInstruction
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Updates InstantiatedTypes attached to a method for each constructor
 * call or array allocation occurring within that method.
 *
 * This is a simple analysis which yields useful results for basic tests,
 * but it does not capture, e.g., indirect constructor calls through reflection.
 *
 * It also updates InstantiatedTypes of multidimensional ArrayTypes (see comments below
 * for more details).
 *
 * The analysis is triggered for a method once it becomes reachable, i.e., a
 * caller has been added. Thus, the property is not computed for unreachable methods.
 *
 * @author Andreas Bauer
 */
// TODO AB replace later with a more sophisticated analysis (based on the RTA one)
final class SimpleInstantiatedTypesAnalysis(
        val project:     SomeProject,
        selectSetEntity: SetEntitySelector
) extends ReachableMethodAnalysis {

    override def processMethod(definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        val code = definedMethod.definedMethod.body.get

        val targetSetEntity = selectSetEntity(definedMethod)

        val instantiatedObjectTypes = code.instructions.collect({
            case NEW(declType) ⇒ declType
        })

        // Exception types are tracked globally.
        val instantiatedExceptionTypes =
            instantiatedObjectTypes.filter(classHierarchy.isSubtypeOf(_, ObjectType.Throwable))
        val exceptionTypePartialResult =
            if (instantiatedExceptionTypes.nonEmpty)
                Some(PartialResult(
                    project,
                    InstantiatedTypes.key,
                    updateForProject(project, UIDSet(instantiatedExceptionTypes.toSeq: _*))
                ))
            else
                None

        // We only care about arrays of reference types.
        val instantiatedArrays = code.instructions.collect({
            case arr: CreateNewArrayInstruction if arr.arrayType.elementType.isReferenceType ⇒ arr.arrayType
        })

        val multidimensionalArrayPartialResults = multidimensionalArrayInitialAssignments(instantiatedArrays)

        Results(
            PartialResult(
                targetSetEntity,
                InstantiatedTypes.key,
                update(targetSetEntity, UIDSet((instantiatedObjectTypes ++ instantiatedArrays).toSeq: _*))
            ),
            exceptionTypePartialResult ++ multidimensionalArrayPartialResults
        )
    }

    /**
     * When allocating array with dimension > 1, the JVM allocates and assigns
     * sub-arrays implicitly. We need to capture these effects for the analysis.
     *
     * E.g., consider the allocation "arr = new A[1][1]". Here, it is necessary that
     * ArrayType(ArrayType(ObjectType(A))) has the instantiated type ArrayType(ObjectType(A)),
     * otherwise reads like arr[0] will return no types which may lead to incorrect results.
     *
     * This implementation has a (sound) over-approximation: Consider an allocation like
     * "arr = new A[1][]", then a[0] == null, which means there is no such implicit assignment.
     * However, capturing this more accurately requires more in-depth analysis of the bytecode/
     * TAC. (TODO AB future work)
     *
     * @param arrays ArrayTypes which were found to be instantiated within the method.
     * @return Partial results for the implicit assignments.
     */
    def multidimensionalArrayInitialAssignments(arrays: Array[ArrayType]): Iterable[PartialResult[ArrayType, InstantiatedTypes]] = {

        val multidimensionalArrays = arrays.filter(_.dimensions > 1)
        if (multidimensionalArrays.isEmpty) {
            return Iterable.empty
        }

        val initialAssignments = multidimensionalArrays.map(at ⇒
            PartialResult(at, InstantiatedTypes.key, update(at, UIDSet(at.componentType.asReferenceType))))

        // these are all ArrayTypes since we filtered for dim > 1 above
        val componentTypes = multidimensionalArrays.map(_.componentType.asArrayType)
        val recursiveAssignments = multidimensionalArrayInitialAssignments(componentTypes)

        initialAssignments ++ recursiveAssignments
    }

    // TODO AB code duplication; something like this appears in many places
    def update[E <: Entity](
        entity:               E,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[E, InstantiatedTypes]
    ): Option[EPS[E, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(entity, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(entity, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }

    // TODO AB This variant is needed for updating project (SomeProject is not compatible with the one above),
    //  although I'm not entirely sure why...
    def updateForProject(
        entity:               SomeProject,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[SomeProject, InstantiatedTypes]
    ): Option[EPS[SomeProject, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(entity, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(entity, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }
}

class SimpleInstantiatedTypesAnalysisScheduler(
        selectSetEntity: SetEntitySelector
) extends BasicFPCFTriggeredAnalysisScheduler {

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new SimpleInstantiatedTypesAnalysis(project, selectSetEntity)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set.empty
    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
    override def triggeredBy: PropertyKind = Callers.key
}
