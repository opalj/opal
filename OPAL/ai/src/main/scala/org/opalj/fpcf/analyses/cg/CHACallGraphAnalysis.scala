/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.NoStandardInvokeCallees
import org.opalj.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCalleesImplementation
import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.InvokedynamicFunctionCall
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

class CHACallGraphAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    private implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        (propertyStore(declaredMethod, CallersProperty.key): @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: SomeEPS ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            processMethod(declaredMethod, tacEP)
        } else {
            InterimResult.forUB(
                declaredMethod,
                NoStandardInvokeCallees,
                Seq(tacEP),
                continuationForTAC(declaredMethod)
            )
        }
    }

    private[this] def continuationForTAC(declaredMethod: DeclaredMethod)(
        someEPS: SomeEPS
    ): ProperPropertyComputationResult = someEPS match {
        case UBP(tac: TACAI) if tac.tac.isDefined ⇒
            processMethod(declaredMethod, someEPS.asInstanceOf[EPS[Method, TACAI]])
        case _ ⇒
            InterimResult.forUB(
                declaredMethod,
                NoStandardInvokeCallees,
                Seq(someEPS),
                continuationForTAC(declaredMethod)
            )
    }

    private[this] def processMethod(
        declaredMethod: DeclaredMethod, tacEP: EOptionP[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val tac = tacEP.ub.tac.get

        // for each call site in the current method, the set of methods that might called
        val calleesAndCallers = new CalleesAndCallers()

        // todo add calls to library targets
        @inline def handleTgts(tgts: Set[Method], pc: Int): Unit = {
            if (tgts.isEmpty) {
                calleesAndCallers.addIncompleteCallsite(pc) // todo is this reasonable?
            } else {
                tgts.foreach { tgt ⇒
                    calleesAndCallers.updateWithCall(declaredMethod, declaredMethods(tgt), pc)
                }
            }
        }

        implicit val p: SomeProject = project

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
                calleesAndCallers.addIncompleteCallsite(idc.pc)
                OPALLogger.logOnce(
                    Warn(
                        "analysis",
                        s"unresolved invokedynamic ignored by call graph construction"
                    )
                )(p.logContext)

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) ⇒
                calleesAndCallers.addIncompleteCallsite(idc.pc)
                OPALLogger.logOnce(
                    Warn(
                        "analysis",
                        s"unresolved invokedynamic ignored by call graph construction"
                    )
                )(p.logContext)

            case _ ⇒

        }

        val callees = if (calleesAndCallers.callees.isEmpty)
            NoStandardInvokeCallees
        else
            new StandardInvokeCalleesImplementation(
                calleesAndCallers.callees, calleesAndCallers.incompleteCallsites
            )

        val calleesResult = if (tacEP.isFinal)
            Result(declaredMethod, callees)
        else
            InterimResult.forUB(
                declaredMethod, callees, Seq(tacEP), continuationForTAC(declaredMethod)
            )

        Results(
            calleesResult :: calleesAndCallers.partialResultsForCallers
        )
    }

}

object CHACallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty),
        PropertyBounds.ub(TACAI)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set(
        PropertyBounds.ub(StandardInvokeCallees)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty)
    )

    /**
     * Updates the caller properties of the initial entry points ([[InitialEntryPointsKey]]) to be
     * called from an unknown context.
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
            ps.preInitialize(ep, CallersProperty.key) {
                case _: EPK[_, _]   ⇒ InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub) ⇒ InterimEUBP(ep, ub.updatedWithUnknownContext())
                case eps            ⇒ throw new IllegalStateException(s"unexpected: $eps")
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
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
