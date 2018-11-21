/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package purity

import scala.annotation.switch

import scala.collection.immutable.IntMap

import net.ceedubs.ficus.Ficus._

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.properties.ClassifiedImpure
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.ContextuallyPure
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.ImpureByAnalysis
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.StaticDataUsage
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.UsesConstantDataOnly
import org.opalj.fpcf.properties.UsesNoStaticData
import org.opalj.fpcf.properties.UsesVaryingData
import org.opalj.fpcf.properties.VExtensibleGetter
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VGetter
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.cfg.CFG
import org.opalj.ai.isImmediateVMException
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.OriginOfThis
import org.opalj.tac.PutField
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.UVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.properties.TACAI

/**
 * An inter-procedural analysis to determine a method's purity.
 *
 * @note This analysis is sound only up to the usual standards, i.e. it does not cope with
 *       VirtualMachineErrors, LinkageErrors and ReflectiveOperationExceptions and may be unsound in
 *       the presence of native code, reflection or `sun.misc.Unsafe`. Calls to native methods are
 *       handled soundly in general as they are considered [[org.opalj.fpcf.properties.ImpureByAnalysis]],
 *       but native methods may break soundness of this analysis by invalidating assumptions such as
 *       which fields are effectively final.
 * @note This analysis is sound even if the three address code hierarchy is not flat, it will
 *       produce better results for a flat hierarchy, though. This is because it will not assess the
 *       types of expressions other than [[org.opalj.tac.Var]]s.
 * @note This analysis derives all purity level. A configurable [[DomainSpecificRater]] is used to
 *       identify calls, expressions and exceptions that are `LBDPure` instead of `LBImpure` or any
 *       `SideEffectFree` purity level. Compared to `L1PurityAnaylsis` it identifies objects/arrays
 *       returned from pure callees that can be considered local. Synchronized methods are treated
 *       as `ExternallyPure`.
 * @author Dominik Helm
 */
class L2PurityAnalysis private[analyses] (val project: SomeProject) extends AbstractPurityAnalysis {

