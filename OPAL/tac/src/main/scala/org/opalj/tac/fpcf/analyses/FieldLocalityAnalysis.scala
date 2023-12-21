/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import java.util.concurrent.ConcurrentHashMap

import org.opalj.ai.ValueOrigin
import org.opalj.br.DeclaredField
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.cfg.ExitNode
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
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
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.fieldaccess.AccessParameter
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

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
    final implicit val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)
    private[this] implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)
    final val typeExtensiblity = project.get(TypeExtensibilityKey)
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
        val fraiEP = propertyStore(declaredFields(state.field), FieldReadAccessInformation.key)
        if (handleFieldReadAccessInformation(fraiEP))
            return Result(state.field, NoLocalField);

        val fwaiEP = propertyStore(declaredFields(state.field), FieldWriteAccessInformation.key)
        if (handleFieldWriteAccessInformation(fwaiEP))
            return Result(state.field, NoLocalField);

        val potentialCloneCallers =
            if ((state.temporaryState meet ExtensibleLocalField) != state.temporaryState)
                allSubclassMethods(state.field)
            else // No need to examine subclass methods if locality is already 'Extensible'
                state.field.classFile.methodsWithBody.filter(!_.isStatic).toSet
        state.potentialCloneCallers = potentialCloneCallers

        val isNonLocal = potentialCloneCallers exists { method =>
            val tacaiAndCallers = getTACAIAndCallers(method)
            if (tacaiAndCallers.isEmpty)
                false
            else
                checkIsFieldNonLocalWithCloneCallers(
                    declaredMethods(method),
                    tacaiAndCallers.get._2,
                    null,
                    tacaiAndCallers.get._1
                )
        }

        if (isNonLocal)
            return Result(state.field, NoLocalField);

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
    private[this] def isLocalForFieldReadAccess(
        context:  Context,
        pc:       Int,
        receiver: AccessReceiver,
        tacai:    TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        // Values read from the field may not escape, except for
        // [[org.opalj.fpcf.properties.EscapeViaReturn]] in which case we have a
        // [[org.opalj.fpcf.properties.LocalFieldWithGetter]].
        val escape = propertyStore(
            (context, definitionSites(context.method.definedMethod, pc)),
            EscapeProperty.key
        )
        val isGetFieldOfReceiver =
            receiver.isDefined && uVarForDefSites(receiver.get, tacai.pcToIndex).definedBy == tac.SelfReferenceParameter
        !fieldValueEscapes(escape, isGetFieldOfReceiver)
    }

    /**
     * Checks a field read or write for locality, i.e, whether the field's value doesn't escape.
     */
    private[this] def isLocalForFieldWriteAccess(
        context: Context,
        pc:      Int,
        value:   AccessParameter,
        tacai:   TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        value.isEmpty || !checkFreshnessAndEscapeOfValue(uVarForDefSites(value.get, tacai.pcToIndex), pc, tacai.stmts, context)
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
        value: V, putField: Int, stmts: Array[Stmt[V]], context: Context
    )(implicit state: FieldLocalityState): Boolean = {
        value.definedBy exists { defSite =>
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

        // IMPROVE - we might treat values returned from a getter as fresh in some cases
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
    private[this] def getTACAIAndCallers(
        method: Method
    )(implicit state: FieldLocalityState): Option[(TACode[TACMethodParameter, V], Callers)] = {
        val tacEP = state.getTACDependee(method) match {
            case Some(tacEP) => tacEP
            case None =>
                val tacEP = propertyStore(method, TACAI.key)
                state.addTACDependee(tacEP)
                tacEP
        }

        val definedMethod = declaredMethods(method)
        val callersEP = state.getCallersDependee(definedMethod) match {
            case Some(callersEP) => callersEP
            case None =>
                val callersEP = propertyStore(definedMethod, Callers.key)
                state.addCallersDependee(callersEP)
                callersEP
        }

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined && callersEP.hasUBP)
            Some((tacEP.ub.tac.get, callersEP.ub))
        else
            None
    }

    /**
     * Handles field read access information property updates by comparing it to the previously saved property in the
     * state (if available).
     */
    private def handleFieldReadAccessInformation(
        faiEP: EOptionP[DeclaredField, FieldReadAccessInformation]
    )(implicit state: FieldLocalityState): Boolean = {
        val isNonLocal = if (faiEP.hasUBP) {
            val (seenDirectAccesses, seenIndirectAccesses) = state.getFieldReadAccessDependee match {
                case Some(UBP(fai)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _              => (0, 0)
            }

            faiEP.ub.getNewestAccesses(
                faiEP.ub.numDirectAccesses - seenDirectAccesses,
                faiEP.ub.numIndirectAccesses - seenIndirectAccesses
            ) exists { wa =>
                    val definedMethod = contextProvider.contextFromId(wa._1).method
                    val method = definedMethod.definedMethod
                    val pc = wa._2
                    val receiver = wa._3
                    state.tacFieldReadAccesses += method -> (state.tacFieldReadAccesses.getOrElse(method, Set.empty) + ((pc, receiver)))

                    val tacaiAndCallers = getTACAIAndCallers(method)
                    if (tacaiAndCallers.isDefined) {
                        val callers = tacaiAndCallers.get._2
                        val tacai = tacaiAndCallers.get._1

                        var isLocal = true
                        callers.forNewCalleeContexts(null, definedMethod) {
                            isLocal &&= isLocalForFieldReadAccess(_, pc, receiver, tacai)
                        }
                        !isLocal
                    } else
                        false
                }
        } else
            false

        state.setFieldReadAccessDependee(faiEP)
        isNonLocal
    }

    /**
     * Handles field access information property updates by comparing it to the previously saved property in the state
     * (if available).
     */
    private def handleFieldWriteAccessInformation(
        faiEP: EOptionP[DeclaredField, FieldWriteAccessInformation]
    )(implicit state: FieldLocalityState): Boolean = {
        val isNonLocal = if (faiEP.hasUBP) {
            val (seenDirectAccesses, seenIndirectAccesses) = state.getFieldWriteAccessDependee match {
                case Some(UBP(fai)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _              => (0, 0)
            }

            faiEP.ub.getNewestAccesses(
                faiEP.ub.numDirectAccesses - seenDirectAccesses,
                faiEP.ub.numIndirectAccesses - seenIndirectAccesses
            ) exists { wa =>
                    val definedMethod = contextProvider.contextFromId(wa._1).method
                    val method = definedMethod.definedMethod
                    val pc = wa._2
                    val parameter = wa._4
                    state.tacFieldWriteAccesses += method ->
                        (state.tacFieldWriteAccesses.getOrElse(method, Set.empty) + ((pc, parameter)))

                    val tacaiAndCallers = getTACAIAndCallers(method)
                    if (tacaiAndCallers.isDefined) {
                        val callers = tacaiAndCallers.get._2
                        val tacai = tacaiAndCallers.get._1

                        var isLocal = true
                        callers.forNewCalleeContexts(null, definedMethod) {
                            isLocal &&= isLocalForFieldWriteAccess(_, pc, parameter, tacai)
                        }
                        !isLocal
                    } else
                        false
                }
        } else
            false

        state.setFieldWriteAccessDependee(faiEP)
        isNonLocal
    }

    private[this] def handleNewTACAI(
        newEP: EOptionP[Method, TACAI]
    )(implicit state: FieldLocalityState): Boolean = {
        val definedMethod = declaredMethods(newEP.e)
        val callersEP = state.getCallersDependee(definedMethod)

        if (newEP.hasUBP && newEP.ub.tac.isDefined && callersEP.isDefined && callersEP.get.hasUBP) {
            val callers = callersEP.get.ub
            val tac = newEP.ub.tac.get

            checkIsFieldNonLocalWithAccessPCs(definedMethod, callers, null, tac) ||
                checkIsFieldNonLocalWithCloneCallers(definedMethod, callers, null, tac)
        } else
            false
    }

    private[this] def handleNewCallers(
        newEP: EOptionP[DeclaredMethod, Callers]
    )(implicit state: FieldLocalityState): Boolean = {
        val definedMethod = newEP.e.asDefinedMethod
        val oldEP = state.getCallersDependee(definedMethod)
        state.addCallersDependee(newEP)

        val tacDependee = state.getTACDependee(definedMethod.definedMethod)
        val oldCallers = oldEP.map(_.ub).orNull

        if (newEP.hasUBP && tacDependee.isDefined && tacDependee.get.hasUBP && tacDependee.get.ub.tac.isDefined) {
            val callers = newEP.ub
            val tac = tacDependee.get.ub.tac.get

            checkIsFieldNonLocalWithAccessPCs(definedMethod, callers, oldCallers, tac) ||
                checkIsFieldNonLocalWithCloneCallers(definedMethod, callers, oldCallers, tac)
        } else
            false
    }

    private[this] def checkIsFieldNonLocalWithAccessPCs(
        definedMethod:   DefinedMethod,
        currentCallers:  Callers,
        previousCallers: Callers,
        tac:             TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: FieldLocalityState): Boolean = {
        val method = definedMethod.definedMethod
        var isLocal = true

        if (state.tacFieldReadAccesses.contains(method)) {
            currentCallers.forNewCalleeContexts(previousCallers, definedMethod) { callContext =>
                isLocal &&= state.tacFieldReadAccesses(method).forall { wa =>
                    isLocalForFieldReadAccess(callContext, wa._1, wa._2, tac)
                }
            }
        }

        if (state.tacFieldWriteAccesses.contains(method)) {
            currentCallers.forNewCalleeContexts(previousCallers, definedMethod) { callContext =>
                isLocal &&= state.tacFieldWriteAccesses(method).forall { wa =>
                    isLocalForFieldWriteAccess(callContext, wa._1, wa._2, tac)
                }
            }
        }

        !isLocal
    }

    private[this] def checkIsFieldNonLocalWithCloneCallers(
        definedMethod:   DefinedMethod,
        currentCallers:  Callers,
        previousCallers: Callers,
        tac:             TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: FieldLocalityState): Boolean = {
        val method = definedMethod.definedMethod
        var isLocal = true

        if (state.potentialCloneCallers.contains(method)) {
            currentCallers.forNewCalleeContexts(previousCallers, definedMethod) {
                isLocal &&= isLocalForSuperCalls(_, tac)
            }
        }

        !isLocal
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
                handleNewTACAI(newEP)

            case Callers.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callers]]
                handleNewCallers(newEP)

            case Callees.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callees]]
                state.updateCalleeDependee(newEP)
                state.getCallsites(newEP.e).exists(pc => handleCallSite(newEP.e, pc))

            case FieldReadAccessInformation.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredField, FieldReadAccessInformation]]
                handleFieldReadAccessInformation(newEP)

            case FieldWriteAccessInformation.key =>
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]]
                handleFieldWriteAccessInformation(newEP)
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
        DeclaredMethodsKey,
        DefinitionSitesKey,
        TypeExtensibilityKey,
        ClosedPackagesKey,
        ContextProviderKey,
        DeclaredFieldsKey
    )

    final override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        TACAI,
        EscapeProperty,
        ReturnValueFreshness,
        Callees,
        FieldReadAccessInformation,
        FieldWriteAccessInformation
    )

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
