/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.ai.ValueOrigin
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI

class SimpleEscapeAnalysisContext(
        val entity:                  (Context, Entity),
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
 * A simple escape analysis that can handle [[org.opalj.tac.common.DefinitionSiteLike]]s and
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s (the this parameter of a constructor). All other
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s are marked as
 * [[org.opalj.br.fpcf.properties.AtMost]]([[org.opalj.br.fpcf.properties.NoEscape]]).
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

    override def determineEscapeOfFP(
        fp: (Context, VirtualFormalParameter)
    ): ProperPropertyComputationResult = {
        fp._2 match {
            case VirtualFormalParameter(dm: DefinedMethod, _) if dm.definedMethod.body.isEmpty =>
                Result(fp, AtMost(NoEscape))
            case VirtualFormalParameter(dm: DefinedMethod, -1) if dm.definedMethod.isInitializer =>
                val ctx = createContext(fp, -1, dm.definedMethod)
                doDetermineEscape(ctx, createState)
            case VirtualFormalParameter(_, _) =>
                Result(fp, AtMost(NoEscape))
        }
    }

    override def createContext(
        entity:       (Context, Entity),
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

trait SimpleEscapeAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, TypeProviderKey)

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(EscapeProperty),
        PropertyBounds.ub(TACAI)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(EscapeProperty)

}

/**
 * A companion object used to start the analysis.
 */
object EagerSimpleEscapeAnalysis
    extends SimpleEscapeAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation ++ Seq(DefinitionSitesKey, SimpleContextsKey)

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val simpleContexts = p.get(SimpleContextsKey)

        val fps = p.get(VirtualFormalParametersKey).virtualFormalParameters.map { fp =>
            (simpleContexts(fp.method), fp)
        }
        val ass = p.get(DefinitionSitesKey).getAllocationSites.map { as =>
            (simpleContexts(declaredMethods(as.method)), as)
        }

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
