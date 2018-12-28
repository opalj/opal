/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.BaseType
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.FieldType
import org.opalj.br.FieldTypes
import org.opalj.br.InvokeInterfaceMethodHandle
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.InvokeVirtualMethodHandle
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.collection.immutable.IntArraySetBuilder
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.RefArray
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.LoadedClasses
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.NoReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.ReflectionRelatedCalleesImplementation
import org.opalj.fpcf.properties.SystemProperties
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.NewArray
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

import scala.language.existentials

/**
 * Finds calls and loaded classes that exist because of reflective calls that are easy to resolve.
 *
 * @author Dominik Helm
 * @author Michael Reif
 * @author Florian Kuebler
 */
class ReflectionRelatedCallsAnalysis private[analyses] (
        final implicit val project: SomeProject
) extends FPCFAnalysis {

    val HIGHSOUNDNESS = false

    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val MethodT = ObjectType("java/lang/reflect/Method")

    val PropertiesT = ObjectType("java/util/Properties")

    val GetPropertyDescriptor = MethodDescriptor(ObjectType.String, ObjectType.String)
    val GetOrDefaultPropertyDescriptor =
        MethodDescriptor(RefArray(ObjectType.String, ObjectType.String), ObjectType.String)
    val GetDescriptor = MethodDescriptor(ObjectType.Object, ObjectType.Object)

    private[this] val allMethodsMatcher = new AllMethodsMatcher(project)
    private[this] var publicMethodsMatcher = new PublicMethodMatcher(project)
    private[this] var staticMethodsMatcher = new StaticMethodMatcher(project)
    private[this] var nonStaticMethodsMatcher = new NonStaticMethodMatcher(project)
    private[this] val constructorMatcher = new NameBasedMethodMatcher(Set("<init>"), project)

    final class State(
            val definedMethod:                           DefinedMethod,
            val loadedClassesUB:                         UIDSet[ObjectType]                              = UIDSet.empty,
            val instantiatedTypesUB:                     UIDSet[ObjectType]                              = UIDSet.empty,
            val calleesAndCallers:                       IndirectCalleesAndCallers                       = new IndirectCalleesAndCallers(),
            val forNamePCs:                              IntTrieSet                                      = IntTrieSet.empty,
            val invocationPCs:                           IntTrieSet                                      = IntTrieSet.empty,
            private[this] var _newLoadedClasses:         UIDSet[ObjectType]                              = UIDSet.empty,
            private[this] var _newInstantiatedTypes:     UIDSet[ObjectType]                              = UIDSet.empty,
            private[this] var _tacode:                   Option[TACode[TACMethodParameter, V]]           = None,
            private[this] var _tacaiDependee:            Option[EOptionP[Method, TACAI]]                 = None,
            private[this] var _systemPropertiesDependee: Option[EOptionP[SomeProject, SystemProperties]] = None,
            private[this] var _systemProperties:         Option[Map[String, Set[String]]]                = None
    ) {

        if (_tacaiDependee.isDefined &&
            _tacaiDependee.get.hasUBP &&
            _tacaiDependee.get.ub.tac.isDefined)
            assert(_tacaiDependee.get.ub.tac == _tacode)

        private[cg] def copy(
            definedMethod:            DefinedMethod                                   = this.definedMethod,
            loadedClassesUB:          UIDSet[ObjectType]                              = this.loadedClassesUB,
            instantiatedTypesUB:      UIDSet[ObjectType]                              = this.instantiatedTypesUB,
            calleesAndCallers:        IndirectCalleesAndCallers                       = this.calleesAndCallers,
            forNamePCs:               IntTrieSet                                      = this.forNamePCs,
            invocationPCs:            IntTrieSet                                      = this.invocationPCs,
            newLoadedClasses:         UIDSet[ObjectType]                              = _newLoadedClasses,
            newInstantiatedTypes:     UIDSet[ObjectType]                              = _newInstantiatedTypes,
            tacode:                   Option[TACode[TACMethodParameter, V]]           = _tacode,
            tacaiDependee:            Option[EOptionP[Method, TACAI]]                 = _tacaiDependee,
            systemPropertiesDependee: Option[EOptionP[SomeProject, SystemProperties]] = _systemPropertiesDependee,
            systemProperties:         Option[Map[String, Set[String]]]                = _systemProperties
        ): State = {
            new State(
                definedMethod,
                loadedClassesUB,
                instantiatedTypesUB,
                calleesAndCallers,
                forNamePCs,
                invocationPCs,
                newLoadedClasses,
                newInstantiatedTypes,
                tacode,
                tacaiDependee,
                systemPropertiesDependee,
                systemProperties
            )
        }

        private[cg] def addNewLoadedClasses(newLoadedClasses: TraversableOnce[ObjectType]): Unit = {
            _newLoadedClasses ++= newLoadedClasses
        }

        private[cg] def addNewInstantiatedTypes(newInstantiatedTypes: TraversableOnce[ObjectType]): Unit = {
            _newInstantiatedTypes ++= newInstantiatedTypes
        }

        private[cg] def newLoadedClasses: UIDSet[ObjectType] = _newLoadedClasses

        private[cg] def newInstantiatedTypes: UIDSet[ObjectType] = _newInstantiatedTypes

        private[cg] def isTACDefined: Boolean = _tacode.isDefined

        private[cg] def tacode: TACode[TACMethodParameter, V] = _tacode.get

        private[cg] def removeTACDependee(): Unit = _tacaiDependee = None

        private[cg] def addTACDependee(ep: EOptionP[Method, TACAI]): Unit = {
            assert(_tacaiDependee.isEmpty)
            if (ep.isRefinable) {
                _tacaiDependee = Some(ep)
            }
            if (ep.hasUBP && ep.ub.tac.isDefined) {
                _tacode = ep.ub.tac
            }
        }

        private[cg] def hasTACDependee: Boolean = { _tacaiDependee.isDefined }

        private[cg] def tacaiDependee: Option[EOptionP[Method, TACAI]] = _tacaiDependee

        private[cg] def removeSystemPropertiesDependee(): Unit = {
            _systemPropertiesDependee = None
        }

        private[cg] def addSystemPropertiesDependee(ep: EOptionP[SomeProject, SystemProperties]): Unit = {
            assert(_systemPropertiesDependee.isEmpty)
            if (ep.isRefinable) {
                _systemPropertiesDependee = Some(ep)
            }
            if (ep.hasUBP) {
                _systemProperties = Some(ep.ub.properties)
            }
        }

        private[cg] def systemPropertiesDependee: Option[EOptionP[SomeProject, SystemProperties]] = {
            _systemPropertiesDependee
        }

        private[cg] def hasSystemPropertiesDependee: Boolean = {
            _systemPropertiesDependee.isDefined
        }

        private[cg] def systemProperties: Map[String, Set[String]] = _systemProperties.get

        private[cg] def hasSystemProperties: Boolean = _systemProperties.isDefined

        private[cg] def hasOpenDependee: Boolean = {
            hasTACDependee || hasSystemPropertiesDependee
        }
    }

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    // todo move this to instantiatedTypes analysis
    private[this] val initialInstantiatedTypes: UIDSet[ObjectType] =
        UIDSet(project.get(InitialInstantiatedTypesKey).toSeq: _*)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
        propertyStore(declaredMethod, CallersProperty.key) match {
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

        var invocationPCs = IntTrieSet.empty
        var forNamePCs = IntTrieSet.empty
        val insts = method.body.get.instructions
        var i = 0
        val max = insts.length
        while (i < max) {
            val inst = insts(i)
            if (inst != null)
                inst.opcode match {
                    case INVOKESTATIC.opcode ⇒
                        val call = inst.asMethodInvocationInstruction
                        if (call.declaringClass == ObjectType.Class && call.name == "forName")
                            forNamePCs += i
                    case INVOKEVIRTUAL.opcode ⇒
                        val call = inst.asMethodInvocationInstruction
                        call.declaringClass match {
                            case ObjectType.Class ⇒
                                if (call.name == "newInstance") invocationPCs += i
                            case ConstructorT ⇒
                                if (call.name == "newInstance") invocationPCs += i
                            case MethodT ⇒
                                if (call.name == "invoke") invocationPCs += i
                            case ObjectType.MethodHandle ⇒
                                if (call.name.startsWith("invoke"))
                                    invocationPCs += i
                            case _ ⇒
                        }
                    case _ ⇒
                }
            i += 1
        }

        if (invocationPCs.isEmpty && forNamePCs.isEmpty)
            return Result(declaredMethod, NoReflectionRelatedCallees);

        val tacEP = propertyStore(method, TACAI.key)
        val tacEPOpt = if (tacEP.isFinal) None else Some(tacEP)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            implicit val state: State = new State(
                definedMethod,
                forNamePCs = forNamePCs,
                invocationPCs = invocationPCs,
                _tacaiDependee = tacEPOpt,
                _tacode = tacEP.ub.tac
            )
            processMethod(state)
        } else {
            implicit val state: State = new State(
                definedMethod,
                forNamePCs = forNamePCs,
                invocationPCs = invocationPCs,
                _tacaiDependee = tacEPOpt
            )
            InterimResult.forUB(definedMethod, NoReflectionRelatedCallees, tacEPOpt, continuation)
        }
    }

    private[this] def processMethod(
        state: State
    ): ProperPropertyComputationResult = {
        assert(state.isTACDefined)
        val (loadedClassesUB, instantiatedTypesUB) = loadedClassesAndInstantiatedTypes()

        val calleesAndCallers = new IndirectCalleesAndCallers()

        // todo maybe move clearing to returnResult (newLoadedClasses/newInstantiatedTypes)
        implicit val newState: State = state.copy(
            loadedClassesUB = loadedClassesUB,
            newLoadedClasses = UIDSet.empty,
            instantiatedTypesUB = instantiatedTypesUB,
            newInstantiatedTypes = UIDSet.empty,
            calleesAndCallers = calleesAndCallers
        )

        analyzeMethod()

        returnResult()
    }

    /**
     * Analyzes all Class.forName calls and all invocations via invoke, invokeExact,
     * invokeWithArguments and newInstance methods.
     */
    private[this] def analyzeMethod()(implicit state: State): Unit = {
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        for {
            pc ← state.forNamePCs
            index = state.tacode.pcToIndex(pc)
            if index >= 0
            stmt = stmts(index)
        } {
            val expr = if (stmt.astID == Assignment.ASTID) stmt.asAssignment.expr
            else stmt.asExprStmt.expr
            handleForName(expr.asStaticFunctionCall.params.head, pc)
        }

        for {
            pc ← state.invocationPCs
            index = state.tacode.pcToIndex(pc)
            if index >= 0
            stmt = stmts(index)
        } {
            stmt.astID match {
                case Assignment.ASTID ⇒
                    handleExpr(state.definedMethod, stmt.pc, stmt.asAssignment.expr)
                case ExprStmt.ASTID ⇒
                    handleExpr(state.definedMethod, stmt.pc, stmt.asExprStmt.expr)
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(pc, declClass, _, name, _, receiver, params) = stmt
                    // todo verify that this is true for exactly these two methods
                    // invoke(Exact) returns Object, but by signature polymorphism the call
                    // may still be a VirtualMethodCall if the return value is void or unused
                    if (declClass == ObjectType.MethodHandle &&
                        (name == "invoke" || name == "invokeExact"))
                        handleMethodHandleInvoke(
                            state.definedMethod,
                            pc,
                            receiver,
                            params,
                            None,
                            isSignaturePolymorphic = true
                        )
                case _ ⇒

            }
        }
    }

    /**
     * Analyzes a single expression that is an invocation via on of the methods invoke,
     * invokeExact, invokeWithArguments and newInstance.
     */
    private[this] def handleExpr(
        caller: DefinedMethod,
        pc:     Int,
        expr:   Expr[V]
    )(implicit state: State, stmts: Array[Stmt[V]]): Unit = expr.astID match {
        case VirtualFunctionCall.ASTID ⇒
            expr.asVirtualFunctionCall match {
                case VirtualFunctionCall(_, ObjectType.Class, _, "newInstance", _, rcvr, _) ⇒
                    handleNewInstance(
                        caller,
                        pc,
                        rcvr
                    )
                case VirtualFunctionCall(_, ConstructorT, _, "newInstance", _, rcvr, params) ⇒
                    assert(params.size == 1)
                    handleConstructorNewInstance(caller, pc, rcvr, params.head)
                case VirtualFunctionCall(_, MethodT, _, "invoke", _, rcvr, params) ⇒
                    handleMethodInvoke(caller, pc, rcvr, params)
                case VirtualFunctionCall(_, ObjectType.MethodHandle, _, "invokeExact", desc, rcvr, params) ⇒
                    handleMethodHandleInvoke(
                        caller,
                        pc,
                        rcvr,
                        params,
                        Some(desc),
                        isSignaturePolymorphic = true
                    )
                case VirtualFunctionCall(_, ObjectType.MethodHandle, _, "invoke", _, rcvr, params) ⇒
                    handleMethodHandleInvoke(
                        caller,
                        pc,
                        rcvr,
                        params,
                        None,
                        isSignaturePolymorphic = true
                    )
                case VirtualFunctionCall(_, ObjectType.MethodHandle, _, "invokeWithArguments", _, rcvr, params) ⇒
                    handleMethodHandleInvoke(
                        caller, pc, rcvr, params, None, isSignaturePolymorphic = false
                    )
                case _ ⇒
            }
        case _ ⇒
    }

    /**
     * Adds classes that can be loaded by an invocation of Class.forName to the set of loaded
     * classes.
     */
    private[this] def handleForName(className: Expr[V], pc: Int)(implicit state: State): Unit = {
        val loadedClassesOpt = getPossibleForNameClasses(className, None)
        if (loadedClassesOpt.isEmpty) {
            if (HIGHSOUNDNESS) {
                state.addNewLoadedClasses(
                    p.allClassFiles.iterator.map(_.thisType).filterNot(state.loadedClassesUB.contains)
                )
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }
        } else {
            state.addNewLoadedClasses(
                loadedClassesOpt.get.filterNot(state.loadedClassesUB.contains)
            )
        }
    }

    /**
     * Given an optional value of type `A` and a `factory` method for a [[MethodMatcher]],
     * it creates a method matcher (using the factory) if the value in `v` is defined.
     * Otherwise, depending on the project setting, it returns an [[AllMethodsMatcher]] or marks
     * the `pc` as incomplete.
     */
    private[this] def retrieveSuitableMatcher[A](
        v:       Option[A],
        pc:      Int,
        factory: A ⇒ MethodMatcher
    )(
        implicit
        state: State
    ): MethodMatcher = {
        if (v.isEmpty) {
            if (HIGHSOUNDNESS) {
                allMethodsMatcher
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                NoMethodsMatcher
            }
        } else {
            factory(v.get)
        }
    }

    /**
     * Given an optional value of type `A` and a `factory` method for a [[MethodMatcher]],
     * it either creates a mather, using the factory and the value in `v` or returns the
     * [[AllMethodsMatcher]].
     */
    private[this] def retrieveSuitableNonEssentialMatcher[A](
        v: Option[A], f: (A) ⇒ MethodMatcher
    ): MethodMatcher = {
        if (v.isEmpty) {
            allMethodsMatcher
        } else {
            f(v.get)
        }
    }

    private[this] def retrieveClassBasedMethodMatcher(
        ref: Expr[V], pc: Int
    )(implicit state: State): MethodMatcher = {
        val typesOpt =
            getPossibleTypes(ref, pc).map(_.map(_.asObjectType)).map(_.toSet)

        retrieveSuitableMatcher[Set[ObjectType]](
            typesOpt,
            pc,
            v ⇒ new ClassBasedMethodMatcher(v, project)
        )
    }

    private[this] def retrieveNameBasedMethodMatcher(
        expr: Expr[V], pc: Int
    )(implicit state: State): MethodMatcher = {
        val namesO = getPossibleStrings(expr, Some(pc))
        retrieveSuitableMatcher[Set[String]](
            namesO,
            pc,
            v ⇒ new NameBasedMethodMatcher(v, project)
        )
    }

    private[this] def retrieveDescriptorBasedMethodMatcher(
        descriptorOpt: Option[MethodDescriptor], expr: Expr[V], pc: Int, isStatic: Boolean, isConstructor: Boolean
    )(implicit state: State): MethodMatcher = {
        val descriptorsOpt =
            if (descriptorOpt.isDefined) descriptorOpt.map(Set(_))
            else getPossibleDescriptorsFormMethodTypes(expr, pc).map(_.toSet)

        val actualDescriptorOpt =
            // for instance methods, we need to peel off the receiver type
            if (!isStatic && !isConstructor)
                descriptorsOpt.map(_.map { md ⇒
                    MethodDescriptor(md.parameterTypes.tail, md.returnType)
                })
            else if (isConstructor)
                // for constructor
                descriptorsOpt.map(_.map { md ⇒
                    MethodDescriptor(md.parameterTypes, VoidType)
                })
            else
                descriptorsOpt

        retrieveSuitableMatcher[Set[MethodDescriptor]](
            actualDescriptorOpt,
            pc,
            v ⇒ new DescriptorBasedMethodMatcher(v, project)

        )
    }

    private[this] def retrieveParameterTypesBasedMethodMatcher(
        varArgs: Expr[V], pc: Int
    )(implicit state: State): MethodMatcher = {
        val paramTypesO = getTypesFromVararg(varArgs)
        retrieveSuitableMatcher[FieldTypes](
            paramTypesO,
            pc,
            v ⇒ new ParameterTypesBasedMethodMatcher(v, project)
        )
    }

    // todo what about the case of an constructor?
    private[this] def retrieveMatchersForMethodHandleConst(
        receiver:      ReferenceType,
        name:          String,
        desc:          MethodDescriptor,
        isStatic:      Boolean,
        isConstructor: Boolean
    ): Set[MethodMatcher] = {
        assert(!isStatic || !isConstructor)
        Set(
            new DescriptorBasedMethodMatcher(
                Set(
                    if (isStatic)
                        desc
                    else if (isConstructor)
                        MethodDescriptor(desc.parameterTypes, VoidType)
                    else
                        MethodDescriptor(desc.parameterTypes.tail, desc.returnType)
                ),
                project
            ),
            new NameBasedMethodMatcher(Set(name), project),
            if (receiver.isArrayType) new ClassBasedMethodMatcher(
                Set(ObjectType.Object), project
            )
            else new ClassBasedMethodMatcher(
                Set(receiver.asObjectType), project
            )

        )
    }

    private[this] def retrieveMethodHandleLookupMatchers(
        refc:          Expr[V],
        name:          Expr[V],
        methodType:    Expr[V],
        descriptorOpt: Option[MethodDescriptor],
        pc:            Int,
        isStatic:      Boolean,
        isConstructor: Boolean
    )(implicit state: State): Seq[MethodMatcher] = {
        Seq(
            retrieveClassBasedMethodMatcher(refc, pc),

            retrieveNameBasedMethodMatcher(name, pc),

            retrieveDescriptorBasedMethodMatcher(
                descriptorOpt, methodType, pc, isStatic, isConstructor
            )
        )
    }

    private[this] def addCalls(
        caller:         DefinedMethod,
        pc:             Int,
        actualReceiver: Option[(ValueInformation, IntTrieSet)],
        actualParams:   Seq[Option[(ValueInformation, IntTrieSet)]], matchers: Traversable[MethodMatcher]
    )(implicit state: State): Unit = {
        MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m ⇒
            if (m.isConstructor && !state.instantiatedTypesUB.contains(m.classFile.thisType))
                state.addNewInstantiatedTypes(Iterator(m.classFile.thisType))
            state.calleesAndCallers.updateWithIndirectCall(
                caller,
                declaredMethods(m),
                pc,
                actualReceiver +: actualParams
            )
        }
    }

    private[this] def handleNewInstance(
        caller:    DefinedMethod,
        pc:        Int,
        classExpr: Expr[V]
    )(implicit state: State): Unit = {
        var matchers: Set[MethodMatcher] = Set(
            constructorMatcher,
            new ParameterTypesBasedMethodMatcher(RefArray.empty, project)
        )

        matchers += retrieveClassBasedMethodMatcher(classExpr, pc)

        addCalls(caller, pc, None, Seq.empty, matchers)
    }

    private[this] def handleConstructorNewInstance(
        caller:            DefinedMethod,
        pc:                Int,
        constructor:       Expr[V],
        newInstanceParams: Expr[V]
    )(implicit state: State, stmts: Array[Stmt[V]]): Unit = {
        val actualParamsNewInstanceOpt = getParamsFromVararg(newInstanceParams)
        val persistentActualParams =
            actualParamsNewInstanceOpt.map(_.map(persistentUVar)).getOrElse(Seq.empty)

        constructor.asVar.definedBy.foreach { index ⇒
            var matchers: Set[MethodMatcher] = Set(
                constructorMatcher,
                retrieveSuitableNonEssentialMatcher[Seq[V]](
                    actualParamsNewInstanceOpt,
                    v ⇒ new ActualParamBasedMethodMatcher(v, project)
                )
            )

            if (index > 0) {
                state.tacode.stmts(index).asAssignment.expr match {
                    case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getConstructor" | "getDeclaredConstructor", _, receiver, params) ⇒

                        if (call.name == "getConstructor") {
                            matchers += publicMethodsMatcher
                        }

                        matchers += retrieveParameterTypesBasedMethodMatcher(params.head, pc)

                        matchers += retrieveClassBasedMethodMatcher(receiver, pc)

                    /*
                     * TODO: case ArrayLoad(_, _, arrayRef) ⇒ // here we could handle getConstructors
                     */

                    case _ if HIGHSOUNDNESS ⇒
                        matchers += allMethodsMatcher

                    case _ ⇒
                        state.calleesAndCallers.addIncompleteCallsite(pc)
                        matchers += NoMethodsMatcher
                }
            } else if (HIGHSOUNDNESS) {
                matchers += allMethodsMatcher
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }

            addCalls(caller, pc, None, persistentActualParams, matchers)
        }
    }

    private[this] def handleMethodInvoke(
        caller:       DefinedMethod,
        pc:           Int,
        method:       Expr[V],
        methodParams: Seq[Expr[V]]
    )(implicit state: State): Unit = {
        assert(methodParams.size == 2)
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        val methodInvokeReceiver = methodParams.head
        val persistentMethodInvokeReceiver = persistentUVar(methodInvokeReceiver.asVar)
        val methodInvokeActualParamsOpt = getParamsFromVararg(methodParams(1))
        val persistentMethodInvokeActualParamsOpt =
            methodInvokeActualParamsOpt.map(_.map(persistentUVar))

        method.asVar.definedBy.foreach { index ⇒
            var matchers: Set[MethodMatcher] = Set(
                retrieveSuitableMatcher[Seq[V]](
                    methodInvokeActualParamsOpt,
                    pc,
                    v ⇒ new ActualParamBasedMethodMatcher(v, project)
                ),
                new ActualReceiverBasedMethodMatcher(
                    methodInvokeReceiver.asVar.value.asReferenceValue, project
                )
            )

            if (index >= 0) {
                val definition = stmts(index).asAssignment.expr
                definition match {
                    case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredMethod" | "getMethod", _, receiver, params) ⇒
                        if (call.name == "getMethod") {
                            matchers += publicMethodsMatcher
                        }

                        matchers += retrieveNameBasedMethodMatcher(params.head, pc)

                        matchers += retrieveParameterTypesBasedMethodMatcher(params(1), pc)

                        matchers += retrieveClassBasedMethodMatcher(receiver, pc)

                    /*case ArrayLoad(_, _, arrayRef) ⇒*/
                    // todo here we can handle getMethods

                    case _ if HIGHSOUNDNESS ⇒
                        matchers += allMethodsMatcher

                    case _ ⇒
                        state.calleesAndCallers.addIncompleteCallsite(pc)
                        matchers += NoMethodsMatcher
                }
            } else if (HIGHSOUNDNESS) {
                matchers += allMethodsMatcher
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
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

    private[this] def handleMethodHandleInvoke(
        caller:                 DefinedMethod,
        pc:                     Int,
        methodHandle:           Expr[V],
        invokeParams:           Seq[Expr[V]],
        descriptor:             Option[MethodDescriptor],
        isSignaturePolymorphic: Boolean
    )(implicit state: State, stmts: Array[Stmt[V]]): Unit = {
        // IMPROVE: for signature polymorphic calls, we could also use the method descriptor (return type)
        val actualInvokeParamsOpt =
            if (isSignaturePolymorphic) Some(invokeParams.map(_.asVar))
            else getParamsFromVararg(invokeParams.head)

        methodHandle.asVar.definedBy.foreach { index ⇒
            // todo here we need to peel of the 1. actual parameter for non static ones
            var matchers: Set[MethodMatcher] = Set.empty /*Set(
                retrieveSuitableNonEssentialMatcher[Seq[V]](
                    actualInvokeParamsOpt,
                    v ⇒ new ActualParamBasedMethodMatcher(v, project)
                )
            )*/

            if (index >= 0) {
                val definition = state.tacode.stmts(index).asAssignment.expr
                if (definition.isMethodHandleConst) {
                    // todo do we need to distinguish the cases below?
                    definition.asMethodHandleConst.value match {
                        case InvokeStaticMethodHandle(receiver, _, name, desc) ⇒
                            matchers ++= retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = true, isConstructor = false)

                        case InvokeVirtualMethodHandle(receiver, name, desc) ⇒
                            matchers ++= retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case InvokeInterfaceMethodHandle(receiver, name, desc) ⇒
                            matchers ++= retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case InvokeSpecialMethodHandle(receiver, _, name, desc) ⇒
                            // todo does this work for super?
                            matchers ++= retrieveMatchersForMethodHandleConst(receiver, name, desc, isStatic = false, isConstructor = false)

                        case NewInvokeSpecialMethodHandle(receiver, desc) ⇒
                            matchers ++= retrieveMatchersForMethodHandleConst(receiver, "<init>", desc, isStatic = false, isConstructor = true)

                        case _ ⇒
                        // getters and setters are not relevant for the call graph
                    }
                } else if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findStatic", _, _, params) ⇒
                            matchers += staticMethodsMatcher

                            val Seq(refc, name, methodType) = params
                            matchers ++= retrieveMethodHandleLookupMatchers(
                                refc, name, methodType, descriptor, pc, isStatic = true, isConstructor = false
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findVirtual", _, _, params) ⇒
                            matchers += nonStaticMethodsMatcher

                            val Seq(refc, name, methodType) = params
                            matchers ++= retrieveMethodHandleLookupMatchers(
                                refc, name, methodType, descriptor, pc, isStatic = false, isConstructor = false
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findSpecial", _, _, params) ⇒
                            matchers += nonStaticMethodsMatcher

                            // todo we can ignore the 4ths param? Does it work for super calls?
                            val Seq(refc, name, methodType, _) = params
                            matchers ++= retrieveMethodHandleLookupMatchers(
                                refc, name, methodType, descriptor, pc, isStatic = false, isConstructor = false
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findConstructor", _, _, params) ⇒
                            matchers += nonStaticMethodsMatcher

                            val Seq(refc, methodType) = params

                            matchers += new NameBasedMethodMatcher(Set("<init>"), project)

                            matchers += retrieveClassBasedMethodMatcher(refc, pc)

                            matchers += retrieveDescriptorBasedMethodMatcher(
                                descriptor, methodType, pc, isStatic = false, isConstructor = true
                            )
                        case _ ⇒
                        // getters and setters are not relevant for the call graph
                    }
                } else if (HIGHSOUNDNESS) {
                    if (descriptor.isDefined) {
                        // we do not know, whether the invoked method is static or not
                        // (i.e. whether the first parameter of the descriptor represent the receiver)
                        val md = descriptor.get
                        val nonStaticDescriptor = MethodDescriptor(
                            md.parameterTypes.tail, md.returnType
                        )
                        matchers += new DescriptorBasedMethodMatcher(
                            Set(md, nonStaticDescriptor), project
                        )
                    }
                    matchers += allMethodsMatcher
                } else {
                    matchers += NoMethodsMatcher
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                }
                // todo we should use the descriptor here
            } else if (HIGHSOUNDNESS) {
                matchers += allMethodsMatcher
            } else {
                matchers += NoMethodsMatcher
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }

            val persistentActualParams =
                actualInvokeParamsOpt.map(_.map(persistentUVar)).getOrElse(Seq.empty)

            // todo refactor this handling
            MethodMatching.getPossibleMethods(matchers.toSeq).foreach { m ⇒
                if (m.isConstructor && !state.instantiatedTypesUB.contains(m.classFile.thisType))
                    state.addNewInstantiatedTypes(Iterator(m.classFile.thisType))
                state.calleesAndCallers.updateWithIndirectCall(
                    caller,
                    declaredMethods(m),
                    pc,
                    // todo: is this sufficient?
                    if (m.isStatic)
                        None +: persistentActualParams
                    else
                        persistentActualParams
                )
            }

        }
    }

    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies local use of String constants and Strings loaded from Properties objects.
     */
    private[this] def getPossibleStrings(
        value:            Expr[V],
        pc:               Option[Int],
        onlyStringConsts: Boolean     = false
    )(implicit state: State): Option[Set[String]] = {
        Some(value.asVar.definedBy.map[Set[String]] { index ⇒
            if (index >= 0) {
                val expr = state.tacode.stmts(index).asAssignment.expr
                // todo we do not want this `getOrElse return` stmts
                expr match {
                    case StringConst(_, v) ⇒ Set(v)
                    case StaticFunctionCall(_, ObjectType.System, _, "getProperty", GetPropertyDescriptor, params) if !onlyStringConsts ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetOrDefaultPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; } ++
                            getPossibleStrings(params(1), None, onlyStringConsts = true).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "get", GetDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }
                    case _ ⇒
                        return None;
                }
            } else {
                return None;
            }
        }.flatten)
    }

    /**
     * Returns Strings that ma be loaded from a Properties object if the key is a constant String
     * that the given expression may evaluate to.
     */
    private[this] def getStringConstsForSystemPropertiesKey(
        value: Expr[V], pc: Option[Int]
    )(implicit state: State): Option[Set[String]] = {
        if (!state.hasSystemPropertiesDependee) {
            state.addSystemPropertiesDependee(propertyStore(project, SystemProperties.key))
        }
        if (pc.isDefined) {
            return None;
        }
        if (!state.hasSystemProperties) {
            Some(Set.empty)
        } else {
            val keysOpt = getPossibleStrings(value, None, onlyStringConsts = true)
            if (keysOpt.isEmpty) {
                None
            } else {
                Some(keysOpt.get.flatMap { key ⇒
                    state.systemProperties.getOrElse(key, Set.empty[String])
                })
            }
        }
    }

    /**
     * Returns types that a given expression potentially evaluates to.
     * Identifies local uses of Class constants, class instances returned from Class.forName,
     * by accesses to a primitive type's class as well as from Object.getClass.
     */
    private[this] def getPossibleTypes(
        value: Expr[V],
        pc:    Int
    )(implicit state: State): Option[Iterator[Type]] = {

        def isForName(expr: Expr[V]): Boolean = { // static call to Class.forName
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.Class) &&
                expr.asStaticFunctionCall.name == "forName"
        }

        def isGetClass(expr: Expr[V]): Boolean = { // virtual call to Object.getClass
            expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
                expr.asVirtualFunctionCall.descriptor == MethodDescriptor.withNoArgs(ObjectType.Class)
        }

        var possibleTypes: Set[Type] = Set.empty
        val defSitesIterator = value.asVar.definedBy.iterator

        while (defSitesIterator.hasNext) {
            val defSite = defSitesIterator.next()
            if (defSite < 0) {
                return None;
            }
            val expr = state.tacode.stmts(defSite).asAssignment.expr

            if (!expr.isClassConst && !isForName(expr) && !isBaseTypeLoad(expr) & !isGetClass(expr)) {
                return None;
            }

            if (expr.isClassConst) {
                possibleTypes += state.tacode.stmts(defSite).asAssignment.expr.asClassConst.value
            } else if (expr.isStaticFunctionCall) {
                val possibleClassesOpt = getPossibleForNameClasses(
                    expr.asStaticFunctionCall.params.head, Some(pc)
                )
                if (possibleClassesOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= possibleClassesOpt.get
            } else if (expr.isVirtualFunctionCall) {
                val typesOfVarOpt = getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar, pc)
                if (typesOfVarOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= typesOfVarOpt.get
            } else {
                possibleTypes += getBaseType(expr)
            }

        }

        Some(possibleTypes.iterator)
    }

    /**
     * Returns true if a given expression is a GetField instruction that retrieves the TYPE field of
     * a primitive types wrapper class, i.e. the class for the primitive type.
     */
    private[this] def isBaseTypeLoad(expr: Expr[V]): Boolean = {
        expr.isGetStatic && expr.asGetStatic.name == "TYPE" && {
            val declClass = expr.asGetStatic.declaringClass
            declClass == VoidType.WrapperType ||
                BaseType.baseTypes.iterator.map(_.WrapperType).contains(declClass)
        }
    }

    /**
     * Returns the [org.opalj.br.Type] for a primitive type where the given expression is a
     * GetStatic expression for the TYPE field of the primitive type's wrapper type.
     */
    private[this] def getBaseType(expr: Expr[V]): Type = {
        val declClass = expr.asGetStatic.declaringClass
        if (declClass == VoidType.WrapperType) VoidType
        else BaseType.baseTypes.iterator.find(declClass == _.WrapperType).get
    }

    /**
     * Retrieves the possible runtime types of a local variable if they are known precisely.
     * Otherwise, the call site is marked as incomplete and an empty Iterator is returned.
     */
    private[this] def getTypesOfVar(
        uvar: V,
        pc:   Int
    ): Option[Iterator[ReferenceType]] = {
        val value = uvar.value.asReferenceValue
        if (value.isPrecise) value.leastUpperType.map(Iterator(_))
        else if (value.allValues.forall(_.isPrecise))
            Some(value.allValues.toIterator.flatMap(_.leastUpperType))
        else {
            None
        }
    }

    /**
     * Returns classes that may be loaded by an invocation of Class.forName.
     */
    private[this] def getPossibleForNameClasses(
        className: Expr[V],
        pc:        Option[Int]
    )(implicit state: State): Option[Set[ObjectType]] = {
        val classNamesOpt = getPossibleStrings(className, pc)
        classNamesOpt.map(_.map(cls ⇒
            ObjectType(cls.replace('.', '/'))).filter(project.classFile(_).isDefined))
    }

    /**
     * Returns method types (aka. descriptors) that a given expression potentially evaluates to.
     * Identifies local use of MethodType constants as well as method types acquired from
     * MethodType.methodType.
     */
    private[this] def getPossibleDescriptorsFormMethodTypes(
        value: Expr[V],
        pc:    Int
    )(implicit state: State): Option[Iterator[MethodDescriptor]] = {

        def isMethodType(expr: Expr[V]): Boolean = {
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.MethodType) &&
                expr.asStaticFunctionCall.name == "methodType"
        }

        val defSitesIterator = value.asVar.definedBy.iterator

        var possibleMethodTypes: Iterator[MethodDescriptor] = Iterator.empty
        while (defSitesIterator.hasNext) {
            val defSite = defSitesIterator.next()

            if (defSite < 0) {
                return None;
            }
            val expr = state.tacode.stmts(defSite).asAssignment.expr
            val isResolvable = expr.isMethodTypeConst || isMethodType(expr)
            if (!isResolvable) {
                return None;
            }

            if (expr.isMethodTypeConst)
                possibleMethodTypes ++=
                    Iterator(state.tacode.stmts(defSite).asAssignment.expr.asMethodTypeConst.value)
            else {
                val call = expr.asStaticFunctionCall
                val pmtOpt = getPossibleMethodTypes(call.params, call.descriptor, pc)
                if (pmtOpt.isEmpty) {
                    return None;
                }
                possibleMethodTypes ++= pmtOpt.get
            }
        }

        Some(possibleMethodTypes)
    }

    /**
     * Returns method types that a call to MethodType.methodType may return.
     */
    private[this] def getPossibleMethodTypes(
        params:     Seq[Expr[V]],
        descriptor: MethodDescriptor,
        pc:         Int
    )(implicit state: State): Option[Iterator[MethodDescriptor]] = {
        val returnTypesOpt = getPossibleTypes(params.head, pc)

        if (returnTypesOpt.isEmpty) {
            // IMPROVE: we could add all return types for method descriptors
            return None;
        }

        val returnTypes = returnTypesOpt.get

        if (params.size == 1) { // methodType(T) => ()T
            Some(returnTypes.map(MethodDescriptor.withNoArgs))
        } else if (params.size == 3) { // methodType(T1, T2, T3, ...) => (T2, T3, ...)T1
            val firstParamTypesOpt = getPossibleTypes(params(1), pc)
            if (firstParamTypesOpt.isEmpty) {
                return None;
            }

            val possibleOtherParamTypes = getTypesFromVararg(params(2))
            if (possibleOtherParamTypes.isEmpty) {
                return None;
            }

            val possibleTypes = for {
                otherParamTypes ← possibleOtherParamTypes.iterator // empty seq. if None
                returnType ← returnTypes
                firstParamType ← firstParamTypesOpt.get.asInstanceOf[Iterator[FieldType]]
            } yield MethodDescriptor(firstParamType +: otherParamTypes, returnType)

            Some(possibleTypes)

        } else {
            val secondParamType = descriptor.parameterType(1)
            if (secondParamType.isArrayType) { // methodType(T1, Class[]{T2, ...}) => (T2, ...)T1
                val possibleOtherParamTypes = getTypesFromVararg(params(1))

                if (possibleOtherParamTypes.isEmpty) {
                    return None;
                }

                val possibleTypes = for {
                    otherParamTypes: FieldType ← possibleOtherParamTypes.get.iterator
                    returnType ← returnTypes
                } yield MethodDescriptor(otherParamTypes, returnType)
                Some(possibleTypes)
            } else if (secondParamType == ObjectType.Class) { // methodType(T1, T2) => (T2)T2
                val paramTypesOpt = getPossibleTypes(params(1), pc)
                if (paramTypesOpt.isEmpty) {
                    return None;
                }
                val paramTypes = paramTypesOpt.get.asInstanceOf[Iterator[FieldType]]
                Some(for {
                    returnType ← returnTypes
                    paramType ← paramTypes
                } yield MethodDescriptor(paramType, returnType))
            } else { // we don't handle methodType(T1, List(T2, ...)) and methodType(T1, MethodType)
                None
            }
        }
    }

    /**
     * Returns the types that a varargs argument of type Class (i.e. Class<?>...) may evaluate to.
     * Only handles the case of a simple array of class constants or primitive types' classes!
     * In case [[None]] is returned, the caller must mark the callsite as incomplete.
     */
    private[this] def getTypesFromVararg(
        expr: Expr[V]
    )(implicit state: State): Option[FieldTypes] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            None
        } else {
            val definition = state.tacode.stmts(definitions.head).asAssignment
            if (definition.expr.isNullExpr) {
                Some(RefArray.empty)
            } else if (definition.expr.astID != NewArray.ASTID) {
                None
            } else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.tacode.cfg.bb(uses.head) != state.tacode.cfg.bb(uses.last)) {
                    None
                } else if (state.tacode.stmts(uses.last).astID != Assignment.ASTID) {
                    None
                } else {
                    var types: RefArray[FieldType] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last)
                            true
                        else {
                            val use = state.tacode.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID)
                                false
                            else {
                                val typeDefs = use.asArrayStore.value.asVar.definedBy
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!typeDefs.isSingletonSet || typeDefs.head < 0 ||
                                    !indices.isSingletonSet || indices.head < 0)
                                    false
                                else {
                                    val typeDef =
                                        state.tacode.stmts(typeDefs.head).asAssignment.expr
                                    val index = state.tacode.stmts(indices.head).asAssignment.expr
                                    if (!typeDef.isClassConst && !isBaseTypeLoad(typeDef))
                                        false
                                    else if (!index.isIntConst) {
                                        false // we don't know the index in the array
                                    } else {
                                        val tpe =
                                            if (typeDef.isClassConst) typeDef.asClassConst.value
                                            else getBaseType(typeDef).asBaseType
                                        types = types.updated(index.asIntConst.value, tpe)
                                        true
                                    }
                                }
                            }
                        }
                    } || types.contains(null)) {
                        None
                    } else {
                        Some(types)
                    }
                }
            }
        }
    }

    /**
     * Returns the origins of parameters that are contained in a varargs argument.
     */
    private[this] def getParamsFromVararg(
        expr: Expr[V]
    )(implicit state: State): Option[Seq[V]] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            None
        } else {
            val definition = state.tacode.stmts(definitions.head).asAssignment
            if (definition.expr.isNullExpr) {
                Some(Seq.empty)
            } else if (definition.expr.astID != NewArray.ASTID) {
                None
            } else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.tacode.cfg.bb(uses.head) != state.tacode.cfg.bb(uses.last)) {
                    // IMPROVE: Here we should also handle the case of non-constant values
                    None
                } else if (state.tacode.stmts(uses.last).astID != Assignment.ASTID &&
                    state.tacode.stmts(uses.last).astID != ExprStmt.ASTID) {
                    None
                } else {
                    var params: Seq[V] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last)
                            true
                        else {
                            val use = state.tacode.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID)
                                false
                            else {
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!indices.isSingletonSet || indices.head < 0)
                                    false
                                else {
                                    val index = state.tacode.stmts(indices.head).asAssignment.expr
                                    if (!index.isIntConst) {
                                        false // we don't know the index in the array
                                    } else {
                                        params = params.updated(
                                            index.asIntConst.value,
                                            use.asArrayStore.value.asVar
                                        )
                                        true
                                    }
                                }
                            }
                        }
                    } || params.contains(null)) {
                        None
                    } else {
                        Some(params)
                    }
                }
            }
        }
    }

    private[this] def continuation(
        eps: SomeEPS
    )(implicit state: State): ProperPropertyComputationResult = eps match {
        case UBPS(ub: SystemProperties, isFinal) ⇒
            val newEPS =
                if (isFinal) None else Some(eps.asInstanceOf[EPS[SomeProject, SystemProperties]])
            // Create new state that reflects changes that may have happened in the meantime
            val (loadedClassesUB, instantiatedTypesUB) = loadedClassesAndInstantiatedTypes()
            // todo maybe move clearing to returnResult (newLoadedClasses/newInstantiatedTypes)
            val newState = state.copy(
                loadedClassesUB = loadedClassesUB,
                newLoadedClasses = UIDSet.empty,
                instantiatedTypesUB = instantiatedTypesUB,
                newInstantiatedTypes = UIDSet.empty,
                calleesAndCallers = new IndirectCalleesAndCallers(state.calleesAndCallers.callees),
                systemPropertiesDependee = newEPS,
                systemProperties = Some(ub.properties)
            )

            // Re-analyze invocations with the new state
            analyzeMethod()(newState)
            returnResult()(newState)

        case UBP(tac: TACAI) if tac.tac.isDefined ⇒
            state.removeTACDependee()
            state.addTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
            processMethod(state)

        case UBP(_: TACAI) ⇒
            InterimResult.forUB(
                state.definedMethod,
                NoReflectionRelatedCallees,
                Some(eps),
                continuation
            )
    }

    /**
     * Retrieves the current state of loaded classes and instantiated types from the property store.
     */
    private[this] def loadedClassesAndInstantiatedTypes(): (UIDSet[ObjectType], UIDSet[ObjectType]) = {
        // the set of classes that are definitely loaded at this point in time
        val loadedClassesEOptP = propertyStore(project, LoadedClasses.key)

        // the upper bound for loaded classes, seen so far
        val loadedClassesUB: UIDSet[ObjectType] = loadedClassesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.classes
            case _              ⇒ UIDSet.empty
        }

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ initialInstantiatedTypes
        }

        (loadedClassesUB, instantiatedTypesUB)
    }

    @inline private[this] def returnResult()(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        var res: List[ProperPropertyComputationResult] =
            state.calleesAndCallers.partialResultsForCallers

        val calleeUB = if (state.calleesAndCallers.callees.isEmpty)
            NoReflectionRelatedCallees
        else
            new ReflectionRelatedCalleesImplementation(
                state.calleesAndCallers.callees,
                state.calleesAndCallers.incompleteCallsites,
                state.calleesAndCallers.parameters
            )

        val calleesResult =
            if (state.hasOpenDependee) {
                InterimResult.forUB(
                    state.definedMethod,
                    calleeUB,
                    state.systemPropertiesDependee ++ state.tacaiDependee,
                    continuation
                )
            } else {
                Result(state.definedMethod, calleeUB)
            }

        res ::= calleesResult

        // todo here we should compare to the current loaded classes ub
        if (state.newLoadedClasses.nonEmpty) {
            res ::= PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case InterimEUBP(p, ub) ⇒
                    val newUb = ub.classes ++ state.newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(InterimEUBP(p, ub.updated(state.newLoadedClasses)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(InterimEUBP(p, LoadedClasses.initial(state.newLoadedClasses)))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
        }

        if (state.newInstantiatedTypes.nonEmpty)
            res ::= PartialResult(
                p,
                InstantiatedTypes.key,
                InstantiatedTypesAnalysis.update(
                    p, state.newInstantiatedTypes, initialInstantiatedTypes
                )
            )

        Results(res)
    }
}

object TriggeredReflectionRelatedCallsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] =
        Set(
            PropertyBounds.ub(CallersProperty),
            PropertyBounds.ub(SystemProperties),
            PropertyBounds.ub(LoadedClasses),
            PropertyBounds.ub(InstantiatedTypes),
            PropertyBounds.ub(TACAI)
        )

    override def derivesCollaboratively: Set[PropertyBounds] =
        Set(
            PropertyBounds.ub(CallersProperty),
            PropertyBounds.ub(LoadedClasses),
            PropertyBounds.ub(InstantiatedTypes)
        )

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReflectionRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(
        PropertyBounds.ub(ReflectionRelatedCallees)
    )
}
