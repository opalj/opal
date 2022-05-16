/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import java.util.concurrent.ConcurrentHashMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.ExtensibleGetter
import org.opalj.br.fpcf.properties.ExtensibleLocalField
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.FreshReturnValue
import org.opalj.br.fpcf.properties.Getter
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.LocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.NoLocalField
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.cfg.ExitNode
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.ai.PCs
import org.opalj.ai.ValueOrigin
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.cg.Callers

/**
 * Determines whether the lifetime of a reference type field is the same as that of its owning
 * instance. Base type fields are treated as local for convenience, but this should never be
 * required anyway.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class FieldLocalityAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    type V = DUVar[ValueInformation]

    final implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)
    final val typeExtensiblity = project.get(TypeExtensibilityKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)

    /**
     * Checks if the field locality can be determined trivially.
     * Otherwise it forwards to `FieldLocalityAnalysis.step2`.
     */
    def step1(field: Field): ProperPropertyComputationResult = {
        val fieldType = field.fieldType
        val thisType = field.classFile.thisType
        // base types can be considered to be local
        if (fieldType.isBaseType) {
            return Result(field, LocalField);
        }

        if (field.isStatic) {
            return Result(field, NoLocalField);
        }

        // this analysis can not track public fields
        if (field.isPublic) {
            return Result(field, NoLocalField);
        }

        // There may be methods in unknown subtypes that leak the field
        if (field.isProtected && typeExtensiblity(thisType).isYesOrUnknown) {
            return Result(field, NoLocalField);
        }

        if (field.isPackagePrivate || field.isProtected) {
            val closedPackages = project.get(ClosedPackagesKey)
            // There may be methods in unknown classes in this package that leak the field
            if (!closedPackages.isClosed(thisType.packageName)) {
                return Result(field, NoLocalField);
            }
        }

        step2(field)
    }

    /**
     * Checks if the class declaring the field overrides clone.
     * If this is not the case and the class implements [[java.lang.Cloneable]], the field is not
     * local.
     * In case it is not cloneable, there might be still a cloneable subtype.
     * In this case, the field is at most an [[ExtensibleLocalField]].
     *
     * Afterwards it calls [[step3]].
     */
    private[this] def step2(field: Field): ProperPropertyComputationResult = {

        val thisType = field.classFile.thisType

        val methodsOfThisType = field.classFile.methodsWithBody
        val thisIsCloneable = isCloneable(thisType)

        implicit val state: FieldLocalityState = new FieldLocalityState(field, thisIsCloneable)

        // If the class does not override clone, it can be leaked by `java.lang.Object.clone` that
        // creates a shallow copy.
        if (!methodsOfThisType.exists(isClone)) {
            // If the class is not [[java.lang.Cloneable]] it can't be cloned directly
            // (`java.lang.Object.clone` with throw a [[java.lang.CloneNotSupportedException]]).
            // Otherwise, the field may be leaked!
            if (thisIsCloneable)
                return Result(field, NoLocalField);

            val subtypes = classHierarchy.allSubtypes(thisType, reflexive = false)
            val existsCloneableSubtypeWithoutClone = subtypes.exists { subtype =>
                isCloneable(subtype) &&
                    !project.classFile(subtype).exists(_.methodsWithBody.exists(isClone))
            }

            state.overridesClone = false

            // If there may be a Cloneable subtype, the field could be leaked through this subtype,
            // but if the client knows that the precise type of the reference is never cloneable,
            // it may treat the field as local (i.e. it is an
            // [[org.opalj.fpcf.properties.ExtensibleLocalField]].
            if (typeExtensiblity(thisType).isYesOrUnknown || existsCloneableSubtypeWithoutClone) {
                state.updateWithMeet(ExtensibleLocalField)
            }
        }

        step3()
    }

    /**
     * Ensures that all field accesses keep the field local (i.e., no non-local value is written to
     * the field and the field's value never escapes). Also, if super.clone is called, the field
     * has to overwritten to prevent it from being leaked by the shallow copy created through
     * `java.lang.Object.clone`.
     */
    private[this] def step3()(implicit state: FieldLocalityState): ProperPropertyComputationResult = {
        for {
            (method, pcs) <- fieldAccessInformation.allAccesses(state.field)
            (tacai, callees) <- getTACAIAndCallers(method, Some(pcs))
            pc <- pcs
        } {
            var isLocal = true
            callees.forNewCalleeContexts(null, declaredMethods(method)) {
                isLocal &&= isLocalForFieldAccess(_, pc, tacai)
            }
            if (!isLocal)
                return Result(state.field, NoLocalField);
        }

        val potentialCloneCallers =
            if ((state.temporaryState meet ExtensibleLocalField) != state.temporaryState)
                allSubclassMethods(state.field)
            else // No need to examine subclass methods if locality is already 'Extensible'
                state.field.classFile.methodsWithBody.filter(!_.isStatic).toSet
        state.potentialCloneCallers = potentialCloneCallers

        for {
            method <- potentialCloneCallers
            (tacai, callees) <- getTACAIAndCallers(method, None)
        } {
            var isLocal = true
            callees.forNewCalleeContexts(null, declaredMethods(method)) {
                isLocal &&= isLocalForSuperCalls(_, tacai)
            }
            if (!isLocal)
                return Result(state.field, NoLocalField);
        }

        returnResult
    }

    /**
     * Determines whether a type is (potentially) cloneable.
     */
    def isCloneable(tpe: ObjectType): Boolean = {
        classHierarchy.isASubtypeOf(tpe, ObjectType.Cloneable).isYesOrUnknown
    }

    /**
     * All methods from all subclasses (including the declaring class) of the fields declaring
     * class.
     */
    def allSubclassMethods(field: Field): Set[Method] = {
        classHierarchy.allSubclassTypes(field.classFile.thisType, reflexive = true).flatMap { ot =>
            project.classFile(ot)
        }.flatMap(_.methodsWithBody.filter(!_.isStatic)).toSet
    }

    /**
     * Checks a field read or write for locality, i.e, whether the field's value doesn't escape.
     */
    private[this] def isLocalForFieldAccess(
        context: Context,
        pc:      Int,
        tacai:   TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        val field = state.field
        val fieldName = field.name
        val fieldType = field.fieldType
        val index = tacai.properStmtIndexForPC(pc)

        if (index < 0)
            return true; // access is dead

        tacai.stmts(index) match {
            // Values read from the field may not escape, except for
            // [[org.opalj.fpcf.properties.EscapeViaReturn]] in which case we have a
            // [[org.opalj.fpcf.properties.LocalFieldWithGetter]].
            case _@ Assignment(_, _, GetField(_, declType, `fieldName`, `fieldType`, objRef)) =>
                project.resolveFieldReference(declType, fieldName, fieldType) match {
                    case Some(`field`) =>
                        val escape = propertyStore(
                            (context, definitionSites(context.method.definedMethod, pc)),
                            EscapeProperty.key
                        )
                        val isGetFieldOfReceiver =
                            objRef.asVar.definedBy == tac.SelfReferenceParameter
                        !fieldValueEscapes(escape, isGetFieldOfReceiver)
                    case _ =>
                        true // A field from a different class (None if that class is unknown)
                }

            // Values assigned to the field must be fresh and non-escaping.
            case PutField(_, declaredFieldType, `fieldName`, `fieldType`, _, value) =>
                project.resolveFieldReference(declaredFieldType, `fieldName`, `fieldType`) match {
                    case Some(`field`) =>
                        // value is fresh and does not escape.
                        // in case of escape via return, we have a local field with getter
                        !checkFreshnessAndEscapeOfValue(value, pc, tacai.stmts, context)
                    case _ =>
                        true // A field from a different class (None if that class is unknown)
                }

            case _ => true
        }
    }

    /**
     * Checks whether a method impedes locality by invoking super.clone and not overwriting the
     * field with a new value.
     */
    private[this] def isLocalForSuperCalls(
        context: Context,
        tacai:   TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        def callsSuperClone(call: NonVirtualFunctionCall[V]): Boolean = {
            call.name == "clone" && call.descriptor.parametersCount == 0 &&
                state.field.classFile.superclassType.get == call.declaringClass
        }

        val method = context.method.definedMethod

        val stmts = tacai.stmts
        val maxIndex = stmts.length
        var index = 0
        while (index < maxIndex) {
            val stmt = stmts(index)
            stmt match {
                case NonVirtualFunctionCallStatement(call) if callsSuperClone(call) =>
                    val uses = stmt.asAssignment.targetVar.usedBy
                    if (!isOverwrittenOnEachPathFromClone(index, stmt.pc, context, uses, tacai)) {
                        if (method.classFile eq state.field.classFile) {
                            if (state.thisIsCloneable)
                                return false;

                            if (!isClone(method)) {
                                // If this is the clone method, the field may be local as long as no
                                // subclass invokes super.clone without assigning the field itself
                                state.updateWithMeet(ExtensibleLocalField)
                            }
                        } else if (!state.overridesClone) {
                            // If the declaring class overrides clone, subclasses may safely invoke
                            // super.clone
                            if (isCloneable(method.classFile.thisType) || !isClone(method)) {
                                // If this type is cloneable, a shallow copy may be made because the
                                // declaring class did not override clone
                                // If this is the clone method, the field may be local as long as no
                                // subclass invokes super.clone without assigning the field itself
                                state.updateWithMeet(ExtensibleLocalField)
                            }
                        }
                    }
                case _ =>
            }
            index += 1
        }
        true
    }

    /**
     * Determines if the given method implements `clone`.
     */
    def isClone(method: Method): Boolean = {
        method.name == "clone" && !method.isSynthetic && method.descriptor.parametersCount == 0
    }

    /**
     * Handles the effect an escape property has on the field locality.
     * @return false if the field may still be local, true otherwise
     * @note (Re-)Adds dependees as necessary.
     */
    private[this] def fieldValueEscapes(
        eOptionP:             EOptionP[(Context, DefinitionSiteLike), EscapeProperty],
        isGetFieldOfReceiver: Boolean
    )(
        implicit
        state: FieldLocalityState
    ): Boolean = eOptionP match {

        case FinalP(NoEscape | EscapeInCallee) => false

        // The field may be leaked by a getter, but only if the field's owning instance is the
        // receiver of the getter method.
        case FinalP(EscapeViaReturn) =>
            if (isGetFieldOfReceiver) {
                state.updateWithMeet(LocalFieldWithGetter)
                false
            } else
                true

        case InterimUBP(NoEscape | EscapeInCallee) =>
            state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
            false

        case InterimUBP(EscapeViaReturn) =>
            if (isGetFieldOfReceiver) {
                state.updateWithMeet(LocalFieldWithGetter)
                state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
                false
            } else
                true

        // The escape state is worse than [[org.opalj.fpcf.properties.EscapeViaReturn]].
        case _: SomeEPS =>
            true

        case _ =>
            state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
            false
    }

    /**
     * Checks whether the value is fresh and non-escaping (except for the PutField statement given).
     * @return false if the value may be fresh and non-escaping, true otherwise
     */
    private[this] def checkFreshnessAndEscapeOfValue(
        value: Expr[V], putField: Int, stmts: Array[Stmt[V]], context: Context
    )(implicit state: FieldLocalityState): Boolean = {
        value.asVar.definedBy exists { defSite =>
            if (defSite < 0)
                true // Parameters are not fresh
            else {
                val stmt = stmts(defSite)

                // is the def site not fresh?
                if (checkFreshnessOfDef(stmt, context.method)) {
                    true
                } else {
                    val method = context.method.definedMethod
                    val defSiteEntity = DefinitionSitesWithoutPutField(method, stmt.pc, putField)
                    val escape = propertyStore((context, defSiteEntity), EscapeProperty.key)
                    // does the value escape?
                    fieldValueEscapes(escape, isGetFieldOfReceiver = false)
                }
            }
        }
    }

    /**
     * We consider a defSite (represented by the stmt) as fresh iff it was allocated in this method,
     * is a constant or is the result of call to a method with fresh return value.
     * @return false if the value may be fresh, true otherwise
     */
    private[this] def checkFreshnessOfDef(
        stmt: Stmt[V], caller: DeclaredMethod
    )(implicit state: FieldLocalityState): Boolean = {
        // the object stored in the field is fresh
        stmt match {
            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) =>
                false // fresh by definition

            case Assignment(pc, _, _: StaticFunctionCall[V] | _: NonVirtualFunctionCall[V] | _: VirtualFunctionCall[V]) =>
                handleCallSite(caller, pc)

            case Assignment(_, _, _: Const) =>
                false

            case _ =>
                true

        }
    }

    /**
     * Handles the influence of a call site on the field locality.
     * @return false if the field may still be local, true otherwise.
     * @note Adds dependees as necessary.
     */
    def handleCallSite(caller: DeclaredMethod, pc: Int)(
        implicit
        state: FieldLocalityState
    ): Boolean = {
        val calleesEP = state.addCallsite(caller, pc)
        if (calleesEP.isEPK) {
            false
        } else {
            val callees = calleesEP.ub

            callees.callerContexts.exists { callerContext =>
                if (callees.isIncompleteCallSite(callerContext, pc)) {
                    true
                } else {
                    callees.callees(callerContext, pc).exists { callee =>
                        callee.method.descriptor.returnType.isReferenceType &&
                            !isFreshReturnValue(propertyStore(callee, ReturnValueFreshness.key))
                    }
                }
            }
        }
    }

    /**
     * Handles the influence of a return value freshness property on the field locality.
     * @return true if the field may still be local, false otherwise.
     * @note Adds dependees as necessary.
     */
    private[this] def isFreshReturnValue(
        eOptionP: EOptionP[Context, ReturnValueFreshness]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case UBP(NoFreshReturnValue) =>
            false

        //IMPROVE - we might treat values returned from a getter as fresh in some cases
        // e.g. if the method's receiver is the same as the analyzed field's owning instance.
        case UBP(Getter) =>
            false

        case UBP(ExtensibleGetter) =>
            false

        case FinalP(FreshReturnValue) => true

        case FinalP(PrimitiveReturnValue) =>
            throw new RuntimeException(s"unexpected property $eOptionP for entity ${state.field}")

        case epkOrCnd =>
            state.addMethodDependee(epkOrCnd)
            true
    }

    /**
     * Checks if the analyzed field is overwritten on each path from a call to super.clone (that
     * might create a shallow copy) to the method's exit node. This ensures that the field is not
     * leaked through a shallow copy.
     * @note If the field is overwritten, whether it is overwritten with a fresh and non-escaping
     *       value is checked by the general checks for PutField statements.
     */
    private[this] def isOverwrittenOnEachPathFromClone(
        defSite: ValueOrigin,
        pc:      Int,
        context: Context,
        uses:    IntTrieSet,
        tacai:   TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        val field = state.field
        val thisType = field.classFile.thisType
        val fieldName = field.name
        val fieldType = field.fieldType

        // The cloned object may not escape except for being returned, because we only check
        // that the field is overwritten before the method's exit, not before a potential escape
        // of the object.
        val escape = propertyStore(
            (context, definitionSites(context.method.definedMethod, pc)), EscapeProperty.key
        )
        if (clonedInstanceEscapes(escape))
            return false;

        var enqueuedBBs: Set[CFGNode] = Set(tacai.cfg.bb(defSite)) // All scheduled BBs
        var worklist: List[CFGNode] = List(tacai.cfg.bb(defSite)) // BBs we still have to visit

        while (worklist.nonEmpty) {
            val currentBB = worklist.head
            worklist = worklist.tail

            var foundPut = false
            currentBB match {
                case currentBB: BasicBlock =>
                    for (index <- currentBB.startPC to currentBB.endPC) {
                        tacai.stmts(index) match {
                            case PutField(_, `thisType`, `fieldName`, `fieldType`, objRef, _) =>
                                if (objRef.asVar.definedBy == IntTrieSet(defSite)) {
                                    // The field's owning instance must be the result of super.clone
                                    foundPut = true // There is a matching PutField in this BB
                                }
                            case _ =>
                        }
                    }
                case exit: ExitNode =>
                    if (exit.isNormalReturnExitNode) {
                        return false; // Found the exit node, but no PutField, so the field may leak
                    }
                case _ =>
            }

            // If there is not PutField in this basic block, we have to inspect all successors
            if (!foundPut) {
                val successors = currentBB.successors.filter(!enqueuedBBs.contains(_))
                worklist ++= successors
                enqueuedBBs ++= successors
            }
        }
        true
    }

    /**
     * Checks, whether the result of a super.clone call does not escape except for being returned.
     */
    private[this] def clonedInstanceEscapes(
        eOptionP: EOptionP[(Context, DefinitionSiteLike), EscapeProperty]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case FinalP(NoEscape | EscapeInCallee) => false

        case InterimUBP(NoEscape | EscapeInCallee) =>
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        case FinalP(EscapeViaReturn) => false

        case InterimUBP(EscapeViaReturn) =>
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        // The escape state is worse than [[org.opalj.fpcf.properties.EscapeViaReturn]]
        case _: SomeEPS => true

        case _ =>
            state.addClonedDefinitionSiteDependee(eOptionP)
            false
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAIAndCallers(
        method: Method,
        pcs:    Option[PCs]
    )(implicit state: FieldLocalityState): Option[(TACode[TACMethodParameter, V], Callers)] = {

        val tacai = propertyStore(method, TACAI.key)

        if (pcs.isDefined) state.addTACDependee(tacai, pcs.get)
        else state.addTACDependee(tacai)

        val callers = propertyStore(declaredMethods(method), Callers.key)

        if (pcs.isDefined) state.addCallersDependee(callers, pcs.get)
        else state.addCallersDependee(callers)

        if (tacai.hasUBP && tacai.ub.tac.isDefined && callers.hasUBP)
            Some((tacai.ub.tac.get, callers.ub))
        else
            None
    }

    private[this] def c(
        someEPS: SomeEPS
    )(implicit state: FieldLocalityState): ProperPropertyComputationResult = {
        val isNotLocal = someEPS.pk match {
            case ReturnValueFreshness.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[Context, ReturnValueFreshness]]
                state.removeMethodDependee(newEP)
                !isFreshReturnValue(newEP)

            case EscapeProperty.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[(Context, DefinitionSiteLike), EscapeProperty]]
                val isGetFieldOfReceiver = state.isGetFieldOfReceiver(newEP.e)
                state.removeDefinitionSiteDependee(newEP)

                if (state.isDefinitionSiteOfClone(newEP.e))
                    clonedInstanceEscapes(newEP)
                else
                    fieldValueEscapes(newEP, isGetFieldOfReceiver)

            case TACAI.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[Method, TACAI]]
                state.addTACDependee(newEP)
                val m = newEP.e
                val dm = declaredMethods(m)
                val callersEP = state.getCallersDependee(dm)
                if (newEP.ub.tac.isDefined && callersEP.hasUBP) {
                    var isLocal = true
                    val tac = newEP.ub.tac.get
                    if (state.tacFieldAccessPCs.contains(m)) {
                        callersEP.ub.forNewCalleeContexts(null, dm) { callContext =>
                            isLocal &&= state.tacFieldAccessPCs(m).forall {
                                isLocalForFieldAccess(callContext, _, tac)
                            }
                        }
                    }
                    if (isLocal && state.potentialCloneCallers.contains(m)) {
                        callersEP.ub.forNewCalleeContexts(null, dm) {
                            isLocal &&= isLocalForSuperCalls(_, tac)
                        }
                    }
                    !isLocal
                } else
                    false

            case Callers.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callers]]
                val dm = newEP.e
                val oldEP = state.getCallersDependee(dm)
                state.addCallersDependee(newEP)
                val m = dm.definedMethod
                val tacDependee = state.getTACDependee(m)
                if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined) {
                    var isLocal = true
                    val tac = tacDependee.ub.tac.get
                    if (state.tacFieldAccessPCs.contains(m)) {
                        newEP.ub.forNewCalleeContexts(oldEP.ub, dm) { callContext =>
                            isLocal &&= state.tacFieldAccessPCs(m).forall {
                                isLocalForFieldAccess(callContext, _, tac)
                            }
                        }
                    }
                    if (isLocal && state.potentialCloneCallers.contains(m)) {
                        newEP.ub.forNewCalleeContexts(oldEP.ub, dm) {
                            isLocal &&= isLocalForSuperCalls(_, tac)
                        }
                    }
                    !isLocal
                } else
                    false

            case Callees.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callees]]
                state.updateCalleeDependee(newEP)
                state.getCallsites(newEP.e).exists(pc => handleCallSite(newEP.e, pc))
        }
        if (isNotLocal) {
            Result(state.field, NoLocalField)
        } else
            returnResult
    }

    private[this] def returnResult(
        implicit
        state: FieldLocalityState
    ): ProperPropertyComputationResult = {
        if (state.hasNoDependees)
            Result(state.field, state.temporaryState)
        else
            InterimResult(state.field, NoLocalField, state.temporaryState, state.dependees, c)
    }
}

