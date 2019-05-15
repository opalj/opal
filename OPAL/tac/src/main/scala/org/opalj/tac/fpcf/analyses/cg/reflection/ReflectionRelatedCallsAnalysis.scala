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
import org.opalj.collection.immutable.RefArray
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.LoadedClasses
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.cfg.CFG
import org.opalj.br.InvokeInterfaceMethodHandle
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.InvokeVirtualMethodHandle
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.BooleanType
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI

sealed trait TypeAndStringMagic extends TACAIBasedAPIBasedCallGraphAnalysis {

    implicit final val HighSoundnessMode: Boolean = {
        val activated = try {
            project.config.getBoolean(ReflectionRelatedCallsAnalysis.ConfigKey)
        } catch {
            case t: Throwable ⇒
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

    def addCalls(
        caller:         DefinedMethod,
        pc:             Int,
        actualReceiver: Option[(ValueInformation, IntTrieSet)],
        actualParams:   Seq[Option[(ValueInformation, IntTrieSet)]], matchers: Traversable[MethodMatcher]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m ⇒
            indirectCalls.addCall(
                caller,
                declaredMethods(m),
                pc,
                actualParams,
                actualReceiver
            )
        }
    }
}

class ClassForNameAnalysis private[analyses] (
        final val project: SomeProject, final override val apiMethod: DeclaredMethod
) extends TypeAndStringMagic {

    private class State(
            private[this] val loadedClassesUB: UIDSet[ObjectType]
    ) {
        private[this] var _newLoadedClasses: UIDSet[ObjectType] = UIDSet.empty

        private[cg] def addNewLoadedClasses(loadedClasses: TraversableOnce[ObjectType]): Unit = {
            _newLoadedClasses ++= loadedClasses.filter(!loadedClassesUB.contains(_))
        }

        def hasNewLoadedClasses: Boolean = _newLoadedClasses.nonEmpty

        def loadedClassesPartialResult: PartialResult[SomeProject, LoadedClasses] = {
            assert(hasNewLoadedClasses)
            PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case InterimEUBP(p, ub) ⇒
                    val newUb = ub.classes ++ _newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(InterimEUBP(p, ub.updated(_newLoadedClasses)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(InterimEUBP(p, LoadedClasses(_newLoadedClasses)))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
        }
    }

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val incompleteCallSites: IncompleteCallSites = new IncompleteCallSites {}

        implicit val state: State = new State(loadedClassesUB())

        if (params.nonEmpty && params.head.isDefined)
            handleForName(params.head.get, pc, tac.stmts)
        else
            incompleteCallSites.addIncompleteCallSite(pc)

        if (state.hasNewLoadedClasses) {
            Results(state.loadedClassesPartialResult, incompleteCallSites.partialResults(caller))
        } else {
            Results(incompleteCallSites.partialResults(caller))
        }
    }

    /**
     * Retrieves the current state of loaded classes and instantiated types from the property store.
     */
    private[this] def loadedClassesUB(): UIDSet[ObjectType] = {
        // the set of classes that are definitely loaded at this point in time
        val loadedClassesEOptP = propertyStore(project, LoadedClasses.key)

        // the upper bound for loaded classes, seen so far
        val loadedClassesUB: UIDSet[ObjectType] = loadedClassesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.classes
            case _              ⇒ UIDSet.empty
        }

        loadedClassesUB
    }

    /**
     * Adds classes that can be loaded by an invocation of Class.forName to the set of loaded
     * classes.
     */
    private[this] def handleForName(
        className: Expr[V], pc: Int, stmts: Array[Stmt[V]]
    )(implicit state: State, incompleteCallSites: IncompleteCallSites): Unit = {
        val loadedClassesOpt = TypesUtil.getPossibleForNameClasses(className, None, stmts, project)
        if (loadedClassesOpt.isEmpty) {
            if (HighSoundnessMode) {
                state.addNewLoadedClasses(
                    p.allClassFiles.iterator.map(_.thisType)
                )
            } else {
                incompleteCallSites.addIncompleteCallSite(pc)
            }
        } else {
            state.addNewLoadedClasses(
                loadedClassesOpt.get
            )
        }
    }
}

class ClassNewInstanceAnalysis private[analyses] (
        final val project: SomeProject
) extends TypeAndStringMagic {
    override val apiMethod: DeclaredMethod =
        declaredMethods(
            ObjectType.Class,
            "",
            ObjectType.Class,
            "newInstance",
            MethodDescriptor.JustReturnsObject
        )

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              UShort,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        if (receiverOption.isDefined)
            handleNewInstance(caller, pc, receiverOption.get, tac.stmts)
        else
            indirectCalls.addIncompleteCallSite(pc)

        Results(indirectCalls.partialResults(caller))
    }

