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
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsMObjectValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.IsSObjectValue
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.cg.CallBySignatureKey
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Generates call graphs based on the used [[TypeProvider]].
 * It uses the AI information of the three-address code to get the most precise information for
 * virtual calls.
 * `handleImpreciseCall` will be invoked for each virtual call, that could not be resolved
 * precisely.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class CallGraphAnalysis private[cg] (
        override val project: SomeProject
) extends ReachableMethodAnalysis with TypeConsumerAnalysis {
    type LocalTypeInformation

    private[this] val isMethodOverridable: Method => Answer = project.get(IsOverridableMethodKey)
    private[this] lazy val getCBSTargets = project.get(CallBySignatureKey)
    private[this] val resovleCallBySignature =
        project.config.getBoolean("org.opalj.br.analyses.cg.callBySignatureResolution")

    def c(state: CGState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tacai: TACAI) if tacai.tac.isDefined =>
                state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])

                // we only want to add the new calls, so we create a fresh object
                processMethod(state, new DirectCalls())

            case UBP(_: TACAI) =>
                throw new IllegalStateException("there was already a tac defined")

            case EPS(e) =>
                val relevantCallSites = state.dependersOf(eps.toEPK).asInstanceOf[Set[CallSite]]

                // ensures, that we only add new calls
                val calls = new DirectCalls()

                for (cs <- relevantCallSites) {
                    val (receiver, cbsTargets) = state.callSiteData(cs)
                    typeProvider.continuation(
                        receiver, eps.asInstanceOf[EPS[Entity, PropertyType]], cbsTargets
                    ) {
                        newType =>
                            val CallSite(pc, name, descriptor, declaredType) = cs
                            val tgtR = project.instanceCall(
                                state.callContext.method.declaringClassType,
                                newType,
                                name,
                                descriptor
                            )
                            handleCall(
                                state.callContext,
                                name,
                                descriptor,
                                declaredType,
                                isStatic = false,
                                newType,
                                pc,
                                tgtR,
                                calls
                            )
                    }(state)
                }

                if (eps.isFinal) {
                    state.removeDependee(eps.toEPK)
                } else {
                    state.updateDependency(eps)
                }

                returnResult(calls)(state)
        }
    }

    override final def processMethod(
        callContext: ContextType, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val state = new CGState[ContextType](callContext, tacEP)
        if (tacEP ne null)
            processMethod(state, new DirectCalls())
        else
            returnResult(new DirectCalls(), enforceCalleesResult = true)(state)
    }

    override final val processesMethodsWithoutBody = true

    protected[this] def doHandleVirtualCall(
        callContext:                   ContextType,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        isPrecise:                     Boolean,
        calleesAndCallers:             DirectCalls
    )(implicit state: CGState[ContextType]): Unit = {
        val callerType = callContext.method.declaringClassType
        val callSite = CallSite(pc, call.name, call.descriptor, call.declaringClass)

        val cbsTargets: Set[ReferenceType] =
            if (!isPrecise && resovleCallBySignature && call.isInterface &&
                call.declaringClass.isObjectType) {
                val cf = project.classFile(call.declaringClass.asObjectType)
                cf.flatMap { _.findMethod(call.name, call.descriptor) }.map {
                    getCBSTargets(_).toSet[ReferenceType]
                }.getOrElse(Set.empty)
            } else Set.empty

        val actualTypes = typeProvider.typesProperty(
            call.receiver.asVar, state.callContext, callSite, state.tac.stmts
        )

        typeProvider.foreachType(call.receiver.asVar, actualTypes, cbsTargets) { possibleTgtType =>
            val tgtR = project.instanceCall(
                callerType, possibleTgtType, call.name, call.descriptor
            )

            handleCall(
                callContext,
                call.name,
                call.descriptor,
                call.declaringClass,
                isStatic = false,
                possibleTgtType,
                pc,
                tgtR,
                calleesAndCallers
            )
        }

        state.addCallSite(callSite, call.receiver.asVar, cbsTargets)

        // Deal with the fact that there may be unknown subtypes of the receiver type that might
        // override the method
        if (!isPrecise) {
            if (specializedDeclaringClassType.isObjectType) {
                val declType = specializedDeclaringClassType.asObjectType

                val mResult = if (classHierarchy.isInterface(declType).isYes)
                    org.opalj.Result(project.resolveInterfaceMethodReference(
                        declType, call.name, call.descriptor
                    ))
                else
                    org.opalj.Result(project.resolveMethodReference(
                        declType,
                        call.name,
                        call.descriptor,
                        forceLookupInSuperinterfacesOnFailure = true
                    ))

                if (mResult.isEmpty) {
                    unknownLibraryCall(
                        callContext,
                        call.name,
                        call.descriptor,
                        call.declaringClass,
                        isStatic = false,
                        declType,
                        callContext.method.definedMethod.classFile.thisType.packageName,
                        pc,
                        calleesAndCallers
                    )
                } else if (isMethodOverridable(mResult.value).isYesOrUnknown) {
                    calleesAndCallers.addIncompleteCallSite(pc)
                }
            }
        }
    }

    protected final def processMethod(
        state: CGState[ContextType], calls: DirectCalls
    ): ProperPropertyComputationResult = {
        val tac = state.tac

        tac.stmts.foreach {
            case stmt @ StaticFunctionCallStatement(call) =>
                handleCall(
                    state.callContext,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    isStatic = true,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget(state.callContext.method.declaringClassType),
                    calls
                )

            case call: StaticMethodCall[V] =>
                handleCall(
                    state.callContext,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    isStatic = true,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget(state.callContext.method.declaringClassType),
                    calls
                )

            case stmt @ NonVirtualFunctionCallStatement(call) =>
                handleCall(
                    state.callContext,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    isStatic = false,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget(state.callContext.method.declaringClassType),
                    calls
                )

            case call: NonVirtualMethodCall[V] =>
                handleCall(
                    state.callContext,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    isStatic = false,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget(state.callContext.method.declaringClassType),
                    calls
                )

            case VirtualFunctionCallStatement(call) =>
                handleVirtualCall(state.callContext, call, call.pc, calls)(state)

            case call: VirtualMethodCall[V] =>
                handleVirtualCall(state.callContext, call, call.pc, calls)(state)

            case Assignment(_, _, idc: InvokedynamicFunctionCall[V]) =>
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) =>
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] =>
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case _ => //nothing to do
        }

        returnResult(calls, true)(state)
    }

    protected[this] def returnResult(
        calleesAndCallers: DirectCalls, enforceCalleesResult: Boolean = false
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.callContext, enforceCalleesResult)

        // FIXME: This won't work for refinable TACs as state.hasNonFinalCallSite may return false
        //  even if an update for the tac might add a non-final call site
        if (state.hasNonFinalCallSite && state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                results
            )
        else
            Results(results)
    }

    protected final def handleCall(
        callContext:        ContextType,
        callName:           String,
        callDescriptor:     MethodDescriptor,
        callDeclaringClass: ReferenceType,
        isStatic:           Boolean,
        receiverType:       ReferenceType,
        pc:                 Int,
        target:             org.opalj.Result[Method],
        calleesAndCallers:  DirectCalls
    ): Unit = {
        if (target.hasValue) {
            val tgtDM = declaredMethods(target.value)
            calleesAndCallers.addCall(
                callContext, pc, typeProvider.expandContext(callContext, tgtDM, pc)
            )
        } else {
            val packageName = callContext.method.definedMethod.classFile.thisType.packageName
            unknownLibraryCall(
                callContext,
                callName,
                callDescriptor,
                callDeclaringClass,
                isStatic,
                receiverType,
                packageName,
                pc,
                calleesAndCallers
            )
        }
    }

    protected final def unknownLibraryCall(
        callContext:         ContextType,
        callName:            String,
        callDescriptor:      MethodDescriptor,
        callDeclaringClass:  ReferenceType,
        isStatic:            Boolean,
        runtimeReceiverType: ReferenceType,
        packageName:         String,
        pc:                  Int,
        calleesAndCallers:   DirectCalls
    ): Unit = {
        val declaringClassType = callDeclaringClass.mostPreciseObjectType
        val runtimeType = runtimeReceiverType.mostPreciseObjectType

        val declTgt = declaredMethods.apply(
            declaringClassType,
            packageName,
            runtimeType,
            callName,
            callDescriptor
        )

        if (declTgt.hasSingleDefinedMethod) {
            if (declTgt.definedMethod.isStatic == isStatic)
                calleesAndCallers.addCall(
                    callContext, pc, typeProvider.expandContext(callContext, declTgt, pc)
                )
        } else if (declTgt.isVirtualOrHasSingleDefinedMethod) {
            calleesAndCallers.addCall(
                callContext, pc, typeProvider.expandContext(callContext, declTgt, pc)
            )
        } else {
            declTgt.definedMethods foreach { m =>
                if (m.isStatic == isStatic) {
                    val dm = declaredMethods(m)
                    calleesAndCallers.addCall(
                        callContext, pc, typeProvider.expandContext(callContext, dm, pc)
                    )
                }
            }
        }

        calleesAndCallers.addIncompleteCallSite(pc)
    }

    /**
     * Computes the calles of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    private[this] def handleVirtualCall(
        callContext:       ContextType,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: DirectCalls
    )(implicit state: CGState[ContextType]): Unit = {
        val rvs = call.receiver.asVar.value.asReferenceValue.allValues
        for (rv <- rvs) rv match {
            case _: IsSArrayValue =>
                handlePreciseCall(ObjectType.Object, callContext, call, pc, calleesAndCallers)

            case ov: IsSObjectValue =>
                if (ov.isPrecise) {
                    handlePreciseCall(
                        ov.theUpperTypeBound, callContext, call, pc, calleesAndCallers
                    )
                } else {
                    doHandleVirtualCall(
                        callContext,
                        call,
                        pc,
                        ov.theUpperTypeBound,
                        isPrecise = false,
                        calleesAndCallers
                    )
                }

            case _: IsMObjectValue =>
                doHandleVirtualCall(
                    callContext,
                    call,
                    pc,
                    call.declaringClass,
                    isPrecise = false,
                    calleesAndCallers
                )

            case _: IsNullValue =>
            // TODO: do not ignore the implicit calls to NullPointerException.<init>
        }
    }

    protected[this] def handlePreciseCall(
        calleeType:        ObjectType,
        callContext:       ContextType,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: DirectCalls
    )(implicit state: CGState[ContextType]): Unit = {
        doHandleVirtualCall(
            callContext,
            call,
            pc,
            calleeType,
            isPrecise = true,
            calleesAndCallers
        )
    }
}

object CallGraphAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, InitialEntryPointsKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callers, Callees, TACAI)

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = {
        p.get(TypeProviderKey).usedPropertyKinds
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callers, Callees)

    /**
     * Updates the caller properties of the initial entry points
     * ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) to be called from an unknown context.
     * This will trigger the computation of the callees for these methods (see `processMethod`).
     */
    override def init(p: SomeProject, ps: PropertyStore): Null = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey).map(declaredMethods.apply)

        if (entryPoints.isEmpty)
            OPALLogger.logOnce(
                Error("project configuration", "the project has no entry points")
            )(p.logContext)

        entryPoints.foreach { ep =>
            ps.preInitialize(ep, Callers.key) {
                case _: EPK[_, _] =>
                    InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub: Callers) =>
                    InterimEUBP(ep, ub.updatedWithUnknownContext())
                case eps =>
                    throw new IllegalStateException(s"unexpected: $eps")
            }
        }

        null
    }

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): CallGraphAnalysis = {
        val analysis = new CallGraphAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def triggeredBy: PropertyKind = Callers

}