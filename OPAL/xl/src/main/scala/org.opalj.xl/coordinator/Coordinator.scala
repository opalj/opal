/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package coordinator
/*
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.analyses.a0.A0TaintAnalysis
import org.opalj.xl.analyses.a2.A2ProjectKey
import org.opalj.xl.analyses.a2.A2TaintAnalysis
import org.opalj.xl.analyses.a2.EagerA0TaintAnalysis
import org.opalj.xl.languages.Language

class Coordinator(val project: SomeProject) extends FPCFAnalysis {
  def coordinate(l: Language): ProperPropertyComputationResult = {
    for {
      corpus <- l.corpi
      function <- corpus.functions
    } {
      project.get(GenericProjectKey)

      val (propertyStore, _) = project
        .get(FPCFAnalysesManagerKey)
        .runAll(
          EagerA0TaintAnalysis
        )
    }
  }

  trait CoordinationScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(TaintLattice)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] =
      Set(PropertyBounds.ub(TaintLattice), PropertyBounds.ub(TaintLattice))
  }

  object EagerCoordination extends CoordinationScheduler with BasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(
        project: SomeProject,
        propertyStore: PropertyStore,
        initData: InitializationData
    ): FPCFAnalysis = {
      val coordinator = new Coordinator(project)
      val a0Project = project.get(A2ProjectKey)
      val l = List[A0TaintAnalysis]
      propertyStore.scheduleEagerComputationsForEntities(l)(coordinator.coordinate)
      coordinator
    }
  }

  object LazyCoordination extends CoordinationScheduler with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        project: SomeProject,
        propertyStore: PropertyStore,
        initData: InitializationData
    ): FPCFAnalysis = {
      val analysis = new A2TaintAnalysis(project)
      propertyStore.registerLazyPropertyComputation(
        TaintLattice.key,
        analysis.lazilyAnalyzeFieldImmutability
      )
      analysis
    }
  }
}
*/