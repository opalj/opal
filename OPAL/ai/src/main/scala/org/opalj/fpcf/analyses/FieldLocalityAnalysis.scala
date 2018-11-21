/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.util.concurrent.ConcurrentHashMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.value.ValueInformation
import org.opalj.br.ClassFile
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.cfg.ExitNode
import org.opalj.ai.ValueOrigin
import org.opalj.ai.common.DefinitionSiteLike
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.tac.Assignment
import org.opalj.tac.Const
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.PutField
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.properties.TACAI

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
    final val typeExtensiblity = project.get(TypeExtensibilityKey)
    final val definitionSites = project.get(DefinitionSitesKey)

    /**
     * Checks if the field locality can be determined trivially.
     * Otherwise it forwards to `FieldLocalityAnalysis.step2`.
     */
    def step1(field: Field): PropertyComputationResult = {
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
    private[this] def step2(field: Field): PropertyComputationResult = {
        implicit val state: FieldLocalityState = new FieldLocalityState(field)

        val thisType = field.classFile.thisType

        val methodsOfThisType = field.classFile.methodsWithBody

        // If the class does not override clone, it can be leaked by `java.lang.Object.clone` that
        // creates a shallow copy.
        if (!methodsOfThisType.exists(m ⇒
            m.name == "clone" && m.descriptor.parametersCount == 0 && !m.isSynthetic)) {
            // If the class is not [[java.lang.Cloneable]] it can't be cloned directly
            // (`java.lang.Object.clone` with throw a [[java.lang.CloneNotSupportedException]]).
            // Otherwise, the field may be leaked!
            if (classHierarchy.isASubtypeOf(thisType, ObjectType.Cloneable).isYesOrUnknown) {
                return Result(field, NoLocalField)
            };

            val subtypes = classHierarchy.allSubtypes(thisType, reflexive = false)
            val existsCloneableSubtype = subtypes.exists { subtype ⇒
                classHierarchy.isASubtypeOf(subtype, ObjectType.Cloneable).isYesOrUnknown
            }

            // If there may be a Cloneable subtype, the field could be leaked through this subtype,
            // but if the client knows that the precise type of the reference is never cloneable,
            // it may treat the field as local (i.e. it is an
            // [[org.opalj.fpcf.properties.ExtensibleLocalField]].
            if (typeExtensiblity(thisType).isYesOrUnknown || existsCloneableSubtype) {
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
    private[this] def step3()(implicit state: FieldLocalityState): PropertyComputationResult = {
        for {
            method ← allMethodsHavingAccess(state.field)
            tacai ← getTACAI(method)
        } {
            if (!isLocalForMethod(method, tacai))
                return Result(state.field, NoLocalField);
        }

        returnResult
    }

    private[this] def isLocalForMethod(
        method: Method,
        tacai:  TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        val field = state.field
        val fieldName = field.name
        val fieldType = field.fieldType
        val stmts = tacai.stmts
        stmts.iterator.zipWithIndex.forall {
            case (stmt, index) ⇒
                stmt match {
                    // Values read from the field may not escape, except for
                    // [[org.opalj.fpcf.properties.EscapeViaReturn]] in which case we have a
                    // [[org.opalj.fpcf.properties.LocalFieldWithGetter]].
                    case _@ Assignment(pc, _, GetField(_, declType, `fieldName`, `fieldType`, objRef)) ⇒
                        project.resolveFieldReference(declType, fieldName, fieldType) match {
                            case Some(`field`) ⇒
                                val escape =
                                    propertyStore(definitionSites(method, pc), EscapeProperty.key)
                                val isGetFieldOfReceiver =
                                    objRef.asVar.definedBy == tac.SelfReferenceParameter
                                !handleEscape(escape, isGetFieldOfReceiver)
                            case _ ⇒
                                true // A field from a different class (None if that class is unknown)
                        }

                    // Values assigned to the field must be fresh and non-escaping.
                    case PutField(pc, declaredFieldType, `fieldName`, `fieldType`, _, value) ⇒
                        project.resolveFieldReference(declaredFieldType, `fieldName`, `fieldType`) match {
                            case Some(`field`) ⇒
                                // value is fresh and does not escape.
                                // in case of escape via return, we have a local field with getter
                                !checkFreshnessAndEscapeOfValue(value, pc, stmts, method)
                            case _ ⇒
                                true // A field from a different class (None if that class is unknown)
                        }

                    // When super.clone is called, the field must always be overwritten.
                    case Assignment(pc, left, NonVirtualFunctionCall(_, dc: ObjectType, false, "clone", descr, _, _)) ⇒
                        if (descr.parametersCount == 0 && field.classFile.superclassType.get == dc) {
                            !handleSuperCloneCall(index, pc, method, left.usedBy, tacai)
                        } else true

                    case _ ⇒ true
                }
        }
    }

    /**
     * Returns the set of all methods that could access a (non-public) field, i.e. all methods of
     * the declaring class, those of classes in the same package for package-private and protected
     * fields as well as those of subclasses for protected fields.
     */
    def allMethodsHavingAccess(field: Field): Iterator[Method] = {
        val thisType = field.classFile.thisType
        val initialClasses: Set[ClassFile] =
            if (field.isPackagePrivate || field.isProtected) {
                project.classesPerPackage(thisType.packageName)
            } else {
                Set(field.classFile)
            }
        if (field.isProtected) {
            val subclassesIterator: Iterator[ClassFile] =
                classHierarchy.allSubclassTypes(thisType, reflexive = false).
                    flatMap { ot ⇒
                        project.classFile(ot).filter(cf ⇒ !initialClasses.contains(cf))
                    }
            (initialClasses.iterator ++ subclassesIterator).flatMap(_.methodsWithBody)
        } else {
            initialClasses.iterator.flatMap(_.methodsWithBody)
        }
    }

    /**
     * Handles the effect an escape property has on the field locality.
     * @return false if the field may still be local, true otherwise
     * @note (Re-)Adds dependees as necessary.
     */
    private[this] def handleEscape(
        eOptionP:             EOptionP[DefinitionSiteLike, EscapeProperty],
        isGetFieldOfReceiver: Boolean
    )(
        implicit
        state: FieldLocalityState
    ): Boolean = eOptionP match {

        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ false

        // The field may be leaked by a getter, but only if the field's owning instance is the
        // receiver of the getter method.
        case FinalEP(_, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver) {
                state.updateWithMeet(LocalFieldWithGetter)
                false
            } else
                true

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
            false

        case IntermediateEP(_, _, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver) {
                state.updateWithMeet(LocalFieldWithGetter)
                state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
                false
            } else
                true

        // The escape state is worse than [[org.opalj.fpcf.properties.EscapeViaReturn]].
        case _: EPS[_, _] ⇒
            true

        case _ ⇒
            state.addDefinitionSiteDependee(eOptionP, isGetFieldOfReceiver)
            false
    }

    /**
     * Checks whether the value is fresh and non-escaping (except for the PutField statement given).
     * @return false if the value may be fresh and non-escaping, true otherwise
     */
    private[this] def checkFreshnessAndEscapeOfValue(
        value: Expr[V], putField: Int, stmts: Array[Stmt[V]], method: Method
    )(implicit state: FieldLocalityState): Boolean = {
        value.asVar.definedBy exists { defSite ⇒
            if (defSite < 0)
                true // Parameters are not fresh
            else {
                val stmt = stmts(defSite)

                // is the def site not fresh?
                if (checkFreshnessOfDef(stmt, method)) {
                    true
                } else {
                    val defSiteEntity = DefinitionSitesWithoutPutField(method, stmt.pc, putField)
                    val escape = propertyStore(defSiteEntity, EscapeProperty.key)
                    // does the value escape?
                    handleEscape(escape, isGetFieldOfReceiver = false)
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
        stmt: Stmt[V], caller: Method
    )(implicit state: FieldLocalityState): Boolean = {
        // the object stored in the field is fresh
        stmt match {
            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒
                false // fresh by definition

            case Assignment(pc, _, _: StaticFunctionCall[V] | _: NonVirtualFunctionCall[V] | _: VirtualFunctionCall[V]) ⇒
                handleCallSite(declaredMethods(caller), pc)

            case Assignment(_, _, _: Const) ⇒
                false

            case _ ⇒
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
        val calleesEP =
            if (state.calleeDependees.contains(caller)) {
                val (ep, pcs) = state.calleeDependees(caller)
                if (!pcs.contains(pc))
                    state.calleeDependees.update(caller, (ep, pcs + pc))
                ep
            } else {
                val ep = propertyStore(caller, Callees.key)
                state.calleeDependees.put(caller, (ep, IntTrieSet(pc)))
                ep
            }
        if (calleesEP.hasNoProperty) {
            false
        } else {
            val callees = calleesEP.ub

            if (callees.isIncompleteCallSite(pc)) {
                true
            } else {
                callees.callees(pc).exists { callee ⇒
                    callee.descriptor.returnType.isReferenceType &&
                        handleReturnValueFreshness(propertyStore(callee, ReturnValueFreshness.key))
                }
            }
        }
    }

    /**
     * Handles the influence of a return value freshness property on the field locality.
     * @return false if the field may still be local, true otherwise.
     * @note Adds dependees as necessary.
     */
    private[this] def handleReturnValueFreshness(
        eOptionP: EOptionP[DeclaredMethod, ReturnValueFreshness]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case EPS(_, _, NoFreshReturnValue) ⇒
            true

        //IMPROVE - we might treat values returned from a getter as fresh in some cases
        // e.g. if the method's receiver is the same as the analyzed field's owning instance.
        case EPS(_, _, Getter) ⇒
            true

        case EPS(_, _, ExtensibleGetter) ⇒
            true

        case FinalEP(_, FreshReturnValue) ⇒ false

        case FinalEP(_, PrimitiveReturnValue) ⇒
            throw new RuntimeException(s"unexpected property $eOptionP for entity ${state.field}")

        case epkOrCnd ⇒
            state.addMethodDependee(epkOrCnd)
            false
    }

    /**
     * Checks if the analyzed field is overwritten on each path from a call to super.clone (that
     * might create a shallow copy) to the method's exit node. This ensures that the field is not
     * leaked through a shallow copy.
     * @note If the field is overwritten, whether it is overwritten with a fresh and non-escaping
     *       value is checked by the general checks for PutField statements.
     */
    private[this] def handleSuperCloneCall(
        defSite: ValueOrigin,
        pc:      Int,
        method:  Method,
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
        val escape = propertyStore(definitionSites(method, pc), EscapeProperty.key)
        if (handleEscapeStateOfResultOfSuperClone(escape))
            return true;

        var enqueuedBBs: Set[CFGNode] = Set(tacai.cfg.bb(defSite)) // All scheduled BBs
        var worklist: List[CFGNode] = List(tacai.cfg.bb(defSite)) // BBs we still have to visit

        while (worklist.nonEmpty) {
            val currentBB = worklist.head
            worklist = worklist.tail

            var foundPut = false
            currentBB match {
                case currentBB: BasicBlock ⇒
                    for (index ← currentBB.startPC to currentBB.endPC) {
                        tacai.stmts(index) match {
                            case PutField(_, `thisType`, `fieldName`, `fieldType`, objRef, _) ⇒
                                if (objRef.asVar.definedBy == IntTrieSet(defSite)) {
                                    // The field's owning instance must be the result of super.clone
                                    foundPut = true // There is a matching PutField in this BB
                                }
                            case _ ⇒
                        }
                    }
                case exit: ExitNode ⇒
                    if (exit.isNormalReturnExitNode) {
                        return true; // Found the exit node, but no PutField, so the field may leak
                    }
                case _ ⇒
            }

            // If there is not PutField in this basic block, we have to inspect all successors
            if (!foundPut) {
                val successors = currentBB.successors.filter(!enqueuedBBs.contains(_))
                worklist ++= successors
                enqueuedBBs ++= successors
            }
        }
        false
    }

    /**
     * Checks, whether the result of a super.clone call does not escape except for being returned.
     */
    private[this] def handleEscapeStateOfResultOfSuperClone(
        eOptionP: EOptionP[DefinitionSiteLike, EscapeProperty]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ false

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        case FinalEP(_, EscapeViaReturn) ⇒ false

        case IntermediateEP(_, _, EscapeViaReturn) ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        // The escape state is worse than [[org.opalj.fpcf.properties.EscapeViaReturn]]
        case _: EPS[_, _] ⇒ true

        case _ ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAI(
        method: Method
    )(implicit state: FieldLocalityState): Option[TACode[TACMethodParameter, V]] = {
        val tacai = propertyStore(method, TACAI.key)

        if (tacai.isRefinable)
            state.addTACDependee(tacai)

        if (tacai.hasProperty) tacai.ub.tac
        else None
    }

    private[this] def continuation(
        someEPS: SomeEPS
    )(implicit state: FieldLocalityState): PropertyComputationResult = {
        val isNotLocal = someEPS.pk match {
            case ReturnValueFreshness.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]]
                state.removeMethodDependee(newEP)
                handleReturnValueFreshness(newEP)

            case EscapeProperty.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSiteLike, EscapeProperty]]
                val isGetFieldOfReceiver = state.isGetFieldOfReceiver(newEP.e)
                state.removeDefinitionSiteDependee(newEP)
                if (state.isDefinitionSiteOfClone(newEP.e))
                    handleEscapeStateOfResultOfSuperClone(newEP)
                else
                    handleEscape(newEP, isGetFieldOfReceiver)

            case TACAI.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[Method, TACAI]]
                state.removeTACDependee(newEP)
                if (newEP.isRefinable) state.addTACDependee(newEP)
                !isLocalForMethod(newEP.e, newEP.ub.tac.get)

            case Callees.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callees]]
                val pcs = state.calleeDependees(newEP.e)._2
                state.calleeDependees.update(newEP.e, (newEP, pcs))
                pcs.exists(pc ⇒ handleCallSite(newEP.e, pc))
        }
        if (isNotLocal) {
            Result(state.field, NoLocalField)
        } else
            returnResult
    }

    private[this] def returnResult(implicit state: FieldLocalityState): PropertyComputationResult = {
        if (state.hasNoDependees)
            Result(state.field, state.temporaryState)
        else
            IntermediateResult(
                state.field,
                NoLocalField,
                state.temporaryState,
                state.dependees,
                continuation,
                if (state.hasTacDependees) DefaultPropertyComputation else CheapPropertyComputation
            )
    }
}

sealed trait FieldLocalityAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(FieldLocality)

    final override def uses: Set[PropertyKind] = {
        Set(TACAI, EscapeProperty, ReturnValueFreshness, Callees)
    }

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

object EagerFieldLocalityAnalysis
    extends FieldLocalityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val allFields = p.allFields
        val analysis = new FieldLocalityAnalysis(p)
        ps.scheduleEagerComputationsForEntities(allFields)(analysis.step1)
        analysis
    }
}

object LazyFieldLocalityAnalysis
    extends FieldLocalityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    final override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new FieldLocalityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldLocality.key, analysis.step1
        )
        analysis
    }
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
        val defSite = tacode.pcToIndex(pc)
        if (defSite == -1) {
            // the code is dead
            IntTrieSet.empty
        } else {
            val Assignment(_, dvar, _) = tacode.stmts(defSite)
            dvar.usedBy - tacode.pcToIndex(putFieldPC)
        }
    }
}
