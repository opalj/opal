/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsReferenceValue
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cg.properties.NoThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.cg.properties.OnlyVMLevelCallers
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * This analysis handles implicit method invocations related to the `java.lang.Thread` API.
 * As an example, a call to `Thread#start` eventual lead to an invocation of the `run` method of
 * the corresponding `java.lang.Runnable` object.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 * @author Michael Reif
 */
class ThreadRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    class State(
            var vmReachableMethods:  Set[DeclaredMethod] = Set.empty,
            var incompleteCallSites: IntTrieSet          = IntTrieSet.empty
    )

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    private[this] val setUncaughtExceptionHandlerDescriptor = {
        MethodDescriptor(ObjectType("java/lang/Thread$UncaughtExceptionHandler"), VoidType)
    }

    private[this] val uncaughtExceptionDescriptor = {
        MethodDescriptor(
            RefArray(ObjectType.Thread.asInstanceOf[FieldType], ObjectType.Throwable), VoidType
        )
    }

    /**
     * This method is triggered each time the property store has a first
     * [[org.opalj.br.fpcf.cg.properties.CallersProperty]] value for the `declaredMethod`. If the
     * method is reachable, it is being checked for calls into the `Thead` API and add the
     * corresponding implicit/evantual method call. It do so by calling `processMethod`.
     */
    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
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

        val definedMethod = declaredMethod.asDefinedMethod

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // if we have a tac already we can start the analysis. otherwise we wait for updates.
        val tacaiEP = propertyStore(method, TACAI.key)
        if (tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined) {
            processMethod(definedMethod, tacaiEP.asEPS)
        } else {
            InterimResult.forUB(
                definedMethod,
                NoThreadRelatedIncompleteCallSites,
                Some(tacaiEP),
                continuation(definedMethod)
            )
        }
    }

    /**
     * If there are updates on the [[org.opalj.tac.fpcf.properties.TACAI]], we have to process the
     * method again.
     */
    private[this] def continuation(
        method: DefinedMethod
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tac: TACAI) if tac.tac.isDefined ⇒
                processMethod(method, eps.asInstanceOf[EPS[Method, TACAI]])
            case UBP(_: TACAI) ⇒
                InterimResult.forUB(
                    method,
                    NoThreadRelatedIncompleteCallSites,
                    Some(eps),
                    continuation(method)
                )
        }
    }

    /**
     * Iterate over the statements of the method and search for calls to `start` and
     * `setUncaughtExceptionHandler`. The corresponding, eventually called methods will be
     * marked as VMLevelReachable.
     */
    private[this] def processMethod(
        definedMethod: DefinedMethod, tacaiEPS: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        assert(tacaiEPS.hasUBP && tacaiEPS.ub.tac.isDefined)
        val stmts = tacaiEPS.ub.tac.get.stmts

        val state = new State()
        for {
            VirtualMethodCall(pc, dc, _, name, descriptor, receiver, params) ← stmts
            if classHierarchy.isSubtypeOf(dc, ObjectType.Thread)
            // TODO handle Runnable#addShutdownHook
        } {
            if (name == "start" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) {
                handleStart(definedMethod, stmts, receiver, pc, state)
            } else if (name == "setUncaughtExceptionHandler" &&
                descriptor == setUncaughtExceptionHandlerDescriptor) {
                handleUncaughtExceptionHandler(
                    definedMethod,
                    params.head,
                    uncaughtExceptionDescriptor,
                    pc,
                    state
                )
            }

        }

        // partial results for all methods that should be made vm reachable
        val results: Iterator[ProperPropertyComputationResult] =
            state.vmReachableMethods.iterator.map { method ⇒
                PartialResult[DeclaredMethod, CallersProperty](
                    method,
                    CallersProperty.key,
                    {
                        case InterimUBP(ub) if !ub.hasVMLevelCallers ⇒
                            Some(InterimEUBP(method, ub.updatedWithVMLevelCall()))

                        case _: InterimEP[_, _] ⇒ None
                        case _: EPK[_, _]       ⇒ Some(InterimEUBP(method, OnlyVMLevelCallers))
                        case r                  ⇒ throw new IllegalStateException(s"unexpected $r")
                    }
                )
            }

        // todo we need to add the incomplete call sites
        val c =
            if (tacaiEPS.isRefinable)
                InterimResult.forUB(
                    definedMethod,
                    ThreadRelatedIncompleteCallSites(state.incompleteCallSites),
                    Some(tacaiEPS),
                    continuation(definedMethod)
                )
            else
                Result(definedMethod, ThreadRelatedIncompleteCallSites(state.incompleteCallSites))

        Results(c, results)
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
        definedMethod: DefinedMethod,
        target:        org.opalj.Result[Method],
        preciseType:   ObjectType,
        name:          String,
        descriptor:    MethodDescriptor,
        pc:            Int,
        state:         State
    ): Unit = {
        if (target.hasValue) {
            state.vmReachableMethods += declaredMethods(target.value)
        } else {
            val declTgt = declaredMethods(
                preciseType,
                definedMethod.declaringClassType.asObjectType.packageName,
                preciseType,
                name,
                descriptor
            )

            // also add calls to virtual declared methods (unpresent library methods)
            if (!declTgt.hasSingleDefinedMethod && !declTgt.hasMultipleDefinedMethods) {
                state.incompleteCallSites += pc
                state.vmReachableMethods += declTgt
            }
        }
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
        definedMethod: DefinedMethod,
        stmts:         Array[Stmt[V]],
        receiver:      Expr[V],
        pc:            Int,
        state:         State
    ): Unit = {
        // a call to Thread.start will trigger the JVM to later on call Thread.exit()
        val exitMethod = project.specialCall(
            ObjectType.Thread,
            ObjectType.Thread,
            isInterface = false,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            definedMethod,
            exitMethod,
            ObjectType.Thread,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid,
            pc,
            state
        )

        // a call to Thread.start will trigger a call to the underlying run method
        val rvs = receiver.asVar.value.asReferenceValue.allValues
        for {
            rv ← rvs
            if rv.isNull.isNoOrUnknown
        } {
            if (rv.isPrecise) {
                val receiverType = rv.leastUpperType.get.asObjectType
                val runMethod = project.instanceCall(
                    receiverType, receiverType, "run", MethodDescriptor.NoArgsAndReturnVoid
                )

                addMethod(
                    definedMethod,
                    runMethod,
                    receiverType,
                    "run",
                    MethodDescriptor.NoArgsAndReturnVoid,
                    pc,
                    state
                )

                if (rv.leastUpperType.get == ObjectType.Thread || (
                    runMethod.hasValue && runMethod.value.classFile.thisType == ObjectType.Thread
                )) {
                    handleThreadWithRunnable(
                        definedMethod,
                        stmts,
                        receiver,
                        pc,
                        state
                    )
                }
            } else {
                state.incompleteCallSites += pc
            }
        }
    }

    /**
     * For the given `expr`, it collects all calls to `<init>` on that expression.
     */
    private[this] def getConstructorCalls(
        expr: Expr[V], defSite: Int, stmts: Array[Stmt[V]]
    ): Iterator[NonVirtualMethodCall[V]] = {
        var r = List.empty[NonVirtualMethodCall[V]]
        expr.asVar.usedBy.foreach { use ⇒
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
     *
     * Note: It takes the given `threadRelatedMethods`, add the relavant ones and returns the
     * updated set.
     */
    private[this] def handleThreadWithRunnable(
        definedMethod: DefinedMethod,
        stmts:         Array[Stmt[V]],
        receiver:      Expr[V],
        pc:            Int,
        state:         State
    ): Unit = {
        for {
            threadDefSite ← receiver.asVar.definedBy
        } {
            if (threadDefSite < 0) {
                // the thread is given as a parameter
                state.incompleteCallSites += pc
            } else {
                stmts(threadDefSite) match {
                    case Assignment(_, thread, New(_, _)) ⇒
                        for {
                            NonVirtualMethodCall(_, _, _, "<init>", descriptor, _, params) ← getConstructorCalls(thread, threadDefSite, stmts)
                        } {
                            val indexOfRunnableParameter = descriptor.parameterTypes.indexWhere {
                                _ == ObjectType.Runnable
                            }

                            // of there is no runnable passed as parameter, we are sound
                            if (indexOfRunnableParameter != -1) {
                                for (runnableValue ← params(indexOfRunnableParameter).asVar.value.asReferenceValue.allValues) {
                                    if (runnableValue.isPrecise) {
                                        handlePreciseUncaughtExceptionHandler(
                                            definedMethod,
                                            runnableValue,
                                            "run",
                                            MethodDescriptor.NoArgsAndReturnVoid,
                                            pc,
                                            state
                                        )
                                    } else {
                                        state.incompleteCallSites += pc
                                    }
                                }
                            }
                        }
                    case _ ⇒
                        // the thread object is not newly allocated
                        state.incompleteCallSites += pc
                }
            }
        }
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
        definedMethod: DefinedMethod,
        receiver:      Expr[V],
        descriptor:    MethodDescriptor,
        pc:            Int,
        state:         State
    ): Unit = {
        val rvs = receiver.asVar.value.asReferenceValue.allValues
        for {
            rv ← rvs
            if rv.isNull.isNoOrUnknown
        } {
            // for precise types we can directly add the call edge here
            if (rv.isPrecise) {
                handlePreciseUncaughtExceptionHandler(
                    definedMethod, rv, "uncaughtException", descriptor, pc, state
                )
            } else {
                state.incompleteCallSites += pc
            }
        }
    }

    private[this] def handlePreciseUncaughtExceptionHandler(
        definedMethod: DefinedMethod,
        receiver:      IsReferenceValue,
        name:          String,
        descriptor:    MethodDescriptor,
        pc:            Int,
        state:         State

    ): Unit = {
        val thisType = definedMethod.declaringClassType.asObjectType
        val preciseType = receiver.leastUpperType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            name,
            descriptor
        )
        addMethod(definedMethod, tgt, preciseType, name, descriptor, pc, state)
    }
}

object TriggeredThreadRelatedCallsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty),
        PropertyBounds.ub(TACAI)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty)
    )

    override def derivesEagerly: Set[PropertyBounds] =
        Set(PropertyBounds.ub(ThreadRelatedIncompleteCallSites))

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ThreadRelatedCallsAnalysis = {
        val analysis = new ThreadRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }
}
