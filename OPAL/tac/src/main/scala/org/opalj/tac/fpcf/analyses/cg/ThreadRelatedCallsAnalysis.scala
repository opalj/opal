/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.value.IsReferenceValue
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

/**
 * On calls to Thread.start(), it adds calls to the corresponding run method.
 * The run method is determined using points-to information.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class ThreadStartAnalysis private[cg] (
        override val project:   SomeProject,
        override val apiMethod: DeclaredMethod
) extends TACAIBasedAPIBasedAnalysis with TypeConsumerAnalysis {

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        callPC:          Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        val indirectCalls = new IndirectCalls()

        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            val receiver = receiverOption.get.asVar
            handleStart(callerContext, callPC, receiver, indirectCalls)
            returnResult(receiver, indirectCalls)
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
            Results(indirectCalls.partialResults(callerContext))
        }
    }

    def returnResult(
        receiver: V, indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(receiver, state)),
                results
            )
        else
            Results(results)
    }

    def c(receiver: V, state: CGState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        val epk = eps.toEPK

        // ensures, that we only add new vm reachable methods
        val indirectCalls = new IndirectCalls()
        implicit val _state: CGState[ContextType] = state

        eps.ub match {
            case _: TACAI =>
                for {
                    (allocationPC, (callPC, allocationContext)) <- state.dependersOf(epk).asInstanceOf[Set[(Int, (Int, ContextType))]]
                } {
                    AllocationsUtil.handleAllocation(
                        allocationContext, allocationPC, allocationContext,
                        () => indirectCalls.addIncompleteCallSite(callPC)
                    ) { (allocationContext, allocationIndex, stmts) =>
                            handleThreadInit(
                                state.callContext,
                                callPC,
                                allocationContext,
                                allocationIndex,
                                stmts,
                                indirectCalls
                            )
                        }
                }

            case _ =>
                val callPC = state.dependersOf(epk).head.asInstanceOf[Int]

                val receiver = state.tac.stmts(
                    state.tac.properStmtIndexForPC(callPC)
                ).asInstanceMethodCall.receiver.asVar

                typeProvider.continuationForAllocations(
                    receiver, eps.asInstanceOf[EPS[Entity, PropertyType]]
                ) { (tpe, allocationContext, allocationPC) =>
                    val hasRunnable = handleTypeAndHasRunnable(
                        tpe, state.callContext, callPC, state.tac.stmts, receiver, indirectCalls
                    )
                    if (hasRunnable)
                        AllocationsUtil.handleAllocation(
                            allocationContext, allocationPC, (callPC, allocationContext),
                            () => indirectCalls.addIncompleteCallSite(callPC)
                        ) { (allocationContext, allocationIndex, stmts) =>
                                handleThreadInit(
                                    state.callContext,
                                    callPC,
                                    allocationContext,
                                    allocationIndex,
                                    stmts,
                                    indirectCalls
                                )
                            }
                }
        }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(receiver, indirectCalls)
    }

    /**
     * A call to `Thread#start` eventually leads to calls to `Thread#exit` and
     * `ConcreteRunnable#run`. These methods will be returned by this method.
     * Note, that if the concrete type of the runnable object is unknown, the corresponding
     * `run` methods might be missing. Thus the resulting call graph may be unsound.
     *
     * Note: It takes the given `threadRelatedMethods`, add the relavant ones and returns the
     * updated set.
     */
    private[this] def handleStart(
        callContext:   ContextType,
        callPC:        Int,
        receiver:      V,
        indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): Unit = {
        // a call to Thread.start will trigger the JVM to later on call Thread.exit()
        val exitMethod = project.specialCall(
            ObjectType.Thread,
            ObjectType.Thread,
            isInterface = false,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            callContext,
            callPC,
            Some(receiver),
            state.tac.stmts,
            exitMethod,
            ObjectType.Thread,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid,
            indirectCalls
        )

        val types = typeProvider.typesProperty(
            receiver, callContext, callPC.asInstanceOf[Entity], state.tac.stmts
        )

        typeProvider.foreachAllocation(receiver, callContext, state.tac.stmts, types) {
            (tpe, method, pc) =>
                val hasRunnable = handleTypeAndHasRunnable(
                    tpe, callContext, callPC, state.tac.stmts, receiver, indirectCalls
                )
                if (hasRunnable)
                    AllocationsUtil.handleAllocation(
                        method, pc, (callPC, method),
                        () => indirectCalls.addIncompleteCallSite(callPC)
                    ) { (allocationContext, allocationIndex, stmts) =>
                            handleThreadInit(
                                callContext,
                                callPC,
                                allocationContext,
                                allocationIndex,
                                stmts,
                                indirectCalls
                            )
                        }
        }
    }

    private[this] def handleTypeAndHasRunnable(
        tpe:           ReferenceType,
        callContext:   ContextType,
        callPC:        Int,
        stmts:         Array[Stmt[V]],
        receiver:      V,
        indirectCalls: IndirectCalls
    ): Boolean = {
        val runMethod = project.instanceCall(
            tpe.asObjectType, tpe, "run", MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            callContext,
            callPC,
            Some(receiver),
            stmts,
            runMethod,
            tpe.asObjectType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid,
            indirectCalls
        )

        (tpe eq ObjectType.Thread) ||
            runMethod.hasValue && (runMethod.value.classFile.thisType eq ObjectType.Thread)
    }

    /**
     * For the given `expr`, it collects all calls to `<init>` on that expression.
     */
    private[this] def getConstructorCalls(
        expr: Expr[V], defSite: Int, stmts: Array[Stmt[V]]
    ): Iterator[NonVirtualMethodCall[V]] = {
        var r = List.empty[NonVirtualMethodCall[V]]
        expr.asVar.usedBy.foreach { use =>
            val stmt = stmts(use)
            if (stmt.isInstanceOf[NonVirtualMethodCall[V]]) {
                val call = stmt.asNonVirtualMethodCall
                if (call.name == "<init>" && call.receiver.asVar.definedBy.contains(defSite)) {
                    r ::= call
                }
            }
        }
        r.iterator
    }

    /**
     * Handles the case of a call to `run` of a thread object, that holds a instance of
     * [[Runnable]] (passed as an argument to the constructor).
     */
    private[this] def handleThreadInit(
        callContext:       ContextType,
        callPC:            Int,
        allocationContext: Context,
        threadDefSite:     Int,
        stmts:             Array[Stmt[V]],
        indirectCalls:     IndirectCalls
    ): Unit = stmts(threadDefSite) match {
        case Assignment(_, thread, New(_, _)) =>
            for {
                NonVirtualMethodCall(_, _, _, "<init>", descriptor, _, params) <- getConstructorCalls(thread, threadDefSite, stmts)
            } {
                val indexOfRunnableParameter = descriptor.parameterTypes.indexWhere {
                    _ == ObjectType.Runnable
                }

                // if there is no runnable passed as parameter, we are sound
                if (indexOfRunnableParameter != -1) {
                    val theReceiver = params(indexOfRunnableParameter).asVar
                    for (runnableValue <- theReceiver.value.asReferenceValue.allValues) {
                        if (runnableValue.isPrecise) {
                            addMethod(
                                callContext,
                                callPC,
                                runnableValue,
                                if (callContext.method == allocationContext.method)
                                    Some(theReceiver)
                                else
                                    None,
                                stmts,
                                indirectCalls
                            )
                        } else {
                            indirectCalls.addIncompleteCallSite(callPC)
                        }
                    }
                }
            }
        case _ =>
            // the thread object is not newly allocated
            indirectCalls.addIncompleteCallSite(callPC)
    }

    /**
     * A helper method, that add the given method to the set of `threadRelatedMethods`.
     * If `target` is defined, it simply adds the corresponding [[org.opalj.br.DeclaredMethod]].
     * Otherwise, it will add the corresponding [[org.opalj.br.DeclaredMethod]] in case it is
     * virtual (i.e. its definition is not available).
     *
     * Note: It takes the given `threadRelatedMethods`, add the relavant ones and returns the
     * updated set.
     */
    private[this] def addMethod(
        callContext:   ContextType,
        callPC:        Int,
        receiverValue: IsReferenceValue,
        receiver:      Option[V],
        stmts:         Array[Stmt[V]],
        indirectCalls: IndirectCalls
    ): Unit = {
        val thisType = callContext.method.declaringClassType
        val preciseType = receiverValue.leastUpperType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            callContext,
            callPC,
            receiver,
            stmts,
            tgt,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid,
            indirectCalls
        )
    }

    /**
     * A helper method, that add the given method to the set of `threadRelatedMethods`.
     * If `target` is defined, it simply adds the corresponding [[org.opalj.br.DeclaredMethod]].
     * Otherwise, it will add the corresponding [[org.opalj.br.DeclaredMethod]] in case it is
     * virtual (i.e. its definition is not available).
     *
     * Note: It takes the given `threadRelatedMethods`, add the relavant ones and returns the
     * updated set.
     */
    private[this] def addMethod(
        callContext:   ContextType,
        callPC:        Int,
        receiver:      Option[V],
        stmts:         Array[Stmt[V]],
        target:        org.opalj.Result[Method],
        preciseType:   ObjectType,
        name:          String,
        descriptor:    MethodDescriptor,
        indirectCalls: IndirectCalls
    ): Unit = {
        val caller = callContext.method.asDefinedMethod
        val persistentReceiver = receiver.flatMap(persistentUVar(_)(stmts))
        if (target.hasValue) {
            indirectCalls.addCall(
                callContext,
                callPC,
                typeProvider.expandContext(callContext, declaredMethods(target.value), callPC),
                Seq.empty,
                persistentReceiver
            )
        } else {
            val declTgt = declaredMethods(
                preciseType, caller.declaringClassType.packageName, preciseType, name, descriptor
            )

            // also add calls to virtual declared methods (unpresent library methods)
            if (!declTgt.hasSingleDefinedMethod && !declTgt.hasMultipleDefinedMethods) {
                indirectCalls.addIncompleteCallSite(callPC)
                indirectCalls.addCall(
                    callContext,
                    callPC,
                    typeProvider.expandContext(callContext, declTgt, callPC),
                    Seq.empty,
                    persistentReceiver
                )
            }
        }
    }
}