    /**
     * Holds the state of this analysis.
     * @param lbPurity The current minimum purity level for the method
     * @param ubPurity The current maximum purity level for the method that will be assigned by
     *                  checkPurityOfX methods to aggregrate the purity
     * @param method The currently analyzed method
     * @param definedMethod The corresponding DefinedMethod we report results for
     * @param declClass The declaring class of the currently analyzed method
     * @param code The code of the currently analyzed method
     */
    class State(
            val method:        Method,
            val definedMethod: DeclaredMethod,
            val declClass:     ObjectType,
            var pcToIndex:     Array[Int]     = Array.empty,
            var code:          Array[Stmt[V]] = Array.empty,
            var lbPurity:      Purity         = CompileTimePure,
            var ubPurity:      Purity         = CompileTimePure
    ) extends AnalysisState {
        var fieldLocalityDependees: Map[Field, (EOptionP[Field, FieldLocality], Set[(Expr[V], Purity)])] = Map.empty

        var fieldMutabilityDependees: Map[Field, (EOptionP[Field, FieldMutability], Set[Option[Expr[V]]])] = Map.empty

        var classImmutabilityDependees: Map[ObjectType, (EOptionP[ObjectType, ClassImmutability], Set[Expr[V]])] = Map.empty
        var typeImmutabilityDependees: Map[ObjectType, (EOptionP[ObjectType, TypeImmutability], Set[Expr[V]])] = Map.empty

        var purityDependees: Map[DeclaredMethod, (EOptionP[DeclaredMethod, Purity], Set[Seq[Expr[V]]])] = Map.empty

        var calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None
        var callees: Option[Callees] = None

        var rvfDependees: Map[DeclaredMethod, (EOptionP[DeclaredMethod, ReturnValueFreshness], Set[(Option[Expr[V]], Purity)])] = Map.empty
        var rvfCallSites: IntMap[(Option[Expr[V]], Purity)] = IntMap.empty

        var staticDataUsage: Option[EOptionP[DeclaredMethod, StaticDataUsage]] = None

        var tacai: Option[EOptionP[Method, TACAI]] = None

        def dependees: Traversable[EOptionP[Entity, Property]] =
            (fieldLocalityDependees.valuesIterator.map(_._1) ++
                fieldMutabilityDependees.valuesIterator.map(_._1) ++
                classImmutabilityDependees.valuesIterator.map(_._1) ++
                typeImmutabilityDependees.valuesIterator.map(_._1) ++
                purityDependees.valuesIterator.map(_._1) ++
                calleesDependee ++
                rvfDependees.valuesIterator.map(_._1) ++
                staticDataUsage ++
                tacai).toTraversable

        def addFieldLocalityDependee(
            f:    Field,
            eop:  EOptionP[Field, FieldLocality],
            data: (Expr[V], Purity)
        ): Unit = {
            if (fieldLocalityDependees.contains(f)) {
                val (_, oldValues) = fieldLocalityDependees(f)
                fieldLocalityDependees += ((f, (eop, oldValues + data)))
            } else {
                fieldLocalityDependees += ((f, (eop, Set(data))))
            }
        }

        def addFieldMutabilityDependee(
            f:     Field,
            eop:   EOptionP[Field, FieldMutability],
            owner: Option[Expr[V]]
        ): Unit = {
            if (fieldMutabilityDependees.contains(f)) {
                val (_, oldOwners) = fieldMutabilityDependees(f)
                fieldMutabilityDependees += ((f, (eop, oldOwners + owner)))
            } else {
                fieldMutabilityDependees += ((f, (eop, Set(owner))))
            }
        }

        def addClassImmutabilityDependee(
            t:     ObjectType,
            eop:   EOptionP[ObjectType, ClassImmutability],
            value: Expr[V]
        ): Unit = {
            if (classImmutabilityDependees.contains(t)) {
                val (_, oldValues) = classImmutabilityDependees(t)
                classImmutabilityDependees += ((t, (eop, oldValues + value)))
            } else {
                classImmutabilityDependees += ((t, (eop, Set(value))))
            }
        }

        def addTypeImmutabilityDependee(
            t:     ObjectType,
            eop:   EOptionP[ObjectType, TypeImmutability],
            value: Expr[V]
        ): Unit = {
            if (typeImmutabilityDependees.contains(t)) {
                val (_, oldValues) = typeImmutabilityDependees(t)
                typeImmutabilityDependees += ((t, (eop, oldValues + value)))
            } else {
                typeImmutabilityDependees += ((t, (eop, Set(value))))
            }
        }

        def addPurityDependee(
            dm:     DeclaredMethod,
            eop:    EOptionP[DeclaredMethod, Purity],
            params: Seq[Expr[V]]
        ): Unit = {
            if (purityDependees.contains(dm)) {
                val (_, oldParams) = purityDependees(dm)
                purityDependees += ((dm, (eop, oldParams + params)))
            } else {
                purityDependees += ((dm, (eop, Set(params))))
            }
        }

        def updateCalleesDependee(eps: EOptionP[DeclaredMethod, Callees]): Unit = {
            if (eps.isFinal) calleesDependee = None
            else calleesDependee = Some(eps)
            if (eps.hasProperty)
                callees = Some(eps.ub)
        }

        def addRVFDependee(
            dm:   DeclaredMethod,
            eop:  EOptionP[DeclaredMethod, ReturnValueFreshness],
            data: (Option[Expr[V]], Purity)
        ): Unit = {
            if (rvfDependees.contains(dm)) {
                val (_, oldValues) = rvfDependees(dm)
                rvfDependees += ((dm, (eop, oldValues + data)))
            } else {
                rvfDependees += ((dm, (eop, Set(data))))
            }
        }

        def removeFieldLocalityDependee(f: Field): Unit = fieldLocalityDependees -= f
        def removeFieldMutabilityDependee(f: Field): Unit = fieldMutabilityDependees -= f
        def removeClassImmutabilityDependee(t: ObjectType): Unit = classImmutabilityDependees -= t
        def removeTypeImmutabilityDependee(t: ObjectType): Unit = typeImmutabilityDependees -= t
        def removePurityDependee(dm: DeclaredMethod): Unit = purityDependees -= dm
        def removeRVFDependee(dm: DeclaredMethod): Unit = rvfDependees -= dm

        def updateStaticDataUsage(eps: Option[EOptionP[DeclaredMethod, StaticDataUsage]]): Unit = {
            staticDataUsage = eps
        }

        def updateTacai(eps: EOptionP[Method, TACAI]): Unit = {
            if (eps.isFinal) tacai = None
            else tacai = Some(eps)
            if (eps.hasProperty) {
                val tac = eps.ub.tac.get
                pcToIndex = tac.pcToIndex
                code = tac.stmts
            }
        }
    }

