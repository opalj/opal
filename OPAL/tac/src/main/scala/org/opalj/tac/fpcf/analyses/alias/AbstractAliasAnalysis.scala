/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.ClassHierarchy
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasEntity
import org.opalj.br.fpcf.properties.alias.NoAlias
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation

/**
 * A base trait for all alias analyses.
 */
trait AbstractAliasAnalysis extends FPCFAnalysis {

    protected type AnalysisContext <: AliasAnalysisContext
    protected type AnalysisState <: AliasAnalysisState
    protected type Tac = TACode[TACMethodParameter, DUVar[ValueInformation]]

    /**
     * Determines the alias relation for the given entity.
     * @param e The entity to determine the aliasing information for.
     * @return The result of the computation.
     */
    def determineAlias(e: Entity): ProperPropertyComputationResult = {
        e match {
            case entity: AliasEntity =>
                implicit val context: AnalysisContext = createContext(entity)

                if (checkTypeCompatibility(context)) doDetermineAlias(using context, createState)
                else result(NoAlias)
            case _ => throw new UnknownError("unhandled entity type")
        }
    }

    /**
     * Determines if the types of the given elements are compatible, i.e. it is possible that they refer to the same objects.
     * Two elements are compatible if they are both reference types and one is a subtype of the other.
     * In any other case, e.g., an Integer and a String, the elements cannot possibly refer to the same object.
     *
     * @param context The alias analysis context to check the types for.
     * @return True if the types are compatible, false otherwise.
     */
    private def checkTypeCompatibility(context: AliasAnalysisContext): Boolean = {

        if (context.element1.isNullValue || context.element2.isNullValue)
            return true

        if (!context.element1.isReferenceType || !context.element2.isReferenceType)
            return false

        val element1ReferenceType = context.element1.referenceType
        val element2ReferenceType = context.element2.referenceType

        val classHierarchy: ClassHierarchy = project.classHierarchy

        classHierarchy.isSubtypeOf(element1ReferenceType, element2ReferenceType) ||
            classHierarchy.isSubtypeOf(element2ReferenceType, element1ReferenceType)
    }

    /**
     * Called to determine the alias relation for the given entity.
     *
     * This method is implemented by the concrete alias analysis.
     *
     * @param context The context to determine the aliasing information for.
     * @param state The state to use for the computation.
     * @return
     */
    protected def doDetermineAlias(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult

    /**
     * Creates the result of the analysis based on the current state.
     */
    protected def createResult()(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): ProperPropertyComputationResult

    /**
     * Creates a final [[Result]] with the given alias property.
     */
    protected def result(alias: Alias)(implicit context: AnalysisContext): ProperPropertyComputationResult = {
        Result(context.entity, alias)
    }

    /**
     * Creates an intermediate result for the given upper and lower bounds of the alias properties.
     */
    protected def interimResult(lb: Alias, ub: Alias)(implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        if (lb == ub) result(lb)
        else InterimResult(context.entity, lb, ub, state.getDependees, continuation)
    }

    /**
     * A continuation function that will be invoked when an entity-property pair that this analysis depends on
     * is updated
     */
    protected def continuation(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult

    /**
     * Creates the state to use for the computation.
     */
    protected def createState: AnalysisState

    /**
     * Creates the context to use for the computation.
     */
    protected def createContext(
        entity: AliasEntity
    ): AnalysisContext

}
