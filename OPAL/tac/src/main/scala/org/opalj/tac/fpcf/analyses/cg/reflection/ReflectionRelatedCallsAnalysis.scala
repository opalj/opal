/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import scala.language.existentials
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ASObjectValue
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.InvokeInterfaceMethodHandle
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.InvokeVirtualMethodHandle
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.BooleanType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.LoadedClasses
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.Method
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.reflection.MatcherUtil.retrieveSuitableMatcher
import org.opalj.tac.fpcf.analyses.cg.reflection.MethodHandlesUtil.retrieveDescriptorBasedMethodMatcher
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

sealed trait ReflectionAnalysis extends TACAIBasedAPIBasedAnalysis {

    implicit final val HighSoundnessMode: Boolean = {
        val activated = try {
            project.config.getBoolean(ReflectionRelatedCallsAnalysis.ConfigKey)
        } catch {
            case t: Throwable =>
                logOnce(Error(
                    "analysis configuration - reflection analysis",
                    s"couldn't read: ${ReflectionRelatedCallsAnalysis.ConfigKey}",
                    t
                ))
                false
        }

        logOnce(Info(
            "analysis configuration",
            "reflection analysis uses "+(if (activated) "high soundness mode" else "standard mode")
        ))
        activated
    }

    def constructorReceiver(pc: Int)(constructor: Method): Some[(ValueInformation, IntTrieSet)] = {
        Some((
            ASObjectValue(isNull = No, isPrecise = false, constructor.classFile.thisType),
            IntTrieSet(pc)
        ))
    }

    def addCalls(
        callContext:    ContextType,
        callPC:         Int,
        actualReceiver: Method => Option[(ValueInformation, IntTrieSet)],
        actualParams:   Seq[Option[(ValueInformation, IntTrieSet)]], matchers: Iterable[MethodMatcher]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m =>
            indirectCalls.addCall(
                callContext,
                callPC,
                typeProvider.expandContext(callContext, declaredMethods(m), callPC),
                actualParams,
                actualReceiver(m)
            )
        }
    }
}

class ClassForNameAnalysis private[analyses] (
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod,
        final val classNameIndex:     Int            = 0
) extends ReflectionAnalysis with TypeConsumerAnalysis {

    private class State(
            val stmts:       Array[Stmt[V]],
            loadedClassesUB: UIDSet[ObjectType],
            callContext:     ContextType
    ) extends CGState[ContextType](callContext, null) {
        private[this] var _loadedClassesUB: UIDSet[ObjectType] = loadedClassesUB
        private[this] var _newLoadedClasses: UIDSet[ObjectType] = UIDSet.empty

        private[cg] def addNewLoadedClasses(loadedClasses: IterableOnce[ObjectType]): Unit = {
            _newLoadedClasses ++= loadedClasses.iterator.filter(!_loadedClassesUB.contains(_))
        }

        def reset(): Unit = {
            _loadedClassesUB ++= _newLoadedClasses
            _newLoadedClasses = UIDSet.empty
        }

        def hasNewLoadedClasses: Boolean = _newLoadedClasses.nonEmpty

        def loadedClassesPartialResult: PartialResult[SomeProject, LoadedClasses] = {
            assert(hasNewLoadedClasses)
            val newLoadedClasses = _newLoadedClasses
            PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case InterimEUBP(p, ub) =>
                    val newUb = ub.classes ++ newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(InterimEUBP(p, ub.updated(newLoadedClasses)))
                    else
                        None

                case EPK(p, _) =>
                    Some(InterimEUBP(p, LoadedClasses(newLoadedClasses)))

                case r => throw new IllegalStateException(s"unexpected previous result $r")
            })
        }
    }

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
        implicit val incompleteCallSites: IncompleteCallSites = new IncompleteCallSites {}
        implicit val state: State = new State(tac.stmts, loadedClassesUB(), callerContext)

        val className = if (params.nonEmpty) params(classNameIndex) else None

        if (className.isDefined) {
            handleForName(className.get.asVar, callerContext, callPC, tac.stmts)
        } else {
            failure(callPC)
        }

        returnResult(className.map(_.asVar).orNull, incompleteCallSites)
    }

    private def returnResult(
        className: V, incompleteCallSites: IncompleteCallSites
    )(implicit state: State): ProperPropertyComputationResult = {
        val iresults: IterableOnce[ProperPropertyComputationResult] =
            incompleteCallSites.partialResults(state.callContext)
        val results = if (state.hasNewLoadedClasses) {
            val r = Iterator(state.loadedClassesPartialResult) ++ iresults
            state.reset()
            r
        } else iresults
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(className, state)),
                results
            )
        else
            Results(results)
    }

    /**
     * Retrieves the current state of loaded classes and instantiated types from the property store.
     */
    private[this] def loadedClassesUB(): UIDSet[ObjectType] = {
        // the set of classes that are definitely loaded at this point in time
        val loadedClassesEOptP = propertyStore(project, LoadedClasses.key)

        // the upper bound for loaded classes, seen so far
        val loadedClassesUB: UIDSet[ObjectType] = loadedClassesEOptP match {
            case eps: EPS[_, _] => eps.ub.classes
            case _              => UIDSet.empty
        }

        loadedClassesUB
    }

    /**
     * Adds classes that can be loaded by an invocation of Class.forName to the set of loaded
     * classes.
     */
    private[this] def handleForName(
        className: V, callContext: ContextType, pc: Int, stmts: Array[Stmt[V]]
    )(implicit state: State, incompleteCallSites: IncompleteCallSites): Unit = {
        val loadedClasses = TypesUtil.getPossibleForNameClasses(
            className, callContext, pc.asInstanceOf[Entity],
            stmts, project, () => failure(pc), onlyObjectTypes = false
        )
        state.addNewLoadedClasses(loadedClasses)
    }

    private[this] def c(
        className: V, state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = {

        // ensures, that we only add new vm reachable methods
        implicit val incompleteCallSites: IncompleteCallSites = new IncompleteCallSites {}
        implicit val _state: State = state

        AllocationsUtil.continuationForAllocation[Int, ContextType](
            eps, state.callContext, _ => (className, state.stmts),
            _.isInstanceOf[Int], callPC => failure(callPC)
        ) { (callPC, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = false
                )
                if (classOpt.isDefined) state.addNewLoadedClasses(classOpt)
                else failure(callPC)
            }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(className, incompleteCallSites)
    }

    private[this] def failure(
        callPC: Int
    )(implicit incompleteCallSites: IncompleteCallSites, state: State): Unit = {
        if (HighSoundnessMode) {
            state.addNewLoadedClasses(p.allClassFiles.iterator.map(_.thisType))
        } else {
            incompleteCallSites.addIncompleteCallSite(callPC)
        }
    }
}

