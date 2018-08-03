/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.properties.AllTypes
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CalleesImplementation
import org.opalj.fpcf.properties.CallersOnlyWithConcreteCallers
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.LowerBoundCallers
import org.opalj.fpcf.properties.NoCallers
import org.opalj.fpcf.properties.OnlyCallersWithUnknownContext
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

import scala.collection.Set
import scala.collection.immutable.IntMap

case class RTAState(
        private[cg] val method:            DefinedMethod,
        private[cg] val virtualCallSites:  Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)],
        private var _calleesOfM:           IntMap[IntTrieSet], // key = PC
        private[cg] var numTypesProcessed: Int
) {
    private[cg] def addCallEdge(pc: Int, tgtId: Int): Unit = {
        _calleesOfM = calleesOfM.updated(pc, calleesOfM.getOrElse(pc, IntTrieSet.empty) + tgtId)
    }

    private[cg] def calleesOfM: IntMap[IntTrieSet] = _calleesOfM
}

/**
 * An rapid type call graph analysis (RTA). For a given [[Method]] it computes the set of outgoing
 * call edges ([[org.opalj.fpcf.properties.Callees]]). Furthermore, it updates the types for which
 * allocations are present in the [[SomeProject]] ([[org.opalj.fpcf.properties.InstantiatedTypes]])
 * and updates the [[org.opalj.fpcf.properties.CallersProperty]].
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

    private[cg] class CalleesAndCallers {

        private[cg] var calleesOfM: IntMap[IntTrieSet] = IntMap.empty
        private[cg] var partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] = List.empty

        private[cg] def updateWithCall(caller: DefinedMethod, callee: DeclaredMethod, pc: Int): Unit = {
            val calleeId = callee.id
            if (!calleesOfM.contains(pc) || !calleesOfM(pc).contains(calleeId)) {
                calleesOfM = calleesOfM.updated(pc, calleesOfM.getOrElse(pc, IntTrieSet.empty) + calleeId)
                partialResultsForCallers ::= updateCallerProperty(caller, callee, pc)

            }
        }

        private def updateCallerProperty(
            caller: DefinedMethod, callee: DeclaredMethod, pc: Int
        ): PartialResult[DeclaredMethod, CallersProperty] = {
            PartialResult[DeclaredMethod, CallersProperty](callee, CallersProperty.key, {
                case EPS(_, lb, ub) ⇒
                    val newCallers = ub.updated(caller, pc)
                    // here we assert that update returns the identity if there is no change
                    if (ub ne newCallers)
                        Some(EPS(callee, lb, newCallers))
                    else
                        None
                case EPK(_, _) ⇒
                    val set = LongTrieSet(CallersProperty.toLong(caller.id, pc))
                    Some(EPS(
                        callee,
                        CallersProperty.fallback(callee, project),
                        new CallersOnlyWithConcreteCallers(set)
                    ))
                case _ ⇒
                    throw new IllegalArgumentException()
            })
        }
    }
    type V = DUVar[KnownTypedValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Updates the caller properties of the initial entry points ([[InitialEntryPointsKey]]) to be
     * called from an unknown context.
     * This will trigger the computation of the callees for these methods (see `processMethod`).
     */
    def processEntryPoints(p: SomeProject): PropertyComputationResult = {
        val entryPoints = project.get(InitialEntryPointsKey).map(declaredMethods.apply)

        if (entryPoints.isEmpty)
            OPALLogger.logOnce(
                Error("analysis", "the project has no entry points")
            )(project.logContext)

        Results(
            entryPoints.map {
                PartialResult[DeclaredMethod, CallersProperty](_, CallersProperty.key, {
                    case EPK(ep, _) ⇒ Some(EPS(
                        ep,
                        new LowerBoundCallers(project, ep), OnlyCallersWithUnknownContext
                    ))
                    case EPS(ep, lb, ub) ⇒
                        Some(EPS(ep, lb, ub.updateWithUnknownContext()))
                        throw new IllegalStateException("this should not happen")
                    case _ ⇒ None
                })
            }
        )
    }

    /**
     * Computes the calls from the given method ([[Callees]] property) and updates the
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

            case EPK(_, _) ⇒
                throw new IllegalStateException("unexpected state")

            case EPS(_, _, NoCallers) ⇒
                // we can not create a dependency here, so the analysis is not allowed to create
                // such a result
                throw new IllegalStateException("illegal immediate result for callers")

            case _@ EPS(_, _, _) ⇒
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType ne declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // the set of types that are definitely initialized at this point in time
        // in case the instantiatedTypes are not finally computed, we depend on them
        val instantiatedTypesEOptP: SomeEOptionP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case EPS(_, _, ub: InstantiatedTypes) ⇒
                ub.types

            case _ ⇒
                InstantiatedTypes.initialTypes
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
            calleesAndCallers.calleesOfM,
            numTypesProcessed
        )

        // here we can ignore the return value, as the state also gets updated
        handleVirtualCallSites(state, instantiatedTypesUB.iterator, calleesAndCallers)

        var results = resultForCallees(instantiatedTypesEOptP, state) :: calleesAndCallers.partialResultsForCallers
        if (newInstantiatedTypes.nonEmpty)
            results ::= partialResultForInstantiatedTypes(method, newInstantiatedTypes)

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
        var virtualCallSites = List.empty[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = UIDSet.empty[ObjectType]

        val stmts = tacaiProvider(method.definedMethod).stmts

        val packageName = method.definedMethod.classFile.thisType.packageName

        // for allocation sites, add new types
        // for calls, add new edges
        for (stmt ← stmts) {
            stmt match {
                case Assignment(_, _, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                    }

                case ExprStmt(_, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                    }

                case StaticFunctionCallStatement(call) ⇒
                    handleCall(
                        method, stmt.pc, call.resolveCallTarget.toSet, calleesAndCallers
                    )
                    unknownLibraryCall(method, call, packageName, stmt.pc, calleesAndCallers)

                case call: StaticMethodCall[V] ⇒
                    handleCall(
                        method, stmt.pc, call.resolveCallTarget.toSet, calleesAndCallers
                    )
                    unknownLibraryCall(method, call, packageName, stmt.pc, calleesAndCallers)

                case NonVirtualFunctionCallStatement(call) ⇒
                    handleCall(
                        method, stmt.pc, call.resolveCallTarget.toSet, calleesAndCallers
                    )
                    unknownLibraryCall(method, call, packageName, stmt.pc, calleesAndCallers)

                case call: NonVirtualMethodCall[V] ⇒
                    handleCall(
                        method, stmt.pc, call.resolveCallTarget.toSet, calleesAndCallers
                    )
                    unknownLibraryCall(method, call, packageName, stmt.pc, calleesAndCallers)

                case VirtualFunctionCallStatement(call) ⇒
                    virtualCallSites = handleVirtualCall(
                        method, call, call.pc, calleesAndCallers, virtualCallSites
                    )

                case call: VirtualMethodCall[V] ⇒
                    virtualCallSites = handleVirtualCall(
                        method, call, call.pc, calleesAndCallers, virtualCallSites
                    )

                case Assignment(_, _, _: Invokedynamic[V]) | ExprStmt(_, _: Invokedynamic[V]) ⇒
                    OPALLogger.logOnce(
                        Warn(
                            "analysis",
                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                        )
                    )(p.logContext)

                case _ ⇒ //nothing to do
            }
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

        if (!declTgt.hasSingleDefinedMethod && !declTgt.hasMultipleDefinedMethods) {
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
        for (rv ← rvs) {
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
                    handleCall(method, pc, tgt.toSet, calleesAndCallers)
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
                        val tgts = project.instanceCall(
                            thisType, receiverType, call.name, call.descriptor
                        ).toSet
                        handleCall(method, pc, tgts, calleesAndCallers)
                    } else {
                        resVirtualCallSites ::= ((pc, receiverType.asObjectType, call.name, call.descriptor))
                    }
                }
            }
        }

        unknownLibraryCall(
            method,
            call,
            thisType.packageName,
            pc,
            calleesAndCallers
        )

        resVirtualCallSites

    }

    /**
     * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
     * edges for all targets.
     */
    private[this] def handleCall(
        caller:            DefinedMethod,
        pc:                Int,
        targets:           Set[Method],
        calleesAndCallers: CalleesAndCallers
    ): Unit = {
        for {
            tgt ← targets
        } {
            val tgtDM = declaredMethods(tgt)
            // add call edge to CG
            calleesAndCallers.updateWithCall(caller, tgtDM, pc)
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
            sti ← classHierarchy.subtypeInformation(typeBound)
            if instantiatedType == typeBound || sti.contains(instantiatedType)
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
        instantiatedTypesEOptP: SomeEPS,
        state:                  RTAState
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
            resultForCallees(instantiatedTypesEOptP, state) :: calleesAndCallers.partialResultsForCallers
        )

    }

    private[this] def partialResultForInstantiatedTypes(
        method: Method, newInstantiatedTypes: UIDSet[ObjectType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case EPS(_, lb, ub) if newInstantiatedTypes.nonEmpty ⇒
                    Some(EPS(
                        project,
                        lb,
                        ub.updated(newInstantiatedTypes)
                    ))

                case EPK(_, _) ⇒
                    Some(EPS(
                        project,
                        AllTypes,
                        InstantiatedTypes.initial(InstantiatedTypes.initialTypes ++ newInstantiatedTypes)
                    ))

                case _ ⇒ None
            })
    }

    private[this] def resultForCallees(
        instantiatedTypesEOptP: SomeEOptionP, state: RTAState
    ): PropertyComputationResult = {
        val calleesLB = Callees.fallback(state.method, p, declaredMethods)

        // here we need a immutable copy of the current state
        val newCallees = new CalleesImplementation(state.calleesOfM, declaredMethods)
        // todo equal size or equal callees?
        if (state.virtualCallSites.isEmpty || instantiatedTypesEOptP.isFinal || newCallees.size == calleesLB.size)
            Result(state.method, newCallees)
        else {
            IntermediateResult(
                state.method,
                calleesLB,
                newCallees,
                Seq(instantiatedTypesEOptP),
                continuation(_, state)
            )
        }
    }
}

object EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override type InitializationData = RTACallGraphAnalysis

    override def start(project: SomeProject, propertyStore: PropertyStore, rtaAnalysis: RTACallGraphAnalysis): FPCFAnalysis = {
        // let the entry points become reachable
        propertyStore.scheduleEagerComputationForEntity(project)(rtaAnalysis.processEntryPoints)
        rtaAnalysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(
        InstantiatedTypes, CallersProperty, Callees
    )

    override def init(p: SomeProject, ps: PropertyStore): RTACallGraphAnalysis = {
        val analysis = new RTACallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.processMethod)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
