/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials

import scala.collection.immutable.IntMap

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.value.KnownTypedValue
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.NoStandardInvokeCallees
import org.opalj.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCalleesImplementation
import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.InvokedynamicFunctionCall
import org.opalj.tac.InvokedynamicMethodCall
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.properties.TACAI

import org.opalj.fpcf.cg.properties.LoadedClasses
import org.opalj.br.ReferenceType

class RTAState private (
        private[cg] val method:                       DefinedMethod,
        private[cg] val virtualCallSites:             Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)],
        private[cg] var incompleteCallSites:          IntTrieSet, // key = PC
        private[cg] var numTypesProcessed:            Int,
        private[this] var _callees:                   IntMap[IntTrieSet], // key = PC
        private[this] var _tacDependee:               Option[EOptionP[Method, TACAI]],
        private[this] var _tac:                       Option[TACode[TACMethodParameter, V]],
        private[this] var _instantiatedTypesDependee: Option[EOptionP[SomeProject, InstantiatedTypes]]
) {
    assert(_tacDependee.isEmpty || _tacDependee.get.isRefinable)
    assert(_instantiatedTypesDependee.isEmpty || _instantiatedTypesDependee.get.isRefinable)

    private[cg] def copy(
        method:                    DefinedMethod                                                    = this.method,
        virtualCallSites:          Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)] = this.virtualCallSites,
        incompleteCallSites:       IntTrieSet                                                       = this.incompleteCallSites, // key = PC
        numTypesProcessed:         Int                                                              = this.numTypesProcessed,
        callees:                   IntMap[IntTrieSet]                                               = _callees, // key = PC
        tacDependee:               Option[EOptionP[Method, TACAI]]                                  = _tacDependee,
        tac:                       Option[TACode[TACMethodParameter, V]]                            = _tac,
        instantiatedTypesDependee: Option[EOptionP[SomeProject, InstantiatedTypes]]                 = _instantiatedTypesDependee
    ): RTAState = {
        new RTAState(
            method,
            virtualCallSites,
            incompleteCallSites,
            numTypesProcessed,
            callees,
            tacDependee,
            tac,
            instantiatedTypesDependee
        )
    }

    private[cg] def addCallEdge(pc: Int, targetMethodId: Int): Unit = {
        _callees = _callees.updated(pc, _callees.getOrElse(pc, IntTrieSet.empty) + targetMethodId)
    }

    private[cg] def callees: IntMap[IntTrieSet] = _callees

    private[cg] def removeTACDependee(): Unit = _tacDependee = None

    private[cg] def addTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        assert(_tacDependee.isEmpty)

        if (tacDependee.isRefinable) {
            _tacDependee = Some(tacDependee)
        }

        if (tacDependee.hasProperty) {
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

    private[cg] def hasOpenDependees(): Boolean = {
        _tacDependee.isDefined || _instantiatedTypesDependee.isDefined
    }

    private[cg] def dependees(): Iterable[EOptionP[Entity, Property]] = {
        _tacDependee ++ _instantiatedTypesDependee
    }

    private[cg] def tac(): Option[TACode[TACMethodParameter, V]] = _tac
}

