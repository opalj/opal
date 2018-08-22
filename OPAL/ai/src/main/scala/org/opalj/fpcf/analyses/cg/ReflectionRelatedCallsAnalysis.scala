/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.FieldType
import org.opalj.br.VoidType
import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.Type
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.InvokeVirtualMethodHandle
import org.opalj.br.InvokeInterfaceMethodHandle
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.NewInvokeSpecialMethodHandle
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.IntArraySetBuilder
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.NoCallers
import org.opalj.fpcf.properties.NoReflectionRelatedCallees
import org.opalj.fpcf.properties.ReflectionRelatedCallees
import org.opalj.fpcf.properties.ReflectionRelatedCalleesImplementation
import org.opalj.fpcf.properties.LoadedClasses
import org.opalj.fpcf.properties.LoadedClassesLowerBound
import org.opalj.fpcf.properties.LowerBoundCallers
import org.opalj.fpcf.properties.OnlyVMLevelCallers
import org.opalj.log.OPALLogger
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.Expr
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NewArray
import org.opalj.tac.TACStmts
import org.opalj.tac.ArrayStore
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.IsReferenceValue

/**
 * Finds calls and loaded classes that exist because of reflective calls that are easy to resolve.
 *
 * @author Dominik Helm
 */
class ReflectionRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val MethodT = ObjectType("java/lang/reflect/Method")

    class State(
            val stmts:                Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            val loadedClassesUB:      UIDSet[ObjectType],
            val instantiatedTypesUB:  UIDSet[ObjectType],
            var newLoadedClasses:     UIDSet[ObjectType]        = UIDSet.empty,
            var newInstantiatedTypes: UIDSet[ObjectType]        = UIDSet.empty,
            val calleesAndCallers:    CalleesAndCallers         = new CalleesAndCallers
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
        if (method.classFile.thisType ne declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

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

        val tacode = tacai(method)
        val stmts = tacode.stmts
        val cfg = tacode.cfg

        implicit val state: State = new State(stmts, cfg, loadedClassesUB, instantiatedTypesUB)

        for (stmt ← stmts) {
            stmt.astID match {
                case Assignment.ASTID ⇒ handleExpr(definedMethod, stmt.pc, stmt.asAssignment.expr)
                case ExprStmt.ASTID   ⇒ handleExpr(definedMethod, stmt.pc, stmt.asExprStmt.expr)
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(pc, declClass, _, name, _, receiver, params) = stmt
                    // invoke(withArguments) returns Object, but by signature polymorphism the call
                    // may still be a VirtualMethodCall if the return value is void or unused
                    if (declClass == ObjectType.MethodHandle &&
                        (name == "invoke" || name == "invokeWithArguments"))
                        handleMethodHandleInvoke(definedMethod, pc, receiver, params, None)
                case _ ⇒

            }
        }

        returnResult(definedMethod)
    }

    private[this] def handleExpr(
        caller: DefinedMethod,
        pc:     Int,
        expr:   Expr[V]
    )(implicit state: State): Unit = expr.astID match {
        case StaticFunctionCall.ASTID ⇒
            expr.asStaticFunctionCall match {
                case StaticFunctionCall(_, ObjectType.Class, _, "forName", _, params) ⇒
                    handleForName(params.head)
                case _ ⇒
            }
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
        val loadedClasses = getPossibleForNameClasses(className) collect {
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
            getPossibleTypes(receiver).asInstanceOf[Iterator[ObjectType]].toIterable
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
                            val parameterTypes = getTypes(params.head)
                            val descriptor = parameterTypes.map(s ⇒ MethodDescriptor(s, VoidType))
                            handleNewInstance(caller, pc, classes, descriptor)
                        case _ ⇒
                            OPALLogger.warn("analysis", "missed invoke call, constructor unknown")
                    }
                } else {
                    OPALLogger.warn("analysis", "missed invoke call, method unknown")
                }
            } else {
                OPALLogger.warn("analysis", "missed invoke call, method unknown")
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
                                getPossibleTypes(receiver).asInstanceOf[Iterator[ReferenceType]]
                            val names = getPossibleStrings(params.head).toIterable
                            val paramTypesO = getTypes(params(1))
                            if (paramTypesO.isDefined) {
                                for {
                                    receiverType ← types
                                    name ← names
                                } {
                                    val callees = resolveCallees(
                                        receiverType,
                                        name,
                                        paramTypesO.get,
                                        definition.asVirtualFunctionCall.name == "getMethod"
                                    )
                                    for (callee ← callees)
                                        state.calleesAndCallers.updateWithCall(caller, callee, pc)
                                }
                            }
                        case _ ⇒ OPALLogger.warn("analysis", "missed invoke call, method unknown")
                    }
                } else {
                    OPALLogger.warn("analysis", "missed invoke call, method unknown")
                }
            } else {
                OPALLogger.warn("analysis", "missed invoke call, method unknown")
            }
        }
    }

    private[this] def resolveCallees(
        classType:   ReferenceType,
        name:        String,
        paramTypes:  IndexedSeq[FieldType],
        recurse:     Boolean,
        allowStatic: Boolean               = true
    ): Traversable[DeclaredMethod] = {
        if (classType.isArrayType && name == "clone") None
        else {
            val recType = if (classType.isArrayType) ObjectType.Object else classType.asObjectType
            val cfO = project.classFile(recType)
            if (cfO.isEmpty) {
                OPALLogger.warn("analysis", "missed invoke call, class not found")
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
                        OPALLogger.warn("analysis", "missed invoke call, method not found")
                        Traversable.empty
                    } else {
                        resolveCallees(superT.get, name, paramTypes, recurse, allowStatic = false)
                    }
                } else if (candidates.exists(!_.returnType.isReferenceType)) {
                    candidates.map(declaredMethods(_))
                } else {
                    val returnTypes = candidates.tail.map(_.returnType.asReferenceType)
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
                        candidates.map(declaredMethods(_))
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
                        case _ ⇒ OPALLogger.warn("analysis", "missed invoke call, method unknown")
                    }
                } else if (definition.isVirtualFunctionCall) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findStatic", _, _, params) ⇒
                            val types =
                                getPossibleTypes(params.head).asInstanceOf[Iterator[ObjectType]]
                            val names = getPossibleStrings(params(1)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) descriptor.toIterable
                                else getPossibleMethodTypes(params(2)).toIterable
                            handleInvokeStatic(caller, pc, types, names, descriptors)
                        case VirtualFunctionCall(_, ObjectType.MethodHandles$Lookup, _, "findVirtual", _, _, params) ⇒
                            val staticTypes = getPossibleTypes(params.head)
                            val names = getPossibleStrings(params(1)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get // Peel off receiver class
                                    Seq(MethodDescriptor(md.parameterTypes.tail, md.returnType))
                                } else
                                    getPossibleMethodTypes(params(2)).toIterable
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
                                getPossibleTypes(params.head).asInstanceOf[Iterator[ObjectType]]
                            val names = getPossibleStrings(params(1)).toIterable
                            val descriptors =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get // Peel off receiver class
                                    Seq(MethodDescriptor(md.parameterTypes.tail, md.returnType))
                                } else
                                    getPossibleMethodTypes(params(2)).toIterable
                            val specialCallers = getPossibleTypes(params(3)).toIterable
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
                            val classes = getPossibleTypes(params.head)
                            val types = classes.asInstanceOf[Iterator[ObjectType]].toIterable
                            val descriptors: Iterable[MethodDescriptor] =
                                if (descriptor.isDefined) {
                                    val md = descriptor.get
                                    Seq(MethodDescriptor(md.parameterTypes, VoidType))
                                } else
                                    getPossibleMethodTypes(params(1)).toIterable
                            handleNewInstances(caller, pc, types, descriptors)
                        case _ ⇒ OPALLogger.warn("analysis", "missed invoke call, method unknown")
                    }
                } else {
                    OPALLogger.warn("analysis", "missed invoke call, method unknown")
                }
            } else {
                OPALLogger.warn("analysis", "missed invoke call, method unknown")
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
        val actualReceiver = invokeParams.head.asVar.value.asReferenceValue
        val dynamicTypes = getTypes(actualReceiver).toIterable
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
        value: Expr[V]
    )(implicit state: State): Iterator[String] = {
        value.asVar.definedBy.iterator filter { index ⇒
            val isStringConst = index >= 0 && state.stmts(index).asAssignment.expr.isStringConst
            if (!isStringConst)
                OPALLogger.warn("analysis", "missed reflective call, unknown string")
            isStringConst
        } map { index ⇒ state.stmts(index).asAssignment.expr.asStringConst.value }
    }

    private[this] def getPossibleTypes(
        value: Expr[V]
    )(implicit state: State): Iterator[Type] = {
        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                OPALLogger.warn("analysis", "missed reflective call, unknown class")
                false
            } else {
                val expr = state.stmts(index).asAssignment.expr
                val isResolvable =
                    expr.isClassConst || isForName(expr) || isBaseTypeLoad(expr) || isGetClass(expr)
                if (!isResolvable)
                    OPALLogger.warn("analysis", "missed reflective call, unknown class")
                isResolvable
            }
        } flatMap { index ⇒
            val expr = state.stmts(index).asAssignment.expr
            if (expr.isClassConst) Iterator(state.stmts(index).asAssignment.expr.asClassConst.value)
            else if (expr.isStaticFunctionCall)
                getPossibleForNameClasses(expr.asStaticFunctionCall.params.head)
            else if (expr.isVirtualFunctionCall) {
                getTypes(expr.asVirtualFunctionCall.receiver.asVar.value.asReferenceValue)
            } else {
                val declClass = expr.asGetStatic.declaringClass
                if (declClass == VoidType.WrapperType) Iterator(VoidType)
                else Iterator(BaseType.baseTypes.iterator.find(declClass == _.WrapperType).get)
            }
        }
    }

    private[this] def getTypes(value: IsReferenceValue): Iterator[ReferenceType] = {
        if (value.isPrecise) value.valueType.iterator
        else if (value.allValues.forall(_.isPrecise))
            value.allValues.toIterator.flatMap(_.valueType)
        else {
            OPALLogger.warn("analysis", "missed reflective call, unknown class")
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
        className: Expr[V]
    )(implicit state: State): Iterator[ReferenceType] = {
        val classNames = getPossibleStrings(className)
        classNames.map(cls ⇒ ObjectType(cls.replace('.', '/')))
    }

    private[this] def getPossibleMethodTypes(
        value: Expr[V]
    )(implicit state: State): Iterator[MethodDescriptor] = {
        value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                OPALLogger.warn("analysis", "missed reflective call, unknown method type")
                false
            } else {
                val expr = state.stmts(index).asAssignment.expr
                val isResolvable = expr.isMethodTypeConst || isMethodType(expr)
                if (!isResolvable)
                    OPALLogger.warn("analysis", "missed reflective call, unknown method type")
                isResolvable
            }
        } flatMap { index ⇒
            val expr = state.stmts(index).asAssignment.expr
            if (expr.isMethodTypeConst)
                Iterator(state.stmts(index).asAssignment.expr.asMethodTypeConst.value)
            else {
                val call = expr.asStaticFunctionCall
                getPossibleMethodTypes(call.params, call.descriptor)
            }
        }
    }

    private[this] def getPossibleMethodTypes(
        params:     Seq[Expr[V]],
        descriptor: MethodDescriptor
    )(implicit state: State): Iterator[MethodDescriptor] = {
        val returnTypes = getPossibleTypes(params.head)
        if (params.size == 1) {
            returnTypes.map(MethodDescriptor.withNoArgs)
        } else if (params.size == 3) {
            val firstParamTypes = getPossibleTypes(params(1)).asInstanceOf[Iterator[FieldType]]
            val possibleOtherParamTypes = getTypes(params(2))
            for {
                returnType ← returnTypes
                firstParamType ← firstParamTypes
                otherParamTypes ← possibleOtherParamTypes
            } yield MethodDescriptor(firstParamType +: otherParamTypes, returnType)
        } else {
            val secondParamType = descriptor.parameterType(1)
            if (secondParamType.isArrayType) {
                val possibleOtherParamTypes = getTypes(params(1))
                for {
                    returnType ← returnTypes
                    otherParamTypes ← possibleOtherParamTypes
                } yield MethodDescriptor(otherParamTypes, returnType)
            } else if (secondParamType == ObjectType.Class) {
                val paramTypes = getPossibleTypes(params(1)).asInstanceOf[Iterator[FieldType]]
                for {
                    returnType ← returnTypes
                    paramType ← paramTypes
                } yield MethodDescriptor(paramType, returnType)
            } else {
                OPALLogger.warn("analysis", "missed reflective call, unknown parameter types")
                Iterator.empty
            }
        }
    }

    private[this] def isMethodType(expr: Expr[V]): Boolean = {
        expr.isStaticFunctionCall &&
            (expr.asStaticFunctionCall.declaringClass eq ObjectType.MethodType) &&
            expr.asStaticFunctionCall.name == "methodType"
    }

    private[this] def getTypes(
        expr: Expr[V]
    )(implicit state: State): Option[IndexedSeq[FieldType]] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            OPALLogger.warn("analysis", "missed reflective call, unknown parameter types")
            None
        } else {
            val definition = state.stmts(definitions.head).asAssignment
            if (definition.expr.astID != NewArray.ASTID) None
            else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (state.cfg.bb(uses.head) != state.cfg.bb(uses.last)) None
                else if (state.stmts(uses.last).astID != Assignment.ASTID) None
                else {
                    val types: Array[FieldType] = new Array(uses.size - 1)
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
                                        types(index.asIntConst.value) = typeDef.asClassConst.value
                                        true
                                    }
                                }
                            }
                        }
                    } || types.contains(null)) {
                        OPALLogger.warn("analysis", "missed reflective call, unknown parameter types")
                        None
                    } else {
                        Some(types)
                    }
                }
            }
        }
    }

    @inline private[this] def returnResult(
        definedMethod: DefinedMethod
    )(implicit state: State): PropertyComputationResult = {
        var res: List[PropertyComputationResult] = state.calleesAndCallers.partialResultsForCallers

        val calleesResult =
            if (state.calleesAndCallers.callees.isEmpty)
                Result(definedMethod, NoReflectionRelatedCallees)
            else
                Result(
                    definedMethod,
                    new ReflectionRelatedCalleesImplementation(state.calleesAndCallers.callees)
                )

        res ::= calleesResult

        if (state.newLoadedClasses.nonEmpty) {
            res ::= PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case EPK(p, _) ⇒
                    Some(EPS(p, LoadedClassesLowerBound, new LoadedClasses(state.newLoadedClasses)))
                case EPS(p, lb, ub) ⇒
                    val newUb = ub.classes ++ state.newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(EPS(p, lb, new LoadedClasses(newUb)))
                    else
                        None

            })
            state.newLoadedClasses flatMap { loaded ⇒
                LoadedClassesAnalysis.retrieveStaticInitializers(loaded, declaredMethods, project)
            } foreach { clInit ⇒
                res ::=
                    PartialResult[DeclaredMethod, CallersProperty](clInit, CallersProperty.key, {
                        case EPK(_, _) ⇒
                            Some(EPS(clInit, LowerBoundCallers, OnlyVMLevelCallers))
                        case EPS(_, lb, ub) if !ub.hasCallersWithUnknownContext ⇒
                            Some(EPS(clInit, lb, ub.updatedWithVMLevelCall()))
                        case _ ⇒ None
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

    override def uses: Set[PropertyKind] = Set(CallersProperty)

    override def derives: Set[PropertyKind] = Set(CallersProperty, ReflectionRelatedCallees)

    override def init(p: SomeProject, ps: PropertyStore): ReflectionRelatedCallsAnalysis = {
        val analysis = new ReflectionRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
