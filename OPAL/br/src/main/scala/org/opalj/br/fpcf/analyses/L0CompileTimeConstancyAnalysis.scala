/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.CompileTimeConstancy
import org.opalj.br.fpcf.properties.CompileTimeConstantField
import org.opalj.br.fpcf.properties.CompileTimeVaryingField
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FinalField
import org.opalj.br.fpcf.properties.LazyInitializedField
import org.opalj.br.fpcf.properties.NonFinalField

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
    def determineConstancy(field: Field): ProperPropertyComputationResult = {
        if (!field.isStatic || field.constantFieldValue.isEmpty)
            return Result(field, CompileTimeVaryingField);

        if (field.isFinal)
            return Result(field, CompileTimeConstantField);

        var dependee: EOptionP[Entity, Property] = {
            propertyStore(field, FieldMutability.key) match {
                case FinalP(LazyInitializedField) => return Result(field, CompileTimeVaryingField);
                case FinalP(_: FinalField)        => return Result(field, CompileTimeConstantField);
                case FinalP(_: NonFinalField)     => return Result(field, CompileTimeVaryingField);
                case ep                           => ep
            }
        }

        // This function updates the compile-time constancy of the field when the field's
        // mutability is updated
        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            (eps: @unchecked) match {
                case _: SomeInterimEP =>
                    dependee = eps
                    InterimResult(
                        field,
                        CompileTimeVaryingField,
                        CompileTimeConstantField,
                        Set(dependee),
                        c
                    )

                case FinalP(LazyInitializedField) => Result(field, CompileTimeVaryingField)
                case FinalP(_: FinalField)        => Result(field, CompileTimeConstantField)
                case FinalP(_: NonFinalField)     => Result(field, CompileTimeVaryingField)
            }
        }

        InterimResult(field, CompileTimeVaryingField, CompileTimeConstantField, Set(dependee), c)
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineConstancy(e: Entity): ProperPropertyComputationResult = {
        e match {
            case f: Field => determineConstancy(f)
            case _ =>
                throw new UnknownError("compile-time constancy is only defined for fields")
        }
    }
}

trait L0CompileTimeConstancyAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    final override def uses: Set[PropertyBounds] = PropertyBounds.lubs(FieldMutability)

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(CompileTimeConstancy)

}

object EagerL0CompileTimeConstancyAnalysis
    extends L0CompileTimeConstancyAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0CompileTimeConstancyAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allFields)(analysis.determineConstancy)
        analysis
    }
}

object LazyL0CompileTimeConstancyAnalysis
    extends L0CompileTimeConstancyAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0CompileTimeConstancyAnalysis(p)
        ps.registerLazyPropertyComputation(CompileTimeConstancy.key, analysis.doDetermineConstancy)
        analysis
    }
}
