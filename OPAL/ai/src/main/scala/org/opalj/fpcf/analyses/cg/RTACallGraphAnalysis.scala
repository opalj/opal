/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.CallersOnlyWithConcreteCallers
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.fpcf.cg.properties.LowerBoundCallers
import org.opalj.fpcf.cg.properties.AllTypes
import org.opalj.fpcf.cg.properties.StandardInvokeCalleesImplementation
import org.opalj.fpcf.cg.properties.NoStandardInvokeCallees
import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.Invokedynamic
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.KnownTypedValue

import scala.collection.immutable.IntMap

case class RTAState(
        private[cg] val method:            DefinedMethod,
        private[cg] val virtualCallSites:  Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)],
        private[cg] var _callees:          IntMap[IntTrieSet], // key = PC
        private[cg] var incompleteCallsites:          IntTrieSet, // key = PC
        private[cg] var numTypesProcessed: Int
) {
    private[cg] def addCallEdge(pc: Int, targetMethodId: Int): Unit = {
        _callees = _callees.updated(pc, _callees.getOrElse(pc, IntTrieSet.empty) + targetMethodId)
    }

    private[cg] def callees: IntMap[IntTrieSet] = _callees
}

private[cg] class CalleesAndCallers(
        private[this] var _callees:             IntMap[IntTrieSet] = IntMap.empty,
) {
    private[this] var _incompleteCallsites: IntTrieSet         = IntTrieSet.empty

    private[this] var _partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] =
        List.empty

    private[cg] def callees: IntMap[IntTrieSet] = _callees
    private[cg] def partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] = {
        _partialResultsForCallers
    }

    private[cg] def incompleteCallsites: IntTrieSet = _incompleteCallsites
    private[cg] def addIncompleteCallsite(pc: Int): Unit = _incompleteCallsites += pc

    private[cg] def updateWithCall(
        caller: DefinedMethod, callee: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        val calleeId = callee.id
        if (!_callees.contains(pc) || !_callees(pc).contains(calleeId)) {
            _callees = _callees.updated(pc, _callees.getOrElse(pc, IntTrieSet.empty) + calleeId)
            _partialResultsForCallers ::= createPartialResultForCaller(caller, callee, pc)
        }
    }

    def updateWithCallOrFallback(
        caller:             DefinedMethod,
        callee:             org.opalj.Result[Method],
        pc:                 Int,
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            updateWithCall(caller, declaredMethods(callee.value), pc)
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            updateWithCall(caller, fallbackCallee, pc)

        }
    }

    private[this] def createPartialResultForCaller(
        caller: DefinedMethod, callee: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): PartialResult[DeclaredMethod, CallersProperty] = {
        PartialResult[DeclaredMethod, CallersProperty](callee, CallersProperty.key, {
            case EPS(_, lb, ub) ⇒
                val newCallers = ub.updated(caller, pc)
                // here we assert that update returns the identity if there is no change
                if (ub ne newCallers)
                    Some(EPS(callee, lb, newCallers))
                else
                    None
            case _: EPK[_, _] ⇒
                val set = LongTrieSet(CallersProperty.toLong(caller.id, pc))
                Some(EPS(
                    callee,
                    LowerBoundCallers,
                    new CallersOnlyWithConcreteCallers(set)
                ))
        })
    }
}