class ClassNewInstanceAnalysis private[analyses] (
        final val project: SomeProject
) extends ReflectionAnalysis with TypeConsumerAnalysis {

    override val apiMethod: DeclaredMethod =
        declaredMethods(
            ObjectType.Class,
            "",
            ObjectType.Class,
            "newInstance",
            MethodDescriptor.JustReturnsObject
        )

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
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleNewInstance(callerContext, callPC, receiverOption.get.asVar, tac.stmts)
        } else {
            failure(callPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull, indirectCalls)
    }

    def returnResult(
        classRef: V, indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(classRef, state)),
                results
            )
        else
            Results(results)
    }

    private[this] def c(
        classRef: V, state: CGState[ContextType]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        implicit val _state: CGState[ContextType] = state

        AllocationsUtil.continuationForAllocation[Int, ContextType](
            eps, state.callContext, _ => (classRef, state.tac.stmts),
            _.isInstanceOf[Int], callPC => failure(callPC)
        ) { (callPC, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, callPC.asInstanceOf[Entity],
                    stmts, project, () => failure(callPC), onlyObjectTypes = true
                )

                val matchers = Set(
                    MatcherUtil.constructorMatcher,
                    new ParameterTypesBasedMethodMatcher(ArraySeq.empty),
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes.asInstanceOf[Set[ObjectType]]),
                        callPC,
                        v => new ClassBasedMethodMatcher(v, true)
                    )
                )

                addCalls(state.callContext, callPC, constructorReceiver(callPC), Seq.empty, matchers)
            }

        AllocationsUtil.continuationForAllocation[(Int, V), ContextType](
            eps, state.callContext, data => (data._2, state.tac.stmts),
            _.isInstanceOf[(_, _)], data => failure(data._1)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = true
                )

                val matchers = Set(
                    MatcherUtil.constructorMatcher,
                    new ParameterTypesBasedMethodMatcher(ArraySeq.empty),
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt.map(Set(_)),
                        data._1,
                        v => new ClassBasedMethodMatcher(v, true)
                    )
                )

                addCalls(state.callContext, data._1, constructorReceiver(data._1), Seq.empty, matchers)
            }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(classRef, indirectCalls)
    }

    private[this] def handleNewInstance(
        callContext: ContextType,
        callPC:      Int,
        classExpr:   V,
        stmts:       Array[Stmt[V]]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Unit = {

        val matchers = Set(
            MatcherUtil.constructorMatcher,
            new ParameterTypesBasedMethodMatcher(ArraySeq.empty),
            MatcherUtil.retrieveClassBasedMethodMatcher(
                callContext,
                classExpr,
                callPC.asInstanceOf[Entity],
                callPC,
                stmts,
                project,
                () => failure(callPC),
                onlyMethodsExactlyInClass = true,
                onlyObjectTypes = true
            )
        )

        addCalls(callContext, callPC, constructorReceiver(callPC), Seq.empty, matchers)
    }

    private[this] def failure(
        callPC: Int
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Unit = {
        if (HighSoundnessMode) {
            val matchers: Set[MethodMatcher] = Set(
                MatcherUtil.constructorMatcher,
                new ParameterTypesBasedMethodMatcher(ArraySeq.empty)
            )
            addCalls(state.callContext, callPC, constructorReceiver(callPC), Seq.empty, matchers)
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }
    }
}

class ConstructorNewInstanceAnalysis private[analyses] (
        final val project: SomeProject
) extends ReflectionAnalysis with TypeConsumerAnalysis {

    private[this] val ConstructorT = ObjectType("java/lang/reflect/Constructor")

    override val apiMethod: DeclaredMethod = declaredMethods(
        ConstructorT,
        "",
        ConstructorT,
        "newInstance",
        MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
    )

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
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleConstructorNewInstance(
                callerContext, callPC, receiverOption.get.asVar, params, tac.stmts
            )
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull, indirectCalls)
    }

    def returnResult(
        constructor: V, indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(constructor, state)),
                results
            )
        else
            Results(results)
    }

    private type constructorDependerType = (Int, Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher])
    private type classDependerType = (Int, Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher], V, Array[Stmt[V]])

    def c(constructor: V, state: CGState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        implicit val _state: CGState[ContextType] = state

        AllocationsUtil.continuationForAllocation[constructorDependerType, ContextType](
            eps, state.callContext, _ => (constructor, state.tac.stmts),
            _.isInstanceOf[(_, _, _)], data => failure(data._1, data._3)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetConstructor(
                    allocationContext, data._1, allocationIndex, data._2, data._3, stmts
                )
                addCalls(
                    state.callContext, data._1,
                    constructorReceiver(data._1), data._2,
                    allMatchers
                )
            }

        AllocationsUtil.continuationForAllocation[classDependerType, ContextType](
            eps, state.callContext, data => (data._4, data._5),
            _.isInstanceOf[(_, _, _, _, _)], data => failure(data._1, data._3)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, data,
                    stmts, project, () => failure(data._1, data._3), onlyObjectTypes = true
                )

                val matchers = data._3 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes.asInstanceOf[Set[ObjectType]]),
                        data._1,
                        v => new ClassBasedMethodMatcher(v, true)
                    )

                addCalls(
                    state.callContext, data._1,
                    constructorReceiver(data._1), data._2,
                    matchers
                )
            }

        AllocationsUtil.continuationForAllocation[(classDependerType, V, Array[Stmt[V]]), ContextType](
            eps, state.callContext, data => (data._2, data._3),
            _.isInstanceOf[(_, _)], data => failure(data._1._1, data._1._3)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = true
                )

                val matchers = data._1._3 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt.map(Set(_)),
                        data._1._1,
                        v => new ClassBasedMethodMatcher(v, true)
                    )

                addCalls(
                    state.callContext, data._1._1,
                    constructorReceiver(data._1._1), data._1._2,
                    matchers
                )
            }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(constructor, indirectCalls)
    }

    private[this] def handleConstructorNewInstance(
        callContext:       ContextType,
        callPC:            Int,
        constructor:       V,
        newInstanceParams: Seq[Option[Expr[V]]],
        stmts:             Array[Stmt[V]]
    )(implicit state: CGState[ContextType], indirectCalls: IndirectCalls): Unit = {

        val actualParamsNewInstanceOpt =
            if (newInstanceParams.nonEmpty && newInstanceParams.head.isDefined)
                VarargsUtil.getParamsFromVararg(newInstanceParams.head.get, stmts)
            else None

        val baseMatchers = Set(
            MatcherUtil.constructorMatcher,
            MatcherUtil.retrieveSuitableNonEssentialMatcher[Seq[V]](
                actualParamsNewInstanceOpt,
                v => new ActualParameterBasedMethodMatcher(v)
            )
        )

        val persistentActualParams =
            actualParamsNewInstanceOpt.map(_.map(persistentUVar(_)(stmts))).getOrElse(Seq.empty)

        val depender: constructorDependerType = (callPC, persistentActualParams, baseMatchers)

        AllocationsUtil.handleAllocations(
            constructor, callContext, depender, state.tac.stmts, _ eq ObjectType.Constructor, () => {
            if (HighSoundnessMode) {
                addCalls(
                    callContext, callPC,
                    constructorReceiver(callPC), persistentActualParams,
                    baseMatchers + AllMethodsMatcher
                )
            } else {
                indirectCalls.addIncompleteCallSite(callPC)
            }
        }
        ) { (allocationContext, allocationIndex, stmts) =>
            val allMatchers = handleGetConstructor(
                allocationContext, callPC, allocationIndex, persistentActualParams, baseMatchers, stmts
            )
            addCalls(
                callContext, callPC,
                constructorReceiver(callPC), persistentActualParams,
                allMatchers
            )
        }
    }

    private[this] def handleGetConstructor(
        context:            ContextType,
        callPC:             Int,
        constructorDefSite: Int,
        actualParams:       Seq[Option[(ValueInformation, IntTrieSet)]],
        baseMatchers:       Set[MethodMatcher],
        stmts:              Array[Stmt[V]]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Set[MethodMatcher] = {
        var matchers = baseMatchers
        stmts(constructorDefSite).asAssignment.expr match {
            case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getConstructor" | "getDeclaredConstructor", _, receiver, params) =>

                if (call.name == "getConstructor") {
                    matchers += PublicMethodMatcher
                }

                matchers += MatcherUtil.retrieveParameterTypesBasedMethodMatcher(
                    params.head, callPC, stmts
                )

                if (!matchers.contains(NoMethodsMatcher))
                    matchers += MatcherUtil.retrieveClassBasedMethodMatcher(
                        context,
                        receiver.asVar,
                        (callPC, actualParams, matchers, receiver.asVar, stmts),
                        callPC,
                        stmts,
                        project,
                        () => failure(callPC, matchers),
                        onlyMethodsExactlyInClass = true,
                        onlyObjectTypes = true
                    )

            /*
             * TODO: case ArrayLoad(_, _, arrayRef) => // here we could handle getConstructors
             */

            case _ =>
                if (HighSoundnessMode) {
                    matchers += AllMethodsMatcher
                } else {
                    indirectCalls.addIncompleteCallSite(callPC)
                    matchers += NoMethodsMatcher
                }
        }

        matchers
    }

    private[this] def failure(
        callPC: Int, baseMatchers: Set[MethodMatcher]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Unit = {
        if (HighSoundnessMode) {
            addCalls(
                state.callContext, callPC,
                constructorReceiver(callPC), Seq.empty,
                baseMatchers + AllMethodsMatcher
            )
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }
    }
}

class MethodInvokeAnalysis private[analyses] (
        final val project: SomeProject
) extends ReflectionAnalysis with TypeConsumerAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Method,
        "",
        ObjectType.Method,
        "invoke",
        MethodDescriptor.apply(
            ArraySeq(ObjectType.Object, ArrayType.ArrayOfObject), ObjectType.Object
        )
    )

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
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleMethodInvoke(callerContext, callPC, receiverOption.get.asVar, params, tac.stmts)
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull, indirectCalls)
    }

    def returnResult(
        methodVar: V, indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(methodVar, state)),
                results
            )
        else
            Results(results)
    }

    private type methodDependerType = (Int, Option[(ValueInformation, IntTrieSet)], Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher])
    private type nameDependerType = (Int, Option[(ValueInformation, IntTrieSet)], Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher], V, Array[Stmt[V]], V, ContextType)
    private type classDependerType = (Int, Option[(ValueInformation, IntTrieSet)], Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher], V, Array[Stmt[V]])

    def c(methodVar: V, state: CGState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        implicit val _state: CGState[ContextType] = state

        AllocationsUtil.continuationForAllocation[methodDependerType, ContextType](
            eps, state.callContext, _ => (methodVar, state.tac.stmts),
            _.isInstanceOf[(_, _, _, _)], data => failure(data._1, data._2, data._3, data._4)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetMethod(
                    allocationContext, data._1, allocationIndex, data._2, data._3, data._4, stmts
                )
                addCalls(state.callContext, data._1, _ => data._2, data._3, allMatchers)
            }

        AllocationsUtil.continuationForAllocation[nameDependerType, ContextType](
            eps, state.callContext, data => (data._5, data._6),
            _.isInstanceOf[(_, _, _, _, _, _, _, _)],
            data => failure(data._1, data._2, data._3, data._4)
        ) { (data, _, allocationIndex, stmts) =>
                val name = StringUtil.getString(allocationIndex, stmts)

                val nameMatcher = retrieveSuitableMatcher[Set[String]](
                    name.map(Set(_)),
                    data._1,
                    v => new NameBasedMethodMatcher(v)
                )

                if (nameMatcher ne NoMethodsMatcher) {
                    val matchers = data._4 + nameMatcher
                    val allMatchers = matchers +
                        MatcherUtil.retrieveClassBasedMethodMatcher(
                            data._8,
                            data._7,
                            (data._1, data._2, data._3, matchers, data._7, data._6),
                            data._1,
                            stmts,
                            project,
                            () => failure(data._1, data._2, data._3, matchers),
                            onlyMethodsExactlyInClass = !data._4.contains(PublicMethodMatcher)
                        )

                    addCalls(state.callContext, data._1, _ => data._2, data._3, allMatchers)
                }
            }

        AllocationsUtil.continuationForAllocation[classDependerType, ContextType](
            eps, state.callContext, data => (data._5, data._6),
            _.isInstanceOf[(_, _, _, _, _, _)], data => failure(data._1, data._2, data._3, data._4)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, data,
                    stmts, project, () => failure(data._1, data._2, data._3, data._4),
                    onlyObjectTypes = false
                )

                val matchers = data._4 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes.map {
                            tpe => if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object
                        }),
                        data._1,
                        v => new ClassBasedMethodMatcher(v, !data._4.contains(PublicMethodMatcher))
                    )

                addCalls(state.callContext, data._1, _ => data._2, data._3, matchers)
            }

        AllocationsUtil.continuationForAllocation[(classDependerType, V, Array[Stmt[V]]), ContextType](
            eps, state.callContext, data => (data._2, data._3),
            _.isInstanceOf[(_, _)], data => failure(data._1._1, data._1._2, data._1._3, data._1._4)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = false
                )

                val matchers = data._1._4 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt.map(Set(_)),
                        data._1._1,
                        v => new ClassBasedMethodMatcher(v, !data._1._4.contains(PublicMethodMatcher))
                    )

                addCalls(state.callContext, data._1._1, _ => data._1._2, data._1._3, matchers)
            }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(methodVar, indirectCalls)
    }

    private[this] def handleMethodInvoke(
        callContext:  ContextType,
        callPC:       Int,
        method:       V,
        methodParams: Seq[Option[Expr[V]]],
        stmts:        Array[Stmt[V]]
    )(implicit state: CGState[ContextType], indirectCalls: IndirectCalls): Unit = {
        val (methodInvokeReceiver, methodInvokeActualParamsOpt) = if (methodParams.size == 2) {
            (
                methodParams.head.map(_.asVar),
                methodParams(1).flatMap(p => VarargsUtil.getParamsFromVararg(p, stmts))
            )
        } else {
            (None, None)
        }

        val baseMatchers = Set(
            MatcherUtil.retrieveSuitableMatcher[Seq[V]](
                methodInvokeActualParamsOpt,
                callPC,
                v => new ActualParameterBasedMethodMatcher(v)
            ),
            MatcherUtil.retrieveSuitableMatcher[V](
                methodInvokeReceiver,
                callPC,
                v => new ActualReceiverBasedMethodMatcher(v.value.asReferenceValue)
            )
        )

        val persistentReceiver = methodInvokeReceiver.flatMap(r => persistentUVar(r)(stmts))
        val persistentActualParams =
            methodInvokeActualParamsOpt.map(_.map(persistentUVar(_)(stmts))).getOrElse(Seq.empty)

        val depender: methodDependerType =
            (callPC, persistentReceiver, persistentActualParams, baseMatchers)

        AllocationsUtil.handleAllocations(
            method, callContext, depender, state.tac.stmts, _ eq ObjectType.Method,
            () => failure(callPC, persistentReceiver, persistentActualParams, baseMatchers)
        ) { (allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetMethod(
                    allocationContext, callPC, allocationIndex,
                    persistentReceiver, persistentActualParams,
                    baseMatchers, stmts
                )
                addCalls(
                    callContext, callPC,
                    _ => persistentReceiver, persistentActualParams,
                    allMatchers
                )
            }
    }

    private[this] def handleGetMethod(
        context:        ContextType,
        callPC:         Int,
        methodDefSite:  Int,
        actualReceiver: Option[(ValueInformation, IntTrieSet)],
        actualParams:   Seq[Option[(ValueInformation, IntTrieSet)]],
        baseMatchers:   Set[MethodMatcher],
        stmts:          Array[Stmt[V]]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Set[MethodMatcher] = {
        var matchers = baseMatchers
        stmts(methodDefSite).asAssignment.expr match {
            case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredMethod" | "getMethod", _, receiver, params) =>

                matchers += MatcherUtil.retrieveParameterTypesBasedMethodMatcher(
                    params(1), callPC, stmts
                )

                val isGetMethod = call.name == "getMethod"

                if (isGetMethod)
                    matchers += PublicMethodMatcher

                if (!matchers.contains(NoMethodsMatcher))
                    matchers += MatcherUtil.retrieveNameBasedMethodMatcher(
                        context,
                        params.head.asVar,
                        (
                            callPC,
                            actualReceiver,
                            actualParams,
                            matchers,
                            params.head.asVar,
                            stmts,
                            receiver.asVar,
                            context
                        ),
                        callPC,
                        stmts,
                        () => failure(callPC, actualReceiver, actualParams, matchers)
                    )

                if (!matchers.contains(NoMethodsMatcher))
                    matchers += MatcherUtil.retrieveClassBasedMethodMatcher(
                        context,
                        receiver.asVar,
                        (callPC, actualReceiver, actualParams, matchers, receiver.asVar, stmts),
                        callPC,
                        stmts,
                        project,
                        () => failure(callPC, actualReceiver, actualParams, matchers),
                        onlyMethodsExactlyInClass = !isGetMethod
                    )

            /*case ArrayLoad(_, _, arrayRef) =>*/
            // TODO here we can handle getMethods

            case _ =>
                if (HighSoundnessMode) {
                    matchers += AllMethodsMatcher
                } else {
                    indirectCalls.addIncompleteCallSite(callPC)
                    matchers += NoMethodsMatcher
                }
        }

        matchers
    }

    private[this] def failure(
        callPC:       Int,
        receiver:     Option[(ValueInformation, IntTrieSet)],
        params:       Seq[Option[(ValueInformation, IntTrieSet)]],
        baseMatchers: Set[MethodMatcher]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Unit = {
        if (HighSoundnessMode) {
            addCalls(
                state.callContext, callPC,
                _ => receiver, params,
                baseMatchers + AllMethodsMatcher
            )
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }
    }
}

class MethodHandleInvokeAnalysis private[analyses] (
        final val project:                SomeProject,
        final override val apiMethod:     DeclaredMethod,
        final val isSignaturePolymorphic: Boolean
) extends ReflectionAnalysis with TypeConsumerAnalysis {

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
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        implicit val state: CGState[ContextType] = new CGState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            val descriptorOpt = if (isDirect && apiMethod.name == "invokeExact") {
                (tac.stmts(tac.properStmtIndexForPC(callPC)): @unchecked) match {
                    case vmc: VirtualMethodCall[V]          => Some(vmc.descriptor)
                    case VirtualFunctionCallStatement(call) => Some(call.descriptor)
                }
            } else {
                None
            }
            handleMethodHandleInvoke(
                callerContext,
                callPC,
                receiverOption.get.asVar,
                params,
                descriptorOpt,
                isSignaturePolymorphic,
                tac.stmts
            )
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull, indirectCalls)
    }

    def returnResult(
        methodHandle: V, indirectCalls: IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {
        val results = indirectCalls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(methodHandle, state)),
                results
            )
        else
            Results(results)
    }

    private type methodHandleDependerType = (Int, Option[MethodDescriptor], Option[Seq[Option[V]]], Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher])
    private type nameDependerType = (Int, Boolean, Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher], V, Array[Stmt[V]], V, ContextType)
    private type classDependerType = (Int, Boolean, Seq[Option[(ValueInformation, IntTrieSet)]], Set[MethodMatcher], V, Array[Stmt[V]])

    def c(methodHandle: V, state: CGState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        implicit val _state: CGState[ContextType] = state

        AllocationsUtil.continuationForAllocation[methodHandleDependerType, ContextType](
            eps, state.callContext, _ => (methodHandle, state.tac.stmts),
            _.isInstanceOf[(_, _, _, _, _)], data => failure(data._1, data._4, data._5)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetMethodHandle(
                    allocationContext, data._1, allocationIndex, data._2, data._3, data._4, data._5, stmts
                )
                addCalls(state.callContext, data._1, allMatchers, data._4)
            }

        AllocationsUtil.continuationForAllocation[nameDependerType, ContextType](
            eps, state.callContext, data => (data._5, data._6),
            _.isInstanceOf[(_, _, _, _, _, _, _, _)], data => failure(data._1, data._3, data._4)
        ) { (data, _, allocationIndex, stmts) =>
                val name = StringUtil.getString(allocationIndex, stmts)

                val nameMatcher = retrieveSuitableMatcher[Set[String]](
                    name.map(Set(_)),
                    data._1,
                    v => new NameBasedMethodMatcher(v)
                )

                if (nameMatcher ne NoMethodsMatcher) {
                    val matchers = data._4 + nameMatcher
                    val allMatchers = matchers +
                        MatcherUtil.retrieveClassBasedMethodMatcher(
                            data._8,
                            data._7,
                            (data._1, data._3, matchers, data._7, data._6),
                            data._1,
                            stmts,
                            project,
                            () => failure(data._1, data._3, matchers),
                            onlyMethodsExactlyInClass = false,
                            considerSubclasses = data._2
                        )

                    addCalls(state.callContext, data._1, allMatchers, data._3)
                }
            }

        AllocationsUtil.continuationForAllocation[classDependerType, ContextType](
            eps, state.callContext, data => (data._5, data._6),
            _.isInstanceOf[(_, _, _, _, _, _)], data => failure(data._1, data._3, data._4)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, data,
                    stmts, project, () => failure(data._1, data._3, data._4),
                    onlyObjectTypes = false
                ).flatMap { tpe =>
                    if (data._2) project.classHierarchy.allSubtypes(tpe.asObjectType, true)
                    else Set(if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object)
                }

                val matchers = data._4 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes),
                        data._1,
                        v => new ClassBasedMethodMatcher(v, false)
                    )

                addCalls(state.callContext, data._1, matchers, data._3)
            }

        AllocationsUtil.continuationForAllocation[(classDependerType, V, Array[Stmt[V]]), ContextType](
            eps, state.callContext, data => (data._2, data._3),
            _.isInstanceOf[(_, _)], data => failure(data._1._1, data._1._3, data._1._4)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = false
                ).map { tpe =>
                    if (data._1._2) project.classHierarchy.allSubtypes(tpe.asObjectType, true)
                    else Set(if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object)
                }

                val matchers = data._1._4 +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt,
                        data._1._1,
                        v => new ClassBasedMethodMatcher(v, false)
                    )

                addCalls(state.callContext, data._1._1, matchers, data._1._3)
            }

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(methodHandle, indirectCalls)
    }

    private[this] def failure(
        callPC:       Int,
        params:       Seq[Option[(ValueInformation, IntTrieSet)]],
        baseMatchers: Set[MethodMatcher]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Unit = {
        if (HighSoundnessMode) {
            addCalls(state.callContext, callPC, baseMatchers + AllMethodsMatcher, params)
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
        }
    }

    private[this] def handleMethodHandleInvoke(
        callContext:            ContextType,
        callPC:                 Int,
        methodHandle:           V,
        invokeParams:           Seq[Option[Expr[V]]],
        descriptorOpt:          Option[MethodDescriptor],
        isSignaturePolymorphic: Boolean,
        stmts:                  Array[Stmt[V]]
    )(implicit state: CGState[ContextType], indirectCalls: IndirectCalls): Unit = {
        // IMPROVE: for signature polymorphic calls, we could also use the method descriptor (return type)
        val actualInvokeParamsOpt =
            if (isSignaturePolymorphic) Some(invokeParams.map(_.map(_.asVar)))
            else if (invokeParams.nonEmpty)
                invokeParams.head.flatMap(p => VarargsUtil.getParamsFromVararg(p, stmts).map(_.map(Some(_))))
            else
                None

        // TODO here we need to peel of the 1. actual parameter for non static ones
        val baseMatchers = Set.empty[MethodMatcher] /*Set(
                retrieveSuitableNonEssentialMatcher[Seq[V]](
                    actualInvokeParamsOpt,
                    v => new ActualParamBasedMethodMatcher(v, project)
                )
            )*/

        val persistentActualParams = actualInvokeParamsOpt.map(_.map(
            _.flatMap(persistentUVar(_)(stmts))
        )).getOrElse(Seq.empty)

        val depender: methodHandleDependerType =
            (callPC, descriptorOpt, actualInvokeParamsOpt, persistentActualParams, baseMatchers)

        AllocationsUtil.handleAllocations(
            methodHandle, callContext, depender, state.tac.stmts,
            project.classHierarchy.isASubtypeOf(_, ObjectType.MethodHandle).isYesOrUnknown,
            () => failure(callPC, persistentActualParams, baseMatchers)
        ) {
                (allocationContext, allocationIndex, stmts) =>
                    val allMatchers = handleGetMethodHandle(
                        allocationContext,
                        callPC,
                        allocationIndex,
                        descriptorOpt,
                        actualInvokeParamsOpt,
                        persistentActualParams,
                        baseMatchers,
                        stmts
                    )
                    addCalls(callContext, callPC, allMatchers, persistentActualParams)
            }
    }

    private[this] def handleGetMethodHandle(
        context:                ContextType,
        callPC:                 Int,
        methodHandleDefSite:    Int,
        descriptorOpt:          Option[MethodDescriptor],
        actualParams:           Option[Seq[Option[V]]],
        persistentActualParams: Seq[Option[(ValueInformation, IntTrieSet)]],
        baseMatchers:           Set[MethodMatcher],
        stmts:                  Array[Stmt[V]]
    )(implicit indirectCalls: IndirectCalls, state: CGState[ContextType]): Set[MethodMatcher] = {
        var matchers = baseMatchers

        val definition = stmts(methodHandleDefSite).asAssignment.expr

        if (definition.isMethodHandleConst) {
            // TODO do we need to distinguish the cases below?
            definition.asMethodHandleConst.value match {
                case InvokeStaticMethodHandle(receiver, _, name, desc) =>
                    matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, None, isVirtual = false, isStatic = true, isConstructor = false)

                case InvokeVirtualMethodHandle(receiver, name, desc) =>
                    val actualReceiverTypes: Option[Set[ObjectType]] =
                        if (actualParams.isDefined && actualParams.get.nonEmpty && actualParams.get.head.isDefined) {
                            val rcvr = actualParams.get.head.get.value.asReferenceValue
                            Some(
                                if (rcvr.isNull.isYes) Set.empty[ObjectType]
                                else if (rcvr.leastUpperType.get.isArrayType) Set(ObjectType.Object)
                                else if (rcvr.isPrecise) Set(rcvr.leastUpperType.get.asObjectType)
                                else project.classHierarchy.allSubtypes(rcvr.leastUpperType.get.asObjectType, true)
                            )
                        } else
                            None
                    matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, actualReceiverTypes, isVirtual = true, isStatic = false, isConstructor = false)

                case InvokeInterfaceMethodHandle(receiver, name, desc) =>
                    matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, None, isVirtual = false, isStatic = false, isConstructor = false)

                case InvokeSpecialMethodHandle(receiver, _, name, desc) =>
                    // TODO does this work for super?
                    matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, None, isVirtual = false, isStatic = false, isConstructor = false)

                case NewInvokeSpecialMethodHandle(receiver, desc) =>
                    matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, "<init>", desc, None, isVirtual = false, isStatic = false, isConstructor = true)

                case _ => // getters and setters are not relevant for the call graph
                    matchers += NoMethodsMatcher
            }
        } else if (definition.isVirtualFunctionCall) {
            val methodHandleData = definition.asVirtualFunctionCall match {
                case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findStatic", _, _, params) =>
                    val Seq(refc, name, methodType) = params
                    Some((refc.asVar, name.asVar, methodType, false, true, false))

                case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findVirtual", _, _, params) =>
                    val Seq(refc, name, methodType) = params
                    Some((refc.asVar, name.asVar, methodType, true, false, false))

                case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findSpecial", _, _, params) =>
                    // TODO we can ignore the 4ths param? Does it work for super calls?
                    val Seq(refc, name, methodType, _) = params
                    Some((refc.asVar, name.asVar, methodType, false, false, false))

                case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findConstructor", _, _, params) =>
                    val Seq(refc, methodType) = params
                    Some((refc.asVar, null, methodType, false, false, true))

                case _ =>
                    None // getters and setters are not relevant for the call graph
            }
            if (methodHandleData.isDefined) {
                val (refc, name, methodType, isVirtual, isStatic, isConstructor) = methodHandleData.get
                matchers += (if (isStatic) StaticMethodMatcher else NonStaticMethodMatcher)
                matchers += retrieveDescriptorBasedMethodMatcher(
                    descriptorOpt, methodType, isConstructor, stmts, project
                )
                if (!matchers.contains(NoMethodsMatcher))
                    matchers +=
                        (if (isConstructor) MatcherUtil.constructorMatcher
                        else MatcherUtil.retrieveNameBasedMethodMatcher(
                            context,
                            name,
                            (callPC, isVirtual, persistentActualParams, matchers, name, stmts, refc, context),
                            callPC,
                            stmts,
                            () => failure(callPC, persistentActualParams, matchers)
                        ))
                if (!matchers.contains(NoMethodsMatcher))
                    if (isVirtual) {
                        val receiverTypes =
                            if (actualParams.isDefined && actualParams.get.nonEmpty && actualParams.get.head.isDefined) {
                                val rcvr = actualParams.get.head.get.value.asReferenceValue
                                Some(
                                    if (rcvr.isNull.isYes) Set.empty[ObjectType]
                                    else if (rcvr.leastUpperType.get.isArrayType) Set(ObjectType.Object)
                                    else if (rcvr.isPrecise) Set(rcvr.leastUpperType.get.asObjectType)
                                    else project.classHierarchy.allSubtypes(rcvr.leastUpperType.get.asObjectType, true)
                                )
                            } else None
                        if (receiverTypes.isDefined)
                            matchers += new ClassBasedMethodMatcher(
                                receiverTypes.get,
                                onlyMethodsExactlyInClass = false
                            )
                        else
                            matchers += MatcherUtil.retrieveClassBasedMethodMatcher(
                                context,
                                refc,
                                (callPC, isVirtual, persistentActualParams, matchers, refc, stmts),
                                callPC,
                                stmts,
                                project,
                                () => failure(callPC, persistentActualParams, matchers),
                                onlyMethodsExactlyInClass = false,
                                considerSubclasses = isVirtual
                            )
                    }
            } else
                matchers += NoMethodsMatcher
        } else if (HighSoundnessMode) {
            if (descriptorOpt.isDefined) {
                // we do not know, whether the invoked method is static or not
                // (i.e. whether the first parameter of the descriptor represent the receiver)
                val md = descriptorOpt.get
                if (md.parametersCount > 0) {
                    val nonStaticDescriptor = MethodDescriptor(
                        md.parameterTypes.tail, md.returnType
                    )
                    matchers += new DescriptorBasedMethodMatcher(
                        Set(md, nonStaticDescriptor)
                    )
                } else {
                    matchers += new DescriptorBasedMethodMatcher(
                        Set(md)
                    )
                }
            }
            matchers += AllMethodsMatcher
        } else {
            matchers += NoMethodsMatcher
            indirectCalls.addIncompleteCallSite(callPC)
        }
        // TODO we should use the descriptor here

        matchers
    }

    private[this] def addCalls(
        callContext:            ContextType,
        callPC:                 Int,
        matchers:               Set[MethodMatcher],
        persistentActualParams: Seq[Option[(ValueInformation, IntTrieSet)]]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        // TODO refactor this handling
        MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m =>
            val (receiver, params) = if (m.isStatic || persistentActualParams.isEmpty)
                (None, persistentActualParams)
            else
                (persistentActualParams.head, persistentActualParams.tail)
            indirectCalls.addCall(
                callContext,
                callPC,
                typeProvider.expandContext(callContext, declaredMethods(m), callPC),
                // TODO: is this sufficient?
                params,
                receiver
            )
        }
    }
}

