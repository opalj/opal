/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
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
import org.opalj.tac.fpcf.properties.TACAI

class CHACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends ReachableMethodAnalysis {

    override def processMethod(
        declaredMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val tac = tacEP.ub.tac.get

        // for each call site in the current method, the set of methods that might called
        val calleesAndCallers = new DirectCalls()

        // todo add calls to library targets
        @inline def handleTgts(tgts: Set[Method], pc: Int): Unit = {
            if (tgts.isEmpty) {
                calleesAndCallers.addIncompleteCallSite(pc) // todo is this reasonable?
            } else {
                tgts.foreach { tgt ⇒
                    calleesAndCallers.addCall(declaredMethod, declaredMethods(tgt), pc)
                }
            }
        }
        tac.stmts.foreach {
            case stmt @ StaticFunctionCallStatement(call) ⇒
                handleTgts(call.resolveCallTarget.toSet, stmt.pc)

            case call: StaticMethodCall[DUVar[ValueInformation]] ⇒
                handleTgts(call.resolveCallTarget.toSet, call.pc)

            case stmt @ NonVirtualFunctionCallStatement(call) ⇒
                handleTgts(
                    call.resolveCallTarget(declaredMethod.declaringClassType.asObjectType).toSet,
                    stmt.pc
                )
            case call: NonVirtualMethodCall[DUVar[ValueInformation]] ⇒
                handleTgts(
                    call.resolveCallTarget(declaredMethod.declaringClassType.asObjectType).toSet,
                    call.pc
                )

            case stmt @ VirtualFunctionCallStatement(call) ⇒
                handleTgts(
                    call.resolveCallTargets(declaredMethod.declaringClassType.asObjectType).toSet,
                    stmt.pc
                )

            case call: VirtualMethodCall[DUVar[ValueInformation]] ⇒
                handleTgts(
                    call.resolveCallTargets(declaredMethod.declaringClassType.asObjectType).toSet,
                    call.pc
                )

            case Assignment(_, _, idc: InvokedynamicFunctionCall[V]) ⇒
                calleesAndCallers.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )(p.logContext)

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) ⇒
                calleesAndCallers.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )(p.logContext)

            case _ ⇒

        }

        if (tacEP.isFinal)
            Results(calleesAndCallers.partialResults(declaredMethod))
        else
            Results(
                InterimPartialResult(Some(tacEP), continuationForTAC(declaredMethod)),
                calleesAndCallers.partialResults(declaredMethod)
            )
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
