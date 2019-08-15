/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.collection.immutable.ConstArray
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsMultipleReferenceValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ReferenceType
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ArrayType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.valueOriginsOfPCs
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI

/**
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToAnalysis extends PointsToAnalysisBase with ReachableMethodAnalysis {

    private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processMethod(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        // TODO remove that ugly hack
        if ((definedMethod.declaringClassType eq ObjectType.Class) && (definedMethod.name == "getConstructor") || definedMethod.name == "getDeclaredConstructor") {
            return Results();
        }
        doProcessMethod(new PointsToAnalysisState[ElementType, PointsToSet](definedMethod, tacEP))
    }

    val javaLangRefReference = ObjectType("java/lang/ref/Reference")

    private[this] def doProcessMethod(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        if (state.hasTACDependee)
            throw new IllegalStateException("points to analysis does not support refinement based tac")
        val tac = state.tac
        val method = state.method.definedMethod
        if (method.returnType.isReferenceType) {
            state.setLocalPointsToSet(state.method, emptyPointsToSet, { t ⇒ classHierarchy.isSubtypeOf(t, method.returnType.asReferenceType) })
        }

        tac.cfg.abnormalReturnNode.predecessors.foreach { throwingBB ⇒
            val throwingStmt = tac.stmts(throwingBB.asBasicBlock.endPC)
            throwingStmt match {
                case Throw(_, UVar(_, defSites)) ⇒
                    val entity = MethodExceptions(state.method)
                    state.includeLocalPointsToSets(
                        entity,
                        currentPointsToOfDefSites(entity, defSites),
                        { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ObjectType.Throwable) }
                    )

                case Assignment(_, _, _: Call[_]) | ExprStmt(_, _: Call[_]) | _: Call[_] ⇒
                    val entity = MethodExceptions(state.method)
                    val callSite = definitionSites(state.method.definedMethod, throwingStmt.pc)
                    state.includeLocalPointsToSet(
                        entity,
                        currentPointsTo(entity, CallExceptions(callSite)),
                        { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ObjectType.Throwable) }
                    )

                case _ ⇒
                // TODO handle implicit exceptions (do that for Throw and Calls, too (NPE!))
            }
        }

        for (stmt ← tac.stmts) stmt match {
            case Assignment(pc, _, New(_, t)) ⇒
                val defSite = definitionSites(method, pc)
                state.setAllocationSitePointsToSet(
                    defSite,
                    createPointsToSet(pc, state.method, t, isConstant = false)
                )

            case Assignment(pc, _, NewArray(_, counts, tpe)) ⇒
                @inline def countIsZero(theCounts: Seq[Expr[V]]): Boolean = {
                    theCounts.head.asVar.definedBy.forall { ds ⇒
                        ds >= 0 &&
                            tac.stmts(ds).asAssignment.expr.isIntConst &&
                            tac.stmts(ds).asAssignment.expr.asIntConst.value == 0
                    }
                }

                val defSite = definitionSites(method, pc)
                val isEmptyArray = countIsZero(counts)
                var arrayReferencePTS = createPointsToSet(
                    pc, state.method, tpe, isConstant = false, isEmptyArray
                )
                state.setAllocationSitePointsToSet(
                    defSite,
                    arrayReferencePTS
                )
                var remainingCounts = counts.tail
                var allocatedType: FieldType = tpe.componentType
                var continue = !isEmptyArray
                while (remainingCounts.nonEmpty && allocatedType.isArrayType && continue) {
                    val theType = allocatedType.asArrayType

                    // TODO: Ugly hack to get the only points-to element from the previously created PTS
                    var arrayEntity: ArrayEntity[ElementType] = null
                    arrayReferencePTS.forNewestNElements(1) { as ⇒ arrayEntity = ArrayEntity(as) }

                    if (countIsZero(remainingCounts))
                        continue = false

                    arrayReferencePTS = createPointsToSet(
                        pc,
                        state.method,
                        theType,
                        isConstant = false,
                        isEmptyArray = !continue
                    )
                    state.includeSharedPointsToSet(
                        arrayEntity,
                        arrayReferencePTS,
                        { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, theType) }
                    )

                    remainingCounts = remainingCounts.tail
                    allocatedType = theType.componentType
                }

            case Assignment(pc, targetVar, const: Const) if targetVar.value.isReferenceValue ⇒
                val defSite = definitionSites(method, pc)
                state.setAllocationSitePointsToSet(
                    defSite,
                    if (const.isNullExpr)
                        emptyPointsToSet
                    // note, this is wrong for alias analyses
                    else
                        createPointsToSet(
                            pc, state.method, const.tpe.asObjectType, isConstant = true
                        )
                )

            // that case should not happen
            case Assignment(pc, DVar(_: IsReferenceValue, _), UVar(_, defSites)) ⇒
                val defSiteObject = definitionSites(method, pc)
                val index = tac.pcToIndex(pc)
                val nextStmt = tac.stmts(index + 1)
                val filter = nextStmt match {
                    case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒ t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
                    case _ ⇒ PointsToSetLike.noFilter
                }
                state.includeLocalPointsToSets(
                    defSiteObject, currentPointsToOfDefSites(defSiteObject, defSites), filter
                )

            case Assignment(pc, _, GetField(_, declaringClass, name, fieldType: ReferenceType, UVar(_, objRefDefSites))) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    handleGetField(RealField(fieldOpt.get), pc, objRefDefSites)
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, _, GetStatic(_, declaringClass, name, fieldType: ReferenceType)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    handleGetStatic(fieldOpt.get, pc)
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, DVar(_: IsReferenceValue, _), ArrayLoad(_, _, UVar(av: IsSArrayValue, arrayDefSites))) ⇒
                val arrayType = av.theUpperTypeBound
                handleArrayLoad(arrayType, pc, arrayDefSites)

            case Assignment(pc, DVar(_: IsReferenceValue, _), ArrayLoad(_, _, UVar(av: IsMultipleReferenceValue, arrayDefSites))) ⇒
                val arrayType = av.leastUpperType.get.asArrayType
                handleArrayLoad(arrayType, pc, arrayDefSites)

            case Assignment(pc, targetVar, call: FunctionCall[DUVar[ValueInformation]]) ⇒
                val callees: Callees = state.callees(ps)
                val targets = callees.callees(pc)
                val defSiteObject = definitionSites(method, pc)

                // TODO fix these ugly hacks
                if (targetVar.value.isReferenceValue &&
                    // Unsafe.getObject and getObjectVolatile are handled like getField
                    ((call.declaringClass ne UnsafeT) || !call.name.startsWith("getObject"))) {

                    val line = state.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
                    val isTamiFlexControlled = call.declaringClass match {
                        case ArrayT ⇒ call.name match {
                            case "newInstance" | "get" ⇒
                                tamiFlexLogData.classes(state.method, line).nonEmpty
                            case _ ⇒ false
                        }
                        case ObjectType.Class ⇒ call.name match {
                            case "forName" | "getDeclaredFields" | "getFields" |
                                "getDeclaredMethods" | "getMethods" | "newInstance" ⇒
                                tamiFlexLogData.classes(state.method, line).nonEmpty
                            case "getDeclaredField" | "getField" ⇒
                                tamiFlexLogData.fields(state.method, line).nonEmpty
                            case "getDeclaredMethod" | "getMethod" ⇒
                                tamiFlexLogData.methods(state.method, line).nonEmpty
                            case _ ⇒ false
                        }
                        case ConstructorT ⇒ call.name == "newInstance" &&
                            tamiFlexLogData.classes(state.method, line).nonEmpty
                        case FieldT ⇒ call.name == "get" &&
                            tamiFlexLogData.fields(state.method, line).nonEmpty
                        case _ ⇒ false
                    }

                    if (!isTamiFlexControlled) {
                        val index = tac.pcToIndex(pc)
                        val nextStmt = tac.stmts(index + 1)
                        val filter = nextStmt match {
                            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
                            case _ ⇒
                                PointsToSetLike.noFilter
                        }
                        if (state.hasCalleesDepenedee) {
                            state.includeLocalPointsToSet(defSiteObject, emptyPointsToSet, filter)
                            state.addDependee(defSiteObject, state.calleesDependee)
                        }
                        state.includeLocalPointsToSets(
                            defSiteObject,
                            targets.map(currentPointsTo(defSiteObject, _)),
                            filter
                        )
                    }
                }
                handleCall(call, pc)

            case Assignment(pc, _, idc: InvokedynamicFunctionCall[_]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(pc, idc: InvokedynamicFunctionCall[V]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] ⇒
                state.addIncompletePointsToInfo(idc.pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case Assignment(_, DVar(_: IsReferenceValue, _), _) ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case call: Call[_] ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], call.pc)

            case ExprStmt(pc, call: Call[_]) ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], pc)

            case PutField(pc, declaringClass, name, fieldType: ReferenceType, UVar(_, objRefDefSites), UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    handlePutField(RealField(fieldOpt.get), objRefDefSites, defSites)
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case PutStatic(pc, declaringClass, name, fieldType: ReferenceType, UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    handlePutStatic(fieldOpt.get, defSites)
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case ArrayStore(_, UVar(av: IsSArrayValue, arrayDefSites), _, UVar(_: IsReferenceValue, defSites)) ⇒
                val arrayType = av.theUpperTypeBound
                handleArrayStore(arrayType, arrayDefSites, defSites)

            case ArrayStore(_, UVar(av: IsMultipleReferenceValue, arrayDefSites), _, UVar(_: IsReferenceValue, defSites)) ⇒
                val arrayType = av.leastUpperType.get.asArrayType
                handleArrayStore(arrayType, arrayDefSites, defSites)

            case ReturnValue(_, UVar(_: IsReferenceValue, defSites)) ⇒
                state.includeLocalPointsToSets(
                    state.method, currentPointsToOfDefSites(state.method, defSites),
                    { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, state.method.descriptor.returnType.asReferenceType) }
                )

            case _ ⇒
        }

        // todo: we have to handle the exceptions that might implicitly be thrown by this method

        returnResult(state)
    }

    // maps the points-to set of actual parameters (including *this*) the the formal parameters
    private[this] def handleCall(
        call: Call[DUVar[ValueInformation]], pc: Int
    )(implicit state: State): Unit = {
        val tac = state.tac
        val callees: Callees = state.callees(ps)

        for (target ← callees.directCallees(pc)) {
            handleDirectCall(call, pc, target)
        }

        for (target ← callees.indirectCallees(pc)) {
            handleIndirectCall(pc, target, callees, tac)
        }

        val callExceptions = CallExceptions(definitionSites(state.method.definedMethod, pc))
        for (target ← callees.callees(pc)) {
            val targetExceptions = MethodExceptions(target)

            state.includeLocalPointsToSet(
                callExceptions,
                currentPointsTo(callExceptions, targetExceptions),
                { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ObjectType.Throwable) }
            )
        }
        if (state.hasCalleesDepenedee) {
            state.addDependee(callExceptions, state.calleesDependee)
            state.includeLocalPointsToSet(callExceptions, emptyPointsToSet,
                { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ObjectType.Throwable) })
        }
    }

    private[this] def handleCallReceiver(
        receiverDefSites: IntTrieSet,
        target:           DeclaredMethod,
        fps:              ConstArray[VirtualFormalParameter],
        isNonVirtualCall: Boolean
    )(implicit state: State): Unit = {
        val fp = fps(0)
        val ptss = currentPointsToOfDefSites(fp, receiverDefSites)
        val declClassType = target.declaringClassType
        val tgtMethod = target.definedMethod
        val filter = if (isNonVirtualCall) {
            t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, declClassType)
        } else {
            val overrides =
                if (project.overridingMethods.contains(tgtMethod))
                    project.overridingMethods(tgtMethod).map(_.classFile.thisType) -
                        declClassType
                else
                    Set.empty
            // TODO this might not be 100% correct in some corner cases
            t: ReferenceType ⇒
                classHierarchy.isSubtypeOf(t, declClassType) &&
                    !overrides.exists(st ⇒ classHierarchy.isSubtypeOf(t, st))
        }
        state.includeSharedPointsToSets(
            fp,
            ptss,
            filter
        )
    }

    private[this] val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    private[this] val ArrayT = ObjectType("java/lang/reflect/Array")
    private[this] val FieldT = ObjectType("java/lang/reflect/Field")
    private[this] val MethodT = ObjectType("java/lang/reflect/Method")
    private[this] val UnsafeT = ObjectType("sun/misc/Unsafe")

    private[this] def handleDirectCall(
        call: Call[V], pc: Int, target: DeclaredMethod
    )(implicit state: State): Unit = {
        val receiverOpt: Option[Expr[DUVar[ValueInformation]]] = call.receiverOption
        val fps = formalParameters(target)

        if (fps != null) {
            // handle receiver for non static methods
            if (receiverOpt.isDefined) {
                val isNonVirtualCall = call match {
                    case _: NonVirtualFunctionCall[V] |
                        _: NonVirtualMethodCall[V] |
                        _: StaticFunctionCall[V] |
                        _: StaticMethodCall[V] ⇒ true
                    case _ ⇒ false
                }
                handleCallReceiver(receiverOpt.get.asVar.definedBy, target, fps, isNonVirtualCall)
            }

            val descriptor = target.descriptor
            // in case of signature polymorphic methods, we give up
            if (call.params.size == descriptor.parametersCount) {
                // handle params
                for (i ← 0 until descriptor.parametersCount) {
                    val paramType = descriptor.parameterType(i)
                    if (paramType.isReferenceType) {
                        val fp = fps(i + 1)
                        state.includeSharedPointsToSets(
                            fp,
                            currentPointsToOfDefSites(fp, call.params(i).asVar.definedBy),
                            { t: ReferenceType ⇒
                                classHierarchy.isSubtypeOf(t, paramType.asReferenceType)
                            }
                        )
                    }
                }
            } else {
                // it is not needed to mark it as incomplete here
            }
        } else {
            state.addIncompletePointsToInfo(pc)
        }

        /*
        Special handling for System.arraycopy
        Uses a fake defsite at the (method!)call to simulate an array load and store
        TODO Integrate into ConfiguredNativeMethodsPointsToAnalysis
         */
        if ((target.declaringClassType eq ObjectType.System) && target.name == "arraycopy") {
            handleArrayLoad(ArrayType.ArrayOfObject, pc, call.params.head.asVar.definedBy)

            val defSites = IntTrieSet(state.tac.pcToIndex(pc))
            handleArrayStore(ArrayType.ArrayOfObject, call.params(2).asVar.definedBy, defSites)
        }

        /*
         * Special handling for Class|Array|Constructor.newInstance using tamiflex
         * TODO: Integrate into a TamiFlex analysis
         */
        val line = state.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        if (target.declaringClassType eq ArrayT) {
            target.name match {
                case "newInstance" ⇒
                    val allocatedTypes = tamiFlexLogData.classes(state.method, line)
                    for (allocatedType ← allocatedTypes) {
                        val pointsToSet = createPointsToSet(
                            pc, state.method, allocatedType, isConstant = false
                        )
                        val defSite = definitionSites(state.method.definedMethod, pc)
                        state.includeLocalPointsToSet(defSite, pointsToSet, PointsToSetLike.noFilter)
                    }

                case "get" ⇒
                    val arrays = tamiFlexLogData.classes(state.method, line)
                    for (array ← arrays) {
                        handleArrayLoad(array.asArrayType, pc, call.params.head.asVar.definedBy)
                    }

                case "set" ⇒
                    val arrays = tamiFlexLogData.classes(state.method, line)
                    for (array ← arrays) {
                        handleArrayStore(
                            array.asArrayType,
                            call.params.head.asVar.definedBy,
                            call.params(2).asVar.definedBy
                        )
                    }

                case _ ⇒
            }
        } else if (target.declaringClassType eq ObjectType.Class) {
            target.name match {
                case "forName" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)
                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ObjectType.Class, isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getDeclaredField" ⇒
                    val fields = tamiFlexLogData.fields(state.method, line)
                    if (fields.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, FieldT, isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getDeclaredFields" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)
                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(FieldT), isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                        // todo store something into the array
                    }

                case "getField" ⇒
                    val fields = tamiFlexLogData.fields(state.method, line)
                    if (fields.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, FieldT, isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getFields" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)
                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(FieldT), isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                        // todo store something into the array
                    }

                case "getDeclaredMethod" ⇒
                    val methods = tamiFlexLogData.methods(state.method, line)
                    if (methods.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, MethodT, isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getDeclaredMethods" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)
                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(MethodT), isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getMethod" ⇒
                    val methods = tamiFlexLogData.methods(state.method, line)
                    if (methods.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, MethodT, isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "getMethods" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)
                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(MethodT), isConstant = false
                            ),
                            PointsToSetLike.noFilter
                        )
                    }

                case "newInstance" ⇒
                    val allocatedTypes = tamiFlexLogData.classes(state.method, line)
                    for (allocatedType ← allocatedTypes) {
                        val pointsToSet = createPointsToSet(
                            pc, state.method, allocatedType, isConstant = false
                        )
                        val defSite = definitionSites(state.method.definedMethod, pc)
                        state.includeLocalPointsToSet(defSite, pointsToSet, PointsToSetLike.noFilter)
                    }

                case _ ⇒

            }
        } else if (target.declaringClassType eq ConstructorT) {
            target.name match {
                case "getModifiers" ⇒
                // TODO
                case "newInstance" ⇒
                    val allocatedTypes = tamiFlexLogData.classes(state.method, line)
                    for (allocatedType ← allocatedTypes) {
                        val pointsToSet = createPointsToSet(
                            pc, state.method, allocatedType, isConstant = false
                        )
                        val defSite = definitionSites(state.method.definedMethod, pc)
                        state.includeLocalPointsToSet(defSite, pointsToSet, PointsToSetLike.noFilter)
                    }
                case _ ⇒
            }

        } else if (target.declaringClassType eq FieldT) {
            if (target.name == "get") {
                val fields = tamiFlexLogData.fields(state.method, line)
                for (field ← fields) {
                    if (field.isStatic) {
                        handleGetStatic(field, pc)
                    } else {
                        handleGetField(RealField(field), pc, call.params.head.asVar.definedBy)
                    }
                }
            } else if (target.name == "set") {
                val fields = tamiFlexLogData.fields(state.method, line)
                for (field ← fields) {
                    if (field.isStatic) {
                        handlePutStatic(field, call.params(1).asVar.definedBy)
                    } else {
                        handlePutField(
                            RealField(field), call.params.head.asVar.definedBy, call.params(1).asVar.definedBy
                        )
                    }
                }
            }
        }

        if (target.declaringClassType eq UnsafeT) {
            target.name match {
                case "getObject" | "getObjectVolatile" ⇒
                    handleGetField(UnsafeFakeField, pc, call.params.head.asVar.definedBy)
                case "putObject" | "putObjectVolatile" | "putOrderedObject" ⇒
                    handlePutField(UnsafeFakeField, call.params.head.asVar.definedBy, call.params(2).asVar.definedBy)
                case "compareAndSwapObject" ⇒
                    handlePutField(UnsafeFakeField, call.params.head.asVar.definedBy, call.params(3).asVar.definedBy)
                case _ ⇒
            }
        }

    }

    // TODO reduce code duplication
    private def handleIndirectCall(
        pc:      Int,
        target:  DeclaredMethod,
        callees: Callees,
        tac:     TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        val fps = formalParameters(target)

        val indirectParams = callees.indirectCallParameters(pc, target)
        val descriptor = target.descriptor

        // Prevent spuriously matched targets (e.g. from tamiflex with unknown source line number)
        // from interfering with the points-to analysis
        // TODO That rather is a responsibility of the reflection analysis though
        if (indirectParams.isEmpty || descriptor.parametersCount == indirectParams.size) {
            if (fps != null) {
                // handle receiver for non static methods
                val receiverOpt = callees.indirectCallReceiver(pc, target)
                if (receiverOpt.isDefined) {
                    val receiverDefSites = valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                    handleCallReceiver(receiverDefSites, target, fps, isNonVirtualCall = false)
                } else if (target.name == "<init>") {
                    handleCallReceiver(
                        IntTrieSet(tac.pcToIndex(pc)), target, fps, isNonVirtualCall = false
                    )
                } else {
                    // TODO distinguish between static methods and unavailable info
                }

                for (i ← indirectParams.indices) {
                    val paramType = descriptor.parameterType(i)
                    if (paramType.isReferenceType) {
                        val fp = fps(i + 1)
                        val indirectParam = indirectParams(i)
                        if (indirectParam.isDefined) {
                            state.includeSharedPointsToSets(
                                fp,
                                currentPointsToOfDefSites(
                                    fp, valueOriginsOfPCs(indirectParam.get._2, tac.pcToIndex)
                                ),
                                { t: ReferenceType ⇒
                                    classHierarchy.isSubtypeOf(t, paramType.asReferenceType)
                                }
                            )
                        } else {
                            state.addIncompletePointsToInfo(pc)
                        }
                    }
                }
            } else {
                state.addIncompletePointsToInfo(pc)
            }
        }
    }

    private[this] def returnResult(state: State): ProperPropertyComputationResult = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        for ((e, pointsToSet) ← state.allocationSitePointsToSetsIterator) {
            results += Result(e, pointsToSet)
        }

        for ((e, (pointsToSet, typeFilter)) ← state.localPointsToSetsIterator) {
            if (state.hasDependees(e)) {
                val dependees = state.dependeesOf(e)
                results += InterimResult.forUB(
                    e,
                    pointsToSet,
                    dependees.values,
                    continuationForLocal(e, dependees, pointsToSet, typeFilter)
                )
            } else {
                results += Result(e, pointsToSet)
            }
        }

        for ((e, (pointsToSet, typeFilter)) ← state.sharedPointsToSetsIterator) {
            // The shared entities are not affected by changes of the tac and use partial results.
            // Thus, we could simply recompute them on updates for the tac
            if (state.hasDependees(e)) {
                val dependees = state.dependeesOf(e)
                results += InterimPartialResult(
                    dependees.values, continuationForShared(e, dependees, typeFilter)
                )
            }
            if (pointsToSet ne emptyPointsToSet) {
                results += PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                    e,
                    pointsToPropertyKey,
                    {
                        case _: EPK[Entity, _] ⇒
                            Some(InterimEUBP(e, pointsToSet))

                        case UBP(ub: PointsToSet @unchecked) ⇒
                            val newPointsTo = ub.included(pointsToSet, 0, 0)
                            if (newPointsTo ne ub) {
                                Some(InterimEUBP(e, newPointsTo))
                            } else {
                                None
                            }

                        case eOptP ⇒
                            throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                    }
                )
            }
        }

        for (fakeEntity ← state.getFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, field, filter) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                results += InterimPartialResult(
                    dependees.values,
                    continuationForNewAllocationSitesAtGetField(
                        defSite, field, filter, dependees
                    )
                )
            }
        }

        for (fakeEntity ← state.putFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSites, field) = fakeEntity
                val defSitesWithoutExceptions = defSites.iterator.filterNot(ai.isImplicitOrExternalException)
                val defSitesEPKs = defSitesWithoutExceptions.map[EPK[Entity, Property]] { ds ⇒
                    val e = EPK(toEntity(ds, state.method, state.tac.stmts), pointsToPropertyKey)
                    // otherwise it might be the case that the property store does not know the epk
                    ps(e)
                    e
                }.toTraversable

                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                if (defSitesEPKs.nonEmpty)
                    results += InterimPartialResult(
                        dependees.values,
                        continuationForNewAllocationSitesAtPutField(
                            defSitesEPKs, field, dependees
                        )
                    )
            }
        }

        for (fakeEntity ← state.arrayLoadsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, arrayType, filter) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                results += InterimPartialResult(
                    dependees.values,
                    continuationForNewAllocationSitesAtArrayLoad(
                        defSite, arrayType, filter, dependees
                    )
                )
            }
        }

        for (fakeEntity ← state.arrayStoresIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSites, arrayType) = fakeEntity
                val defSitesWithoutExceptions = defSites.iterator.filterNot(ai.isImplicitOrExternalException)
                val defSitesEPKs = defSitesWithoutExceptions.map[EPK[Entity, Property]] { ds ⇒
                    val e = EPK(toEntity(ds, state.method, state.tac.stmts), pointsToPropertyKey)
                    // otherwise it might be the case that the property store does not know the epk
                    ps(e)
                    e
                }.toTraversable

                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                if (defSitesEPKs.nonEmpty)
                    results += InterimPartialResult(
                        dependees.values,
                        continuationForNewAllocationSitesAtArrayStore(
                            defSitesEPKs, arrayType, dependees
                        )
                    )
            }
        }

        if (state.hasCalleesDepenedee) {
            val calleesDependee = state.calleesDependee
            results += InterimPartialResult(
                Some(calleesDependee),
                continuationForCallees(
                    calleesDependee,
                    new PointsToAnalysisState[ElementType, PointsToSet](state.method, state.tacDependee)
                )
            )
        }

        Results(results)
    }

    private[this] def continuationForLocal(
        e: Entity, dependees: Map[SomeEPK, SomeEOptionP], pointsToSetUB: PointsToSet, typeFilter: ReferenceType ⇒ Boolean
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                val newPointsToUB = updatedPointsToSet(
                    pointsToSetUB,
                    newDependeePointsTo,
                    eps,
                    dependees,
                    typeFilter
                )

                if (newDependees.isEmpty) {
                    Result(e, newPointsToUB)
                } else {
                    InterimResult.forUB(
                        e,
                        newPointsToUB,
                        newDependees.values,
                        continuationForLocal(e, newDependees, newPointsToUB, typeFilter)
                    )
                }
            case UBP(callees: Callees) ⇒
                // this will never happen for method return values or method exceptions
                val (defSite, dependeeIsExceptions) = e match {
                    case ds: DefinitionSite ⇒ (ds, false)
                    case ce: CallExceptions ⇒ (ce.defSite, true)
                }
                // TODO: Only handle new callees
                val results = ArrayBuffer.empty[ProperPropertyComputationResult]
                val tgts = callees.callees(defSite.pc)

                var newDependees = updatedDependees(eps, dependees)
                var newPointsToSet = pointsToSetUB
                tgts.foreach { target ⇒
                    val entity = if (dependeeIsExceptions) MethodExceptions(target) else target
                    // if we already have a dependency to that method, we do not need to process it
                    // otherwise, it might still be the case that we processed it before but it is
                    // final and thus not part of dependees anymore
                    if (!dependees.contains(EPK(entity, pointsToPropertyKey))) {
                        val p2s = ps(entity, pointsToPropertyKey)
                        if (p2s.isRefinable) {
                            newDependees += (p2s.toEPK → p2s)
                        }
                        newPointsToSet = newPointsToSet.included(pointsToUB(p2s), typeFilter)
                    }
                }

                if (newDependees.nonEmpty) {
                    results += InterimResult.forUB(
                        e,
                        newPointsToSet,
                        newDependees.values,
                        continuationForLocal(e, newDependees, newPointsToSet, typeFilter)
                    )
                } else {
                    results += Result(e, newPointsToSet)
                }

                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    def continuationForCallees(
        oldCalleeEOptP: EOptionP[DeclaredMethod, Callees],
        state:          State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newCallees: Callees) ⇒
                val tac = state.tac
                val oldCallees = if (oldCalleeEOptP.hasUBP) oldCalleeEOptP.ub else NoCallees
                for {
                    (pc, targets) ← newCallees.directCallSites()
                    target ← targets
                } {
                    if (!oldCallees.containsDirectCall(pc, target)) {
                        val call = tac.stmts(tac.pcToIndex(pc)) match {
                            case call: Call[DUVar[ValueInformation]] @unchecked ⇒
                                call
                            case Assignment(_, _, call: Call[DUVar[ValueInformation]] @unchecked) ⇒
                                call
                            case ExprStmt(_, call: Call[DUVar[ValueInformation]] @unchecked) ⇒
                                call
                            case e ⇒ throw new IllegalArgumentException(s"unexpected stmt $e")
                        }
                        handleDirectCall(call, pc, target)(state)
                    }
                }
                for {
                    (pc, targets) ← newCallees.indirectCallSites()
                    target ← targets
                } {
                    if (!oldCallees.containsIndirectCall(pc, target)) {
                        handleIndirectCall(pc, target, newCallees, tac)(state)
                    }
                }

                state.setCalleesDependee(eps.asInstanceOf[EPS[DeclaredMethod, Callees]])

                returnResult(state)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }
}

trait AbstractPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject ⇒ AbstractPointsToAnalysis

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        TACAI,
        propertyKind
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.ub(propertyKind))

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AbstractPointsToAnalysis = {
        val analysis = createAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}

