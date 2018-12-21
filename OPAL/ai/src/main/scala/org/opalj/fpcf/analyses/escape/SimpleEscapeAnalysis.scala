/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.ai.ValueOrigin
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.fpcf.properties.TACAI

class SimpleEscapeAnalysisContext(
        val entity:                  Entity,
        val defSitePC:               ValueOrigin,
        val targetMethod:            Method,
        val declaredMethods:         DeclaredMethods,
        val virtualFormalParameters: VirtualFormalParameters,
        val project:                 SomeProject,
        val propertyStore:           PropertyStore
) extends AbstractEscapeAnalysisContext
    with PropertyStoreContainer
    with VirtualFormalParametersContainer
    with DeclaredMethodsContainer

/**
 * A simple escape analysis that can handle [[org.opalj.ai.common.DefinitionSiteLike]]s and
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s (the this parameter of a constructor). All other
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s are marked as
 * [[org.opalj.fpcf.properties.AtMost]]([[org.opalj.fpcf.properties.NoEscape]]).
 *
 *
 *
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis( final val project: SomeProject)
    extends DefaultEscapeAnalysis
    with ConstructorSensitiveEscapeAnalysis
    with ConfigurationBasedConstructorEscapeAnalysis
    with SimpleFieldAwareEscapeAnalysis
    with ExceptionAwareEscapeAnalysis {

    override type AnalysisContext = SimpleEscapeAnalysisContext
    override type AnalysisState = AbstractEscapeAnalysisState

    override def determineEscapeOfFP(fp: VirtualFormalParameter): ProperPropertyComputationResult = {
        fp match {
            case VirtualFormalParameter(dm: DefinedMethod, _) if dm.definedMethod.body.isEmpty ⇒
                Result(fp, AtMost(NoEscape))
            case VirtualFormalParameter(dm: DefinedMethod, -1) if dm.definedMethod.isInitializer ⇒
                val ctx = createContext(fp, -1, dm.definedMethod)
                doDetermineEscape(ctx, createState)
            case VirtualFormalParameter(_, _) ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))
        }
    }

    override def createContext(
        entity:       Entity,
        defSite:      ValueOrigin,
        targetMethod: Method
    ): SimpleEscapeAnalysisContext = new SimpleEscapeAnalysisContext(
        entity,
        defSite,
        targetMethod,
        declaredMethods,
        virtualFormalParameters,
        project,
        propertyStore
    )

    override def createState: AbstractEscapeAnalysisState = new AbstractEscapeAnalysisState {}
}

trait SimpleEscapeAnalysisScheduler extends ComputationSpecification[FPCFAnalysis] {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(EscapeProperty)

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(EscapeProperty),
        PropertyBounds.ub(TACAI)
    )
}

/**
 * A companion object used to start the analysis.
 */
object EagerSimpleEscapeAnalysis
    extends SimpleEscapeAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val fps = p.get(VirtualFormalParametersKey).virtualFormalParameters
        val ass = p.get(DefinitionSitesKey).getAllocationSites
        val analysis = new SimpleEscapeAnalysis(p)
        ps.scheduleEagerComputationsForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazySimpleEscapeAnalysis
    extends SimpleEscapeAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(p)
        ps.registerLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
