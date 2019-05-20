/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.cg.pointsto

import org.opalj.log.Error
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.Call
import org.opalj.tac.VirtualCall
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysis
import org.opalj.tac.fpcf.analyses.cg.DirectCalls
import org.opalj.tac.fpcf.analyses.pointsto.PointsToBasedAnalysis

/**
 * Uses the [[org.opalj.br.fpcf.pointsto.properties.PointsTo]] of
 * [[org.opalj.tac.common.DefinitionSite]] and[[org.opalj.br.analyses.VirtualFormalParameter]]s
 * in order to determine the targets of virtual method calls.
 *
 * @author Florian Kuebler
 */
class PointsToBasedCallGraph private[analyses] (
        final val project: SomeProject
) extends CallGraphAnalysis with PointsToBasedAnalysis {

    override type State = PointsToBasedCGState

    /**
     * Computes the calles of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    override def handleImpreciseCall(
        caller: DefinedMethod,
        call:   Call[V] with VirtualCall[V],
        pc:     Int, specializedDeclaringClassType: ReferenceType,
        potentialTargets:  ForeachRefIterator[ObjectType],
        calleesAndCallers: DirectCalls
    )(implicit state: PointsToBasedCGState): Unit = {
        val callerType = caller.definedMethod.classFile.thisType
        val callSite = (pc, call.name, call.descriptor, call.declaringClass)
        val pointsToSet = handleDefSites(callSite, call.receiver.asVar.definedBy)

        var types = IntTrieSet.empty

        for (newType ← potentialTargets) {
            if (pointsToSet.contains(newType)) {
                val tgtR = project.instanceCall(
                    callerType,
                    newType,
                    call.name,
                    call.descriptor
                )
                handleCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            } else {
                types += newType.id
            }
        }
        state.initialPotentialTypesOfCallSite(callSite, types)

    }

    override def c(
        state: PointsToBasedCGState
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
            state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
            processMethod(state, new DirectCalls())

        case UBP(_: TACAI) ⇒
            InterimPartialResult(Some(eps), c(state))

        case EUBPS(e, ub: PointsTo, isFinal) ⇒
            val relevantCallSites = state.callSitesForDefSite(e)

            // ensures, that we only add new calls
            val calls = new DirectCalls()

            for (callSite ← relevantCallSites) {
                val oldEOptP = state.getPointsToEPS(eps.e)
                val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
                val typesLeft = state.typesForCallSite(callSite)
                for (newType ← ub.drop(seenTypes)) {
                    if (typesLeft.contains(newType.id)) {
                        state.removeTypeForCallSite(callSite, newType)
                        val (pc, name, descriptor, declaredType) = callSite
                        val tgtR = project.instanceCall(
                            state.method.declaringClassType.asObjectType,
                            newType,
                            name,
                            descriptor
                        )
                        handleCall(
                            state.method, name, descriptor, declaredType, pc, tgtR, calls
                        )
                    }

                }
            }

            if (isFinal) {
                state.removePointsToDependency(e)
            } else {
                state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsTo]])
            }
            returnResult(calls)(state)
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): PointsToBasedCGState = new PointsToBasedCGState(definedMethod, tacEP)

}

object PointsToBasedCallGraphScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        PointsTo, Callees, Callers, TACAI
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        Callees, Callers
    )

    override def register(p: SomeProject, ps: PropertyStore, i: Null): PointsToBasedCallGraph = {
        val analysis = new PointsToBasedCallGraph(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    /**
     * Updates the caller properties of the initial entry points
     * ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) to be called from an unknown context.
     * This will trigger the computation of the callees for these methods (see `processMethod`).
     */
    def processEntryPoints(p: SomeProject, ps: PropertyStore): Unit = {
        implicit val logContext: LogContext = p.logContext
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey).map(declaredMethods.apply)

        if (entryPoints.isEmpty)
            logOnce(Error("project configuration", "the project has no entry points"))

        entryPoints.foreach { ep ⇒
            ps.preInitialize(ep, Callers.key) {
                case _: EPK[_, _] ⇒
                    InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub: Callers) ⇒
                    InterimEUBP(ep, ub.updatedWithUnknownContext())
                case r ⇒
                    throw new IllegalStateException(s"unexpected eps $r")
            }
        }
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        processEntryPoints(p, ps)
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(
        p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis
    ): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}