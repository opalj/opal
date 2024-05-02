/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import scala.annotation.tailrec

import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
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

    protected[this] type AnalysisContext <: AliasAnalysisContext
    protected[this] type AnalysisState <: AliasAnalysisState
    protected[this] type Tac = TACode[TACMethodParameter, DUVar[ValueInformation]]

    /**
     * Determines the alias relation for the given entity.
     * @param e The entity to determine the aliasing information for.
     * @return The result of the computation.
     */
    def determineAlias(e: Entity): ProperPropertyComputationResult = {
        e match {
            case entity: AliasEntity =>
                val context = createContext(entity)

                if (checkTypeCompatibility(context)) doDetermineAlias(context, createState)
                else result(NoAlias)(context)
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
    private[this] def checkTypeCompatibility(context: AliasAnalysisContext): Boolean = {

        if (context.element1.isNullValue || context.element2.isNullValue)
            return true

        if (!context.element1.isReferenceType || !context.element2.isReferenceType)
            return false

        if (context.element1.referenceType.isArrayType != context.element2.referenceType.isArrayType)
            return false

        val type1 = getObjectType(context.element1.referenceType)
        val type2 = getObjectType(context.element2.referenceType)
        implicit val classHierarchy: ClassHierarchy = project.classHierarchy

        if (type1.isEmpty || type2.isEmpty)
            return false

        if (type1.get._2 != type2.get._2)
            return false

        if ((type1.get._1.isASubtypeOf(type2.get._1) || type2.get._1.isASubtypeOf(type1.get._1)).isNo)
            return false

        true
    }

    @tailrec
    private[this] def getObjectType(refType: ReferenceType, arrayDepth: Int = 0): Option[(ObjectType, Int)] = {
        refType match {
            case ot: ObjectType                          => Some((ot, arrayDepth))
            case ArrayType(_: BaseType)                  => None
            case ArrayType(componentType: ReferenceType) => getObjectType(componentType, arrayDepth + 1)
            case _                                       => None
        }
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
    protected[this] def doDetermineAlias(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult

    /**
     * Creates the result of the analysis based on the current state.
     */
    protected[this] def createResult()(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): ProperPropertyComputationResult

    /**
     * Creates a final [[Result]] with the given alias property.
     */
    protected[this] def result(alias: Alias)(implicit context: AnalysisContext): ProperPropertyComputationResult = {
        Result(context.entity, alias)
    }

    /**
     * Creates a intermediate result for the given upper and lower bounds of the alias properties.
     */
    protected[this] def interimResult(lb: Alias, ub: Alias)(implicit
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
    protected[this] def continuation(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult

    /**
     * Creates the state to use for the computation.
     */
    protected[this] def createState: AnalysisState

    /**
     * Creates the context to use for the computation.
     */
    protected[this] def createContext(
        entity: AliasEntity
    ): AnalysisContext

}