object RTAState {
    def apply(method: DefinedMethod, tacDependee: EOptionP[Method, TACAI]): RTAState = {
        new RTAState(
            method,
            virtualCallSites = Traversable.empty,
            incompleteCallSites = IntTrieSet.empty,
            numTypesProcessed = 0,
            _callees = IntMap.empty,
            if (tacDependee.isFinal) None else Some(tacDependee),
            if (tacDependee.hasProperty) tacDependee.ub.tac else None,
            None
        )
    }
    def apply(
        method:                    DefinedMethod,
        virtualCallSites:          Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)],
        incompleteCallSites:       IntTrieSet, // key = PC
        numTypesProcessed:         Int,
        callees:                   IntMap[IntTrieSet], // key = PC
        tacDependee:               Option[EOptionP[Method, TACAI]],
        tac:                       Option[TACode[TACMethodParameter, V]],
        instantiatedTypesDependee: EOptionP[SomeProject, InstantiatedTypes]
    ): RTAState = {
        new RTAState(
            method,
            virtualCallSites,
            incompleteCallSites,
            numTypesProcessed,
            callees,
            tacDependee,
            tac,
            if (instantiatedTypesDependee.isFinal) None else Some(instantiatedTypesDependee)
        )
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

    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)
    private[this] val initialInstantiatedTypes: UIDSet[ObjectType] =
        UIDSet(project.get(InitialInstantiatedTypesKey).toSeq: _*)

    /**
     * Computes the calls from the given method ([[StandardInvokeCallees]] property) and updates the
     * [[CallersProperty]] and the [[InstantiatedTypes]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback [[NoCallers]] will be used by the
     * framework and this method returns [[NoResult]].
     */
    def analyze(
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

        if (method.isNative)
            return handleNativeMethod(declaredMethod, method);

        if (method.body.isEmpty)
            // happens in particular for native methods TODO does it happen for non-native methods?
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)

        val state = RTAState(declaredMethod.asDefinedMethod, tacEP)

        if (tacEP.hasProperty)
            processMethod(state)
        else {
            SimplePIntermediateResult(
                declaredMethod,
                NoStandardInvokeCallees,
                Seq(tacEP),
                continuation(state)
            )
        }
    }

    private[this] def processMethod(
        state: RTAState
    ): PropertyComputationResult = {
        assert(state.tac().isDefined)
        val tac = state.tac().get

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = getInstantiatedTypesUB(instantiatedTypesEOptP)

        val instantiatedTypesDependee =
            if (instantiatedTypesEOptP.isFinal) None else Some(instantiatedTypesEOptP)

        // process each stmt in the current method to compute:
        //  1. newly allocated types
        //  2. methods (+ pc) called by the current method
        //  3. compute the call sites of virtual calls, whose targets are not yet final
        val (newInstantiatedTypes, calleesAndCallers, virtualCallSites) = handleStmts(
            state.method, tac, instantiatedTypesUB
        )

        // the number of types, already seen by the analysis
        val numTypesProcessed = instantiatedTypesUB.size

        val newState = state.copy(
            virtualCallSites = virtualCallSites,
            incompleteCallSites = calleesAndCallers.incompleteCallsites,
            numTypesProcessed = numTypesProcessed,
            callees = calleesAndCallers.callees,
            instantiatedTypesDependee = instantiatedTypesDependee

        )

        // here we can ignore the return value, as the state also gets updated
        handleVirtualCallSites(newState, instantiatedTypesUB.iterator, calleesAndCallers)

        var results = resultForStandardInvokeCallees(newState) :: calleesAndCallers.partialResultsForCallers
        if (newInstantiatedTypes.nonEmpty)
            results ::= RTACallGraphAnalysis.partialResultForInstantiatedTypes(
                p, newInstantiatedTypes, initialInstantiatedTypes
            )

        Results(results)
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ObjectType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ initialInstantiatedTypes
        }
    }

    def handleStmts(
        method:              DefinedMethod,
        tac:                 TACode[TACMethodParameter, V],
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

        // for allocation sites, add new types
        // for calls, add new edges
        tac.stmts.foreach {
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

            case InvokedynamicMethodCall(pc, _, _, _, _) ⇒
                calleesAndCallers.addIncompleteCallsite(pc)
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
        method:              DefinedMethod,
        call:                Call[V],
        runtimeReceiverType: ReferenceType,
        packageName:         String,
        pc:                  Int,
        calleesAndCallers:   CalleesAndCallers
    ): Unit = {
        val declaringClassType = if (call.declaringClass.isArrayType)
            ObjectType.Object
        else
            call.declaringClass.asObjectType

        val runtimeType = if (runtimeReceiverType.isArrayType)
            ObjectType.Object
        else
            runtimeReceiverType.asObjectType

        val declTgt = declaredMethods.apply(
            declaringClassType,
            packageName,
            runtimeType,
            call.name,
            call.descriptor
        )

        if (declTgt.hasSingleDefinedMethod) {
            if (isMethodOverridable(declTgt.definedMethod).isNotNo)
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
        caller:            DefinedMethod,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: CalleesAndCallers,
        virtualCallSites:  List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]
    ): List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)] = {
        val callerType = caller.definedMethod.classFile.thisType

        var resVirtualCallSites = virtualCallSites
        val rvs = call.receiver.asVar.value.asReferenceValue.allValues
        for (rv ← rvs) { //TODO filter duplicates
            // for null there is no call
            if (rv.isNull.isNoOrUnknown) {
                // for precise types we can directly add the call edge here
                if (rv.isPrecise) {
                    val tgt = project.instanceCall(
                        callerType,
                        rv.valueType.get,
                        call.name,
                        call.descriptor
                    )
                    handleCall(caller, call, pc, tgt, calleesAndCallers)
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
                            callerType, receiverType, call.name, call.descriptor
                        )
                        handleCall(caller, call, pc, tgt, calleesAndCallers)
                    } else {
                        val receiverObjectType = receiverType.asObjectType
                        resVirtualCallSites ::= ((pc, receiverObjectType, call.name, call.descriptor))

                        unknownLibraryCall(
                            caller,
                            call,
                            receiverObjectType,
                            callerType.packageName,
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
            unknownLibraryCall(caller, call, call.declaringClass, packageName, pc, calleesAndCallers)
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
        eps: SomeEPS
    ): PropertyComputationResult = {
        eps match {
            case ESimplePS(_, _: TACAI, _) ⇒
                state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
                processMethod(state)

            case ESimplePS(_, ub: InstantiatedTypes, _) ⇒
                state.updateInstantiatedTypesDependee(
                    eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]]
                )
                val toBeDropped = state.numTypesProcessed
                state.numTypesProcessed = ub.numElements
                val newInstantiatedTypes = ub.getNewTypes(toBeDropped)

                val calleesAndCallers = new CalleesAndCallers()

                // the new edges in the call graph due to the new types
                handleVirtualCallSites(
                    state, newInstantiatedTypes, calleesAndCallers
                )

                Results(
                    resultForStandardInvokeCallees(state) :: calleesAndCallers.partialResultsForCallers
                )

        }
    }

    private[this] def resultForStandardInvokeCallees(
        state: RTAState
    ): PropertyComputationResult = {

        // here we need a immutable copy of the current state
        val newCallees =
            if (state.callees.isEmpty)
                NoStandardInvokeCallees
            else
                new StandardInvokeCalleesImplementation(state.callees, state.incompleteCallSites)

        if (state.virtualCallSites.isEmpty || !state.hasOpenDependees())
            Result(state.method, newCallees)
        else {
            SimplePIntermediateResult(
                state.method,
                newCallees,
                state.dependees(),
                continuation(state)
            )
        }
    }

    private case class ImplicitAction(
        cf:                String,
        m:                 String,
        desc:              String,
        instantiatedTypes: Option[Seq[String]],
        reachableMethods:  Option[Seq[ReachableMethod]]
    )
    private case class ReachableMethod(cf: String, m: String, desc: String)

    private val actions: Map[(String, String, String), (Option[Seq[String]], Option[Seq[ReachableMethod]])] =
        project.config.as[Iterator[ImplicitAction]](
            "org.opalj.fpcf.analysis.RTACallGraphAnalysis.implicitActions"
        ).map { action ⇒
                (action.cf, action.m, action.desc) →
                    ((action.instantiatedTypes, action.reachableMethods))
            }.toMap

    def handleNativeMethod(
        declaredMethod: DeclaredMethod,
        m:              Method
    ): PropertyComputationResult = {
        val actionsO =
            actions.get((m.classFile.thisType.fqn, m.name, m.descriptor.toJVMDescriptor))

        if (actionsO.isEmpty)
            return NoResult;

        val (instantiatedTypesO, reachableMethodsO) = actionsO.get

        val instantiatedTypesResults = instantiatedTypesO.map { fqns ⇒
            val instantiatedTypesUB =
                getInstantiatedTypesUB(propertyStore(project, InstantiatedTypes.key))

            val newInstantiatedTypes =
                UIDSet(fqns.map(ObjectType(_)).filterNot(instantiatedTypesUB.contains): _*)

            val instantiatedTypesResultList =
                List(RTACallGraphAnalysis.partialResultForInstantiatedTypes(
                    p, newInstantiatedTypes, initialInstantiatedTypes
                ))

            val loadedClassesUB = propertyStore(project, LoadedClasses.key) match {
                case eps: EPS[_, _] ⇒ eps.ub.classes
                case _              ⇒ UIDSet.empty[ObjectType]
            }

            val newLoadedClasses = newInstantiatedTypes.filterNot(loadedClassesUB.contains)

            val loadedClassesResult =
                PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                    case IntermediateESimpleP(p, ub) ⇒
                        val newUb = ub.classes ++ newLoadedClasses
                        // due to monotonicity:
                        // the size check sufficiently replaces the subset check
                        if (newUb.size > ub.classes.size)
                            Some(IntermediateESimpleP(p, new LoadedClasses(newUb)))
                        else
                            None

                    case EPK(p, _) ⇒
                        Some(IntermediateESimpleP(p, new LoadedClasses(newLoadedClasses)))

                    case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
                })

            if (newInstantiatedTypes.isEmpty) List.empty
            else if (newLoadedClasses.isEmpty) instantiatedTypesResultList
            else loadedClassesResult :: instantiatedTypesResultList
        }.getOrElse(List.empty)

        val callResults = reachableMethodsO.map { reachableMethods ⇒
            val calleesAndCallers = new CalleesAndCallers()
            for (reachableMethod ← reachableMethods.iterator) {
                val classType = ObjectType(reachableMethod.cf)
                val name = reachableMethod.m
                val descriptor = MethodDescriptor(reachableMethod.desc)
                val callee =
                    declaredMethods(classType, classType.packageName, classType, name, descriptor)
                calleesAndCallers.updateWithCall(declaredMethod, callee, 0)
            }
            val callees =
                new StandardInvokeCalleesImplementation(calleesAndCallers.callees, IntTrieSet.empty)
            Result(declaredMethod, callees) :: calleesAndCallers.partialResultsForCallers
        }.getOrElse(List.empty)

        Results(instantiatedTypesResults ::: callResults)
    }
}