    type StateType = State

    val raterFqn: String = project.config.as[String](
        "org.opalj.fpcf.analyses.L2PurityAnalysis.domainSpecificRater"
    )

    val rater: DomainSpecificRater =
        L2PurityAnalysis.rater.getOrElse(resolveDomainSpecificRater(raterFqn))

    /**
     * Examines whether the given expression denotes an object/array that is local to the current
     * method, i.e. the method has control over the object/array and actions on it might not
     * influence purity.
     *
     * @param otherwise The maxPurity will be reduced to at most this level if the expression is not
     *             local.
     */
    override def isLocal(
        expr:             Expr[V],
        otherwise:        Purity,
        excludedDefSites: IntTrieSet = EmptyIntTrieSet
    )(implicit state: State): Boolean = {
        isLocalInternal(
            expr,
            otherwise,
            _ ⇒ CompileTimePure,
            treatParamsAsFresh = false
        )
    }

    /**
     * Examines whether the given expression denotes an object/array that is local to the current
     * method, i.e. the method has control over the object/array and actions on it might not
     * influence purity.
     *
     * @note Fresh references can be treated as non-escaping as the analysis result will be impure
     *       if anything escapes the method via parameters, static field assignments or calls.
     *
     * @param otherwise The maxPurity will be reduced to at most this level if the expression is not
     *                  local
     * @param onParameter The maxPurity will be reduced to at most this level if the expression can
     *                    be a parameter
     * @param treatParamsAsFresh The value to be returned if the expression can be a parameter
     */
    def isLocalInternal(
        expr:               Expr[V],
        otherwise:          Purity,
        onParameter:        Int ⇒ Purity,
        treatParamsAsFresh: Boolean,
        excludedDefSites:   IntTrieSet   = EmptyIntTrieSet
    )(implicit state: State): Boolean = {
        if (expr eq null) // Expression is unknown due to an indirect call (e.g. reflection)
            return false;

        if (expr.isConst)
            return true;

        if (!expr.isVar) {
            // The expression could refer to further expressions in a non-flat representation.
            // In that case it could be, e.g., a GetStatic. In that case the reference is not
            // locally created and/or initialized. To avoid special handling, we just fallback to
            // false here as the analysis is intended to be used on flat representations anyway.
            atMost(otherwise)
            return false;
        }

        // Primitive values are always local (required for parameters of contextually pure calls)
        // TODO (value is null for the self reference of a throwable constructor...)
        if (expr.asVar.value != null &&
            (expr.asVar.value.computationalType ne ComputationalTypeReference))
            return true;

        val defSites = expr.asVar.definedBy -- excludedDefSites
        val isLocal =
            defSites.forall(
                isLocalDefsite(
                    _,
                    otherwise,
                    onParameter,
                    treatParamsAsFresh,
                    defSites,
                    excludedDefSites
                )
            )
        if (!isLocal) atMost(otherwise)
        isLocal
    }

