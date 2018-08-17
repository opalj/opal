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
                case _                ⇒

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
                    handleInvoke(caller, pc, receiver)
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
            getPossibleClasses(receiver).asInstanceOf[Iterator[ObjectType]].toIterable
        val newInstantiatedTypes = instantiatedTypes.filter(!state.instantiatedTypesUB.contains(_))
        state.newInstantiatedTypes ++= newInstantiatedTypes

        if (descriptor.isDefined) {
            for (instantiatedType ← instantiatedTypes) {
                val method = project.specialCall(
                    instantiatedType,
                    isInterface = false,
                    "<init>",
                    descriptor.get
                )

                state.calleesAndCallers.updateWithCallOrFallback(
                    caller,
                    method,
                    pc,
                    caller.declaringClassType.asObjectType.packageName,
                    instantiatedType,
                    "<init>",
                    descriptor.get
                )
            }
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
                if (definition.astID == VirtualFunctionCall.ASTID) {
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

    private[this] def handleInvoke(
        caller: DefinedMethod,
        pc:     Int,
        method: Expr[V]
    )(implicit state: State): Unit = {
        method.asVar.definedBy.foreach { index ⇒
            if (index >= 0) {
                val definition = state.stmts(index).asAssignment.expr
                if (definition.astID == VirtualFunctionCall.ASTID) {
                    definition.asVirtualFunctionCall match {
                        case VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredMethod" | "getMethod", _, receiver, params) ⇒
                            val classes = getPossibleClasses(receiver)
                            val names = getPossibleStrings(params.head)
                            val paramTypesO = getTypes(params(1))
                            if (paramTypesO.isDefined) {
                                for {
                                    receiverClass ← classes
                                    name ← names
                                } {
                                    val callees = resolveCallees(
                                        receiverClass,
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

    private[this] def getPossibleClasses(
        value: Expr[V]
    )(implicit state: State): Iterator[ReferenceType] = {
        val possibleStmts = value.asVar.definedBy.iterator.filter { index ⇒
            if (index < 0) {
                OPALLogger.warn("analysis", "missed reflective call, unknown class")
                false
            } else {
                val expr = state.stmts(index).asAssignment.expr
                val isResolvable = expr.isClassConst || isClassForName(expr)
                if (!isResolvable)
                    OPALLogger.warn("analysis", "missed reflective call, unknown class")
                isResolvable
            }
        }

        possibleStmts.flatMap { index ⇒
            val expr = state.stmts(index).asAssignment.expr
            if (expr.isClassConst) Iterator(state.stmts(index).asAssignment.expr.asClassConst.value)
            else getPossibleForNameClasses(expr.asStaticFunctionCall.params.head)
        }
    }

    private[this] def getPossibleForNameClasses(
        className: Expr[V]
    )(implicit state: State): Iterator[ReferenceType] = {
        val classNames = getPossibleStrings(className)
        classNames.map(cls ⇒ ObjectType(cls.replace('.', '/')))
    }

    private[this] def isClassForName(expr: Expr[V]): Boolean = {
        expr.astID == StaticFunctionCall.ASTID &&
            (expr.asStaticFunctionCall.declaringClass eq ObjectType.Class) &&
            expr.asStaticFunctionCall.name == "forName"
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
                    var index = uses.size - 2
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last) true
                        else {
                            val use = state.stmts(useSite)
                            if (use.astID != ArrayStore.ASTID) false
                            else {
                                val typeDefs = use.asArrayStore.value.asVar.definedBy
                                if (!typeDefs.isSingletonSet || typeDefs.head < 0) false
                                else {
                                    val typeDef = state.stmts(typeDefs.head).asAssignment.expr
                                    if (!typeDef.isClassConst) false
                                    else {
                                        types(index) = typeDef.asClassConst.value
                                        index -= 1
                                        true
                                    }
                                }
                            }
                        }
                    }) {
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