object RTACallGraphAnalysis {

    def partialResultForInstantiatedTypes(
        p:                        SomeProject,
        newInstantiatedTypes:     UIDSet[ObjectType],
        initialInstantiatedTypes: UIDSet[ObjectType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case IntermediateESimpleP(_, ub) ⇒
                    Some(IntermediateESimpleP(
                        p,
                        ub.updated(newInstantiatedTypes)
                    ))

                case _: EPK[_, _] ⇒
                    Some(IntermediateESimpleP(
                        p,
                        InstantiatedTypes.initial(newInstantiatedTypes, initialInstantiatedTypes)
                    ))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
    }
}

object EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override type InitializationData = RTACallGraphAnalysis

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes, TACAI)

    override def derives: Set[PropertyKind] = Set(
        LoadedClasses, InstantiatedTypes, CallersProperty, StandardInvokeCallees
    )

    override def init(p: SomeProject, ps: PropertyStore): RTACallGraphAnalysis = {
        val analysis = new RTACallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
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
                    case EPK(_, _) ⇒ Some(IntermediateESimpleP(
                        ep,
                        OnlyCallersWithUnknownContext
                    ))
                    case IntermediateESimpleP(_, ub) ⇒
                        Some(IntermediateESimpleP(ep, ub.updatedWithUnknownContext()))
                    case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
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
