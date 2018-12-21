/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field
import org.opalj.fpcf.properties.FieldPrematurelyRead
import org.opalj.fpcf.properties.NotPrematurelyReadField

/**
 * Unsound 'analysis' that declares all fields to be
 * [[org.opalj.fpcf.properties.NotPrematurelyReadField]]s.
 *
 * @author Dominik Helm
 */
class UnsoundPrematurelyReadFieldsAnalysis private[analyses] (val project: SomeProject)
        extends FPCFAnalysis {

    def determinePrematureReads(field: Field): ProperPropertyComputationResult = {
        Result(field, NotPrematurelyReadField)
    }
}

trait UnsoundPrematurelyReadFieldsAnalysisScheduler extends ComputationSpecification[FPCFAnalysis] {
    final override def uses: Set[PropertyBounds] = Set.empty

    final def derivedProperty: PropertyBounds = PropertyBounds.finalP(FieldPrematurelyRead)
}

/**
 * Factory object to create instances of the FieldMutabilityAnalysis.
 */
object EagerUnsoundPrematurelyReadFieldsAnalysis
        extends UnsoundPrematurelyReadFieldsAnalysisScheduler
        with BasicFPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new UnsoundPrematurelyReadFieldsAnalysis(project)

        propertyStore.scheduleEagerComputationsForEntities(project.allFields)(
            analysis.determinePrematureReads
        )
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazyUnsoundPrematurelyReadFieldsAnalysis
        extends UnsoundPrematurelyReadFieldsAnalysisScheduler
        with BasicFPCFLazyAnalysisScheduler {

    def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        unused:        Null
    ): FPCFAnalysis = {
        val analysis = new UnsoundPrematurelyReadFieldsAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldPrematurelyRead.key,
            (field: Field) ⇒ analysis.determinePrematureReads(field)
        )
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
