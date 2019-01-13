/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.language.existentials

import scala.collection.immutable.IntMap
import scala.collection.mutable

import org.opalj.log.Error
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.EOptionP
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.cg.properties.NoStandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCalleesImplementation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.Result
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsMObjectValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.IsSObjectValue
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI

class RTAState private (
        private[cg] val method:                       DefinedMethod,
        private[cg] var numTypesProcessed:            Int,
        private[this] val _virtualCallSites:          mutable.LongMap[Set[CallSiteT]],
        private[this] var _calleesAndCallers:         CalleesAndCallers,
        private[this] var _tacDependee:               Option[EOptionP[Method, TACAI]],
        private[this] var _tac:                       Option[TACode[TACMethodParameter, V]],
        private[this] var _instantiatedTypesDependee: Option[EOptionP[SomeProject, InstantiatedTypes]]
) {

    assert(_tacDependee.isEmpty || _tacDependee.get.isRefinable)
    assert(_instantiatedTypesDependee.isEmpty || _instantiatedTypesDependee.get.isRefinable)

    private[cg] def copy(
        method:                    DefinedMethod                                    = this.method,
        numTypesProcessed:         Int                                              = this.numTypesProcessed,
        virtualCallSites:          mutable.LongMap[Set[CallSiteT]]                  = _virtualCallSites,
        calleesAndCallers:         CalleesAndCallers                                = _calleesAndCallers,
        tacDependee:               Option[EOptionP[Method, TACAI]]                  = _tacDependee,
        tac:                       Option[TACode[TACMethodParameter, V]]            = _tac,
        instantiatedTypesDependee: Option[EOptionP[SomeProject, InstantiatedTypes]] = _instantiatedTypesDependee
    ): RTAState = {
        new RTAState(
            method,
            numTypesProcessed,
            virtualCallSites,
            calleesAndCallers,
            tacDependee,
            tac,
            instantiatedTypesDependee
        )
    }

    private[cg] def addCallEdge(pc: Int, targetMethod: DeclaredMethod): Unit = {
        _calleesAndCallers.updateWithCall(method, targetMethod, pc)
    }

    private[cg] def callees: IntMap[IntTrieSet] = _calleesAndCallers.callees

    private[cg] def addIncompleteCallSite(pc: Int): Unit = {
        _calleesAndCallers.addIncompleteCallsite(pc)
    }

    private[cg] def partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] = {
        _calleesAndCallers.partialResultsForCallers
    }

    private[cg] def clearPartialResultsForCallers(): Unit = {
        _calleesAndCallers.clearPartialResultsForCallers()
    }

    private[cg] def incompleteCallSites: IntTrieSet = _calleesAndCallers.incompleteCallsites

    private[cg] def removeTACDependee(): Unit = _tacDependee = None

    private[cg] def addTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        assert(_tacDependee.isEmpty)

        if (tacDependee.isRefinable) {
            _tacDependee = Some(tacDependee)
        }

        if (tacDependee.hasUBP) {
            _tac = tacDependee.ub.tac
        }
    }

    private[cg] def updateTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        removeTACDependee()
        addTACDependee(tacDependee)
    }

    private[cg] def tacDependee(): Option[EOptionP[Method, TACAI]] = {
        _tacDependee
    }

    private[cg] def removeInstantiatedTypesDependee(): Unit = _instantiatedTypesDependee = None

    private[cg] def addInstantiatedTypesDependee(
        instantiatedTypesDependee: EOptionP[SomeProject, InstantiatedTypes]
    ): Unit = {
        assert(_instantiatedTypesDependee.isEmpty)
        if (instantiatedTypesDependee.isRefinable)
            _instantiatedTypesDependee = Some(instantiatedTypesDependee)
    }

    private[cg] def updateInstantiatedTypesDependee(
        instantiatedTypesDependee: EOptionP[SomeProject, InstantiatedTypes]
    ): Unit = {
        removeInstantiatedTypesDependee()
        addInstantiatedTypesDependee(instantiatedTypesDependee)
    }

    private[cg] def instantiatedTypesDependee(): Option[EOptionP[SomeProject, InstantiatedTypes]] = {
        _instantiatedTypesDependee
    }

    private[cg] def hasOpenDependees: Boolean = {
        _tacDependee.isDefined || _instantiatedTypesDependee.isDefined
    }

    private[cg] def dependees(): Iterable[EOptionP[Entity, Property]] = {
        _tacDependee ++ _instantiatedTypesDependee
    }

    private[cg] def tac(): Option[TACode[TACMethodParameter, V]] = _tac

    private[cg] def virtualCallSites: mutable.LongMap[Set[CallSiteT]] = {
        _virtualCallSites
    }

    private[cg] def addVirtualCallSite(objectType: ObjectType, callSite: CallSiteT): Unit = {
        val oldVal = _virtualCallSites.getOrElse(objectType.id.toLong, Set.empty)
        _virtualCallSites.update(objectType.id.toLong, oldVal + callSite)
    }

    private[cg] def removeCallSite(instantiatedType: ObjectType): Unit = {
        _virtualCallSites -= instantiatedType.id.toLong
    }
}

