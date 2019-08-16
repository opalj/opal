/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
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
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.Method
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.properties.TACAI

trait PointsToBasedThreadStartAnalysis
        extends APIBasedAnalysis
        with AbstractPointsToBasedAnalysis {

    def threadStartMethod: DeclaredMethod
    override val apiMethod: DeclaredMethod = threadStartMethod

    override type State = PointsToBasedCGState[PointsToSet]
    override type DependerType = CallSiteT

    override def handleNewCaller(
        caller: DefinedMethod, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val indirectCalls = new IndirectCalls()

        val tacEPS = propertyStore(caller.definedMethod, TACAI.key)
        implicit val state: State = new PointsToBasedCGState[PointsToSet](caller, tacEPS)

        if (isDirect) {
            val receiver = state.tac.stmts(state.tac.pcToIndex(pc)).asVirtualMethodCall.receiver
            handleStart(caller, receiver, pc, indirectCalls)
        } else
            indirectCalls.addIncompleteCallSite(pc)

        returnResult(indirectCalls)
    }

    def returnResult(
        indirectCalls: IndirectCalls
    )(implicit state: State): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.method)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                results
            )
        else
            Results(results)
    }

    def c(
        state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBPS(e, ub: PointsToSetLike[_, _, _], isFinal) ⇒
                val relevantCallSites = state.dependersOf(e)

                // ensures, that we only add new vm reachable methods
                val indirectCalls = new IndirectCalls()

                val oldEOptP: EOptionP[Entity, PointsToSet] = state.getPointsToProperty(eps.e)
                val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numTypes else 0

                for (cs ← relevantCallSites) {
                    val pc = cs._1
                    val receiver =
                        state.tac.stmts(state.tac.pcToIndex(pc)).asVirtualMethodCall.receiver
                    ub.forNewestNTypes(ub.numTypes - seenTypes) { newType ⇒
                        val theType = newType.asObjectType
                        handleType(
                            theType, state.method, state.tac.stmts, receiver, pc, indirectCalls
                        )
                    }
                }

                // The method removeTypesForCallSite might have made the dependency obsolete, so
                // only update or remove it, if we still need updates for that type.
                if (state.hasPointsToDependee(e)) {
                    if (isFinal) {
                        state.removePointsToDependee(e)
                    } else {
                        state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsToSet]])
                    }
                }

                returnResult(indirectCalls)(state)
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
        receiver:      Expr[V],
        pc:            Int,
        indirectCalls: IndirectCalls
    )(implicit state: State): Unit = {
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
            receiver.asVar,
            state.tac.stmts,
            exitMethod,
            ObjectType.Thread,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid,
            pc,
            indirectCalls
        )

        val callSite = (
            pc,
            "start",
            MethodDescriptor.NoArgsAndReturnVoid,
            state.tac.stmts(state.tac.pcToIndex(pc)).asInstanceMethodCall.declaringClass
        )

        // get the upper bound of the pointsToSet and creates a dependency if needed
        val currentPointsToSets = currentPointsToOfDefSites(callSite, receiver.asVar.definedBy)
        val pointsToSet = currentPointsToSets.foldLeft(emptyPointsToSet) { (r, l) ⇒ r.included(l) }

        pointsToSet.forNewestNTypes(pointsToSet.numTypes) { tpe ⇒
            if (classHierarchy.isSubtypeOf(tpe, ObjectType.Thread) ||
                classHierarchy.isSubtypeOf(tpe, ObjectType.Runnable))
                handleType(
                    tpe.asObjectType, definedMethod, state.tac.stmts, receiver, pc, indirectCalls
                )
        }
    }

    private[this] def handleType(
        tpe:           ObjectType,
        definedMethod: DefinedMethod,
        stmts:         Array[Stmt[V]],
        receiver:      Expr[V],
        pc:            Int,
        indirectCalls: IndirectCalls
    ): Unit = {
        val runMethod = project.instanceCall(
            tpe, tpe, "run", MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            definedMethod,
            receiver.asVar,
            stmts,
            runMethod,
            tpe,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid,
            pc,
            indirectCalls
        )

        if ((tpe eq ObjectType.Thread) || (
            runMethod.hasValue && (runMethod.value.classFile.thisType eq ObjectType.Thread)
        )) {
            handleThreadWithRunnable(
                definedMethod,
                stmts,
                receiver,
                pc,
                indirectCalls
            )
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
        indirectCalls: IndirectCalls
    ): Unit = {
        for {
            threadDefSite ← receiver.asVar.definedBy
        } {
            if (threadDefSite < 0) {
                // the thread is given as a parameter
                indirectCalls.addIncompleteCallSite(pc)
            } else {
                stmts(threadDefSite) match {
                    case Assignment(_, thread, New(_, _)) ⇒
                        for {
                            NonVirtualMethodCall(_, _, _, "<init>", descriptor, _, params) ← getConstructorCalls(thread, threadDefSite, stmts)
                        } {
                            val indexOfRunnableParameter = descriptor.parameterTypes.indexWhere {
                                _ == ObjectType.Runnable
                            }

                            // if there is no runnable passed as parameter, we are sound
                            if (indexOfRunnableParameter != -1) {
                                val theReceiver = params(indexOfRunnableParameter).asVar
                                for (runnableValue ← theReceiver.value.asReferenceValue.allValues) {
                                    if (runnableValue.isPrecise) {
                                        addMethod(
                                            definedMethod,
                                            runnableValue,
                                            theReceiver,
                                            stmts,
                                            pc,
                                            indirectCalls
                                        )
                                    } else {
                                        indirectCalls.addIncompleteCallSite(pc)
                                    }
                                }
                            }
                        }
                    case _ ⇒
                        // the thread object is not newly allocated
                        indirectCalls.addIncompleteCallSite(pc)
                }
            }
        }
    }

    private[this] def addMethod(
        definedMethod: DefinedMethod,
        receiverValue: IsReferenceValue,
        receiver:      V,
        stmts:         Array[Stmt[V]],
        pc:            Int,
        indirectCalls: IndirectCalls
    ): Unit = {
        val thisType = definedMethod.declaringClassType
        val preciseType = receiverValue.leastUpperType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            definedMethod,
            receiver,
            stmts,
            tgt,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid,
            pc,
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
        definedMethod: DefinedMethod,
        receiver:      V,
        stmts:         Array[Stmt[V]],
        target:        org.opalj.Result[Method],
        preciseType:   ObjectType,
        name:          String,
        descriptor:    MethodDescriptor,
        pc:            Int,
        indirectCalls: IndirectCalls
    ): Unit = {
        val persistentReceiver = persistentUVar(receiver)(stmts)
        if (target.hasValue) {
            indirectCalls.addCall(
                definedMethod, declaredMethods(target.value), pc, Seq.empty, persistentReceiver
            )
        } else {
            val declTgt = declaredMethods(
                preciseType,
                definedMethod.declaringClassType.packageName,
                preciseType,
                name,
                descriptor
            )

            // also add calls to virtual declared methods (unpresent library methods)
            if (!declTgt.hasSingleDefinedMethod && !declTgt.hasMultipleDefinedMethods) {
                indirectCalls.addIncompleteCallSite(pc)
                indirectCalls.addCall(definedMethod, declTgt, pc, Seq.empty, persistentReceiver)
            }
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: CallSiteT, dependee: Entity, typeFilter: ReferenceType ⇒ Boolean = { _ ⇒ true }
    )(implicit state: State): PointsToSet = {
        if (state.hasPointsToDependee(dependee)) {
            val p2s = state.getPointsToProperty(dependee)

            // It might be the case that there a dependency for that points-to state in the state
            // from another depender.
            if (!state.hasPointsToDependency(depender, dependee)) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
        } else {
            val p2s = propertyStore(dependee, pointsToPropertyKey)
            if (p2s.isRefinable) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
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
trait PointsToBasedThreadRelatedCallsAnalysis extends FPCFAnalysis {
    val createAnalysis: (SomeProject, DeclaredMethod) ⇒ PointsToBasedThreadStartAnalysis

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

        classHierarchy.foreachSubclass(ObjectType.Thread, project) { cf ⇒
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

        val uncaughtExceptionHandlerResults = setUncaughtExceptionHandlerMethods.iterator.map { m ⇒
            new UncaughtExceptionHandlerAnalysis(p, m).registerAPIMethod()
        }

        val threadStartResults = threadStartMethods.iterator.map { m ⇒
            createAnalysis(p, m).registerAPIMethod()
        }

        Results(uncaughtExceptionHandlerResults ++ threadStartResults)
    }
}

class TypeBasedPointsToBasedThreadStartAnalysis private[pointsto] (
    final val project:                    SomeProject,
    override final val threadStartMethod: DeclaredMethod
) extends PointsToBasedThreadStartAnalysis with AllocationSiteBasedAnalysis

class TypeBasedPointsToBasedThreadRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToBasedThreadRelatedCallsAnalysis {
    override val createAnalysis: (SomeProject, DeclaredMethod) ⇒ TypeBasedPointsToBasedThreadStartAnalysis =
        (p, m) ⇒ new TypeBasedPointsToBasedThreadStartAnalysis(p, m)
}

object TypeBasedPointsToBasedThreadRelatedCallsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers, TACAI)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): TypeBasedPointsToBasedThreadRelatedCallsAnalysis = {
        val analysis = new TypeBasedPointsToBasedThreadRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

class AllocationSiteBasedPointsToBasedThreadStartAnalysis private[pointsto] (
    final val project:                    SomeProject,
    override final val threadStartMethod: DeclaredMethod
) extends PointsToBasedThreadStartAnalysis with AllocationSiteBasedAnalysis

class AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToBasedThreadRelatedCallsAnalysis {
    override val createAnalysis: (SomeProject, DeclaredMethod) ⇒ AllocationSiteBasedPointsToBasedThreadStartAnalysis =
        (p, m) ⇒ new AllocationSiteBasedPointsToBasedThreadStartAnalysis(p, m)
}

object AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers, TACAI)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysis = {
        val analysis = new AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}