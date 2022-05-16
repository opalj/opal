/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.ArrayType
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.instructions.CreateNewArrayInstruction
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EPS
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.tac.fpcf.properties.TACAI
import scala.collection.mutable

import org.opalj.tac.cg.TypeProviderKey

/**
 * Updates InstantiatedTypes attached to a method's set for each array allocation
 * occurring within that method.
 *
 * It also updates InstantiatedTypes of multidimensional ArrayTypes (see comments below
 * for more details).
 *
 * The analysis is triggered for a method once it becomes reachable, i.e., a
 * caller has been added. Thus, there are no property computations for unreachable methods.
 *
 * @author Andreas Bauer
 */
final class ArrayInstantiationsAnalysis(
        val project:     SomeProject,
        selectSetEntity: TypeSetEntitySelector
) extends ReachableMethodAnalysis {

    override implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    override def processMethod(
        callContext: ContextType,
        tacEP:       EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val code = callContext.method.definedMethod.body.get

        val targetSetEntity = selectSetEntity(callContext.method)

        // We only care about arrays of reference types.
        val instantiatedArrays = code.instructions.collect {
            case arr: CreateNewArrayInstruction if arr.arrayType.elementType.isReferenceType => arr.arrayType
        }

        val multidimensionalArrayPartialResults = multidimensionalArrayInitialAssignments(instantiatedArrays)

        Results(
            PartialResult(
                targetSetEntity,
                InstantiatedTypes.key,
                InstantiatedTypes.update(targetSetEntity, UIDSet(instantiatedArrays.toSeq: _*))
            ),
            multidimensionalArrayPartialResults
        )
    }

    /**
     * When allocating array with dimension > 1, the JVM allocates and assigns
     * sub-arrays implicitly. We need to capture these effects for the analysis.
     *
     * E.g., consider the allocation "arr = new A[1][1]". Here, it is necessary that
     * ArrayType(ArrayType(ObjectType(A))) has the type ArrayType(ObjectType(A)) in its type set,
     * otherwise reads like arr[0] will return no types when propagating which may lead to
     * incorrect results.
     *
     * This implementation has a (sound) over-approximation: Consider an allocation like
     * "arr = new A[1][]", then a[0] == null, which means there is no such implicit assignment.
     * However, capturing this more accurately requires more in-depth analysis of the bytecode/
     * TAC. (TODO Future work.)
     *
     * @param arrays ArrayTypes which were found to be instantiated within the method.
     * @return Partial results for the implicit assignments.
     */
    def multidimensionalArrayInitialAssignments(
        arrays: Iterable[ArrayType]
    ): Iterable[PartialResult[ArrayType, InstantiatedTypes]] = {

        val buffer = mutable.Iterable.newBuilder[PartialResult[ArrayType, InstantiatedTypes]]

        // Note: Since 'until' is an exclusive range, all array types in 'arrays' with
        // dimension 1 are not processed here.
        for (
            at <- arrays;
            dim <- 1 until at.dimensions
        ) {

            val targetAT = ArrayType(dim + 1, at.elementType)
            val assignedAT = targetAT.componentType.asArrayType
            buffer += PartialResult(
                targetAT,
                InstantiatedTypes.key,
                InstantiatedTypes.update(targetAT, UIDSet(assignedAT))
            )
        }

        buffer.result()
    }
}

class ArrayInstantiationsAnalysisScheduler(
        selectSetEntity: TypeSetEntitySelector
) extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeProviderKey)

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis =
            new ArrayInstantiationsAnalysis(project, selectSetEntity)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI)
    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
    override def triggeredBy: PropertyKind = Callers.key
}
