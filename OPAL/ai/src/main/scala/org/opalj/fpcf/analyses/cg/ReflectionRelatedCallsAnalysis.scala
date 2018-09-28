/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

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
import org.opalj.br.MethodDescriptor
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
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
import org.opalj.fpcf.cg.properties.OnlyVMLevelCallers
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.ReflectionRelatedCalleesImplementation
import org.opalj.fpcf.properties.SystemProperties
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.NewArray
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

import scala.collection.immutable.IntMap
import scala.language.existentials

/**
 * Finds calls and loaded classes that exist because of reflective calls that are easy to resolve.
 *
 * @author Dominik Helm
 * @author Michael Reif
 */
class ReflectionRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val MethodT = ObjectType("java/lang/reflect/Method")

    val PropertiesT = ObjectType("java/util/Properties")

    val GetPropertyDescriptor = MethodDescriptor(ObjectType.String, ObjectType.String)
    val GetOrDefaultPropertyDescriptor = MethodDescriptor(RefArray(ObjectType.String, ObjectType.String), ObjectType.String)
    val GetDescriptor = MethodDescriptor(ObjectType.Object, ObjectType.Object)

    class State(
            val definedMethod:        DefinedMethod,
            val stmts:                Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            val pcToIndex:            Array[Int],
            val loadedClassesUB:      UIDSet[ObjectType],
            val instantiatedTypesUB:  UIDSet[ObjectType],
            val calleesAndCallers:    CalleesAndCallers,
            var newLoadedClasses:     UIDSet[ObjectType]                              = UIDSet.empty,
            var newInstantiatedTypes: UIDSet[ObjectType]                              = UIDSet.empty,
            var dependee:             Option[EOptionP[SomeProject, SystemProperties]] = None
    )

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val tacai = project.get(SimpleTACAIKey)

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

        var relevantPCs: IntMap[IntTrieSet] = IntMap.empty
        var forNamePCs: IntMap[IntTrieSet] = IntMap.empty
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
                            forNamePCs += i → IntTrieSet.empty
                    case INVOKEVIRTUAL.opcode ⇒
                        val call = inst.asMethodInvocationInstruction
                        call.declaringClass match {
                            case ObjectType.Class ⇒
                                if (call.name == "newInstance") relevantPCs += i → IntTrieSet.empty
                            case ConstructorT ⇒
                                if (call.name == "newInstance") relevantPCs += i → IntTrieSet.empty
                            case MethodT ⇒
                                if (call.name == "invoke") relevantPCs += i → IntTrieSet.empty
                            case ObjectType.MethodHandle ⇒
                                if (call.name.startsWith("invoke"))
                                    relevantPCs += i → IntTrieSet.empty
                            case _ ⇒
                        }
                    case _ ⇒
                }
            i += 1
        }

        if (relevantPCs.isEmpty && forNamePCs.isEmpty)
            return Result(declaredMethod, NoReflectionRelatedCallees);

        val (loadedClassesUB, instantiatedTypesUB) = getLoadedClassesAndInstantiatedTypes()

        val tacode = tacai(method)
        val stmts = tacode.stmts
        val cfg = tacode.cfg
        val pcToIndex = tacode.pcToIndex

        val calleesAndCallers = new CalleesAndCallers(relevantPCs)

        implicit val state: State =
            new State(
                definedMethod,
                stmts,
                cfg,
                pcToIndex,
                loadedClassesUB,
                instantiatedTypesUB,
                calleesAndCallers
            )

        for {
            pc ← forNamePCs.keysIterator
            index = pcToIndex(pc)
            if index >= 0
            stmt = stmts(index)
        } {
            val expr = if (stmt.astID == Assignment.ASTID) stmt.asAssignment.expr
            else stmt.asExprStmt.expr
            handleForName(expr.asStaticFunctionCall.params.head)
        }

        analyzeInvocations()

        returnResult()
    }

    private[this] def analyzeInvocations()(implicit state: State): Unit = {
        for {
            pc ← state.calleesAndCallers.callees.keysIterator
            index = state.pcToIndex(pc)
            if index >= 0
            stmt = state.stmts(index)
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
                        handleMethodHandleInvoke(state.definedMethod, pc, receiver, params, None)
                case _ ⇒

            }
        }
    }

    private[this] def handleExpr(
        caller: DefinedMethod,
        pc:     Int,
        expr:   Expr[V]
    )(implicit state: State): Unit = expr.astID match {
        case VirtualFunctionCall.ASTID ⇒
            expr.asVirtualFunctionCall match {
                case VirtualFunctionCall(_, ObjectType.Class, _, "newInstance", _, receiver, _) ⇒
                    handleClassNewInstance(caller, pc, receiver)
                case VirtualFunctionCall(_, ConstructorT, _, "newInstance", _, receiver, _) ⇒
                    handleConstructorNewInstance(caller, pc, receiver)
                case VirtualFunctionCall(_, MethodT, _, "invoke", _, receiver, _) ⇒
                    handleMethodInvoke(caller, pc, receiver)
                case VirtualFunctionCall(_, ObjectType.MethodHandle, _, "invokeExact", desc, receiver, params) ⇒
                    handleMethodHandleInvoke(caller, pc, receiver, params, Some(desc))
                case VirtualFunctionCall(_, ObjectType.MethodHandle, _, "invoke" |
                    "invokeWithArguments", _, receiver, params) ⇒
                    handleMethodHandleInvoke(caller, pc, receiver, params, None)
                case _ ⇒
            }
        case _ ⇒
    }

    private[this] def handleForName(className: Expr[V])(implicit state: State): Unit = {
        val loadedClasses = getPossibleForNameClasses(className, None) collect {
            case r: ObjectType                              ⇒ r
            case a: ArrayType if a.elementType.isObjectType ⇒ a.elementType.asObjectType
        }
        state.newLoadedClasses ++= loadedClasses.filter(!state.loadedClassesUB.contains(_))
    }

    private[this] def handleClassNewInstance(
        caller:  DefinedMethod,
        pc:      Int,
        classes: Expr[V]
    )(implicit state: State): Unit = {
        handleNewInstance(caller, pc, classes, Some(MethodDescriptor.NoArgsAndReturnVoid))
    }

    private[this] def handleNewInstance(
        caller:     DefinedMethod,
        pc:         Int,
        receiver:   Expr[V],
        descriptor: Option[MethodDescriptor]
    )(implicit state: State): Unit = {
        val instantiatedTypes =
            getPossibleTypes(receiver, pc).asInstanceOf[Iterator[ObjectType]].toIterable
        handleNewInstances(caller, pc, instantiatedTypes, descriptor)
    }

    private[this] def handleNewInstances(
        caller:            DefinedMethod,
        pc:                Int,
        instantiatedTypes: Iterable[ObjectType],
        descriptor:        Iterable[MethodDescriptor]
    )(implicit state: State): Unit = {
        val newInstantiatedTypes = instantiatedTypes.filter(!state.instantiatedTypesUB.contains(_))
        state.newInstantiatedTypes ++= newInstantiatedTypes

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

            state.calleesAndCallers.updateWithCallOrFallback(
                caller,
                method,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                instantiatedType,
                "<init>",
                descriptor
            )
        }
    }

    private[this] def handleConstructorNewInstance(
        caller:      DefinedMethod,
        pc:          Int,
        constructor: Expr[V]
    )(implicit state: State): Unit = {
        constructor.asVar.definedBy.foreach { index ⇒
            if (index > 0) {
                val definition = state.stmts(index).asAssignment.expr
                if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.Class, _, "getConstructor", _, classes, params) ⇒
                            val parameterTypes = getTypesFromVararg(params.head, pc)

                            val descriptor = parameterTypes.map(s ⇒ MethodDescriptor(s, VoidType))
                            handleNewInstance(caller, pc, classes, descriptor)
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

    private[this] def handleMethodInvoke(
        caller: DefinedMethod,
        pc:     Int,
        method: Expr[V]
    )(implicit state: State): Unit = {
        method.asVar.definedBy.foreach { index ⇒
            if (index >= 0) {
                val definition = state.stmts(index).asAssignment.expr
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
                                        state.calleesAndCallers.updateWithCall(caller, callee, pc)
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
                    (!recurse || method.isPublic) &&
                        (allowStatic || !method.isStatic) &&
                        method.name == name &&
                        method.parameterTypes == paramTypes
                }
                if (candidates.size == 1) {
                    Traversable(declaredMethods(candidates.head))
                } else if (candidates.isEmpty) {
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
                            allowStatic = false
                        )
                    }
                } else if (candidates.exists(!_.returnType.isReferenceType)) {
                    candidates.map[DeclaredMethod](declaredMethods(_))
                } else {
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

    private[this] def handleMethodHandleInvoke(
        caller:       DefinedMethod,
        pc:           Int,
        methodHandle: Expr[V],
        invokeParams: Seq[Expr[V]],
        descriptor:   Option[MethodDescriptor]
    )(implicit state: State): Unit = {
        methodHandle.asVar.definedBy.foreach { index ⇒
            if (index >= 0) {
                val definition = state.stmts(index).asAssignment.expr
                if (definition.isMethodHandleConst) {
                    definition.asMethodHandleConst.value match {
                        case InvokeStaticMethodHandle(receiver, _, name, desc) ⇒
                            handleInvokeStatic(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Seq(name),
                                Seq(desc)
                            )
                        case InvokeVirtualMethodHandle(receiver, name, desc) ⇒
                            handleInvokeVirtual(
                                caller,
                                pc,
                                Iterator(receiver),
                                Seq(name),
                                Seq(desc),
                                invokeParams
                            )
                        case InvokeInterfaceMethodHandle(receiver, name, desc) ⇒
                            handleInvokeVirtual(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Seq(name),
                                Seq(desc),
                                invokeParams
                            )
                        case InvokeSpecialMethodHandle(receiver, isInterface, name, desc) ⇒
                            handleInvokeSpecial(
                                caller,
                                pc,
                                Iterator(receiver.asObjectType),
                                Some(isInterface),
                                Seq(name),
                                Seq(desc),
                                Seq(caller.declaringClassType.asObjectType)
                            )
                        case NewInvokeSpecialMethodHandle(receiver, desc) ⇒
                            handleNewInstances(caller, pc, Seq(receiver), Seq(desc))
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
                            handleInvokeStatic(caller, pc, types, names, descriptors)

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
                                invokeParams
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
                                specialCallers.asInstanceOf[Iterable[ObjectType]]
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
                            handleNewInstances(caller, pc, types, descriptors)
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

    private[this] def handleInvokeStatic(
        caller:        DefinedMethod,
        pc:            Int,
        receiverTypes: Iterator[ObjectType],
        names:         Iterable[String],
        descriptors:   Iterable[MethodDescriptor]
    )(implicit state: State): Unit = {
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
            state.calleesAndCallers.updateWithCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                receiverType,
                name,
                desc
            )
        }
    }

    private[this] def handleInvokeVirtual(
        caller:       DefinedMethod,
        pc:           Int,
        staticTypes:  Iterator[ReferenceType],
        names:        Iterable[String],
        descriptors:  Iterable[MethodDescriptor],
        invokeParams: Seq[Expr[V]]
    )(implicit state: State): Unit = {
        val actualReceiver = invokeParams.head.asVar
        val dynamicTypes = getTypesOfVar(actualReceiver, pc).toIterable
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
            state.calleesAndCallers.updateWithCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                if (receiverType.isArrayType) ObjectType.Object else receiverType.asObjectType,
                name,
                desc
            )
        }
    }

    private[this] def handleInvokeSpecial(
        caller:         DefinedMethod,
        pc:             Int,
        receiverTypes:  Iterator[ObjectType],
        isInterface:    Option[Boolean],
        names:          Iterable[String],
        descriptors:    Iterable[MethodDescriptor],
        specialCallers: Iterable[ObjectType]
    )(implicit state: State): Unit = {
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
            state.calleesAndCallers.updateWithCallOrFallback(
                caller,
                callee,
                pc,
                caller.declaringClassType.asObjectType.packageName,
                receiverType,
                name,
                desc
            )
        }
    }

    private[this] def getPossibleStrings(
        value:            Expr[V],
        pc:               Option[Int],
        onlyStringConsts: Boolean     = false
    )(implicit state: State): Iterator[String] = {
        value.asVar.definedBy.iterator.map[Iterator[String]] { index ⇒
            if (index >= 0) {
                val expr = state.stmts(index).asAssignment.expr
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

    private[this] def getStringConstsForSystemPropertiesKey(
        value: Expr[V], pc: Option[Int]
    )(implicit state: State): Iterator[String] = {
        if (state.dependee.isEmpty) {
            state.dependee = Some(propertyStore(project, SystemProperties.key))
        }
        if (pc.isDefined) {
            state.calleesAndCallers.addIncompleteCallsite(pc.get)
        }
        if (state.dependee.get.isInstanceOf[SomeEPK]) {
            Iterator.empty
        } else {
            val keys = getPossibleStrings(value, None, onlyStringConsts = true)
            keys.flatMap { key ⇒
                state.dependee.get.ub.properties.getOrElse(key, Set.empty[String])
            }
        }
    }

    private[this] def getPossibleTypes(
        value: Expr[V],
        pc:    Int
    )(implicit state: State): Iterator[Type] = {
        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                false
            } else {
                val expr = state.stmts(index).asAssignment.expr
                val isResolvable =
                    expr.isClassConst || isForName(expr) || isBaseTypeLoad(expr) || isGetClass(expr)
                if (!isResolvable)
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                isResolvable
            }
        } flatMap { index: Int ⇒
            val expr = state.stmts(index).asAssignment.expr
            if (expr.isClassConst) Iterator(state.stmts(index).asAssignment.expr.asClassConst.value)
            else if (expr.isStaticFunctionCall)
                getPossibleForNameClasses(expr.asStaticFunctionCall.params.head, Some(pc))
            else if (expr.isVirtualFunctionCall) {
                getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar, pc)
            } else {
                val declClass = expr.asGetStatic.declaringClass
                if (declClass == VoidType.WrapperType) Iterator(VoidType)
                else Iterator(BaseType.baseTypes.iterator.find(declClass == _.WrapperType).get)
            }
        }
    }

    private[this] def getTypesOfVar(
        uvar: V,
        pc:   Int
    )(implicit state: State): Iterator[ReferenceType] = {
        val value = uvar.value.asReferenceValue
        if (value.isPrecise) value.valueType.iterator
        else if (value.allValues.forall(_.isPrecise))
            value.allValues.toIterator.flatMap(_.valueType)
        else {
            state.calleesAndCallers.addIncompleteCallsite(pc)
            Iterator.empty
        }
    }

    private[this] def isForName(expr: Expr[V]): Boolean = {
        expr.isStaticFunctionCall &&
            (expr.asStaticFunctionCall.declaringClass eq ObjectType.Class) &&
            expr.asStaticFunctionCall.name == "forName"
    }

    private[this] def isBaseTypeLoad(expr: Expr[V]): Boolean = {
        expr.isGetStatic && expr.asGetStatic.name == "TYPE" && {
            val declClass = expr.asGetStatic.declaringClass
            declClass == VoidType.WrapperType ||
                BaseType.baseTypes.iterator.map(_.WrapperType).contains(declClass)
        }
    }

    private[this] def isGetClass(expr: Expr[V]): Boolean = {
        expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
            expr.asVirtualFunctionCall.descriptor == MethodDescriptor.withNoArgs(ObjectType.Class)
    }

    private[this] def getPossibleForNameClasses(
        className: Expr[V],
        pc:        Option[Int]
    )(implicit state: State): Iterator[ReferenceType] = {
        val classNames = getPossibleStrings(className, pc)
        classNames.map(cls ⇒
            ObjectType(cls.replace('.', '/'))
        ).filter(project.classFile(_).isDefined)
    }

    private[this] def getPossibleMethodTypes(
        value: Expr[V],
        pc:    Int
    )(implicit state: State): Iterator[MethodDescriptor] = {
        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                false
            } else {
                val expr = state.stmts(index).asAssignment.expr
                val isResolvable = expr.isMethodTypeConst || isMethodType(expr)
                if (!isResolvable)
                    state.calleesAndCallers.addIncompleteCallsite(pc)
                isResolvable
            }
        } flatMap { index: Int ⇒
            val expr = state.stmts(index).asAssignment.expr
            if (expr.isMethodTypeConst)
                Iterator(state.stmts(index).asAssignment.expr.asMethodTypeConst.value)
            else {
                val call = expr.asStaticFunctionCall
                getPossibleMethodTypes(call.params, call.descriptor, pc)
            }
        }
    }

    private[this] def getPossibleMethodTypes(
        params:     Seq[Expr[V]],
        descriptor: MethodDescriptor,
        pc:         Int
    )(implicit state: State): Iterator[MethodDescriptor] = {
        val returnTypes = getPossibleTypes(params.head, pc)
        if (params.size == 1) {
            returnTypes.map(MethodDescriptor.withNoArgs)
        } else if (params.size == 3) {
            val firstParamTypes = getPossibleTypes(params(1), pc).asInstanceOf[Iterator[FieldType]]
            val possibleOtherParamTypes = getTypesFromVararg(params(2), pc)
            for {
                returnType ← returnTypes
                firstParamType ← firstParamTypes
                otherParamTypes ← possibleOtherParamTypes
            } yield MethodDescriptor(firstParamType +: otherParamTypes, returnType)
        } else {
            val secondParamType = descriptor.parameterType(1)
            if (secondParamType.isArrayType) {
                val possibleOtherParamTypes = getTypesFromVararg(params(1), pc)
                for {
                    returnType ← returnTypes
                    otherParamTypes ← possibleOtherParamTypes
                } yield MethodDescriptor(otherParamTypes, returnType)
            } else if (secondParamType == ObjectType.Class) {
                val paramTypes = getPossibleTypes(params(1), pc).asInstanceOf[Iterator[FieldType]]
                for {
                    returnType ← returnTypes
                    paramType ← paramTypes
                } yield MethodDescriptor(paramType, returnType)
            } else {
                state.calleesAndCallers.addIncompleteCallsite(pc)
                Iterator.empty
            }
        }
    }

    private[this] def isMethodType(expr: Expr[V]): Boolean = {
        expr.isStaticFunctionCall &&
            (expr.asStaticFunctionCall.declaringClass eq ObjectType.MethodType) &&
            expr.asStaticFunctionCall.name == "methodType"
    }

    private[this] def getTypesFromVararg(
        expr: Expr[V],
        pc:   Int
    )(implicit state: State): Option[FieldTypes] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            state.calleesAndCallers.addIncompleteCallsite(pc)
            None
        } else {
            val definition = state.stmts(definitions.head).asAssignment
            if (definition.expr.astID != NewArray.ASTID) None
            else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.cfg.bb(uses.head) != state.cfg.bb(uses.last)) None
                else if (state.stmts(uses.last).astID != Assignment.ASTID) None
                else {
                    var types: RefArray[FieldType] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last) true
                        else {
                            val use = state.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID) false
                            else {
                                val typeDefs = use.asArrayStore.value.asVar.definedBy
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!typeDefs.isSingletonSet || typeDefs.head < 0 ||
                                    !indices.isSingletonSet || indices.head < 0) false
                                else {
                                    val typeDef = state.stmts(typeDefs.head).asAssignment.expr
                                    val index = state.stmts(indices.head).asAssignment.expr
                                    if (!typeDef.isClassConst || !index.isIntConst) false
                                    else {
                                        types = types.updated(index.asIntConst.value, typeDef.asClassConst.value)
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

    private[this] def continuation(eps: SomeEPS)(implicit state: State): PropertyComputationResult = eps match {
        case ESimplePS(_, _: SystemProperties, _) ⇒
            // Create new state that reflects changes that may have happened in the meantime
            val (loadedClassesUB, instantiatedTypesUB) = getLoadedClassesAndInstantiatedTypes()
            val newState: State = new State(
                state.definedMethod,
                state.stmts,
                state.cfg,
                state.pcToIndex,
                loadedClassesUB,
                instantiatedTypesUB,
                new CalleesAndCallers(state.calleesAndCallers.callees)
            )

            // Re-analyze invocations with the new state
            analyzeInvocations()(newState)
            returnResult()(newState)
    }

    private[this] def getLoadedClassesAndInstantiatedTypes(): (UIDSet[ObjectType], UIDSet[ObjectType]) = {
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
            case _              ⇒ InstantiatedTypes.initialTypes
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
                state.calleesAndCallers.incompleteCallsites
            )

        val calleesResult =
            if (state.dependee.isEmpty || state.dependee.get.isFinal) {
                Result(state.definedMethod, calleeUB)
            } else {
                SimplePIntermediateResult(
                    state.definedMethod,
                    calleeUB,
                    state.dependee,
                    continuation
                )
            }

        res ::= calleesResult

        if (state.newLoadedClasses.nonEmpty) {
            res ::= PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case IntermediateESimpleP(p, ub) ⇒
                    val newUb = ub.classes ++ state.newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(IntermediateESimpleP(p, new LoadedClasses(newUb)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(IntermediateESimpleP(p, new LoadedClasses(state.newLoadedClasses)))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
            state.newLoadedClasses flatMap { loaded ⇒
                LoadedClassesAnalysis.retrieveStaticInitializers(loaded, declaredMethods, project)
            } foreach { clInit ⇒
                res ::=
                    PartialResult[DeclaredMethod, CallersProperty](clInit, CallersProperty.key, {
                        case _: EPK[_, _] ⇒
                            Some(IntermediateESimpleP(clInit, OnlyVMLevelCallers))

                        case IntermediateESimpleP(_, ub) if !ub.hasCallersWithUnknownContext ⇒
                            Some(IntermediateESimpleP(clInit, ub.updatedWithVMLevelCall()))

                        case _: IntermediateESimpleP[_, _] ⇒
                            None

                        case r ⇒
                            throw new IllegalStateException(s"unexpected previous result $r")
                    })
            }
        }

        if (state.newInstantiatedTypes.nonEmpty)
            res ::=
                RTACallGraphAnalysis.partialResultForInstantiatedTypes(p, state.newInstantiatedTypes)

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
        Set(CallersProperty, SystemProperties, LoadedClasses, InstantiatedTypes)

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