    /**
     * Examines whether the given defsite denotes an object/array that is local to the current
     * method, i.e. the method has control over the object/array and actions on it might not
     * influence purity.
     *
     * @param otherwise The maxPurity will be reduced to at most this level if the defsite is not
     *                  local
     * @param onParameter The maxPurity will be reduced to at most this level if the defsite is a
     *                     parameter
     * @param treatParamsAsFresh The value to be returned if the defsite is a parameter
     */
    def isLocalDefsite(
        defSite:            Int,
        otherwise:          Purity,
        onParameter:        Int ⇒ Purity,
        treatParamsAsFresh: Boolean,
        defSites:           IntTrieSet,
        excludedDefSites:   IntTrieSet
    )(implicit state: State): Boolean = {
        if (isImmediateVMException(defSite))
            return true; // VMLevelValues are freshly created

        if (ai.isMethodExternalExceptionOrigin(defSite))
            return false; // Method external exceptions are not freshly created

        if (defSite == OriginOfThis) {
            if (!state.method.isConstructor) {
                atMost(onParameter(0))
            }
            return treatParamsAsFresh | state.method.isConstructor;
        }

        if (defSite < 0) {
            atMost(onParameter(-defSite - 1))
            return treatParamsAsFresh;
        }

        val stmt = state.code(defSite)
        assert(stmt.astID == Assignment.ASTID, "defSite should be assignment")

        val rhs = stmt.asAssignment.expr
        if (rhs.isConst)
            return true;

        (rhs.astID: @switch) match {
            case New.ASTID | NewArray.ASTID ⇒ true
            case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                VirtualFunctionCall.ASTID ⇒
                val oldPurityLevel =
                    state.rvfCallSites.get(stmt.pc).map(_._2).getOrElse(CompileTimePure)
                val data = (rhs.asFunctionCall.receiverOption, otherwise meet oldPurityLevel)
                if (state.callees.isDefined) {
                    checkFreshnessOfReturn(stmt.pc, data, state.callees.get)
                } else {
                    state.rvfCallSites += stmt.pc → data
                    reducePurityLB(otherwise)
                }
                true
            case GetField.ASTID ⇒
                val GetField(_, declClass, name, fieldType, objRef) = rhs
                project.resolveFieldReference(declClass, name, fieldType) match {
                    case Some(field) ⇒
                        val locality = propertyStore(field, FieldLocality.key)
                        checkLocalityOfField(locality, (objRef, otherwise)) &&
                            isLocalInternal(
                                objRef,
                                otherwise,
                                onParameter,
                                treatParamsAsFresh,
                                excludedDefSites ++ defSites
                            )
                    case None ⇒ false
                }
            case _ ⇒ false
        }
    }

    def checkLocalityOfField(
        ep:   EOptionP[Field, FieldLocality],
        data: (Expr[V], Purity)
    )(implicit state: State): Boolean = {
        val isLocal = ep match {
            case FinalEP(_, LocalField | LocalFieldWithGetter) ⇒
                true
            case FinalEP(_, ExtensibleLocalField | ExtensibleLocalFieldWithGetter) ⇒
                if (data._1.isVar) {
                    val value = data._1.asVar.value.asReferenceValue
                    value.isPrecise &&
                        !classHierarchy.isSubtypeOf(value.asReferenceType, ObjectType.Cloneable)
                } else
                    false
            case EPS(_, _, NoLocalField) ⇒
                false
            case _ ⇒
                reducePurityLB(data._2)
                if (data._2 meet state.ubPurity ne state.ubPurity)
                    state.addFieldLocalityDependee(ep.e, ep, data)
                true
        }
        if (!isLocal)
            atMost(data._2)
        isLocal
    }