/**
 * A rapid type call graph analysis (RTA). For a given [[Method]] it computes the set of outgoing
 * call edges ([[org.opalj.fpcf.cg.properties.StandardInvokeCallees]]). Furthermore, it updates the types for which
 * allocations are present in the [[SomeProject]] ([[InstantiatedTypes]])
 * and updates the [[CallersProperty]].
 *
 * This analysis does not handle features such as JVM calls to static initializers, finalize etc.
 * However, analyses for these features (e.g. [[org.opalj.fpcf.analyses.cg.FinalizerAnalysis]] or
 * the [[org.opalj.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the same batch
 * and the call graph will be generated in collaboration)
 *
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    type V = DUVar[KnownTypedValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val isMethodOverridable: Method => Answer = project.get(IsOverridableMethodKey)

    /**
     * Computes the calls from the given method ([[StandardInvokeCallees]] property) and updates the
     * [[CallersProperty]] and the [[InstantiatedTypes]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback [[NoCallers]] will be used by the
     * framework and this method returns [[NoResult]].
     */
    def processMethod(
        declaredMethod: DeclaredMethod
    ): PropertyComputationResult = {

        propertyStore(declaredMethod, CallersProperty.key) match {
            case FinalEP(_, NoCallers) ⇒
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

        // the set of types that are definitely initialized at this point in time
        // in case the instantiatedTypes are not finally computed, we depend on them
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types

            case _              ⇒ InstantiatedTypes.initialTypes
        }

        // process each stmt in the current method to compute:
        //  1. newly allocated types
        //  2. methods (+ pc) called by the current method
        //  3. compute the call sites of virtual calls, whose targets are not yet final
        val (newInstantiatedTypes, calleesAndCallers, virtualCallSites) = handleStmts(
            declaredMethod.asDefinedMethod, instantiatedTypesUB
        )

        // the number of types, already seen by the analysis
        val numTypesProcessed = instantiatedTypesUB.size

        val state = RTAState(
            declaredMethod.asDefinedMethod,
            virtualCallSites,
            calleesAndCallers.callees,
            calleesAndCallers.incompleteCallsites,
            numTypesProcessed
        )

        // here we can ignore the return value, as the state also gets updated
        handleVirtualCallSites(state, instantiatedTypesUB.iterator, calleesAndCallers)

        var results = resultForStandardInvokeCallees(instantiatedTypesEOptP, state) :: calleesAndCallers.partialResultsForCallers
        if (newInstantiatedTypes.nonEmpty)
            results ::= RTACallGraphAnalysis.partialResultForInstantiatedTypes(
                p, newInstantiatedTypes
            )

        Results(results)
    }

    def handleStmts(
        method:              DefinedMethod,
        instantiatedTypesUB: UIDSet[ObjectType]
    // (new instantiated types, callees map, virtual call sites)
    ): (UIDSet[ObjectType], CalleesAndCallers, Traversable[(Int, ObjectType, String, MethodDescriptor)]) = {
        implicit val p: SomeProject = project

        // for each call site in the current method, the set of methods that might called
        val calleesAndCallers = new CalleesAndCallers()

        // the virtual call sites, where we can not determine the precise tgts
        var virtualCallSites = List.empty[(Int, ObjectType, String, MethodDescriptor)]

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = UIDSet.empty[ObjectType]

        val stmts = tacaiProvider(method.definedMethod).stmts

        // for allocation sites, add new types
        // for calls, add new edges
        stmts.foreach {
            case Assignment(_, _, New(_, allocatedType)) ⇒
                if (!instantiatedTypesUB.contains(allocatedType)) {
                    newInstantiatedTypes += allocatedType
                }

            case ExprStmt(_, New(_, allocatedType)) ⇒
                if (!instantiatedTypesUB.contains(allocatedType)) {
                    newInstantiatedTypes += allocatedType
                }

            case stmt @ StaticFunctionCallStatement(call) ⇒
                handleCall(
                    method, call, stmt.pc, call.resolveCallTarget, calleesAndCallers
                )

            case call: StaticMethodCall[V] ⇒
                handleCall(
                    method, call, call.pc, call.resolveCallTarget, calleesAndCallers
                )

            case stmt @ NonVirtualFunctionCallStatement(call) ⇒
                handleCall(
                    method,
                    call,
                    stmt.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType),
                    calleesAndCallers
                )

            case call: NonVirtualMethodCall[V] ⇒
                handleCall(
                    method,
                    call,
                    call.pc,
                    call.resolveCallTarget(method.declaringClassType.asObjectType),
                    calleesAndCallers
                )

            case VirtualFunctionCallStatement(call) ⇒
                virtualCallSites = handleVirtualCall(
                    method, call, call.pc, calleesAndCallers, virtualCallSites
                )

            case call: VirtualMethodCall[V] ⇒
                virtualCallSites = handleVirtualCall(
                    method, call, call.pc, calleesAndCallers, virtualCallSites
                )

            case Assignment(_, _, idc: Invokedynamic[V])=>
                calleesAndCallers.addIncompleteCallsite(idc.pc)
                OPALLogger.logOnce(
                    Warn(
                        "analysis",
                        s"unresolved invokedynamic ignored by call graph construction"
                    )
                )(p.logContext)

            case ExprStmt(_, idc: Invokedynamic[V]) ⇒
                calleesAndCallers.addIncompleteCallsite(idc.pc)
                OPALLogger.logOnce(
                    Warn(
                        "analysis",
                        s"unresolved invokedynamic ignored by call graph construction"
                    )
                )(p.logContext)

            case _ ⇒ //nothing to do
        }

        (newInstantiatedTypes, calleesAndCallers, virtualCallSites)
    }

    private[this] def unknownLibraryCall(
        method:            DefinedMethod,
        call:              Call[V],
        packageName:       String,
        pc:                Int,
        calleesAndCallers: CalleesAndCallers
    ): Unit = {
        val declaringClassType = if (call.declaringClass.isArrayType)
            ObjectType.Object
        else
            call.declaringClass.asObjectType

        val declTgt = declaredMethods.apply(
            declaringClassType,
            packageName,
            declaringClassType,
            call.name,
            call.descriptor
        )

        if(declTgt.hasSingleDefinedMethod) {
            if(isMethodOverridable(declTgt.definedMethod).isNotNo)
                calleesAndCallers.addIncompleteCallsite(pc)
        } else if (!declTgt.hasMultipleDefinedMethods) {
            calleesAndCallers.updateWithCall(method, declTgt, pc)
        }
    }

    /**
     * Computes the calles of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    private[this] def handleVirtualCall(
        method:            DefinedMethod,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: CalleesAndCallers,
        virtualCallSites:  List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]
    ): List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)] = {
        val thisType = method.definedMethod.classFile.thisType

        var resVirtualCallSites = virtualCallSites
        val rvs = call.receiver.asVar.value.asReferenceValue.allValues
        for (rv ← rvs) { //TODO filter duplicates
            // for null there is no call
            if (rv.isNull.isNoOrUnknown) {
                // for precise types we can directly add the call edge here
                if (rv.isPrecise) {
                    val tgt = project.instanceCall(
                        thisType,
                        rv.valueType.get,
                        call.name,
                        call.descriptor
                    )
                    handleCall(method, call, pc, tgt, calleesAndCallers)
                } else {
                    val typeBound =
                        project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                            rv.upperTypeBound
                        )
                    val receiverType =
                        if (project.classHierarchy.isSubtypeOf(typeBound, call.declaringClass))
                            typeBound
                        else
                            call.declaringClass

                    if (receiverType.isArrayType) {
                        val tgt = project.instanceCall(
                            thisType, receiverType, call.name, call.descriptor
                        )
                        handleCall(method, call, pc, tgt, calleesAndCallers)
                    } else {
                        val receiverObjectType = receiverType.asObjectType
                        resVirtualCallSites ::= ((pc, receiverObjectType, call.name, call.descriptor))

                        unknownLibraryCall(
                            method,
                            call,
                            thisType.packageName,
                            pc,
                            calleesAndCallers
                        )
                    }
                }
            }
        }

        resVirtualCallSites

    }

    /**
     * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
     * edges for all targets.
     */
    private[this] def handleCall(
        caller:            DefinedMethod,
        call:              Call[V],
        pc:                Int,
        target:            org.opalj.Result[Method],
        calleesAndCallers: CalleesAndCallers
    ): Unit = {
        if (target.hasValue) {
            val tgtDM = declaredMethods(target.value)
            // add call edge to CG
            calleesAndCallers.updateWithCall(caller, tgtDM, pc)
        } else {
            val packageName = caller.definedMethod.classFile.thisType.packageName
            unknownLibraryCall(caller, call, packageName, pc, calleesAndCallers)
        }
    }

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        state:                RTAState,
        newInstantiatedTypes: Iterator[ObjectType],
        calleesAndCallers:    CalleesAndCallers
    ): Unit = {
        for {
            instantiatedType ← newInstantiatedTypes // only iterate once!
            (pc, typeBound, name, descr) ← state.virtualCallSites
            if classHierarchy.isSubtypeOf(instantiatedType, typeBound)
            tgt ← project.instanceCall(
                state.method.definedMethod.classFile.thisType, instantiatedType, name, descr
            )
        } {
            val tgtDM = declaredMethods(tgt)
            calleesAndCallers.updateWithCall(state.method, tgtDM, pc)
            state.addCallEdge(pc, tgtDM.id)

        }
    }

    private[this] def continuation(
        state: RTAState
    )(
        instantiatedTypesEOptP: SomeEPS
    ): PropertyComputationResult = {
        // find the new types, that should be processed
        val newInstantiatedTypes = instantiatedTypesEOptP match {
            case EPS(_, _, ub: InstantiatedTypes) ⇒
                val toBeDropped = state.numTypesProcessed
                state.numTypesProcessed = ub.numElements
                ub.getNewTypes(toBeDropped)
            case _ ⇒ Iterator.empty // the initial types are already processed
        }

        val calleesAndCallers = new CalleesAndCallers

        // the new edges in the call graph due to the new types
        handleVirtualCallSites(
            state, newInstantiatedTypes, calleesAndCallers
        )

        Results(
            resultForStandardInvokeCallees(instantiatedTypesEOptP, state) :: calleesAndCallers.partialResultsForCallers
        )

    }

    private[this] def resultForStandardInvokeCallees(
        instantiatedTypesEOptP: SomeEOptionP, state: RTAState
    ): PropertyComputationResult = {

        // here we need a immutable copy of the current state
        val newCallees =
            if (state.callees.isEmpty)
                NoStandardInvokeCallees
            else
                new StandardInvokeCalleesImplementation(state.callees, state.incompleteCallsites)

        if (state.virtualCallSites.isEmpty || instantiatedTypesEOptP.isFinal)
            Result(state.method, newCallees)
        else {
            SimplePIntermediateResult(
                state.method,
                newCallees,
                Seq(instantiatedTypesEOptP),
                continuation(state)
            )
        }
    }
}

