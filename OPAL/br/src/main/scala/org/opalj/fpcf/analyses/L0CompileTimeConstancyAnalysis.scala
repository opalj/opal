/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.NonFinalField
import org.opalj.fpcf.properties.CompileTimeVaryingField
import org.opalj.fpcf.properties.CompileTimeConstancy
import org.opalj.fpcf.properties.LazyInitializedField
import org.opalj.fpcf.properties.CompileTimeConstantField

/**
 * A simple analysis that identifies constant (effectively) final static fields that are
 * deterministically initialized to the same value on every program execution.
 * This analysis just examines the ConstantValue attribute of the field.
 *
 * @author Dominik Helm
 */
class L0CompileTimeConstancyAnalysis private[analyses] ( final val project: SomeProject)
    extends FPCFAnalysis {

    /**
     * Determines the compile-time constancy of the field.
     *
     * This function encapsulates the continuation.
     */
    def determineConstancy(
        field: Field
    ): PropertyComputationResult = {
        if (!field.isStatic || field.constantFieldValue.isEmpty)
            return Result(field, CompileTimeVaryingField);

        if (field.isFinal)
            return Result(field, CompileTimeConstantField);

        var dependee: EOptionP[Entity, Property] = propertyStore(field, FieldMutability.key) match {
            case FinalP(_, LazyInitializedField) ⇒ return Result(field, CompileTimeVaryingField);
            case FinalP(_, _: FinalField)        ⇒ return Result(field, CompileTimeConstantField);
            case FinalP(_, _: NonFinalField)     ⇒ return Result(field, CompileTimeVaryingField);
            case ep                              ⇒ ep
        }

        // This function updates the compile-time constancy of the field when the field's
        // mutability is updated
        def c(eps: SomeEPS): PropertyComputationResult = {
            eps match {
                case FinalP(_, LazyInitializedField) ⇒
                    Result(field, CompileTimeVaryingField);
                case FinalP(_, _: FinalField) ⇒
                    Result(field, CompileTimeConstantField);
                case FinalP(_, _: NonFinalField) ⇒
                    Result(field, CompileTimeVaryingField);

                case InterimP(_, _, _) ⇒
                    dependee = eps
                    InterimResult(
                        field,
                        CompileTimeVaryingField,
                        CompileTimeConstantField,
                        Seq(dependee),
                        c,
                        CheapPropertyComputation
                    )
            }
        }

        InterimResult(
            field,
            CompileTimeVaryingField,
            CompileTimeConstantField,
            Seq(dependee),
            c,
            CheapPropertyComputation
        )
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineConstancy(e: Entity): PropertyComputationResult = {
        e match {
            case f: Field ⇒ determineConstancy(f)
            case _ ⇒
                throw new UnknownError("compile-time constancy is only defined for fields")
        }
    }
}

trait L0CompileTimeConstancyAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(CompileTimeConstancy)

    final override def uses: Set[PropertyKind] = Set(FieldMutability)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

object EagerL0CompileTimeConstancyAnalysis
    extends L0CompileTimeConstancyAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0CompileTimeConstancyAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allFields)(analysis.determineConstancy)
        analysis
    }
}

object LazyL0CompileTimeConstancyAnalysis extends L0CompileTimeConstancyAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0CompileTimeConstancyAnalysis(p)
        ps.registerLazyPropertyComputation(CompileTimeConstancy.key, analysis.doDetermineConstancy)
        analysis
    }
}
