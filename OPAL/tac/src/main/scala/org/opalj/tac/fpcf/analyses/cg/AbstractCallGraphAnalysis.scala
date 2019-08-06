/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.collection.ForeachRefIterator
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
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.TACAI

trait CGState extends TACAIBasedAnalysisState {

    def hasNonFinalCallSite: Boolean
}

/**
 * A base class for call graph analyses based on the FPCF framework.
 * It uses the AI information of the three-address code to get the most precise information for
 * virtual calls.
 * `handleImpreciseCall` will be invoked for each virtual call, that could not be resolved
 * precisely.
 *
 * @see [[org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysis]] or
 *     [[org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysis]] for example analyses.
 *
 * @author Florian Kuebler
 */
trait AbstractCallGraphAnalysis extends ReachableMethodAnalysis {
    type State <: CGState

    def createInitialState(definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]): State

    /**
     * Handles updates for the three-address code.
     * Subclasses that might have other dependencies must override this method and should call
     * `super.c(...)` for updates of other property kinds then the new one.
     *
     * @see [[org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysis.c*]] for an example.
     */
    def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
            state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])

            // we only want to add the new calls, so we create a fresh object
            processMethod(state, new DirectCalls())

        case UBP(_: TACAI) ⇒
            throw new IllegalStateException("there was already a tac defined")
    }

    override final def processMethod(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val state = createInitialState(definedMethod, tacEP)
        processMethod(state, new DirectCalls())
    }

    def handleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: State): Unit

    protected final def processMethod(
        state: State, calls: DirectCalls
    ): ProperPropertyComputationResult = {
        val tac = state.tac

        tac.stmts.foreach {
            case stmt @ StaticFunctionCallStatement(call) ⇒
                handleCall(
                    state.method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget,
                    calls
                )

            case call: StaticMethodCall[V] ⇒
                handleCall(
                    state.method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget,
                    calls
                )

            case stmt @ NonVirtualFunctionCallStatement(call) ⇒
                handleCall(
                    state.method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget(state.method.declaringClassType),
                    calls
                )

            case call: NonVirtualMethodCall[V] ⇒
                handleCall(
                    state.method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget(state.method.declaringClassType),
                    calls
                )

            case VirtualFunctionCallStatement(call) ⇒
                handleVirtualCall(state.method, call, call.pc, calls)(state)

            case call: VirtualMethodCall[V] ⇒
                handleVirtualCall(state.method, call, call.pc, calls)(state)

            case Assignment(_, _, idc: InvokedynamicFunctionCall[V]) ⇒
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) ⇒
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] ⇒
                calls.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case _ ⇒ //nothing to do
        }

        returnResult(calls)(state)
    }

    protected[this] def returnResult(
        calleesAndCallers: DirectCalls
    )(implicit state: State): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

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
        caller:             DefinedMethod,
        callName:           String,
        callDescriptor:     MethodDescriptor,
        callDeclaringClass: ReferenceType,
        pc:                 Int,
        target:             org.opalj.Result[Method],
        calleesAndCallers:  DirectCalls
    ): Unit = {
        if (target.hasValue) {
            val tgtDM = declaredMethods(target.value)
            calleesAndCallers.addCall(caller, tgtDM, pc)
        } else {
            val packageName = caller.definedMethod.classFile.thisType.packageName
            unknownLibraryCall(
                caller,
                callName,
                callDescriptor,
                callDeclaringClass,
                callDeclaringClass,
                packageName,
                pc,
                calleesAndCallers
            )
        }
    }

    protected final def unknownLibraryCall(
        caller:              DefinedMethod,
        callName:            String,
        callDescriptor:      MethodDescriptor,
        callDeclaringClass:  ReferenceType,
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

        if (declTgt.isVirtualOrHasSingleDefinedMethod) {
            calleesAndCallers.addCall(caller, declTgt, pc)
        } else {
            declTgt.definedMethods foreach { m ⇒
                val dm = declaredMethods(m)
                calleesAndCallers.addCall(caller, dm, pc)
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
    /*TODO private[this]*/ def handleVirtualCall(
        caller:            DefinedMethod,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: DirectCalls
    )(implicit state: State): Unit = {
        // TODO: Since Java 11, invokevirtual does also work for private methods, this must be fixed!
        val callerType = caller.definedMethod.classFile.thisType

        val rvs = call.receiver.asVar.value.asReferenceValue.allValues
        for (rv ← rvs) rv match {
            case _: IsSArrayValue ⇒
                val tgtR = project.instanceCall(
                    caller.declaringClassType,
                    ObjectType.Object,
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

            case ov: IsSObjectValue ⇒
                if (ov.isPrecise) {
                    val tgt = project.instanceCall(
                        callerType,
                        rv.leastUpperType.get,
                        call.name,
                        call.descriptor
                    )

                    handleCall(
                        caller,
                        call.name,
                        call.descriptor,
                        call.declaringClass,
                        pc,
                        tgt,
                        calleesAndCallers
                    )
                } else {
                    val potentialTypes = classHierarchy.allSubtypesForeachIterator(
                        ov.theUpperTypeBound, reflexive = true
                    ).filter { subtype ⇒
                            val cfOption = project.classFile(subtype)
                            cfOption.isDefined && {
                                val cf = cfOption.get
                                !cf.isInterfaceDeclaration && !cf.isAbstract
                            }
                        }

                    handleImpreciseCall(
                        caller,
                        call,
                        pc,
                        ov.theUpperTypeBound,
                        potentialTypes,
                        calleesAndCallers
                    )
                }

            case mv: IsMObjectValue ⇒
                val typeBounds = mv.upperTypeBound
                val remainingTypeBounds = typeBounds.tail
                val firstTypeBound = typeBounds.head
                val potentialTypes = ch.allSubtypesForeachIterator(
                    firstTypeBound, reflexive = true
                ).filter { subtype ⇒
                    val cfOption = project.classFile(subtype)
                    cfOption.isDefined && {
                        val cf = cfOption.get
                        !cf.isInterfaceDeclaration && !cf.isAbstract &&
                            remainingTypeBounds.forall { supertype ⇒
                                ch.isSubtypeOf(subtype, supertype)
                            }
                    }
                }

                handleImpreciseCall(
                    caller,
                    call,
                    pc,
                    call.declaringClass,
                    potentialTypes,
                    calleesAndCallers
                )
            case _: IsNullValue ⇒
            // TODO: do not ignore the implicit calls to NullPointerException.<init>
        }
    }
}
