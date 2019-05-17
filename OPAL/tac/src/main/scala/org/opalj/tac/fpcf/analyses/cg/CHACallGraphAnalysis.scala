/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.TACAI

class CHAState(
        val method:                                DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState {

    override def hasNonFinalCallSite: Boolean = false

    override def hasOpenDependencies: Boolean = _tacDependee.isRefinable

    override def dependees: Traversable[SomeEOptionP] = tacDependee()
}

class CHACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends CallGraphAnalysis {
    override type State = CHAState

    override def handleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: CHAState): Unit = {
        for (tgt ← potentialTargets) {
            val tgtR = project.instanceCall(
                caller.declaringClassType.asObjectType, tgt, call.name, call.descriptor
            )
            handleCall(
                caller, call.name, call.descriptor, call.declaringClass, pc, tgtR, calleesAndCallers
            )
        }
    }

    override def c(state: CHAState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: TACAI) ⇒
                processMethod(state.method, eps.asInstanceOf[EPS[Method, TACAI]])
            case _ ⇒ throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): CHAState = {
        new CHAState(definedMethod, tacEP)
    }
}

object CHACallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        TACAI
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callers, Callees)

    /**
     * Updates the caller properties of the initial entry points
     * ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) to be called from an unknown context.
     * This will trigger the computation of the callees for these methods (see `processMethod`).
     */
    def processEntryPoints(p: SomeProject, ps: PropertyStore): Unit = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey).map(declaredMethods.apply)

        if (entryPoints.isEmpty)
            OPALLogger.logOnce(
                Error("project configuration", "the project has no entry points")
            )(p.logContext)

        entryPoints.foreach { ep ⇒
            ps.preInitialize(ep, Callers.key) {
                case _: EPK[_, _] ⇒
                    InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub: Callers) ⇒
                    InterimEUBP(ep, ub.updatedWithUnknownContext())
                case eps ⇒
                    throw new IllegalStateException(s"unexpected: $eps")
            }
        }
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        processEntryPoints(p, ps)
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): CHACallGraphAnalysis = {
        val analysis = new CHACallGraphAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}