    private[this] def handleNewInstance(
        caller:    DefinedMethod,
        pc:        Int,
        classExpr: Expr[V],
        stmts:     Array[Stmt[V]]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        var matchers: Set[MethodMatcher] = Set(
            MatcherUtil.constructorMatcher,
            new ParameterTypesBasedMethodMatcher(RefArray.empty)
        )

        matchers += MatcherUtil.retrieveClassBasedMethodMatcher(classExpr, pc, stmts, project)

        addCalls(caller, pc, None, Seq.empty, matchers)
    }
}

class ConstructorNewInstanceAnalysis private[analyses] (
        final val project: SomeProject
) extends TypeAndStringMagic {

    private[this] val ConstructorT = ObjectType("java/lang/reflect/Constructor")

    override val apiMethod: DeclaredMethod = declaredMethods(
        ConstructorT,
        "",
        ConstructorT,
        "newInstance",
        MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
    )

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              UShort,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        if (receiverOption.isDefined)
            handleConstructorNewInstance(caller, pc, receiverOption.get, params, tac.stmts, tac.cfg)
        else
            indirectCalls.addIncompleteCallSite(pc)

        Results(indirectCalls.partialResults(caller))
    }

    private[this] def handleConstructorNewInstance(
        caller:            DefinedMethod,
        pc:                Int,
        constructor:       Expr[V],
        newInstanceParams: Seq[Option[Expr[V]]],
        stmts:             Array[Stmt[V]],
        cfg:               CFG[Stmt[V], TACStmts[V]]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        val actualParamsNewInstanceOpt =
            if (newInstanceParams.nonEmpty && newInstanceParams.head.isDefined)
                VarargsUtil.getParamsFromVararg(newInstanceParams.head.get, stmts, cfg)
            else None
        val persistentActualParams =
            actualParamsNewInstanceOpt.map(_.map(persistentUVar(_)(stmts))).getOrElse(Seq.empty)

        constructor.asVar.definedBy.foreach { index ⇒
            var matchers: Set[MethodMatcher] = Set(
                MatcherUtil.constructorMatcher,
                MatcherUtil.retrieveSuitableNonEssentialMatcher[Seq[V]](
                    actualParamsNewInstanceOpt,
                    v ⇒ new ActualParamBasedMethodMatcher(v)
                )
            )

            if (index > 0) {
                stmts(index).asAssignment.expr match {
                    case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getConstructor" | "getDeclaredConstructor", _, receiver, params) ⇒

                        if (call.name == "getConstructor") {
                            matchers += PublicMethodMatcher
                        }
                        matchers += MatcherUtil.retrieveParameterTypesBasedMethodMatcher(params.head, pc, stmts, cfg)
                        matchers += MatcherUtil.retrieveClassBasedMethodMatcher(receiver, pc, stmts, project)

                    /*
                     * TODO: case ArrayLoad(_, _, arrayRef) ⇒ // here we could handle getConstructors
                     */

                    case _ ⇒
                        if (HighSoundnessMode) {
                            matchers += AllMethodsMatcher
                        } else {
                            indirectCalls.addIncompleteCallSite(pc)
                            matchers += NoMethodsMatcher
                        }
                }
            } else if (HighSoundnessMode) {
                matchers += AllMethodsMatcher
            } else {
                indirectCalls.addIncompleteCallSite(pc)
                matchers += NoMethodsMatcher
            }

            addCalls(caller, pc, None, persistentActualParams, matchers)
        }
    }
}

