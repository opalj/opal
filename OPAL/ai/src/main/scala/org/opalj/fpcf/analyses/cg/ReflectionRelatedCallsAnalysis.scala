/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials

import org.opalj.br.ArrayType
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

/**
 * Finds calls and loaded classes that exist because of reflective calls that are easy to resolve.
 *
 * @author Dominik Helm
 * @author Michael Reif
 * @author Florian Kuebler
 */
class ReflectionRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val MethodT = ObjectType("java/lang/reflect/Method")

    val PropertiesT = ObjectType("java/util/Properties")

    val GetPropertyDescriptor = MethodDescriptor(ObjectType.String, ObjectType.String)
    val GetOrDefaultPropertyDescriptor =
        MethodDescriptor(RefArray(ObjectType.String, ObjectType.String), ObjectType.String)
    val GetDescriptor = MethodDescriptor(ObjectType.Object, ObjectType.Object)

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

        if (_tacaiDependee.isDefined && _tacaiDependee.get.hasProperty)
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
            systemProperties:         Option[Map[String, Set[String]]]                = None
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
            if (ep.hasProperty) {
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
            if (ep.hasProperty) {
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
    private[this] val initialInstantiatedTypes: UIDSet[ObjectType] =
        UIDSet(project.get(InitialInstantiatedTypesKey).toSeq: _*)

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

        if (tacEP.hasProperty) {
            implicit val state: State = new State(
                definedMethod,
                forNamePCs = forNamePCs,
                invocationPCs = invocationPCs,
                _tacaiDependee = tacEPOpt,
                _tacode = tacEP.ub.tac
            )
            processMethod(state)
        } else {
            implicit val state: State = new State(definedMethod, _tacaiDependee = Some(tacEP))
            SimplePIntermediateResult(
                definedMethod, NoReflectionRelatedCallees, tacEPOpt, continuation
            )
        }
    }

    private[this] def processMethod(
        state: State
    ): PropertyComputationResult = {
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
        for {
            pc ← state.forNamePCs
            index = state.tacode.pcToIndex(pc)
            if index >= 0
            stmt = state.tacode.stmts(index)
        } {
            val expr = if (stmt.astID == Assignment.ASTID) stmt.asAssignment.expr
            else stmt.asExprStmt.expr
            handleForName(expr.asStaticFunctionCall.params.head)
        }

        for {
            pc ← state.invocationPCs
            index = state.tacode.pcToIndex(pc)
            if index >= 0
            stmt = state.tacode.stmts(index)
        } {
            stmt.astID match {
                case Assignment.ASTID ⇒
                    handleExpr(state.definedMethod, stmt.pc, stmt.asAssignment.expr)
                case ExprStmt.ASTID ⇒
                    handleExpr(state.definedMethod, stmt.pc, stmt.asExprStmt.expr)
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(pc, declClass, _, name, _, receiver, params) = stmt
                    // invoke(withArguments) returns Object, but by signature polymorphism the call
                    // may still be a VirtualMethodCall if the return value is void or unused
                    if (declClass == ObjectType.MethodHandle &&
                        (name == "invoke" || name == "invokeWithArguments"))
                        handleMethodHandleInvoke(
                            state.definedMethod,
                            pc,
                            receiver,
                            params,
                            None,
                            isSignaturePolymorphic = name == "invoke"
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
    )(implicit state: State): Unit = expr.astID match {
        case VirtualFunctionCall.ASTID ⇒
            expr.asVirtualFunctionCall match {
                case VirtualFunctionCall(_, ObjectType.Class, _, "newInstance", _, rcvr, _) ⇒
                    handleNewInstance(
                        caller,
                        pc,
                        rcvr,
                        Some(MethodDescriptor.NoArgsAndReturnVoid),
                        Seq.empty // invokes default constructor, i.e. no arguments
                    )
                case VirtualFunctionCall(_, ConstructorT, _, "newInstance", _, rcvr, params) ⇒
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
                    handleMethodHandleInvoke(caller, pc, rcvr, params, None, isSignaturePolymorphic = false)
                case _ ⇒
            }
        case _ ⇒
    }

    /**
     * Adds classes that can be loaded by an invocation of Class.forName to the set of loaded
     * classes.
     */
    private[this] def handleForName(className: Expr[V])(implicit state: State): Unit = {
        val loadedClasses = getPossibleForNameClasses(className, None) collect {
            case r: ObjectType                              ⇒ r
            case a: ArrayType if a.elementType.isObjectType ⇒ a.elementType.asObjectType
        }
        state.addNewLoadedClasses(loadedClasses.filter(!state.loadedClassesUB.contains(_)))
    }

    /**
     * Analyzes an invocation of Constructor.newInstance.
     * Adds invocations of constructors as well as instantiated types if the Constructor instance
     * was acquired via Class.getConstructor where the possible classes are (partially) known.
     */
    private[this] def handleConstructorNewInstance(
        caller:            DefinedMethod,
        pc:                Int,
        constructor:       Expr[V],
        newInstanceParams: Expr[V]
    )(implicit state: State): Unit = {
        val actualParams = getParamsFromVararg(newInstanceParams)
        constructor.asVar.definedBy.foreach { index ⇒
            if (index > 0) {
                val definition = state.tacode.stmts(index).asAssignment.expr
                if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.Class, _, "getConstructor", _, classes, params) ⇒
                            val parameterTypes = getTypesFromVararg(params.head, pc)
                            val descriptor = parameterTypes.map(s ⇒ MethodDescriptor(s, VoidType))
                            handleNewInstance(caller, pc, classes, descriptor, actualParams)
                        case _ ⇒ state.calleesAndCallers.addIncompleteCallsite(pc)
                    }
                } else {
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                }
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }
        }
    }

    /**
     * Handles invocations of constructors for all classes that the receiver could represent.
     */
    private[this] def handleNewInstance(
        caller:     DefinedMethod,
        pc:         Int,
        receiver:   Expr[V],
        descriptor: Option[MethodDescriptor],
        params:     Seq[V]
    )(implicit state: State): Unit = {
        val instantiatedTypes =
            getPossibleTypes(receiver, pc).asInstanceOf[Iterator[ObjectType]].toIterable
        handleNewInstances(caller, pc, instantiatedTypes, descriptor, params)
    }

    /**
     * Handles invocations of constructors for the given types and constructor descriptors.
     */
    private[this] def handleNewInstances(
        caller:            DefinedMethod,
        pc:                Int,
        instantiatedTypes: Iterable[ObjectType],
        descriptor:        Iterable[MethodDescriptor],
        params:            Seq[V]
    )(implicit state: State): Unit = {
        val newInstantiatedTypes = instantiatedTypes.filter(!state.instantiatedTypesUB.contains(_))
        state.addNewInstantiatedTypes(newInstantiatedTypes)

        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        // We don't have access to the receiver here!
        val actualParams = None +: params.map(persistentUVar)

        for {
            descriptor ← descriptor
            instantiatedType ← instantiatedTypes
        } {
            val method = project.specialCall(
                instantiatedType,
                instantiatedType,
                isInterface = false,
                "<init>",
                descriptor
            )

            state.calleesAndCallers.updateWithIndirectCallOrFallback(
                caller,
                method,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                instantiatedType,
                "<init>",
                descriptor,
                actualParams
            )
        }
    }

    /**
     * Analyzes an invocation of Method.invoke.
     * Adds calls to the potentially invoked methods if the Method instance was acquired via
     * Class.getDeclaredMethod or Class.getMethod and the possible classes are (partially) known.
     */
    private[this] def handleMethodInvoke(
        caller:       DefinedMethod,
        pc:           Int,
        method:       Expr[V],
        methodParams: Seq[Expr[V]]
    )(implicit state: State): Unit = {
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        val receiverDefinition = persistentUVar(methodParams.head.asVar)
        val paramDefinitions = getParamsFromVararg(methodParams(1)).map(persistentUVar)
        method.asVar.definedBy.foreach { index ⇒
            if (index >= 0) {
                val definition = stmts(index).asAssignment.expr
                if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredMethod" | "getMethod", _, receiver, params) ⇒
                            val types =
                                getPossibleTypes(receiver, pc).asInstanceOf[Iterator[ReferenceType]]
                            val names = getPossibleStrings(params.head, Some(pc)).toIterable
                            val paramTypesO = getTypesFromVararg(params(1), pc)
                            if (paramTypesO.isDefined) {
                                for {
                                    receiverType ← types
                                    name ← names
                                } {
                                    val callees = resolveCallees(
                                        receiverType,
                                        name,
                                        paramTypesO.get,
                                        definition.asVirtualFunctionCall.name == "getMethod",
                                        pc
                                    )
                                    for (callee ← callees)
                                        state.calleesAndCallers.updateWithIndirectCall(
                                            caller,
                                            callee,
                                            pc,
                                            receiverDefinition +: paramDefinitions
                                        )
                                }
                            }
                        case _ ⇒ state.calleesAndCallers.addIncompleteCallsite(pc)
                    }
                } else {
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                }
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }
        }
    }

    /**
     * Resolves a method as specified by Class.getDeclaredMethod and Class.getMethod.
     * @param recurse True to resolve as specified by Class.getMethod (recursing to superclasses),
     *                false to resolve as specified by Class.getDeclaredMethod (not recursing).
     */
    private[this] def resolveCallees(
        classType:   ReferenceType,
        name:        String,
        paramTypes:  FieldTypes,
        recurse:     Boolean,
        pc:          Int,
        allowStatic: Boolean       = true
    )(implicit state: State): Traversable[DeclaredMethod] = {
        if (classType.isArrayType && name == "clone") None
        else {
            val recType = if (classType.isArrayType) ObjectType.Object else classType.asObjectType
            val cfO = project.classFile(recType)
            if (cfO.isEmpty) {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                Traversable.empty
            } else {
                val candidates = cfO.get.methods.filter { method ⇒
                    (!recurse || method.isPublic) /* getMethod only returns public methods */ &&
                        (allowStatic || !method.isStatic) &&
                        method.name == name &&
                        method.parameterTypes == paramTypes
                }
                if (candidates.size == 1) {
                    // Single candidate is chosen
                    Traversable(declaredMethods(candidates.head))
                } else if (candidates.isEmpty) {
                    // No candidate => recurse to superclass if allowed, otherwise return no method
                    val superT = cfO.get.superclassType
                    if (!recurse || superT.isEmpty) {
                        state.calleesAndCallers.addIncompleteCallsite(pc)
                        Traversable.empty
                    } else {
                        resolveCallees(
                            superT.get,
                            name,
                            paramTypes,
                            recurse,
                            pc,
                            allowStatic = false // Static methods are not resolved on superclasses
                        )
                    }
                } else if (candidates.exists(!_.returnType.isReferenceType)) {
                    // There is no most specific return type for primitive types.
                    candidates.map[DeclaredMethod](declaredMethods(_))
                } else {
                    // Resolve to the candidate with the most specific return type.
                    val returnTypes = candidates.tail.map[ReferenceType](_.returnType.asReferenceType)
                    val first: Option[ReferenceType] = Some(returnTypes.head)
                    val foundType = returnTypes.tail.foldLeft(first) { (mostSpecific, current) ⇒
                        if (mostSpecific.isEmpty) None
                        else if (project.classHierarchy.isSubtypeOf(mostSpecific.get, current))
                            mostSpecific
                        else if (project.classHierarchy.isSubtypeOf(current, mostSpecific.get))
                            Some(current)
                        else {
                            None
                        }
                    }
                    if (foundType.isEmpty) {
                        // No most specific return type found => any of the methods could be invoked
                        candidates.map[DeclaredMethod](declaredMethods(_))
                    } else {
                        Traversable(
                            declaredMethods(candidates.find(_.returnType eq foundType.get).get)
                        )
                    }
                }
            }
        }
    }

    /**
     * Analyzes an invocation of MethodHandle.invoke.
     * Adds calls to the potentially invoked methods if the MethodHandle instance is a MethodHandle
     * constant or if it was required via a call to MethodHandles$Lookup.{findStatic, findVirtual,
     * findSpecial, findConstructor}.
     */
    private[this] def handleMethodHandleInvoke(
        caller:                 DefinedMethod,
        pc:                     Int,
        methodHandle:           Expr[V],
        invokeParams:           Seq[Expr[V]],
        descriptor:             Option[MethodDescriptor],
        isSignaturePolymorphic: Boolean
    )(implicit state: State): Unit = {
        val actualInvokeParams =
            if (isSignaturePolymorphic) invokeParams.asInstanceOf[Seq[V]]
            else getParamsFromVararg(invokeParams.head)
        methodHandle.asVar.definedBy.foreach { index ⇒
            if (index >= 0) {
                val definition = state.tacode.stmts(index).asAssignment.expr
                if (definition.isMethodHandleConst) {
                    definition.asMethodHandleConst.value match {
                        case InvokeStaticMethodHandle(receiver, _, name, desc) ⇒
                            handleInvokeStatic(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Seq(name),
                                Seq(desc),
                                actualInvokeParams
                            )
                        case InvokeVirtualMethodHandle(receiver, name, desc) ⇒
                            handleInvokeVirtual(
                                caller,
                                pc,
                                Iterator(receiver),
                                Seq(name),
                                Seq(desc),
                                actualInvokeParams
                            )
                        case InvokeInterfaceMethodHandle(receiver, name, desc) ⇒
                            handleInvokeVirtual(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Seq(name),
                                Seq(desc),
                                actualInvokeParams
                            )
                        case InvokeSpecialMethodHandle(receiver, isInterface, name, desc) ⇒
                            handleInvokeSpecial(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Some(isInterface),
                                Seq(name),
                                Seq(desc),
                                Seq(caller.declaringClassType.asObjectType),
                                actualInvokeParams
                            )
                        case NewInvokeSpecialMethodHandle(receiver, desc) ⇒
                            handleNewInstances(
                                caller,
                                pc,
                                Seq(receiver),
                                Seq(desc),
                                actualInvokeParams
                            )
                        case _ ⇒ state.calleesAndCallers.addIncompleteCallsite(pc)
                    }
                } else if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findStatic", _, _, params) ⇒
                            val types =
                                getPossibleTypes(params.head, pc).asInstanceOf[Iterator[ObjectType]]
                            val names = getPossibleStrings(params(1), Some(pc)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) descriptor.toIterable
                                else getPossibleMethodTypes(params(2), pc).toIterable
                            handleInvokeStatic(
                                caller,
                                pc,
                                types,
                                names,
                                descriptors,
                                actualInvokeParams
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findVirtual", _, _, params) ⇒
                            val staticTypes = getPossibleTypes(params.head, pc)
                            val names = getPossibleStrings(params(1), Some(pc)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get // Peel off receiver class
                                    Seq(MethodDescriptor(md.parameterTypes.tail, md.returnType))
                                } else
                                    getPossibleMethodTypes(params(2), pc).toIterable
                            handleInvokeVirtual(
                                caller,
                                pc,
                                staticTypes.asInstanceOf[Iterator[ReferenceType]],
                                names,
                                descriptors,
                                actualInvokeParams
                            )

                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findSpecial", _, _, params) ⇒
                            val types =
                                getPossibleTypes(params.head, pc).asInstanceOf[Iterator[ObjectType]]
                            val names = getPossibleStrings(params(1), Some(pc)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get // Peel off receiver class
                                    Seq(MethodDescriptor(md.parameterTypes.tail, md.returnType))
                                } else
                                    getPossibleMethodTypes(params(2), pc).toIterable
                            val specialCallers = getPossibleTypes(params(3), pc).toIterable
                            handleInvokeSpecial(
                                caller,
                                pc,
                                types,
                                None,
                                names,
                                descriptors,
                                specialCallers.asInstanceOf[Iterable[ObjectType]],
                                actualInvokeParams
                            )
                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findConstructor", _, _, params) ⇒
                            val classes = getPossibleTypes(params.head, pc)
                            val types = classes.asInstanceOf[Iterator[ObjectType]].toIterable
                            val descriptors: Iterable[MethodDescriptor] =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get
                                    Seq(MethodDescriptor(md.parameterTypes, VoidType))
                                } else
                                    getPossibleMethodTypes(params(1), pc).toIterable
                            handleNewInstances(caller, pc, types, descriptors, actualInvokeParams)
                        case _ ⇒ state.calleesAndCallers.addIncompleteCallsite(pc)
                    }
                } else {
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                }
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
            }
        }
    }

    /**
     * Handles an invocation of a static method via MethodHandle.invoke.
     */
    private[this] def handleInvokeStatic(
        caller:        DefinedMethod,
        pc:            Int,
        receiverTypes: Iterator[ObjectType],
        names:         Iterable[String],
        descriptors:   Iterable[MethodDescriptor],
        invokeParams:  Seq[V]
    )(implicit state: State): Unit = {
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        for {
            receiverType ← receiverTypes
            name ← names
            desc ← descriptors
        } {
            val callee = project.staticCall(
                receiverType,
                project.classFile(receiverType).exists(_.isInterfaceDeclaration),
                name,
                desc
            )
            state.calleesAndCallers.updateWithIndirectCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                receiverType,
                name,
                desc,
                None +: invokeParams.map(persistentUVar)
            )
        }
    }

    /**
     * Handles an invocation of a virtual/interface method via MethodHandle.invoke.
     */
    private[this] def handleInvokeVirtual(
        caller:       DefinedMethod,
        pc:           Int,
        staticTypes:  Iterator[ReferenceType],
        names:        Iterable[String],
        descriptors:  Iterable[MethodDescriptor],
        invokeParams: Seq[V]
    )(implicit state: State): Unit = {
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        val dynamicTypes = getTypesOfVar(invokeParams.head, pc).toIterable
        for {
            typeBound ← staticTypes
            receiverType ← dynamicTypes
            if classHierarchy.isASubtypeOf(receiverType, typeBound).isYesOrUnknown
            name ← names
            desc ← descriptors
        } {
            val callee = project.instanceCall(
                caller.declaringClassType.asObjectType,
                receiverType,
                name,
                desc
            )
            state.calleesAndCallers.updateWithIndirectCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                if (receiverType.isArrayType) ObjectType.Object else receiverType.asObjectType,
                name,
                desc,
                invokeParams.map(persistentUVar)
            )
        }
    }

    /**
     * Handles an invocation of a private/super method via MethodHandle.invoke.
     */
    private[this] def handleInvokeSpecial(
        caller:         DefinedMethod,
        pc:             Int,
        receiverTypes:  Iterator[ObjectType],
        isInterface:    Option[Boolean],
        names:          Iterable[String],
        descriptors:    Iterable[MethodDescriptor],
        specialCallers: Iterable[ObjectType],
        invokeParams:   Seq[V]
    )(implicit state: State): Unit = {
        implicit val stmts: Array[Stmt[V]] = state.tacode.stmts
        for {
            receiverType ← receiverTypes
            name ← names
            desc ← descriptors
            specialCaller ← specialCallers
        } {
            val callee = project.specialCall(
                specialCaller,
                receiverType,
                isInterface.getOrElse(project.classHierarchy.isInterface(receiverType).isYes),
                name,
                desc
            )
            state.calleesAndCallers.updateWithIndirectCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                receiverType,
                name,
                desc,
                invokeParams.map(persistentUVar)
            )
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
    )(implicit state: State): Iterator[String] = {
        value.asVar.definedBy.iterator.map[Iterator[String]] { index ⇒
            if (index >= 0) {
                val expr = state.tacode.stmts(index).asAssignment.expr
                expr match {
                    case StringConst(_, v) ⇒ Iterator(v)
                    case StaticFunctionCall(_, ObjectType.System, _, "getProperty", GetPropertyDescriptor, params) if !onlyStringConsts ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc)
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc)
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetOrDefaultPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc) ++
                            getPossibleStrings(params(1), None, onlyStringConsts = true)
                    case VirtualFunctionCall(_, dc, _, "get", GetDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc)
                    case _ ⇒
                        if (pc.isDefined) {
                            state.calleesAndCallers.addIncompleteCallsite(pc.get)
                        }
                        Iterator.empty
                }
            } else {
                if (pc.isDefined) {
                    state.calleesAndCallers.addIncompleteCallsite(pc.get)
                }
                Iterator.empty
            }
        }.flatten
    }

    /**
     * Returns Strings that ma be loaded from a Properties object if the key is a constant String
     * that the given expression may evaluate to.
     */
    private[this] def getStringConstsForSystemPropertiesKey(
        value: Expr[V], pc: Option[Int]
    )(implicit state: State): Iterator[String] = {
        if (!state.hasSystemPropertiesDependee) {
            state.addSystemPropertiesDependee(propertyStore(project, SystemProperties.key))
        }
        if (pc.isDefined) {
            state.calleesAndCallers.addIncompleteCallsite(pc.get)
        }
        if (state.hasSystemProperties) {
            Iterator.empty
        } else {
            val keys = getPossibleStrings(value, None, onlyStringConsts = true)
            keys.flatMap { key ⇒
                state.systemProperties.getOrElse(key, Set.empty[String])
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
    )(implicit state: State): Iterator[Type] = {

        def isForName(expr: Expr[V]): Boolean = { // static call to Class.forName
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.Class) &&
                expr.asStaticFunctionCall.name == "forName"
        }

        def isGetClass(expr: Expr[V]): Boolean = { // virtual call to Object.getClass
            expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
                expr.asVirtualFunctionCall.descriptor == MethodDescriptor.withNoArgs(ObjectType.Class)
        }

        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                false
            } else {
                val expr = state.tacode.stmts(index).asAssignment.expr
                val isResolvable =
                    expr.isClassConst || isForName(expr) || isBaseTypeLoad(expr) || isGetClass(expr)
                if (!isResolvable)
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                isResolvable
            }
        } flatMap { index: Int ⇒
            val expr = state.tacode.stmts(index).asAssignment.expr
            if (expr.isClassConst) {
                Iterator(state.tacode.stmts(index).asAssignment.expr.asClassConst.value)
            } else if (expr.isStaticFunctionCall) {
                getPossibleForNameClasses(expr.asStaticFunctionCall.params.head, Some(pc))
            } else if (expr.isVirtualFunctionCall) {
                getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar, pc)
            } else {
                Iterator(getBaseType(expr))
            }
        }
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
    )(implicit state: State): Iterator[ReferenceType] = {
        val value = uvar.value.asReferenceValue
        if (value.isPrecise) value.leastUpperType.iterator
        else if (value.allValues.forall(_.isPrecise))
            value.allValues.toIterator.flatMap(_.leastUpperType)
        else {
            state.calleesAndCallers.addIncompleteCallsite(pc)
            Iterator.empty
        }
    }

    /**
     * Returns classes that may be loaded by an invocation of Class.forName.
     */
    private[this] def getPossibleForNameClasses(
        className: Expr[V],
        pc:        Option[Int]
    )(implicit state: State): Iterator[ReferenceType] = {
        val classNames = getPossibleStrings(className, pc)
        classNames.map(cls ⇒
            ObjectType(cls.replace('.', '/'))).filter(project.classFile(_).isDefined)
    }

    /**
     * Returns method types (aka. descriptors) that a given expression potentially evaluates to.
     * Identifies local use of MethodType constants as well as method types acquired from
     * MethodType.methodType.
     */
    private[this] def getPossibleMethodTypes(
        value: Expr[V],
        pc:    Int
    )(implicit state: State): Iterator[MethodDescriptor] = {

        def isMethodType(expr: Expr[V]): Boolean = {
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.MethodType) &&
                expr.asStaticFunctionCall.name == "methodType"
        }

        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                false
            } else {
                val expr = state.tacode.stmts(index).asAssignment.expr
                val isResolvable = expr.isMethodTypeConst || isMethodType(expr)
                if (!isResolvable)
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                isResolvable
            }
        } flatMap { index: Int ⇒
            val expr = state.tacode.stmts(index).asAssignment.expr
            if (expr.isMethodTypeConst)
                Iterator(state.tacode.stmts(index).asAssignment.expr.asMethodTypeConst.value)
            else {
                val call = expr.asStaticFunctionCall
                getPossibleMethodTypes(call.params, call.descriptor, pc)
            }
        }
    }

    /**
     * Returns method types that a call to MethodType.methodType may return.
     */
    private[this] def getPossibleMethodTypes(
        params:     Seq[Expr[V]],
        descriptor: MethodDescriptor,
        pc:         Int
    )(implicit state: State): Iterator[MethodDescriptor] = {
        val returnTypes = getPossibleTypes(params.head, pc)
        if (params.size == 1) { // methodType(T) => ()T
            returnTypes.map(MethodDescriptor.withNoArgs)
        } else if (params.size == 3) { // methodType(T1, T2, T3, ...) => (T2, T3, ...)T1
            val firstParamTypes = getPossibleTypes(params(1), pc).asInstanceOf[Iterator[FieldType]]
            val possibleOtherParamTypes = getTypesFromVararg(params(2), pc)
            for {
                returnType ← returnTypes
                firstParamType ← firstParamTypes
                otherParamTypes ← possibleOtherParamTypes
            } yield MethodDescriptor(firstParamType +: otherParamTypes, returnType)
        } else {
            val secondParamType = descriptor.parameterType(1)
            if (secondParamType.isArrayType) { // methodType(T1, Class[]{T2, ...}) => (T2, ...)T1
                val possibleOtherParamTypes = getTypesFromVararg(params(1), pc)
                for {
                    returnType ← returnTypes
                    otherParamTypes ← possibleOtherParamTypes
                } yield MethodDescriptor(otherParamTypes, returnType)
            } else if (secondParamType == ObjectType.Class) { // methodType(T1, T2) => (T2)T2
                val paramTypes = getPossibleTypes(params(1), pc).asInstanceOf[Iterator[FieldType]]
                for {
                    returnType ← returnTypes
                    paramType ← paramTypes
                } yield MethodDescriptor(paramType, returnType)
            } else { // we don't handle methodType(T1, List(T2, ...)) and methodType(T1, MethodType)
                state.calleesAndCallers.addIncompleteCallsite(pc)
                Iterator.empty
            }
        }
    }

    /**
     * Returns the types that a varargs argument of type Class (i.e. Class<?>...) may evaluate to.
     * Only handles the case of a simple array of class constants or primitive types' classes!
     */
    private[this] def getTypesFromVararg(
        expr: Expr[V],
        pc:   Int
    )(implicit state: State): Option[FieldTypes] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            state.calleesAndCallers.addIncompleteCallsite(pc)
            None
        } else {
            val definition = state.tacode.stmts(definitions.head).asAssignment
            if (definition.expr.astID != NewArray.ASTID) None
            else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.tacode.cfg.bb(uses.head) != state.tacode.cfg.bb(uses.last)) None
                else if (state.tacode.stmts(uses.last).astID != Assignment.ASTID) None
                else {
                    var types: RefArray[FieldType] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last) true
                        else {
                            val use = state.tacode.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID) false
                            else {
                                val typeDefs = use.asArrayStore.value.asVar.definedBy
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!typeDefs.isSingletonSet || typeDefs.head < 0 ||
                                    !indices.isSingletonSet || indices.head < 0) false
                                else {
                                    val typeDef = state.tacode.stmts(typeDefs.head).asAssignment.expr
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
                        state.calleesAndCallers.addIncompleteCallsite(pc)
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
    )(implicit state: State): Seq[V] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            Seq.empty
        } else {
            val definition = state.tacode.stmts(definitions.head).asAssignment
            if (definition.expr.astID != NewArray.ASTID) Seq.empty
            else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.tacode.cfg.bb(uses.head) != state.tacode.cfg.bb(uses.last)) Seq.empty
                else if (state.tacode.stmts(uses.last).astID != Assignment.ASTID) Seq.empty
                else {
                    var params: Seq[V] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last) true
                        else {
                            val use = state.tacode.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID) false
                            else {
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!indices.isSingletonSet || indices.head < 0) false
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
                        Seq.empty
                    } else {
                        params
                    }
                }
            }
        }
    }

    private[this] def continuation(eps: SomeEPS)(implicit state: State): PropertyComputationResult = eps match {
        case ESimplePS(_, ub: SystemProperties, isFinal) ⇒
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

        case ESimplePS(_, _: TACAI, isFinal) ⇒
            state.removeTACDependee()
            state.addTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
            processMethod(state)

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

    @inline private[this] def returnResult()(implicit state: State): PropertyComputationResult = {
        var res: List[PropertyComputationResult] = state.calleesAndCallers.partialResultsForCallers

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
                SimplePIntermediateResult(
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
                case IntermediateESimpleP(p, ub) ⇒
                    val newUb = ub.classes ++ state.newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(IntermediateESimpleP(p, ub.updated(state.newLoadedClasses)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(IntermediateESimpleP(p, LoadedClasses.initial(state.newLoadedClasses)))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
        }

        if (state.newInstantiatedTypes.nonEmpty)
            res ::= RTACallGraphAnalysis.partialResultForInstantiatedTypes(
                p, state.newInstantiatedTypes, initialInstantiatedTypes
            )

        Results(res)
    }
}

object EagerReflectionRelatedCallsAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = ReflectionRelatedCallsAnalysis

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      ReflectionRelatedCallsAnalysis
    ): FPCFAnalysis = {
        analysis
    }

    override def uses: Set[PropertyKind] =
        Set(CallersProperty, SystemProperties, LoadedClasses, InstantiatedTypes, TACAI)

    override def derives: Set[PropertyKind] =
        Set(CallersProperty, ReflectionRelatedCallees, LoadedClasses, InstantiatedTypes)

    override def init(p: SomeProject, ps: PropertyStore): ReflectionRelatedCallsAnalysis = {
        val analysis = new ReflectionRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