class UncaughtExceptionHandlerAnalysis private[analyses] (
        override val project:   SomeProject,
        override val apiMethod: DeclaredMethod
) extends TACAIBasedAPIBasedAnalysis with TypeConsumerAnalysis {

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        callPC:          Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        val vmReachableMethods = new VMReachableMethods()

        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (params.nonEmpty && params.head.isDefined) {
            val receiver = params.head.get.asVar
            handleUncaughtExceptionHandler(callerContext, receiver, callPC, vmReachableMethods)
            returnResult(receiver, vmReachableMethods)(state)
        } else {
            vmReachableMethods.addIncompleteCallSite(callPC)
            Results(vmReachableMethods.partialResults(callerContext))
        }
    }

    def c(
        receiver: V,
        state:    CGState[ContextType]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        val epk = eps.toEPK
        val pc = state.dependersOf(epk).head.asInstanceOf[Int]

        // ensures, that we only add new vm reachable methods
        val vmReachableMethods = new VMReachableMethods()

        typeProvider.continuation(receiver, eps.asInstanceOf[EPS[Entity, PropertyType]]) { newType =>
            handleType(newType, state.callContext, pc, vmReachableMethods)
        }(state)

        if (eps.isFinal) {
            state.removeDependee(epk)
        } else {
            state.updateDependency(eps)
        }

        returnResult(receiver, vmReachableMethods)(state)
    }

    def returnResult(
        receiver:           V,
        vmReachableMethods: VMReachableMethods
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = vmReachableMethods.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(receiver, state)),
                results
            )
        else
            Results(results)
    }

    /**
     * Handles the case of a call to `setUncaughtExceptionHandler` by adding a call to
     * `uncaughtException` if the runtime type is precisely known.
     * Otherwise, we remain unsound.
     *
     * Note: It takes the given `threadRelatedMethods`, add the relavant ones and returns the
     * updated set.
     */
    private[this] def handleUncaughtExceptionHandler(
        callContext:        ContextType,
        receiver:           V,
        callPC:             Int,
        vmReachableMethods: VMReachableMethods
    )(implicit state: CGState[ContextType]): Unit = {
        typeProvider.foreachType(
            receiver,
            typeProvider.typesProperty(
                receiver, callContext, callPC.asInstanceOf[Entity], state.tac.stmts
            )
        ) { tpe => handleType(tpe, callContext, callPC, vmReachableMethods) }
    }

    // todo refactor
    private[this] def handleType(
        receiverType:       ReferenceType,
        callContext:        ContextType,
        callPC:             Int,
        vmReachableMethods: VMReachableMethods
    ): Unit = {
        if (classHierarchy.isASubtypeOf(
            receiverType, ObjectType("java/lang/Thread$UncaughtExceptionHandler")
        ).isNo)
            return

        val thisType = callContext.method.declaringClassType
        val tgt = project.instanceCall(
            thisType,
            receiverType,
            "uncaughtException",
            ThreadRelatedCallsAnalysisScheduler.uncaughtExceptionDescriptor
        )

        if (tgt.hasValue) {
            vmReachableMethods.addVMReachableMethod(declaredMethods(tgt.value))
        } else {
            val declTgt = declaredMethods(
                receiverType.asObjectType,
                callContext.method.declaringClassType.packageName,
                receiverType.asObjectType,
                "uncaughtException",
                ThreadRelatedCallsAnalysisScheduler.uncaughtExceptionDescriptor
            )

            assert(!declTgt.hasSingleDefinedMethod)

            vmReachableMethods.addIncompleteCallSite(callPC)
            vmReachableMethods.addVMReachableMethod(declTgt)
        }
    }
}

