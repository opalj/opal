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
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.Field
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.allocationSiteLongToTypeId
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.isEmptyArrayAllocationSite
import org.opalj.br.ArrayType
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.valueOriginsOfPCs
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI

case class ArrayEntity[ElementType](element: ElementType)
case class MethodExceptions(dm: DeclaredMethod)
case class CallExceptions(defSite: DefinitionSite)

trait AField {
    def classType: ObjectType
    def fieldType: FieldType
}

case class RealField(field: Field) extends AField {
    override def classType: ObjectType = field.classFile.thisType
    override def fieldType: FieldType = field.fieldType
}

case object UnsafeFakeField extends AField {
    override def classType: ObjectType = ObjectType.Object
    override def fieldType: FieldType = ObjectType.Object
}

/**
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToAnalysis[ElementType, PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]]
    extends AbstractPointsToBasedAnalysis[Entity, PointsToSet]
    with ReachableMethodAnalysis {

    protected[this] implicit val formalParameters: VirtualFormalParameters = {
        p.get(VirtualFormalParametersKey)
    }
    protected[this] implicit val definitionSites: DefinitionSites = {
        p.get(DefinitionSitesKey)
    }

    private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): PointsToSet

    type State = PointsToAnalysisState[ElementType, PointsToSet]

    override def processMethod(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        doProcessMethod(new PointsToAnalysisState[ElementType, PointsToSet](definedMethod, tacEP))
    }

    val javaLangRefReference = ObjectType("java/lang/ref/Reference")

    private[this] def handleGetField(
        field: AField, pc: Int, objRefDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val tac = state.tac
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
            case _ ⇒
                AllocationSitePointsToSet.noFilter
        }
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val fakeEntity = (defSiteObject, field)
        state.addGetFieldEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                // TODO: Refactor
                val fieldClassType = field.classType
                val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        // IMPROVE: Use LongRefPair to avoid boxing
                        currentPointsTo(defSiteObject, (as, field)),
                        filter
                    )
                }
            }
        }
    }

    private[this] def handleGetStatic(field: Field, pc: Int)(implicit state: State): Unit = {
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val tac = state.tac
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
            case _ ⇒
                AllocationSitePointsToSet.noFilter
        }
        state.includeLocalPointsToSet(
            defSiteObject,
            currentPointsTo(defSiteObject, field),
            filter
        )
    }

    private[this] def handleArrayLoad(
        arrayType: ArrayType, pc: Int, arrayDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val tac = state.tac
        val fakeEntity = (defSiteObject, arrayType)
        state.addArrayLoadEntity(fakeEntity)
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
            case _ ⇒
                AllocationSitePointsToSet.noFilter
        }
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (typeId < 0 &&
                    classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType)) {
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, ArrayEntity(as)),
                        filter
                    )
                }
            }
        }
    }

    private[this] def handlePutField(
        field: AField, objRefDefSites: IntTrieSet, rhsDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val fakeEntity = (rhsDefSites, field)
        state.addPutFieldEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                // TODO: Refactor
                val fieldClassType = field.classType
                val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                    val fieldEntity = (as, field)
                    state.includeSharedPointsToSets(
                        fieldEntity,
                        currentPointsToOfDefSites(fieldEntity, rhsDefSites),
                        { t: ReferenceType ⇒
                            classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
                        }
                    )
                }
            }
        }
    }

    private[this] def handlePutStatic(field: Field, rhsDefSites: IntTrieSet)(implicit state: State): Unit = {
        state.includeSharedPointsToSets(
            field,
            currentPointsToOfDefSites(field, rhsDefSites),
            { t: ReferenceType ⇒
                classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
            }
        )
    }

    private[this] def handleArrayStore(
        arrayType: ArrayType, arrayDefSites: IntTrieSet, rhsDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val fakeEntity = (rhsDefSites, arrayType)
        state.addArrayStoredEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (typeId < 0 &&
                    classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                    !isEmptyArrayAllocationSite(as.asInstanceOf[Long])) {
                    val arrayEntity = ArrayEntity(as)
                    state.includeSharedPointsToSets(
                        arrayEntity,
                        currentPointsToOfDefSites(arrayEntity, rhsDefSites),
                        { t: ReferenceType ⇒
                            classHierarchy.isSubtypeOf(t, ArrayType.lookup(allocationSiteLongToTypeId(as.asInstanceOf[Long])).componentType.asReferenceType)
                        }
                    )
                }
            }
        }
    }

    private[this] def handleCallReceiver(
        receiverDefSites: IntTrieSet, target: DeclaredMethod, fps: ConstArray[VirtualFormalParameter], isNonVirtualCall: Boolean
    )(implicit state: State): Unit = {
        val fp = fps(0)
        val ptss = currentPointsToOfDefSites(fp, receiverDefSites)
        val declClassType = target.declaringClassType.asObjectType
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

    private[this] def doProcessMethod(
        implicit
        state: State
    ): ProperPropertyComputationResult = {

        // TODO Do not handle this here!
        // The garbage collector assigns every Reference to Reference.pending
        if (state.method.declaringClassType == javaLangRefReference && state.method.definedMethod.isConstructor) {
            val fieldOpt = p.resolveFieldReference(javaLangRefReference, "pending", javaLangRefReference)
            if (fieldOpt.isDefined) {
                state.includeSharedPointsToSet(
                    fieldOpt.get,
                    currentPointsToDefSite(fieldOpt.get, -1),
                    AllocationSitePointsToSet.noFilter
                )
            }
        }

        if (state.hasTACDependee)
            throw new IllegalStateException("points to analysis does not support refinement based tac")
        val tac = state.tac
        val method = state.method.definedMethod
        if (method.returnType.isReferenceType) {
            state.setLocalPointsToSet(state.method, emptyPointsToSet, { t ⇒ classHierarchy.isSubtypeOf(t, method.returnType.asReferenceType) })
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
                    case _ ⇒ AllocationSitePointsToSet.noFilter
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

            case Assignment(pc, targetVar, call: FunctionCall[DUVar[ValueInformation]]) ⇒
                val callees: Callees = state.callees(ps)
                val targets = callees.callees(pc)
                val defSiteObject = definitionSites(method, pc)

                if (targetVar.value.isReferenceValue &&
                    // Unsafe.getObject and getObjectVolatile are handled like getField
                    ((call.declaringClass ne UnsafeT) || !call.name.startsWith("getObject"))) {
                    val index = tac.pcToIndex(pc)
                    val nextStmt = tac.stmts(index + 1)
                    val filter = nextStmt match {
                        case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                            t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
                        case _ ⇒
                            AllocationSitePointsToSet.noFilter
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

            case Assignment(_, DVar(rv: IsReferenceValue, _), _) ⇒
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

            case ReturnValue(_, UVar(_: IsReferenceValue, defSites)) ⇒
                state.includeLocalPointsToSets(
                    state.method, currentPointsToOfDefSites(state.method, defSites),
                    { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, state.method.descriptor.returnType.asReferenceType) }
                )

            case Throw(_, UVar(_, defSites)) ⇒
                val entity = MethodExceptions(state.method)
                state.includeLocalPointsToSets(
                    entity, currentPointsToOfDefSites(entity, defSites),
                    { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ObjectType.Throwable) }
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
                        state.includeLocalPointsToSet(defSite, pointsToSet, AllocationSitePointsToSet.noFilter)
                    }
                case "get" ⇒
                    val arrays = tamiFlexLogData.arrays(state.method, line)

                    for (array ← arrays) {
                        handleArrayLoad(array, pc, call.params.head.asVar.definedBy)
                    }
                case "set" ⇒
                    val arrays = tamiFlexLogData.arrays(state.method, line)

                    for (array ← arrays) {
                        handleArrayStore(
                            array, call.params.head.asVar.definedBy, call.params(2).asVar.definedBy
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
                                pc, state.method, ObjectType.Class, isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter

                        )
                    }
                case "getDeclaredField" ⇒
                    val fields = tamiFlexLogData.classes(state.method, line)
                    if (fields.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, FieldT, isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter
                        )
                    }

                case "getDeclaredFields" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)

                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(FieldT), isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter
                        )
                        // todo store something into the array
                    }

                case "getDeclaredMethod" ⇒
                    val methods = tamiFlexLogData.methods(state.method, line)
                    if (methods.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, MethodT, isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter

                        )
                    }

                case "getDeclaredMethods" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)

                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(MethodT), isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter

                        )
                    }

                case "getMethod" ⇒
                    val methods = tamiFlexLogData.methods(state.method, line)
                    if (methods.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, MethodT, isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter

                        )
                    }

                case "getMethods" ⇒
                    val classTypes = tamiFlexLogData.classes(state.method, line)

                    if (classTypes.nonEmpty) {
                        state.includeLocalPointsToSet(
                            definitionSites(state.method.definedMethod, pc),
                            createPointsToSet(
                                pc, state.method, ArrayType(MethodT), isConstant = true
                            ),
                            AllocationSitePointsToSet.noFilter

                        )
                    }

                case "newInstance" ⇒
                    val allocatedTypes = tamiFlexLogData.classes(state.method, line)
                    if (allocatedTypes.nonEmpty) {
                        println(s"allocating ${allocatedTypes.mkString(",")}")
                    }

                    for (allocatedType ← allocatedTypes) {
                        val pointsToSet = createPointsToSet(
                            pc, state.method, allocatedType, isConstant = false
                        )
                        val defSite = definitionSites(state.method.definedMethod, pc)
                        state.includeLocalPointsToSet(defSite, pointsToSet, AllocationSitePointsToSet.noFilter)
                    }

                case _ ⇒

            }
        } else if (target.declaringClassType eq ConstructorT) {
            target.name match {
                case "getModifiers" ⇒
                // TODO
                case "newInstance" ⇒
                    val allocatedTypes = tamiFlexLogData.classes(state.method, line)
                    if (allocatedTypes.nonEmpty) {
                        println(s"allocating ${allocatedTypes.mkString(",")}")
                    }

                    for (allocatedType ← allocatedTypes) {
                        val pointsToSet = createPointsToSet(
                            pc, state.method, allocatedType, isConstant = false
                        )
                        val defSite = definitionSites(state.method.definedMethod, pc)
                        state.includeLocalPointsToSet(defSite, pointsToSet, AllocationSitePointsToSet.noFilter)
                    }
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
                case "putObject" | "putObjectVolative" | "putOrderedObject" ⇒
                    handlePutField(UnsafeFakeField, call.params.head.asVar.definedBy, call.params(2).asVar.definedBy)
                case "compareAndSwapObject" ⇒
                    handlePutField(UnsafeFakeField, call.params.head.asVar.definedBy, call.params(3).asVar.definedBy)
                case _ ⇒
            }
        }

    }

    // todo: reduce code duplication
    private def handleIndirectCall(
        pc:      Int,
        target:  DeclaredMethod,
        callees: Callees,
        tac:     TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        val fps = formalParameters(target)

        if (fps != null) {
            // handle receiver for non static methods
            val receiverOpt = callees.indirectCallReceiver(pc, target)
            if (receiverOpt.isDefined) {
                val receiverDefSites = valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                handleCallReceiver(receiverDefSites, target, fps, false)
            } else {
                // todo: distinguish between static methods and unavailable info
            }

            val indirectParams = callees.indirectCallParameters(pc, target)
            val descriptor = target.descriptor
            for (i ← 0 until descriptor.parametersCount) {
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
                val (defSite, field) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                results += InterimPartialResult(
                    dependees.values,
                    continuationForNewAllocationSitesAtGetField(defSite, field, dependees)
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
                        continuationForNewAllocationSitesAtPutField(defSitesEPKs, field, dependees)
                    )
            }
        }

        for (fakeEntity ← state.arrayLoadsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, arrayType) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                results += InterimPartialResult(
                    dependees.values,
                    continuationForNewAllocationSitesAtArrayLoad(defSite, arrayType, dependees)
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
                        continuationForNewAllocationSitesAtArrayStore(defSitesEPKs, arrayType, dependees)
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

    private[this] def updatedDependees(
        eps: SomeEPS, oldDependees: Map[SomeEPK, SomeEOptionP]
    ): Map[SomeEPK, SomeEOptionP] = {
        val epk = eps.toEPK
        if (eps.isRefinable) {
            oldDependees + (epk → eps)
        } else {
            oldDependees - epk
        }
    }

    private[this] def updatedPointsToSet(
        oldPointsToSet:         PointsToSet,
        newDependeePointsToSet: PointsToSet,
        dependee:               SomeEPS,
        oldDependees:           Map[SomeEPK, SomeEOptionP],
        typeFilter:             ReferenceType ⇒ Boolean
    ): PointsToSet = {
        val oldDependeePointsTo = oldDependees(dependee.toEPK) match {
            case UBP(ub: PointsToSet @unchecked) ⇒ ub
            case _: EPK[_, PointsToSet]          ⇒ emptyPointsToSet
            case d ⇒
                throw new IllegalArgumentException(s"unexpected dependee $d")
        }

        oldPointsToSet.included(
            newDependeePointsToSet,
            oldDependeePointsTo.numElements,
            oldDependeePointsTo.numTypes,
            typeFilter
        )
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

    private def getNumElements(eopt: SomeEOptionP): Int = {
        if (eopt.isEPK) 0
        else eopt.ub.asInstanceOf[PointsToSet].numElements
    }

    private[this] def continuationForNewAllocationSitesAtPutField(
        rhsDefSitesEPS: Traversable[SomeEPK], field: AField, dependees: Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    // TODO: Refactor
                    val fieldClassType = field.classType
                    val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                        results ::= InterimPartialResult(
                            rhsDefSitesEPS,
                            continuationForShared(
                                (as, field),
                                rhsDefSitesEPS.toIterator.map(d ⇒ d → d).toMap,
                                { t: ReferenceType ⇒
                                    classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
                                }
                            )
                        )
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtPutField(rhsDefSitesEPS, field, newDependees)
                    )
                }
                Results(
                    results
                )
        }
    }

    private[this] def continuationForNewAllocationSitesAtArrayStore(
        rhsDefSitesEPS: Traversable[SomeEPK], arrayType: ArrayType, dependees: Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (typeId < 0 &&
                        classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                        !isEmptyArrayAllocationSite(as.asInstanceOf[Long])) {
                        results ::= InterimPartialResult(
                            rhsDefSitesEPS,
                            continuationForShared(
                                ArrayEntity(as),
                                rhsDefSitesEPS.toIterator.map(d ⇒ d → d).toMap,
                                { t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, ArrayType.lookup(allocationSiteLongToTypeId(as.asInstanceOf[Long])).componentType.asReferenceType) }
                            )
                        )
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtArrayStore(rhsDefSitesEPS, arrayType, newDependees)
                    )
                }
                Results(
                    results
                )
        }
    }

    // todo name
    private[this] def continuationForNewAllocationSitesAtGetField(
        defSiteObject: DefinitionSite,
        field:         AField,
        dependees:     Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEPK] = Nil
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    // TODO: Refactor
                    val fieldClassType = field.classType
                    val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                        val epk = EPK((as, field), pointsToPropertyKey)
                        ps(epk)
                        nextDependees ::= epk
                    }
                }

                var results: List[ProperPropertyComputationResult] = Nil
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtGetField(defSiteObject, field, newDependees)
                    )
                }
                if (nextDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        nextDependees,
                        continuationForShared(
                            defSiteObject,
                            nextDependees.iterator.map(d ⇒ d → d).toMap,
                            { t: ReferenceType ⇒
                                classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
                            }
                        )
                    )
                }
                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    // todo name
    private[this] def continuationForNewAllocationSitesAtArrayLoad(
        defSiteObject: DefinitionSite,
        arrayType:     ArrayType,
        dependees:     Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEPK] = Nil
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (typeId < 0 && classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType)) {
                        val epk = EPK(ArrayEntity(as), pointsToPropertyKey)
                        ps(epk)
                        nextDependees ::= epk
                    }
                }

                var results: List[ProperPropertyComputationResult] = Nil
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtArrayLoad(defSiteObject, arrayType, newDependees)
                    )
                }
                if (nextDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        nextDependees,
                        continuationForShared(
                            defSiteObject,
                            nextDependees.iterator.map(d ⇒ d → d).toMap,
                            AllocationSitePointsToSet.noFilter
                        )
                    )
                }
                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    private[this] def continuationForShared(
        e: Entity, dependees: Map[SomeEPK, SomeEOptionP], typeFilter: ReferenceType ⇒ Boolean
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                if (newDependeePointsTo ne emptyPointsToSet) {
                    val pr = PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                        e, pointsToPropertyKey, {
                        case UBP(ub: PointsToSet @unchecked) ⇒
                            val newPointsToSet = updatedPointsToSet(
                                ub,
                                newDependeePointsTo,
                                eps,
                                dependees,
                                typeFilter
                            )

                            if (newPointsToSet ne ub) {
                                Some(InterimEUBP(e, newPointsToSet))
                            } else {
                                None
                            }

                        case _: EPK[Entity, _] ⇒
                            Some(InterimEUBP(
                                e,
                                newDependeePointsTo.filter(typeFilter)
                            ))

                        case eOptP ⇒
                            throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                    }
                    )

                    if (newDependees.nonEmpty) {
                        val ipr = InterimPartialResult(
                            newDependees.values, continuationForShared(e, newDependees, typeFilter)
                        )
                        Results(pr, ipr)
                    } else {
                        pr
                    }
                } else if (newDependees.nonEmpty) {
                    InterimPartialResult(
                        newDependees.values,
                        continuationForShared(e, newDependees, typeFilter)
                    )
                } else {
                    Results()
                }

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

    @inline private[this] def currentPointsTo(
        depender: Entity, dependee: Entity
    )(implicit state: State): PointsToSet = {
        dependee match {
            case ds: DefinitionSite if state.hasAllocationSitePointsToSet(ds) ⇒
                state.allocationSitePointsToSet(ds)

            case ds: DefinitionSite if state.hasLocalPointsToSet(ds) ⇒
                if (!state.hasDependency(depender, EPK(dependee, pointsToPropertyKey))) {
                    val p2s = propertyStore(dependee, pointsToPropertyKey)

                    //TODO: is it safe to comment this assertion out?  assert(p2s.isEPK)
                    state.addDependee(depender, p2s)
                }

                /*
                // TODO: It might be even more efficient to forward the dependencies.
                // However, this requires the continuation functions to handle Callees
                if (state.hasDependees(ds))
                    state.plainDependeesOf(ds).foreach(state.addDependee(depender, _))
                 */

                state.localPointsToSet(ds)
            case _ ⇒
                val epk = EPK(dependee, pointsToPropertyKey)
                val p2s = if (state.hasDependency(depender, epk)) {
                    // IMPROVE: add a method to the state
                    state.dependeesOf(depender)(epk)
                } else {
                    val p2s = propertyStore(dependee, pointsToPropertyKey)
                    if (p2s.isRefinable) {
                        state.addDependee(depender, p2s)
                    }
                    p2s
                }
                pointsToUB(p2s.asInstanceOf[EOptionP[Entity, PointsToSet]])
        }
    }

    @inline private[this] def currentPointsToOfDefSites(
        depender: Entity,
        defSites: IntTrieSet
    )(implicit state: State): Iterator[PointsToSet] = {
        defSites.iterator.map[PointsToSet](currentPointsToDefSite(depender, _))
    }

    @inline private[this] def currentPointsToDefSite(
        depender: Entity, dependeeDefSite: Int
    )(implicit state: State): PointsToSet = {
        if (ai.isMethodExternalExceptionOrigin(dependeeDefSite)) {
            val pc = ai.pcOfMethodExternalException(dependeeDefSite)
            val defSite = toEntity(pc, state.method, state.tac.stmts).asInstanceOf[DefinitionSite]
            currentPointsTo(depender, CallExceptions(defSite))
        } else if (ai.isImmediateVMException(dependeeDefSite)) {
            // FIXME -  we need to get the actual exception type here
            createPointsToSet(ai.pcOfImmediateVMException(dependeeDefSite), state.method, ObjectType.Throwable, false)
        } else {
            currentPointsTo(depender, toEntity(dependeeDefSite, state.method, state.tac.stmts))
        }
    }

    @inline private[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }
}
