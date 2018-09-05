/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

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
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.cg.properties.OnlyVMLevelCallers
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.LowerBoundCallers
import org.opalj.log.OPALLogger
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.IsReferenceValue

/**
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 * @author Michael Reif
 */
class ThreadRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val tacai = project.get(SimpleTACAIKey)

    private[this] val setUncaughtExceptionHandlerDescriptor = {
        MethodDescriptor(ObjectType("java/lang/Thread$UncaughtExceptionHandler"), VoidType)
    }

    private[this] val uncaughtExceptionDescriptor = {
        MethodDescriptor(
            RefArray(ObjectType.Thread.asInstanceOf[FieldType], ObjectType.Throwable), VoidType
        )
    }

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
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

        val definedMethod = declaredMethod.asDefinedMethod

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        var threadRelatedMethods: Set[DeclaredMethod] = Set.empty

        val stmts = tacai(method).stmts
        for {
            VirtualMethodCall(_, dc, _, name, descriptor, receiver, params) ← stmts
            if classHierarchy.isSubtypeOf(dc, ObjectType.Thread)
        } {
            if (name == "start" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) {
                threadRelatedMethods =
                    handleStart(definedMethod, threadRelatedMethods, stmts, receiver)
            } else if (name == "setUncaughtExceptionHandler" &&
                descriptor == setUncaughtExceptionHandlerDescriptor) {

                threadRelatedMethods =
                    handleUncaughtExceptionHandler(
                        definedMethod,
                        params.head,
                        "uncaughtException",
                        uncaughtExceptionDescriptor,
                        threadRelatedMethods
                    )
            }

        }

        Results(
            threadRelatedMethods.map { method ⇒
                PartialResult[DeclaredMethod, CallersProperty](method, CallersProperty.key, {
                    case EPK(_, _) ⇒
                        Some(EPS(method, LowerBoundCallers, OnlyVMLevelCallers))
                    case EPS(_, lb, ub) if !ub.hasCallersWithUnknownContext ⇒
                        Some(EPS(method, lb, ub.updatedWithVMLevelCall()))
                    case _ ⇒ None
                })
            }
        )
    }

    private[this] def handleStart(
        definedMethod:        DefinedMethod,
        threadRelatedMethods: Set[DeclaredMethod],
        stmts:                Array[Stmt[V]],
        receiver:             Expr[V]
    ): Set[DeclaredMethod] = {
        // a call to Thread.start will trigger the JVM to later on call Thread.exit()
        val exitMethod = project.specialCall(
            ObjectType.Thread,
            ObjectType.Thread,
            isInterface = false,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid
        )

        var newThreadRelatedMethods = addMethod(
            definedMethod,
            exitMethod,
            ObjectType.Thread,
            "exit",
            MethodDescriptor.NoArgsAndReturnVoid,
            threadRelatedMethods
        )

        // a call to Thread.start will trigger a call to the underlying run method
        val rvs = receiver.asVar.value.asReferenceValue.allValues
        for {
            rv ← rvs
            if rv.isNull.isNoOrUnknown
        } {
            if (rv.isPrecise) {
                val receiverType = rv.valueType.get.asObjectType
                val runMethod = project.instanceCall(
                    receiverType, receiverType, "run", MethodDescriptor.NoArgsAndReturnVoid
                )

                newThreadRelatedMethods = addMethod(
                    definedMethod,
                    runMethod,
                    receiverType,
                    "run",
                    MethodDescriptor.NoArgsAndReturnVoid,
                    threadRelatedMethods
                )

                if (rv.valueType.get == ObjectType.Thread || (
                    runMethod.hasValue && runMethod.value.classFile.thisType == ObjectType.Thread
                )) {
                    newThreadRelatedMethods = handleThreadWithRunnable(
                        definedMethod,
                        stmts,
                        receiver,
                        threadRelatedMethods
                    )
                }
            } else {
                OPALLogger.warn("analysis", "missed call to `run`")
            }
        }

        newThreadRelatedMethods
    }

    def getConstructorCalls(expr: Expr[V], defSite: Int, stmts: Array[Stmt[V]]): Iterator[NonVirtualMethodCall[V]] = {
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

    private def handleThreadWithRunnable(
        definedMethod:        DefinedMethod,
        stmts:                Array[Stmt[V]],
        receiver:             Expr[V],
        threadRelatedMethods: Set[DeclaredMethod]
    ): Set[DeclaredMethod] = {
        var newThreadRelatedMethods = threadRelatedMethods

        for {
            threadDefSite ← receiver.asVar.definedBy
        } {
            if (threadDefSite < 0) {
                // the thread is given as a parameter
                OPALLogger.warn("analysis", "missed call to `run`")
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
                                        newThreadRelatedMethods = handlePrecise(
                                            definedMethod,
                                            runnableValue,
                                            "run",
                                            MethodDescriptor.NoArgsAndReturnVoid,
                                            newThreadRelatedMethods
                                        )
                                    } else {

                                        OPALLogger.warn("analysis", "missed call to `run`")
                                    }
                                }
                            }
                        }
                    case _ ⇒
                        // the thread object is not newly allocated
                        OPALLogger.warn("analysis", "missed call to `run`")
                }
            }
        }

        newThreadRelatedMethods
    }

    private[this] def handleUncaughtExceptionHandler(
        definedMethod:        DefinedMethod,
        receiver:             Expr[V],
        name:                 String,
        descriptor:           MethodDescriptor,
        threadRelatedMethods: Set[DeclaredMethod]
    ): Set[DeclaredMethod] = {
        var newThreadRelatedMethods = threadRelatedMethods

        val rvs = receiver.asVar.value.asReferenceValue.allValues
        for {
            rv ← rvs
            if rv.isNull.isNoOrUnknown
        } {
            // for precise types we can directly add the call edge here
            if (rv.isPrecise) {
                newThreadRelatedMethods =
                    handlePrecise(definedMethod, rv, name, descriptor, threadRelatedMethods)
            } else {
                OPALLogger.warn("analysis", "missed call to `uncaughtException`")
            }
        }

        newThreadRelatedMethods
    }

    private[this] def handlePrecise(
        definedMethod:        DefinedMethod,
        receiver:             IsReferenceValue,
        name:                 String,
        descriptor:           MethodDescriptor,
        threadRelatedMethods: Set[DeclaredMethod]

    ): Set[DeclaredMethod] = {
        val thisType = definedMethod.declaringClassType.asObjectType
        val preciseType = receiver.valueType.get.asObjectType
        val tgt = project.instanceCall(
            thisType,
            preciseType,
            name,
            descriptor
        )
        addMethod(definedMethod, tgt, preciseType, name, descriptor, threadRelatedMethods)
    }

    private[this] def addMethod(
        definedMethod:        DefinedMethod,
        target:               org.opalj.Result[Method],
        preciseType:          ObjectType,
        name:                 String,
        descriptor:           MethodDescriptor,
        threadRelatedMethods: Set[DeclaredMethod]
    ): Set[DeclaredMethod] = {
        if (target.hasValue) {
            threadRelatedMethods + declaredMethods(target.value)
        } else {
            val declTgt = declaredMethods.apply(
                preciseType,
                definedMethod.declaringClassType.asObjectType.packageName,
                preciseType,
                name,
                descriptor
            )

            if (declTgt.hasSingleDefinedMethod || declTgt.hasMultipleDefinedMethods) {
                threadRelatedMethods
            } else {
                threadRelatedMethods + declTgt
            }
        }
    }

}

object EagerThreadRelatedCallsAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = ThreadRelatedCallsAnalysis

    override def start(
        project: SomeProject, propertyStore: PropertyStore, analysis: ThreadRelatedCallsAnalysis
    ): FPCFAnalysis = {
        analysis
    }

    override def uses: Set[PropertyKind] = Set(CallersProperty)

    override def derives: Set[PropertyKind] = Set(CallersProperty)

    override def init(p: SomeProject, ps: PropertyStore): ThreadRelatedCallsAnalysis = {
        val analysis = new ThreadRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