object RTACallGraphAnalysis {
    def partialResultForInstantiatedTypes(
        p:                    SomeProject,
        newInstantiatedTypes: UIDSet[ObjectType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case EPS(_, lb, ub) ⇒
                    Some(EPS(
                        p,
                        lb,
                        ub.updated(newInstantiatedTypes)
                    ))

                case _ ⇒
                    Some(EPS(
                        p,
                        AllTypes,
                        InstantiatedTypes.initial(newInstantiatedTypes)
                    ))
            })
    }
}

object EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override type InitializationData = RTACallGraphAnalysis

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes)

    override def derives: Set[PropertyKind] = Set(
        InstantiatedTypes, CallersProperty, StandardInvokeCallees
    )

    override def init(p: SomeProject, ps: PropertyStore): RTACallGraphAnalysis = {
        val analysis = new RTACallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.processMethod)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

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
            ps.handleResult(
                PartialResult[DeclaredMethod, CallersProperty](ep, CallersProperty.key, {
                    case EPK(_, _) ⇒ Some(EPS(
                        ep,
                        LowerBoundCallers,
                        OnlyCallersWithUnknownContext
                    ))
                    case EPS(_, lb, ub) ⇒
                        Some(EPS(ep, lb, ub.updatedWithUnknownContext()))
                    case _ ⇒ None
                })
            )
        }

    }

    override def start(
        project: SomeProject, propertyStore: PropertyStore, rtaAnalysis: RTACallGraphAnalysis
    ): FPCFAnalysis = {
        // let the entry points become reachable
        processEntryPoints(project, propertyStore)
        rtaAnalysis
    }

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}