    def checkLocalityOfReturn(
        ep:   EOptionP[DeclaredMethod, Property],
        data: (Option[Expr[V]], Purity)
    )(implicit state: State): Unit = {
        import project.classHierarchy.isSubtypeOf
        ep match {
            case EPS(_, PrimitiveReturnValue | FreshReturnValue |
                VPrimitiveReturnValue | VFreshReturnValue, _) ⇒
            case FinalEP(_, Getter | VGetter) ⇒
                if (data._2 meet state.ubPurity ne state.ubPurity)
                    isLocal(data._1.get, data._2)
            case FinalEP(_, ExtensibleGetter | VExtensibleGetter) ⇒
                if (data._1.get.isVar) {
                    val value = data._1.get.asVar.value.asReferenceValue
                    if (value.isPrecise && !isSubtypeOf(value.asReferenceType, ObjectType.Cloneable)) {
                        if (data._2 meet state.ubPurity ne state.ubPurity)
                            isLocal(data._1.get, data._2)
                    } else {
                        atMost(data._2)
                    }
                } else {
                    atMost(data._2)
                }
            case EPS(_, _, NoFreshReturnValue | VNoFreshReturnValue) ⇒
                atMost(data._2)
            case EOptionP(e, pk) ⇒
                reducePurityLB(data._2)
                if (data._2 meet state.ubPurity ne state.ubPurity) {
                    state.addRVFDependee(
                        e,
                        ep.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]],
                        data
                    )
                }
        }
    }

    def checkFreshnessOfReturn(
        pc:      Int,
        data:    (Option[Expr[V]], Purity),
        callees: Callees
    )(implicit state: State): Unit = {
        callees.callees(pc).foreach { callee ⇒
            checkLocalityOfReturn(propertyStore(callee, ReturnValueFreshness.key), data)
        }
    }

    /**
     * Examines a statement for its influence on the method's purity.
     * This method will return false for impure statements, so evaluation can be terminated early.
     */
    override def checkPurityOfStmt(stmt: Stmt[V])(implicit state: State): Boolean =
        (stmt.astID: @switch) match {
            // Synchronization on non-escaping local objects/arrays is pure (and useless...)
            case MonitorEnter.ASTID | MonitorExit.ASTID ⇒
                val objRef = stmt.asSynchronizationStmt.objRef
                isLocalInternal(
                    objRef,
                    ImpureByAnalysis,
                    param ⇒ ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)

            // Storing into non-escaping locally initialized arrays/objects is pure
            case ArrayStore.ASTID ⇒
                val arrayRef = stmt.asArrayStore.arrayRef
                isLocalInternal(
                    arrayRef,
                    ImpureByAnalysis,
                    param ⇒ ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)
            case PutField.ASTID ⇒
                val objRef = stmt.asPutField.objRef
                isLocalInternal(
                    objRef,
                    ImpureByAnalysis,
                    param ⇒ ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)

            case _ ⇒ super.checkPurityOfStmt(stmt)
        }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendees when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[DeclaredMethod, Purity],
        params: Seq[Expr[V]]
    )(implicit state: State): Boolean = ep match {
        case EPS(_, _, _: ClassifiedImpure) ⇒
            atMost(ImpureByAnalysis)
            false
        case eps @ EPS(_, lb, ub) ⇒
            if (eps.isRefinable && ((lb meet state.ubPurity) ne state.ubPurity)) {
                // On conditional, keep dependence
                state.addPurityDependee(ep.e, ep, params)
                reducePurityLB(lb)
            }
            // Contextual/external purity is handled below
            atMost(ub.withoutContextual)
            ub.modifiedParams.forall(param ⇒
                isLocalInternal(
                    params(param),
                    ImpureByAnalysis,
                    param ⇒ ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ))
        case EOptionP(_, pk) ⇒
            reducePurityLB(ImpureByAnalysis)
            state.addPurityDependee(ep.e, ep, params)
            true
    }

    /**
     * Handles the effect of static data usage on the purity level.
     *
     * @note Modifies dependees as necessary.
     */
    def checkStaticDataUsage(ep: EOptionP[DeclaredMethod, StaticDataUsage])(implicit state: State): Unit = {
        ep match {
            case EPS(_, UsesNoStaticData | UsesConstantDataOnly, _) ⇒
                state.updateStaticDataUsage(None)
            case EPS(_, _, UsesVaryingData) ⇒
                state.updateStaticDataUsage(None)
                atMost(Pure)
            case _ ⇒
                reducePurityLB(Pure)
                state.updateStaticDataUsage(Some(ep))
        }
    }

    /**
     * Adds the dependee necessary if a field mutability is not known yet.
     */
    override def handleUnknownFieldMutability(
        ep:     EOptionP[Field, FieldMutability],
        objRef: Option[Expr[V]]
    )(implicit state: State): Unit = {
        state.addFieldMutabilityDependee(ep.e, ep, objRef)
    }

    /**
     * Adds the dependee necessary if a type mutability is not known yet.
     */
    override def handleUnknownTypeMutability(
        ep:   EOptionP[ObjectType, Property],
        expr: Expr[V]
    )(implicit state: State): Unit = {
        if (ep.pk == ClassImmutability.key)
            state.addClassImmutabilityDependee(
                ep.e,
                ep.asInstanceOf[EOptionP[ObjectType, ClassImmutability]],
                expr
            )
        else
            state.addTypeImmutabilityDependee(
                ep.e,
                ep.asInstanceOf[EOptionP[ObjectType, TypeImmutability]],
                expr
            )
    }

    /**
     * Add or remove the dependee when the callees property changes.
     */
    override def handleCalleesUpdate(
        callees: EOptionP[DeclaredMethod, Callees]
    )(implicit state: State): Unit = {
        state.updateCalleesDependee(callees)
        if (callees.isRefinable)
            reducePurityLB(ImpureByAnalysis)
    }

    /*
     * Adds the dependee necessary if the TACAI is not yet final.
     */
    override def handleTACAI(ep: EOptionP[Method, TACAI])(implicit state: State): Unit = {
        state.updateTacai(ep)
    }

    /**
     * Removes dependees that are known to not be needed anymore as they can not reduce the max
     * purity level further.
     */
    def cleanupDependees()(implicit state: State): Unit = {
        if (state.ubPurity ne CompileTimePure)
            state.updateStaticDataUsage(None)

        if (!state.ubPurity.isDeterministic) {
            state.fieldMutabilityDependees = Map.empty
            state.classImmutabilityDependees = Map.empty
            state.typeImmutabilityDependees = Map.empty
            state.updateStaticDataUsage(None)
        }

        var newFieldLocalityDependees: Map[Field, (EOptionP[Field, FieldLocality], Set[(Expr[V], Purity)])] = Map.empty
        for ((dependee, (eop, data)) ← state.fieldLocalityDependees) {
            val newData = data.filter(_._2 meet state.ubPurity ne state.ubPurity)
            if (newData.nonEmpty) newFieldLocalityDependees += ((dependee, (eop, newData)))
        }
        state.fieldLocalityDependees = newFieldLocalityDependees

        var newRVFCallsites: IntMap[(Option[Expr[V]], Purity)] = IntMap.empty
        for ((callsite, data) ← state.rvfCallSites) {
            if (data._2 meet state.ubPurity ne state.ubPurity) newRVFCallsites += ((callsite, data))
        }
        state.rvfCallSites = newRVFCallsites

        var newRVFDependees: Map[DeclaredMethod, (EOptionP[DeclaredMethod, ReturnValueFreshness], Set[(Option[Expr[V]], Purity)])] = Map.empty
        for ((dependee, (eop, data)) ← state.rvfDependees) {
            val newData = data.filter(_._2 meet state.ubPurity ne state.ubPurity)
            if (newData.nonEmpty) newRVFDependees += ((dependee, (eop, newData)))
        }
        state.rvfDependees = newRVFDependees

        var newPurityDependees: Map[DeclaredMethod, (EOptionP[DeclaredMethod, Purity], Set[Seq[Expr[V]]])] = Map.empty
        for ((dependee, eAndD) ← state.purityDependees) {
            if (eAndD._1.hasNoProperty || (eAndD._1.lb meet state.ubPurity ne state.ubPurity))
                newPurityDependees += ((dependee, eAndD))
        }
        state.purityDependees = newPurityDependees
    }

    /**
     * Raises the lower bound on the purity whenever possible.
     */
    def adjustLowerBound()(implicit state: State): Unit = {
        if (state.calleesDependee.isDefined)
            return ; // Nothing to be done, lower bound is still LBImpure

        var newLowerBound = state.ubPurity

        if (state.tacai.isDefined) return ; // Nothing to be done, lower bound is still LBImpure

        for ((eop, _) ← state.purityDependees.valuesIterator) {
            eop match {
                case EPS(_, lb, _) ⇒ newLowerBound = newLowerBound meet lb
                case _ ⇒
                    return ; // Nothing to be done, lower bound is still LBImpure
            }
        }

        if (state.staticDataUsage.isDefined) newLowerBound = newLowerBound meet Pure

        if (state.fieldMutabilityDependees.nonEmpty ||
            state.classImmutabilityDependees.nonEmpty ||
            state.typeImmutabilityDependees.nonEmpty) {
            newLowerBound = newLowerBound meet SideEffectFree
        }

        for {
            (_, data) ← state.fieldLocalityDependees.valuesIterator
            (_, purity) ← data
        } {
            newLowerBound = newLowerBound meet purity
        }

        for {
            (_, purity) ← state.rvfCallSites.valuesIterator
        } {
            newLowerBound = newLowerBound meet purity
        }

        for {
            (_, data) ← state.rvfDependees.valuesIterator
            (_, purity) ← data
        } {
            newLowerBound = newLowerBound meet purity
        }

        state.lbPurity = newLowerBound
    }

    /**
     * Continuation to handle updates to properties of dependees.
     * Dependees may be
     *     - methods / virtual methods called (for their purity)
     *     - fields read (for their mutability)
     *     - classes files for class types returned (for their mutability)
     */
    def c(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {
        val oldPurity = state.ubPurity
        eps.ub.key match {
            case Purity.key ⇒
                val e = eps.e.asInstanceOf[DeclaredMethod]
                val dependees = state.purityDependees(e)
                state.removePurityDependee(e)
                dependees._2.foreach { e ⇒
                    checkMethodPurity(eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]], e)
                }
            case FieldMutability.key ⇒
                val e = eps.e.asInstanceOf[Field]
                val dependees = state.fieldMutabilityDependees(e)
                state.removeFieldMutabilityDependee(e)
                dependees._2.foreach { e ⇒
                    checkFieldMutability(eps.asInstanceOf[EOptionP[Field, FieldMutability]], e)
                }
            case ClassImmutability.key ⇒
                val e = eps.e.asInstanceOf[ObjectType]
                val dependees = state.classImmutabilityDependees(e)
                state.removeClassImmutabilityDependee(e)
                dependees._2.foreach { e ⇒
                    checkTypeMutability(
                        eps.asInstanceOf[EOptionP[ObjectType, ClassImmutability]],
                        e
                    )
                }
            case TypeImmutability.key ⇒
                val e = eps.e.asInstanceOf[ObjectType]
                val dependees = state.typeImmutabilityDependees(e)
                state.removeTypeImmutabilityDependee(e)
                dependees._2.foreach { e ⇒
                    checkTypeMutability(eps.asInstanceOf[EOptionP[ObjectType, TypeImmutability]], e)
                }
            case ReturnValueFreshness.key ⇒
                val e = eps.e.asInstanceOf[DeclaredMethod]
                val dependees = state.rvfDependees(e)
                state.removeRVFDependee(e)
                dependees._2.foreach { e ⇒
                    checkLocalityOfReturn(
                        eps.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]],
                        e
                    )
                }
            case FieldLocality.key ⇒
                val e = eps.e.asInstanceOf[Field]
                val dependees = state.fieldLocalityDependees(e)
                state.removeFieldLocalityDependee(e)
                dependees._2.foreach { e ⇒
                    checkLocalityOfField(eps.asInstanceOf[EOptionP[Field, FieldLocality]], e)
                }
            case Callees.key ⇒
                checkPurityOfCallees(eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]])
                state.rvfCallSites.foreach {
                    case (pc, data) ⇒
                        checkFreshnessOfReturn(pc, data, eps.ub.asInstanceOf[Callees])
                }
            case StaticDataUsage.key ⇒
                checkStaticDataUsage(eps.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]])
            case TACAI.key ⇒
                state.updateTacai(eps.asInstanceOf[EOptionP[Method, TACAI]])
                return determineMethodPurity(eps.ub.asInstanceOf[TACAI].tac.get.cfg);
        }

        if (state.ubPurity eq ImpureByAnalysis)
            return Result(state.definedMethod, ImpureByAnalysis);

        if (state.ubPurity ne oldPurity)
            cleanupDependees() // Remove dependees that we don't need anymore.
        adjustLowerBound()

        val dependees = state.dependees
        if (dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.definedMethod, state.ubPurity)
        } else {
            IntermediateResult(
                state.definedMethod,
                state.lbPurity,
                state.ubPurity,
                dependees,
                c
            )
        }
    }

    /**
     * Determines the purity of a method once TACAI is available.
     */
    def determineMethodPurity(
        cfg: CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): PropertyComputationResult = {
        // Special case: The Throwable constructor is `LBSideEffectFree`, but subtype constructors
        // may not be because of overridable fillInStackTrace method
        if (state.method.isConstructor && state.declClass.isSubtypeOf(ObjectType.Throwable))
            project.instanceMethods(state.declClass).foreach { mdc ⇒
                if (mdc.name == "fillInStackTrace" &&
                    mdc.method.classFile.thisType != ObjectType.Throwable) {
                    // "The value" is actually not used at all - hence, we can use "null"
                    // over here.
                    val selfReference = UVar(null, SelfReferenceParameter)
                    val fISTPurity = propertyStore(declaredMethods(mdc.method), Purity.key)
                    if (!checkMethodPurity(fISTPurity, Seq(selfReference))) {
                        // Early return for impure fillInStackTrace
                        return Result(state.definedMethod, state.ubPurity);
                    }
                }
            }

        // Synchronized methods have a visible side effect on the receiver
        // Static synchronized methods lock the class which is potentially globally visible
        if (state.method.isSynchronized)
            if (state.method.isStatic) return Result(state.definedMethod, ImpureByAnalysis);
            else atMost(ContextuallyPure(IntTrieSet(0)))

        val stmtCount = state.code.length
        var s = 0
        while (s < stmtCount) {
            if (!checkPurityOfStmt(state.code(s))) { // Early return for impure statements
                assert(state.ubPurity.isInstanceOf[ClassifiedImpure])
                return Result(state.definedMethod, state.ubPurity);
            }
            s += 1
        }

        val callees = propertyStore(state.definedMethod, Callees.key)
        if (!checkPurityOfCallees(callees))
            return Result(state.definedMethod, state.ubPurity)

        if (callees.hasProperty)
            state.rvfCallSites.foreach {
                case (pc, data) ⇒
                    checkFreshnessOfReturn(pc, data, callees.ub)
            }

        // Creating implicit exceptions is side-effect free (because of fillInStackTrace)
        // but it may be ignored as domain-specific
        val bbsCausingExceptions = cfg.abnormalReturnNode.predecessors
        for {
            bb ← bbsCausingExceptions
            pc = bb.asBasicBlock.endPC
            if isSourceOfImmediateException(pc)
        } {
            val throwingStmt = state.code(pc)
            val ratedResult = rater.handleException(throwingStmt)
            if (ratedResult.isDefined) atMost(ratedResult.get)
            else atMost(SideEffectFree)
        }

        if (state.ubPurity eq CompileTimePure) // Check static data usage only if necessary
            checkStaticDataUsage(propertyStore(state.definedMethod, StaticDataUsage.key))
        else
            cleanupDependees() // Remove dependees we already know we won't need

        val dependees = state.dependees
        if (dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.definedMethod, state.ubPurity)
        } else {
            IntermediateResult(state.definedMethod, state.lbPurity, state.ubPurity, dependees, c)
        }
    }

    /**
     * Determines the purity of the given method.
     *
     * @param definedMethod A defined method with a body.
     */
    def determinePurity(definedMethod: DefinedMethod): PropertyComputationResult = {
        val method = definedMethod.definedMethod
        val declClass = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declClass ne definedMethod.declaringClassType)
            return baseMethodPurity(definedMethod.asDefinedMethod);

        implicit val state: State =
            new State(method, definedMethod, declClass)

        val tacaiO = getTACAI(method)

        if (tacaiO.isEmpty)
            return IntermediateResult(
                definedMethod,
                ImpureByAnalysis,
                CompileTimePure,
                state.dependees,
                c
            );

        determineMethodPurity(tacaiO.get.cfg)
    }
}

object L2PurityAnalysis {

    /**
     * Domain-specific rater used to examine whether certain statements and expressions are
     * domain-specific.
     * If the Option is None, a rater is created from a config file option.
     */
    var rater: Option[DomainSpecificRater] = None

    def setRater(newRater: Option[DomainSpecificRater]): Unit = {
        rater = newRater
    }
}

trait L2PurityAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(Purity)

    final override def uses: Set[PropertyKind] = {
        Set(
            TACAI,
            FieldMutability,
            ClassImmutability,
            TypeImmutability,
            FieldLocality,
            ReturnValueFreshness,
            Callees
        )
    }

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerL2PurityAnalysis extends L2PurityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L2PurityAnalysis(p)
        val dms = p.get(DeclaredMethodsKey).declaredMethods
        val methodsWithBody = dms.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined ⇒ dm.asDefinedMethod
        }
        ps.scheduleEagerComputationsForEntities(methodsWithBody.filterNot(analysis.configuredPurity.wasSet))(
            analysis.determinePurity
        )
        analysis
    }
}

object LazyL2PurityAnalysis extends L2PurityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L2PurityAnalysis(p)
        ps.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }
}