class MethodInvokeAnalysis private[analyses] (
        final val project: SomeProject
) extends TypeAndStringMagic {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Method,
        "",
        ObjectType.Method,
        "invoke",
        MethodDescriptor.apply(
            RefArray(ObjectType.Object, ArrayType.ArrayOfObject), ObjectType.Object
        )
    )

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              UShort,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        if (receiverOption.isDefined)
            handleMethodInvoke(caller, pc, receiverOption.get, params, tac.stmts, tac.cfg)
        else
            indirectCalls.addIncompleteCallSite(pc)

        Results(indirectCalls.partialResults(caller))
    }

    private[this] def handleMethodInvoke(
        caller:       DefinedMethod,
        pc:           Int,
        method:       Expr[V],
        methodParams: Seq[Option[Expr[V]]],
        stmts:        Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        val (methodInvokeReceiver, methodInvokeActualParamsOpt) = if (methodParams.size == 2) {
            (
                methodParams.head.map(_.asVar),
                methodParams(1).flatMap(p ⇒ VarargsUtil.getParamsFromVararg(p, stmts, cfg))
            )
        } else {
            (None, None)
        }
        val persistentMethodInvokeReceiver = methodInvokeReceiver.flatMap(r ⇒ persistentUVar(r)(stmts))
        val persistentMethodInvokeActualParamsOpt =
            methodInvokeActualParamsOpt.map(_.map(persistentUVar(_)(stmts)))

        method.asVar.definedBy.foreach { index ⇒
            var matchers: Set[MethodMatcher] = Set(
                MatcherUtil.retrieveSuitableMatcher[Seq[V]](
                    methodInvokeActualParamsOpt,
                    pc,
                    v ⇒ new ActualParamBasedMethodMatcher(v)
                ),
                MatcherUtil.retrieveSuitableMatcher[V](
                    methodInvokeReceiver,
                    pc,
                    v ⇒ new ActualReceiverBasedMethodMatcher(v.value.asReferenceValue)
                )
            )

            if (index >= 0) {
                val definition = stmts(index).asAssignment.expr
                definition match {
                    case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredMethod" | "getMethod", _, receiver, params) ⇒
                        if (call.name == "getMethod") {
                            matchers += PublicMethodMatcher
                        }

                        matchers += MatcherUtil.retrieveNameBasedMethodMatcher(params.head, pc, stmts)

                        matchers += MatcherUtil.retrieveParameterTypesBasedMethodMatcher(params(1), pc, stmts, cfg)

                        matchers += MatcherUtil.retrieveClassBasedMethodMatcher(receiver, pc, stmts, project)

                    /*case ArrayLoad(_, _, arrayRef) ⇒*/
                    // TODO here we can handle getMethods

                    case _ ⇒
                        if (HighSoundnessMode) {
                            matchers += AllMethodsMatcher
                        } else {
                            indirectCalls.addIncompleteCallSite(pc)
                            matchers += NoMethodsMatcher
                        }
                }
            } else if (HighSoundnessMode) {
                matchers += AllMethodsMatcher
            } else {
                indirectCalls.addIncompleteCallSite(pc)
                matchers += NoMethodsMatcher
            }

            addCalls(
                caller,
                pc,
                persistentMethodInvokeReceiver,
                persistentMethodInvokeActualParamsOpt.getOrElse(Seq.empty),
                matchers
            )
        }
    }
}

