/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.value.IsReferenceValue
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.Method
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI

class ThreadStartAnalysis private[analyses] (
        final val project: SomeProject, final val threadStartMethod: DeclaredMethod
) extends TACAIBasedAPIBasedCallGraphAnalysis {

    override val apiMethod: DeclaredMethod = threadStartMethod

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        val vmReachableMethods = new VMReachableMethods()
        implicit val stmts: Array[Stmt[V]] = tac.stmts

        if (receiverOption.isDefined)
            handleStart(caller, stmts, receiverOption.get, pc, vmReachableMethods)
        else
            vmReachableMethods.addIncompleteCallSite(pc)

        Results(vmReachableMethods.partialResults(caller))
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
        definedMethod:      DefinedMethod,
        stmts:              Array[Stmt[V]],
        receiver:           Expr[V],
        pc:                 Int,
        vmReachableMethods: VMReachableMethods
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
            vmReachableMethods
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
                    vmReachableMethods
                )

                if (rv.leastUpperType.get == ObjectType.Thread || (
                    runMethod.hasValue && runMethod.value.classFile.thisType == ObjectType.Thread
                )) {
                    handleThreadWithRunnable(
                        definedMethod,
                        stmts,
                        receiver,
                        pc,
                        vmReachableMethods
                    )
                }
            } else {
                vmReachableMethods.addIncompleteCallSite(pc)
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
        definedMethod:      DefinedMethod,
        stmts:              Array[Stmt[V]],
        receiver:           Expr[V],
        pc:                 Int,
        vmReachableMethods: VMReachableMethods
    ): Unit = {
        for {
            threadDefSite ← receiver.asVar.definedBy
        } {
            if (threadDefSite < 0) {
                // the thread is given as a parameter
                vmReachableMethods.addIncompleteCallSite(pc)
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
                                for (runnableValue ← params(indexOfRunnableParameter).asVar.value.asReferenceValue.allValues) {
                                    if (runnableValue.isPrecise) {
                                        addMethod(
                                            definedMethod,
                                            runnableValue,
                                            pc,
                                            vmReachableMethods
                                        )
                                    } else {
                                        vmReachableMethods.addIncompleteCallSite(pc)
                                    }
                                }
                            }
                        }
                    case _ ⇒
                        // the thread object is not newly allocated
                        vmReachableMethods.addIncompleteCallSite(pc)
                }
            }
        }
    }

    private[this] def addMethod(
        definedMethod:      DefinedMethod,
        receiver:           IsReferenceValue,
        pc:                 Int,
        vmReachableMethods: VMReachableMethods
    ): Unit = {
        val thisType = definedMethod.declaringClassType.asObjectType
        val preciseType = receiver.leastUpperType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        addMethod(
            definedMethod,
            tgt,
            preciseType,
            "run",
            MethodDescriptor.NoArgsAndReturnVoid,
            pc,
            vmReachableMethods
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
        definedMethod:      DefinedMethod,
        target:             org.opalj.Result[Method],
        preciseType:        ObjectType,
        name:               String,
        descriptor:         MethodDescriptor,
        pc:                 Int,
        vmReachableMethods: VMReachableMethods
    ): Unit = {
        if (target.hasValue) {
            vmReachableMethods.addVMReachableMethod(declaredMethods(target.value))
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
                vmReachableMethods.addIncompleteCallSite(pc)
                vmReachableMethods.addVMReachableMethod(declTgt)
            }
        }
    }
}

class UncaughtExceptionHandlerAnalysis private[analyses] (
        final val project: SomeProject, final val setUncaughtExceptionHandlerMethod: DeclaredMethod
) extends TACAIBasedAPIBasedCallGraphAnalysis {

    override val apiMethod: DeclaredMethod = setUncaughtExceptionHandlerMethod

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        val vmReachableMethods = new VMReachableMethods()
        if (params.nonEmpty && params.head.isDefined)
            handleUncaughtExceptionHandler(caller, params.head.get, pc, vmReachableMethods)
        else
            vmReachableMethods.addIncompleteCallSite(pc)

        Results(vmReachableMethods.partialResults(caller))
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
        definedMethod:      DefinedMethod,
        receiver:           Expr[V],
        pc:                 Int,
        vmReachableMethods: VMReachableMethods
    ): Unit = {
        val rvs = receiver.asVar.value.asReferenceValue.allValues
        for {
            rv ← rvs
            if rv.isNull.isNoOrUnknown
        } {
            // for precise types we can directly add the call edge here
            if (rv.isPrecise) {
                handlePreciseUncaughtExceptionHandler(
                    definedMethod, rv, "uncaughtException", pc, vmReachableMethods
                )
            } else {
                vmReachableMethods.addIncompleteCallSite(pc)
            }
        }
    }

    // todo refactor
    private[this] def handlePreciseUncaughtExceptionHandler(
        definedMethod:      DefinedMethod,
        receiver:           IsReferenceValue,
        name:               String,
        pc:                 Int,
        vmReachableMethods: VMReachableMethods

    ): Unit = {
        val thisType = definedMethod.declaringClassType.asObjectType
        val preciseType = receiver.leastUpperType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            name,
            ThreadRelatedCallsAnalysisScheduler.uncaughtExceptionDescriptor
        )

        if (tgt.hasValue) {
            vmReachableMethods.addVMReachableMethod(declaredMethods(tgt.value))
        } else {
            val declTgt = declaredMethods(
                preciseType,
                definedMethod.declaringClassType.asObjectType.packageName,
                preciseType,
                name,
                ThreadRelatedCallsAnalysisScheduler.uncaughtExceptionDescriptor
            )

            assert(!declTgt.hasSingleDefinedMethod)

            vmReachableMethods.addIncompleteCallSite(pc)
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
class ThreadRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
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
            new ThreadStartAnalysis(p, m).registerAPIMethod()
        }

        Results(uncaughtExceptionHandlerResults ++ threadStartResults)
    }
}

object ThreadRelatedCallsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    private[cg] val uncaughtExceptionDescriptor = {
        MethodDescriptor(
            RefArray(ObjectType.Thread, ObjectType.Throwable), VoidType
        )
    }

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers, TACAI)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ThreadRelatedCallsAnalysis = {
        val analysis = new ThreadRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}