object ReflectionRelatedCallsAnalysis {

    final val ConfigKey = {
        "org.opalj.fpcf.analyses.cg.reflection.ReflectionRelatedCallsAnalysis.highSoundness"
    }

}

/**
 * Handles the effect of serialization to the call graph.
 * As an example models the invocation of constructors when `readObject` is called, if there is a
 * cast afterwards.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class ReflectionRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    def process(p: SomeProject): PropertyComputationResult = {
        val declaredMethods = project.get(DeclaredMethodsKey)

        val analyses = List(
            /*
             * Class.forName
             */
            new ClassForNameAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(ObjectType.String, ObjectType.Class)
                )
            ),
            new ClassForNameAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(
                        ArraySeq(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")),
                        ObjectType.Class
                    )
                )
            ),
            new ClassForNameAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(
                        ArraySeq(ObjectType("java/lang/Module"), ObjectType.String),
                        ObjectType.Class
                    )
                ),
                classNameIndex = 1
            ),

            /*
             * Class.newInstance
             */
            new ClassNewInstanceAnalysis(project),

            /*
             * Constructor.newInstance
             */
            new ConstructorNewInstanceAnalysis(project),

            /*
             * Method.invoke
             */
            new MethodInvokeAnalysis(project),

            /*
             * MethodHandle.invoke//invokeExact//invokeWithArguments
             */
            new MethodHandleInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandle,
                    "",
                    ObjectType.MethodHandle,
                    "invoke",
                    MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                ),
                isSignaturePolymorphic = true
            ),
            new MethodHandleInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandle,
                    "",
                    ObjectType.MethodHandle,
                    "invokeExact",
                    MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                ),
                isSignaturePolymorphic = true
            ),
            new MethodHandleInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandle,
                    "",
                    ObjectType.MethodHandle,
                    "invokeWithArguments",
                    MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                ),
                isSignaturePolymorphic = false
            ),
            new MethodHandleInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandle,
                    "",
                    ObjectType.MethodHandle,
                    "invokeWithArguments",
                    MethodDescriptor(ObjectType("java/util/List"), ObjectType.Object)
                ),
                isSignaturePolymorphic = false
            )
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

object ReflectionRelatedCallsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, ProjectIndexKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        LoadedClasses,
        TACAI
    )

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = {
        p.get(TypeProviderKey).usedPropertyKinds
    }

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        LoadedClasses
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReflectionRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}