class MethodHandleInvokeAnalysis private[analyses] (
        final val project:                SomeProject,
        final override val apiMethod:     DeclaredMethod,
        final val isSignaturePolymorphic: Boolean
) extends TypeAndStringMagic {

    /*override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.MethodHandle,
        "",
        ObjectType.MethodHandle,
        "invoke",
        MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
    )*/

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              UShort,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()
        val descriptorOpt = if (isDirect && apiMethod.name == "invokeExact") {
            tac.stmts(tac.pcToIndex(pc)) match {
                case vmc: VirtualMethodCall[V]          ⇒ Some(vmc.descriptor)
                case VirtualFunctionCallStatement(call) ⇒ Some(call.descriptor)
            }
        } else {
            None
        }
        handleMethodHandleInvoke(
            caller,
            pc,
            receiverOption.get,
            params,
            descriptorOpt,
            isSignaturePolymorphic,
            tac.stmts,
            tac.cfg
        )
        Results(indirectCalls.partialResults(caller))
    }

    private[this] def handleMethodHandleInvoke(
        caller:                 DefinedMethod,
        pc:                     Int,
        methodHandle:           Expr[V],
        invokeParams:           Seq[Option[Expr[V]]],
        descriptor:             Option[MethodDescriptor],
        isSignaturePolymorphic: Boolean,
        stmts:                  Array[Stmt[V]],
        cfg:                    CFG[Stmt[V], TACStmts[V]]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        // IMPROVE: for signature polymorphic calls, we could also use the method descriptor (return type)
        val actualInvokeParamsOpt =
            if (isSignaturePolymorphic) Some(invokeParams.map(_.map(_.asVar)))
            else if (invokeParams.nonEmpty)
                invokeParams.head.flatMap(p ⇒ VarargsUtil.getParamsFromVararg(p, stmts, cfg).map(_.map(Some(_))))
            else
                None

        methodHandle.asVar.definedBy.foreach { index ⇒
            // TODO here we need to peel of the 1. actual parameter for non static ones
            var matchers: Set[MethodMatcher] = Set.empty /*Set(
                retrieveSuitableNonEssentialMatcher[Seq[V]](
                    actualInvokeParamsOpt,
                    v ⇒ new ActualParamBasedMethodMatcher(v, project)
                )
            )*/

            if (index >= 0) {
                val definition = stmts(index).asAssignment.expr
                if (definition.isMethodHandleConst) {
                    // TODO do we need to distinguish the cases below?
                    definition.asMethodHandleConst.value match {
                        case InvokeStaticMethodHandle(receiver, _, name, desc) ⇒
                            matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = true, isConstructor = false)

                        case InvokeVirtualMethodHandle(receiver, name, desc) ⇒
                            matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case InvokeInterfaceMethodHandle(receiver, name, desc) ⇒
                            matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case InvokeSpecialMethodHandle(receiver, _, name, desc) ⇒
                            // TODO does this work for super?
                            matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case NewInvokeSpecialMethodHandle(receiver, desc) ⇒
                            matchers ++= MethodHandlesUtil.retrieveMatchersForMethodHandleConst(receiver, "<init>", desc, isStatic = false, isConstructor = true)

                        case _ ⇒
                        // getters and setters are not relevant for the call graph
                    }
                } else if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findStatic", _, _, params) ⇒
                            matchers += StaticMethodMatcher

                            val Seq(refc, name, methodType) = params
                            matchers ++= MethodHandlesUtil.retrieveMethodHandleLookupMatchers(
                                refc,
                                name,
                                methodType,
                                descriptor,
                                pc,
                                isStatic = true,
                                isConstructor = false,
                                stmts,
                                cfg,
                                project
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findVirtual", _, _, params) ⇒
                            matchers += NonStaticMethodMatcher

                            val Seq(refc, name, methodType) = params
                            matchers ++= MethodHandlesUtil.retrieveMethodHandleLookupMatchers(
                                refc,
                                name,
                                methodType,
                                descriptor,
                                pc,
                                isStatic = false,
                                isConstructor = false,
                                stmts,
                                cfg,
                                project
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findSpecial", _, _, params) ⇒
                            matchers += NonStaticMethodMatcher

                            // TODO we can ignore the 4ths param? Does it work for super calls?
                            val Seq(refc, name, methodType, _) = params
                            matchers ++= MethodHandlesUtil.retrieveMethodHandleLookupMatchers(
                                refc,
                                name,
                                methodType,
                                descriptor,
                                pc,
                                isStatic = false,
                                isConstructor = false,
                                stmts,
                                cfg,
                                project
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findConstructor", _, _, params) ⇒
                            matchers += NonStaticMethodMatcher

                            val Seq(refc, methodType) = params

                            matchers += MatcherUtil.constructorMatcher

                            matchers += MatcherUtil.retrieveClassBasedMethodMatcher(refc, pc, stmts, project)

                            matchers += MethodHandlesUtil.retrieveDescriptorBasedMethodMatcher(
                                descriptor,
                                methodType,
                                pc,
                                isStatic = false,
                                isConstructor = true,
                                stmts,
                                cfg,
                                project
                            )
                        case _ ⇒
                        // getters and setters are not relevant for the call graph
                    }
                } else if (HighSoundnessMode) {
                    if (descriptor.isDefined) {
                        // we do not know, whether the invoked method is static or not
                        // (i.e. whether the first parameter of the descriptor represent the receiver)
                        val md = descriptor.get
                        val nonStaticDescriptor = MethodDescriptor(
                            md.parameterTypes.tail, md.returnType
                        )
                        matchers += new DescriptorBasedMethodMatcher(
                            Set(md, nonStaticDescriptor)
                        )
                    }
                    matchers += AllMethodsMatcher
                } else {
                    matchers += NoMethodsMatcher
                    indirectCalls.addIncompleteCallSite(pc)
                }
                // TODO we should use the descriptor here
            } else if (HighSoundnessMode) {
                matchers += AllMethodsMatcher
            } else {
                matchers += NoMethodsMatcher
                indirectCalls.addIncompleteCallSite(pc)
            }

            val persistentActualParams =
                actualInvokeParamsOpt.map(_.map(_.flatMap(persistentUVar(_)(stmts)))).getOrElse(Seq.empty)

            // TODO refactor this handling
            MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m ⇒
                val (receiver, params) = if (m.isStatic || persistentActualParams.isEmpty)
                    (None, persistentActualParams)
                else
                    (persistentActualParams.head, persistentActualParams.tail)
                indirectCalls.addCall(
                    caller,
                    declaredMethods(m),
                    pc,
                    // TODO: is this sufficient?
                    params,
                    receiver
                )
            }

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
                        RefArray(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")),
                        ObjectType.Class
                    )
                )
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

object TriggeredReflectionRelatedCallsAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
        Callees,
        LoadedClasses,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
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
