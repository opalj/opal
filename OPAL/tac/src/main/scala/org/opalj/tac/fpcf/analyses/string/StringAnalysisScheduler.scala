/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

/**
 * A trait for FPCF analysis schedulers that combine the parts of the string analysis that produce
 * [[StringConstancyProperty]]s.
 *
 * @author Maximilian Rüsch
 */
sealed trait StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(MethodStringFlow))

    override final type InitializationData =
        (ContextFreeStringAnalysis, ContextStringAnalysis, MethodParameterContextStringAnalysis)

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = (
        new ContextFreeStringAnalysis(p),
        new ContextStringAnalysis(p),
        new MethodParameterContextStringAnalysis(p)
    )

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

/**
 * A lazy adaptation of the [[StringAnalysisScheduler]]. All three string analyses are combined since the property store
 * does not allow registering more than one lazy analysis scheduler for a given property.
 *
 * @author Maximilian Rüsch
 */
object LazyStringAnalysis
    extends StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, data: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(
            StringConstancyProperty.key,
            (entity: Entity) => {
                entity match {
                    case vd: VariableDefinition     => data._1.analyze(vd)
                    case vc: VariableContext        => data._2.analyze(vc)
                    case vc: MethodParameterContext => data._3.analyze(vc)
                    case e                          => throw new IllegalArgumentException(s"Cannot process entity $e")
                }
            }
        )
        data._1
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey)
}