object RTAState {
    def apply(method: DefinedMethod, tacDependee: EOptionP[Method, TACAI]): RTAState = {
        new RTAState(
            method,
            numTypesProcessed = 0,
            _virtualCallSites = new mutable.LongMap[Set[CallSiteT]](),
            _calleesAndCallers = new CalleesAndCallers(),
            if (tacDependee.isFinal) None else Some(tacDependee),
            if (tacDependee.hasUBP) tacDependee.ub.tac else None,
            None
        )
    }
}

/**
 * A rapid type call graph analysis (RTA). For a given [[org.opalj.br.Method]] it computes the set
 * of outgoing call edges ([[org.opalj.br.fpcf.cg.properties.StandardInvokeCallees]]). Furthermore, it
 * updates the types for which allocations are present in the [[org.opalj.br.analyses.SomeProject]]
 * ([[org.opalj.br.fpcf.cg.properties.InstantiatedTypes]]) and updates the
 * [[org.opalj.br.fpcf.cg.properties.CallersProperty]].
 *
 * This analysis does not handle features such as JVM calls to static initializers or finalize
 * calls.
 * However, analyses for these features (e.g. [[org.opalj.tac.fpcf.analyses.cg.FinalizerAnalysis]]
 * or the [[org.opalj.tac.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the
 * same batch and the call graph will be generated in collaboration.
 *
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    /**
     * Computes the calls from the given method
     * ([[org.opalj.br.fpcf.cg.properties.StandardInvokeCallees]] property) and updates the
     * [[org.opalj.br.fpcf.cg.properties.CallersProperty]] and the
     * [[org.opalj.br.fpcf.cg.properties.InstantiatedTypes]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback
     * [[org.opalj.br.fpcf.cg.properties.NoCallers]] will be used by the framework and this method
     * returns [[org.opalj.fpcf.NoResult]].
     */
    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        (propertyStore(declaredMethod, CallersProperty.key): @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
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
            // happens in particular for native methods
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)

        val state = RTAState(declaredMethod.asDefinedMethod, tacEP)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined)
            processMethod(state)
        else {
            InterimResult.forUB(declaredMethod, NoStandardInvokeCallees, Seq(tacEP), c(state))
        }
    }

    private[this] def processMethod(state: RTAState): ProperPropertyComputationResult = {
        val tac = state.tac().get

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] =
            if (instantiatedTypesEOptP.hasUBP)
                instantiatedTypesEOptP.ub.types
            else
                UIDSet.empty

        val instantiatedTypesDependee =
            if (instantiatedTypesEOptP.isFinal) None else Some(instantiatedTypesEOptP)

        // the number of types, already seen by the analysis
        val numTypesProcessed = instantiatedTypesUB.size
        implicit val newState = state.copy(
            numTypesProcessed = numTypesProcessed,
            instantiatedTypesDependee = instantiatedTypesDependee
        )

        // process each stmt in the current method to compute:
        //  1. newly allocated types
        //  2. methods (+ pc) called by the current method
        //  3. compute the call sites of virtual calls, whose targets are not yet final
        handleStmts(tac, instantiatedTypesUB)

        returnResult
    }

    def handleStmts(
        tac:                 TACode[TACMethodParameter, V],
        instantiatedTypesUB: UIDSet[ObjectType]
    // (callees map, virtual call sites)
    )(
        implicit
        state: RTAState
    ): Unit = {
        val method = state.method

        // for allocation sites, add new types
        // for calls, add new edges
        tac.stmts.foreach {
            case stmt @ StaticFunctionCallStatement(call) ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget
                )

            case call: StaticMethodCall[V] ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget
                )

            case stmt @ NonVirtualFunctionCallStatement(call) ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType)
                )

            case call: NonVirtualMethodCall[V] ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType)
                )

            case VirtualFunctionCallStatement(call) ⇒
                handleVirtualCall(
                    method, call, call.pc, instantiatedTypesUB
                )

            case call: VirtualMethodCall[V] ⇒
                handleVirtualCall(
                    method, call, call.pc, instantiatedTypesUB
                )

            case Assignment(_, _, idc: InvokedynamicFunctionCall[V]) ⇒
                state.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) ⇒
                state.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case idc @ InvokedynamicMethodCall(pc, _, _, _, _) ⇒
                state.addIncompleteCallSite(pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case _ ⇒ //nothing to do
        }
    }

    private[this] def unknownLibraryCall(
        caller:              DefinedMethod,
        callName:            String,
        callDescriptor:      MethodDescriptor,
        callDeclaringClass:  ReferenceType,
        runtimeReceiverType: ReferenceType,
        packageName:         String,
        pc:                  Int
    )(implicit state: RTAState): Unit = {
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
            state.addCallEdge(pc, declTgt)
        } else {
            declTgt.definedMethods foreach { m ⇒
                val dm = declaredMethods(m)
                state.addCallEdge(pc, dm)
            }
        }

        state.addIncompleteCallSite(pc)
    }

    /**
     * Computes the calles of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    private[this] def handleVirtualCall(
        caller:              DefinedMethod,
        call:                Call[V] with VirtualCall[V],
        pc:                  Int,
        instantiatedTypesUB: UIDSet[ObjectType]
    )(implicit state: RTAState): Unit = {
        val callerType = caller.definedMethod.classFile.thisType

        val rvs = call.receiver.asVar.value.asReferenceValue.allValues
        for (rv ← rvs) rv match {
            case _: IsSArrayValue ⇒
                val tgtR = project.instanceCall(
                    caller.declaringClassType.asObjectType,
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
                    tgtR
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
                        tgt
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
                        instantiatedTypesUB,
                        potentialTypes
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
                    instantiatedTypesUB,
                    potentialTypes
                )
            case _: IsNullValue ⇒
            // for now, we ignore the implicit calls to NullPointerException.<init>
        }
    }

    private[this] def handleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        instantiatedTypesUB:           UIDSet[ObjectType],
        potentialTargets:              ForeachRefIterator[ObjectType]
    )(implicit state: RTAState): Unit = {
        for (possibleTgtType ← potentialTargets) {
            if (instantiatedTypesUB.contains(possibleTgtType)) {
                val tgtR = project.instanceCall(
                    caller.declaringClassType.asObjectType,
                    possibleTgtType,
                    call.name,
                    call.descriptor
                )

                handleCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    pc,
                    tgtR
                )
            } else {
                state.addVirtualCallSite(
                    possibleTgtType, (pc, call.name, call.descriptor, call.declaringClass)
                )
            }
        }

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
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    declType,
                    caller.definedMethod.classFile.thisType.packageName,
                    pc
                )
            } else if (isMethodOverridable(mResult.value).isYesOrUnknown) {
                state.addIncompleteCallSite(pc)
            }
        }
    }

    /**
     * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
     * edges for all targets.
     */
    private[this] def handleCall(
        caller:             DefinedMethod,
        callName:           String,
        callDescriptor:     MethodDescriptor,
        callDeclaringClass: ReferenceType,
        pc:                 Int,
        target:             org.opalj.Result[Method]
    )(
        implicit
        state: RTAState
    ): Unit = {
        if (target.hasValue) {
            val tgtDM = declaredMethods(target.value)
            state.addCallEdge(pc, tgtDM)
        } else {
            val packageName = caller.definedMethod.classFile.thisType.packageName
            unknownLibraryCall(
                caller,
                callName,
                callDescriptor,
                callDeclaringClass,
                callDeclaringClass,
                packageName,
                pc
            )
        }
    }

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        newInstantiatedTypes: Iterator[ObjectType]
    )(implicit state: RTAState): Unit = {
        newInstantiatedTypes.foreach { instantiatedType ⇒
            val callSitesOpt = state.virtualCallSites.get(instantiatedType.id.toLong)
            if (callSitesOpt.isDefined) {
                callSitesOpt.get.foreach { callSite ⇒
                    val (pc, name, descr, declaringClass) = callSite
                    val tgtR = project.instanceCall(
                        state.method.definedMethod.classFile.thisType,
                        instantiatedType,
                        name,
                        descr
                    )

                    handleCall(
                        state.method,
                        name,
                        descr,
                        declaringClass,
                        pc,
                        tgtR
                    )
                }
            }

            state.removeCallSite(instantiatedType)
        }
    }

    private[this] def returnResult(implicit state: RTAState): ProperPropertyComputationResult = {
        val results = Results(
            resultForStandardInvokeCallees(state) :: state.partialResultsForCallers
        )
        state.clearPartialResultsForCallers()
        results
    }

    private[this] def c(state: RTAState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
                state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
                processMethod(state)

            case UBP(_: TACAI) ⇒
                InterimResult.forUB(
                    state.method,
                    NoStandardInvokeCallees,
                    Seq(eps),
                    c(state)
                )
            case UBP(ub: InstantiatedTypes) ⇒
                state.updateInstantiatedTypesDependee(
                    eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]]
                )
                val toBeDropped = state.numTypesProcessed
                state.numTypesProcessed = ub.numElements
                val newInstantiatedTypes = ub.getNewTypes(toBeDropped)

                // the new edges in the call graph due to the new types
                handleVirtualCallSites(newInstantiatedTypes)(state)

                returnResult(state)
        }
    }

    private[this] def resultForStandardInvokeCallees(
        state: RTAState
    ): ProperPropertyComputationResult = {

        // here we need a immutable copy of the current state
        val newCallees =
            if (state.callees.isEmpty && state.incompleteCallSites.isEmpty)
                NoStandardInvokeCallees
            else
                new StandardInvokeCalleesImplementation(state.callees, state.incompleteCallSites)

        if (state.virtualCallSites.isEmpty || !state.hasOpenDependees) {
            Result(state.method, newCallees)
        } else {
            InterimResult.forUB(
                state.method,
                newCallees,
                state.dependees(),
                c(state)
            )
        }
    }
}

object RTACallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        InstantiatedTypes,
        CallersProperty,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(CallersProperty)

    override def derivesEagerly: Set[PropertyBounds] = PropertyBounds.ubs(StandardInvokeCallees)

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
            ps.preInitialize(ep, CallersProperty.key) {
                case _: EPK[_, _]   ⇒ InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub) ⇒ InterimEUBP(ep, ub.updatedWithUnknownContext())
                case r              ⇒ throw new IllegalStateException(s"unexpected eps $r")
            }
        }
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        processEntryPoints(p, ps)
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): RTACallGraphAnalysis = {
        val analysis = new RTACallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
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
