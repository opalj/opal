/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.language.existentials

import scala.collection.mutable

import org.opalj.log.Error
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
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
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
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
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.tac.fpcf.properties.TACAI

class RTAState private (
        private[cg] val method:                       DefinedMethod,
        private[cg] var numTypesProcessed:            Int,
        private[this] val _virtualCallSites:          mutable.LongMap[Set[CallSiteT]],
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
        tacDependee:               Option[EOptionP[Method, TACAI]]                  = _tacDependee,
        tac:                       Option[TACode[TACMethodParameter, V]]            = _tac,
        instantiatedTypesDependee: Option[EOptionP[SomeProject, InstantiatedTypes]] = _instantiatedTypesDependee
    ): RTAState = {
        new RTAState(
            method,
            numTypesProcessed,
            virtualCallSites,
            tacDependee,
            tac,
            instantiatedTypesDependee
        )
    }

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
            if (tacDependee.isFinal) None else Some(tacDependee),
            if (tacDependee.hasUBP) tacDependee.ub.tac else None,
            None
        )
    }
}

/**
 * A rapid type call graph analysis (RTA). For a given [[org.opalj.br.Method]] it computes the set
 * of outgoing call edges ([[org.opalj.br.fpcf.cg.properties.Callees]]). Furthermore, it updates the
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
     * ([[org.opalj.br.fpcf.cg.properties.Callees]] property) and updates the
     * [[org.opalj.br.fpcf.cg.properties.CallersProperty]].
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
            processMethod(state, new DirectCalls())
        else {
            InterimPartialResult(Seq(tacEP), c(state))
        }
    }

    private[this] def processMethod(
        state: RTAState, calleesAndCallers: DirectCalls
    ): ProperPropertyComputationResult = {
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
        implicit val newState: RTAState = state.copy(
            numTypesProcessed = numTypesProcessed,
            instantiatedTypesDependee = instantiatedTypesDependee
        )

        // process each stmt in the current method to compute:
        //  1. newly allocated types
        //  2. methods (+ pc) called by the current method
        //  3. compute the call sites of virtual calls, whose targets are not yet final
        handleStmts(tac, instantiatedTypesUB, calleesAndCallers)

        returnResult(calleesAndCallers)
    }

    def handleStmts(
        tac:                 TACode[TACMethodParameter, V],
        instantiatedTypesUB: UIDSet[ObjectType],
        calleesAndCallers:   DirectCalls
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
                    call.resolveCallTarget,
                    calleesAndCallers
                )

            case call: StaticMethodCall[V] ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget,
                    calleesAndCallers
                )

            case stmt @ NonVirtualFunctionCallStatement(call) ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    stmt.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType),
                    calleesAndCallers
                )

            case call: NonVirtualMethodCall[V] ⇒
                handleCall(
                    method,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    call.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType),
                    calleesAndCallers
                )

            case VirtualFunctionCallStatement(call) ⇒
                handleVirtualCall(
                    method, call, call.pc, instantiatedTypesUB, calleesAndCallers
                )

            case call: VirtualMethodCall[V] ⇒
                handleVirtualCall(
                    method, call, call.pc, instantiatedTypesUB, calleesAndCallers
                )

            case Assignment(_, _, idc: InvokedynamicFunctionCall[V]) ⇒
                calleesAndCallers.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(_, idc: InvokedynamicFunctionCall[V]) ⇒
                calleesAndCallers.addIncompleteCallSite(idc.pc)
                logOnce(
                    Warn("analysis - call graph construction", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] ⇒
                calleesAndCallers.addIncompleteCallSite(idc.pc)
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
    private[this] def handleVirtualCall(
        caller:              DefinedMethod,
        call:                Call[V] with VirtualCall[V],
        pc:                  Int,
        instantiatedTypesUB: UIDSet[ObjectType],
        calleesAndCallers:   DirectCalls
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
                        instantiatedTypesUB,
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
                    instantiatedTypesUB,
                    potentialTypes,
                    calleesAndCallers
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
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
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
                    tgtR,
                    calleesAndCallers
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
                    pc,
                    calleesAndCallers
                )
            } else if (isMethodOverridable(mResult.value).isYesOrUnknown) {
                calleesAndCallers.addIncompleteCallSite(pc)
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

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        newInstantiatedTypes: Iterator[ObjectType], calleesAndCallers: DirectCalls
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
                        tgtR,
                        calleesAndCallers
                    )
                }
            }

            state.removeCallSite(instantiatedType)
        }
    }

    private[this] def returnResult(
        calleesAndCallers: DirectCalls
    )(implicit state: RTAState): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

        if (state.virtualCallSites.isEmpty || !state.hasOpenDependees)
            Results(results)
        else Results(
            InterimPartialResult(state.dependees(), c(state)),
            results
        )
    }

    private[this] def c(state: RTAState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
                state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
                // as the callees and callers creates partial results for both (callees and callers)
                // we should create an empty/new one!
                processMethod(state, new DirectCalls())

            case UBP(_: TACAI) ⇒
                InterimPartialResult(
                    Some(eps), c(state)
                )
            case UBP(ub: InstantiatedTypes) ⇒
                state.updateInstantiatedTypesDependee(
                    eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]]
                )
                val toBeDropped = state.numTypesProcessed
                state.numTypesProcessed = ub.numElements
                val newInstantiatedTypes = ub.getNewTypes(toBeDropped)

                val calleesAndCallers = new DirectCalls()

                // the new edges in the call graph due to the new types
                // as the callees and callers creates partial results for both (callees and callers)
                // we should create an empty/new one!
                handleVirtualCallSites(newInstantiatedTypes, calleesAndCallers)(state)

                returnResult(calleesAndCallers)(state)
        }
    }
}

object RTACallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        InstantiatedTypes,
        Callees,
        CallersProperty,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, CallersProperty)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

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
                case _: EPK[_, _] ⇒
                    InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub: CallersProperty) ⇒
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