sealed trait FieldLocalityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        FieldAccessInformationKey,
        DeclaredMethodsKey,
        DefinitionSitesKey,
        TypeExtensibilityKey,
        ClosedPackagesKey,
        TypeProviderKey
    )

    final override def uses: Set[PropertyBounds] = {
        Set(
            PropertyBounds.ub(TACAI),
            PropertyBounds.ub(EscapeProperty),
            PropertyBounds.ub(ReturnValueFreshness),
            PropertyBounds.ub(Callees)
        )
    }

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldLocality)

}

object EagerFieldLocalityAnalysis
    extends FieldLocalityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val allFields = p.allFields
        val analysis = new FieldLocalityAnalysis(p)
        ps.scheduleEagerComputationsForEntities(allFields)(analysis.step1)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazyFieldLocalityAnalysis
    extends FieldLocalityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    final override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new FieldLocalityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldLocality.key, analysis.step1
        )
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}

/**
 * Holds a map of [[DefinitionSiteWithoutPutField]] values, in order to provide unique identities
 * (enable comparison via eq/neq).
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
object DefinitionSitesWithoutPutField {

    private val defSites = {
        new ConcurrentHashMap[DefinitionSiteWithoutPutField, DefinitionSiteWithoutPutField]()
    }

    def apply(method: Method, pc: Int, putFieldPC: Int): DefinitionSiteWithoutPutField = {
        val defSite = DefinitionSiteWithoutPutField(method, pc, putFieldPC)
        val prev = defSites.putIfAbsent(defSite, defSite)
        if (prev == null) defSite else prev
    }
}

/**
 * Represents a definition site of an object that is stored into a field (that is being analyzed
 * for locality) where the field write use-site is removed from the set of use-sites.
 * It acts as an entity for the escape analysis (we are interested whether the objects stored into
 * a field are local, i.e. doe not escape).
 * Here, the [[org.opalj.tac.PutField]] would let the object escape.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
final case class DefinitionSiteWithoutPutField(
        method: Method, pc: Int, putFieldPC: Int
) extends DefinitionSiteLike {
    override def usedBy[V <: ValueInformation](
        tacode: TACode[TACMethodParameter, DUVar[V]]
    ): IntTrieSet = {
        val defSite = tacode.properStmtIndexForPC(pc)
        if (defSite == -1) {
            // the code is dead
            IntTrieSet.empty
        } else {
            val Assignment(_, dvar, _) = tacode.stmts(defSite)
            dvar.usedBy - tacode.properStmtIndexForPC(putFieldPC)
        }
    }
}
