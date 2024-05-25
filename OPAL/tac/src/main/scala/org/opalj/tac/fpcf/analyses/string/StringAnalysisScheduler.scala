/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
sealed trait StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(MethodStringFlow))

    override final type InitializationData = StringAnalysis

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = new StringAnalysis(p)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

object LazyStringAnalysis
    extends StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, analysis: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty
}

sealed trait MethodStringFlowAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.ub(MethodStringFlow)

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(StringFlowFunctionProperty)
    )

    override final type InitializationData = MethodStringFlowAnalysis
    override def init(p: SomeProject, ps: PropertyStore): InitializationData = new MethodStringFlowAnalysis(p)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

object LazyMethodStringFlowAnalysis
    extends MethodStringFlowAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(MethodStringFlow.key, initData.analyze)
        initData
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}

sealed trait StringFlowAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringFlowFunctionProperty)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI)

    override final type InitializationData = InterpretationHandler

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

trait LazyStringFlowAnalysis
    extends StringFlowAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(StringFlowFunctionProperty.key, initData.analyze)

        initData
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}
