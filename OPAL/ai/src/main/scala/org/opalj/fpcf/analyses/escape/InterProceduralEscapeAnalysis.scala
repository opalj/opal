/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.ai.ValueOrigin
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.properties._
import org.opalj.value.ValueInformation
import org.opalj.tac.DUVar
import org.opalj.tac.fpcf.properties.TACAI

class InterProceduralEscapeAnalysisContext(
    val entity:                  Entity,
    val defSitePC:               ValueOrigin,
    val targetMethod:            Method,
    val declaredMethods:         DeclaredMethods,
    val virtualFormalParameters: VirtualFormalParameters,
    val project:                 SomeProject,
    val propertyStore:           PropertyStore,
    val isMethodOverridable:     Method ⇒ Answer
) extends AbstractEscapeAnalysisContext
        with PropertyStoreContainer
        with IsMethodOverridableContainer
        with VirtualFormalParametersContainer
        with DeclaredMethodsContainer

class InterProceduralEscapeAnalysisState
    extends AbstractEscapeAnalysisState with DependeeCache with ReturnValueUseSites

/**
 * A flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterProceduralEscapeAnalysis private[analyses] (
    final val project: SomeProject
) extends DefaultEscapeAnalysis
        with AbstractInterProceduralEscapeAnalysis
        with ConstructorSensitiveEscapeAnalysis
        with ConfigurationBasedConstructorEscapeAnalysis
        with SimpleFieldAwareEscapeAnalysis
        with ExceptionAwareEscapeAnalysis {

    override type AnalysisContext = InterProceduralEscapeAnalysisContext
    type AnalysisState = InterProceduralEscapeAnalysisState

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    override def determineEscapeOfFP(fp: VirtualFormalParameter): ProperPropertyComputationResult = {
        fp match {
            // if the underlying method is inherited, we avoid recomputation and query the
            // result of the method for its defining class.
            case VirtualFormalParameter(dm: DefinedMethod, i) if dm.declaringClassType != dm.definedMethod.classFile.thisType ⇒
                def handleEscapeState(eOptionP: SomeEOptionP): ProperPropertyComputationResult = {
                    eOptionP match {
                        case FinalP(p) ⇒
                            Result(fp, p)

                        case InterimLUBP(lb, ub) ⇒
                            InterimResult.create(
                                fp,
                                lb,
                                ub,
                                Set(eOptionP),
                                handleEscapeState,
                                CheapPropertyComputation
                            )

                        case _ ⇒
                            InterimResult(
                                fp,
                                GlobalEscape,
                                NoEscape,
                                Set(eOptionP),
                                handleEscapeState,
                                CheapPropertyComputation
                            )
                    }
                }

                val parameterOfBase = virtualFormalParameters(
                    declaredMethods(dm.definedMethod)
                )(-i - 1)

                handleEscapeState(propertyStore(parameterOfBase, EscapeProperty.key))

            case VirtualFormalParameter(dm: DefinedMethod, _) if dm.definedMethod.body.isEmpty ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))

            // parameters of base types are not considered
            case VirtualFormalParameter(m, i) if i != -1 && m.descriptor.parameterType(-i - 2).isBaseType ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))

            case VirtualFormalParameter(dm: DefinedMethod, i) ⇒
                val ctx = createContext(fp, i, dm.definedMethod)
                doDetermineEscape(ctx, createState)

            case VirtualFormalParameter(_: VirtualDeclaredMethod, _) ⇒
                throw new IllegalArgumentException()
        }
    }

    override def createContext(
        entity:       Entity,
        defSitePC:    ValueOrigin,
        targetMethod: Method
    ): InterProceduralEscapeAnalysisContext = new InterProceduralEscapeAnalysisContext(
        entity,
        defSitePC,
        targetMethod,
        declaredMethods,
        virtualFormalParameters,
        project,
        propertyStore,
        isMethodOverridable
    )

    override def createState: InterProceduralEscapeAnalysisState = new InterProceduralEscapeAnalysisState()
}

sealed trait InterProceduralEscapeAnalysisScheduler extends ComputationSpecification[FPCFAnalysis] {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(EscapeProperty)

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.lub(EscapeProperty)
    )
}

object EagerInterProceduralEscapeAnalysis
        extends InterProceduralEscapeAnalysisScheduler
        with BasicFPCFEagerAnalysisScheduler {
    type V = DUVar[ValueInformation]

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InterProceduralEscapeAnalysis(p)
        val fps = p.get(VirtualFormalParametersKey).virtualFormalParameters
        val ass = p.get(DefinitionSitesKey).getAllocationSites
        ps.scheduleEagerComputationsForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazyInterProceduralEscapeAnalysis
        extends InterProceduralEscapeAnalysisScheduler
        with BasicFPCFLazyAnalysisScheduler {

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InterProceduralEscapeAnalysis(p)
        ps.registerLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
