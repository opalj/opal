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

    def determinePrematureReads(field: Field): PropertyComputationResult = {
        Result(field, NotPrematurelyReadField)
    }
}

trait UnsoundPrematurelyReadFieldsAnalysisScheduler extends ComputationSpecification {
    def uses: Set[PropertyKind] = Set.empty

    def derives: Set[PropertyKind] = Set(FieldPrematurelyRead)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

/**
 * Factory object to create instances of the FieldMutabilityAnalysis.
 */
object EagerUnsoundPrematurelyReadFieldsAnalysis
    extends UnsoundPrematurelyReadFieldsAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new UnsoundPrematurelyReadFieldsAnalysis(project)

        propertyStore.scheduleEagerComputationsForEntities(project.allFields)(
            analysis.determinePrematureReads
        )
        analysis
    }
}

object LazyUnsoundPrematurelyReadFieldsAnalysis
    extends UnsoundPrematurelyReadFieldsAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    def startLazily(
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

}