/**
 * This analysis handles implicit method invocations related to the `java.lang.Thread` API.
 * As an example, a call to `Thread#start` eventual lead to an invocation of the `run` method of
 * the corresponding `java.lang.Runnable` object.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 * @author Michael Reif
 */
class ThreadRelatedCallsAnalysis private[cg] (
        val project: SomeProject
) extends FPCFAnalysis {

    def process(p: SomeProject): PropertyComputationResult = {
        val declaredMethods = p.get(DeclaredMethodsKey)

        val setUncaughtExceptionHandlerDescriptor = {
            MethodDescriptor(ObjectType("java/lang/Thread$UncaughtExceptionHandler"), VoidType)
        }

        var setUncaughtExceptionHandlerMethods: List[DeclaredMethod] = List(
            declaredMethods(
                ObjectType.Thread,
                "",
                ObjectType.Thread,
                "setUncaughtExceptionHandler",
                setUncaughtExceptionHandlerDescriptor
            )
        )
        var threadStartMethods = List(declaredMethods(
            ObjectType.Thread, "", ObjectType.Thread, "start", MethodDescriptor.NoArgsAndReturnVoid
        ))

        classHierarchy.foreachSubclass(ObjectType.Thread, project) { cf =>
            val setUncaughtExcpetionHandlerOpt = cf.findMethod(
                "setUncaughtExceptionHandler", setUncaughtExceptionHandlerDescriptor
            ).map(declaredMethods.apply)

            if (setUncaughtExcpetionHandlerOpt.isDefined)
                setUncaughtExceptionHandlerMethods ::= setUncaughtExcpetionHandlerOpt.get

            val threadStartOpt = cf.findMethod(
                "start", MethodDescriptor.NoArgsAndReturnVoid
            ).map(declaredMethods.apply)

            if (threadStartOpt.isDefined)
                threadStartMethods ::= threadStartOpt.get
        }

        val uncaughtExceptionHandlerResults = setUncaughtExceptionHandlerMethods.iterator.map { m =>
            new UncaughtExceptionHandlerAnalysis(p, m).registerAPIMethod()
        }

        val threadStartResults = threadStartMethods.iterator.map { m =>
            new ThreadStartAnalysis(p, m).registerAPIMethod()
        }

        Results(uncaughtExceptionHandlerResults ++ threadStartResults)
    }
}

object ThreadRelatedCallsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers, TACAI)

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = {
        p.get(TypeProviderKey).usedPropertyKinds
    }

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ThreadRelatedCallsAnalysis = {
        val analysis = new ThreadRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    private[cg] val uncaughtExceptionDescriptor = {
        MethodDescriptor(
            ArraySeq(ObjectType.Thread, ObjectType.Throwable), VoidType
        )
    }
}