/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import scala.annotation.switch

import scala.collection.immutable.IntMap

import net.ceedubs.ficus.Ficus._

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.LUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.ASObjectValue
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.ClassifiedImpure
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.ContextuallyPure
import org.opalj.br.fpcf.properties.ExtensibleGetter
import org.opalj.br.fpcf.properties.ExtensibleLocalField
import org.opalj.br.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FreshReturnValue
import org.opalj.br.fpcf.properties.Getter
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.LocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.NoLocalField
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.StaticDataUsage
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.fpcf.properties.UsesConstantDataOnly
import org.opalj.br.fpcf.properties.UsesNoStaticData
import org.opalj.br.fpcf.properties.UsesVaryingData
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ConfiguredPurityKey
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.MethodDescriptor
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.ai.isImmediateVMException
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.cg.NoCallers

/**
 * An inter-procedural analysis to determine a method's purity.
 *
 * @note This analysis is sound only up to the usual standards, i.e. it does not cope with
 *       VirtualMachineErrors, LinkageErrors and ReflectiveOperationExceptions and may be unsound in
 *       the presence of native code, reflection or `sun.misc.Unsafe`. Calls to native methods are
 *       handled soundly in general as they are considered [[org.opalj.br.fpcf.properties.ImpureByAnalysis]],
 *       but native methods may break soundness of this analysis by invalidating assumptions such as
 *       which fields are effectively final.
 * @note This analysis is sound even if the three address code hierarchy is not flat, it will
 *       produce better results for a flat hierarchy, though. This is because it will not assess the
 *       types of expressions other than [[org.opalj.tac.Var]]s.
 * @note This analysis derives all purity level.
 *       A configurable [[org.opalj.tac.fpcf.analyses.purity.DomainSpecificRater]] is used to
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
     * @param context The corresponding Context we report results for
     * @param declClass The declaring class of the currently analyzed method
     * @param code The code of the currently analyzed method
     */
    class State(
            val method:    Method,
            val context:   Context,
            val declClass: ObjectType,
            var tac:       TACode[TACMethodParameter, V] = null,
            var lbPurity:  Purity                        = CompileTimePure,
            var ubPurity:  Purity                        = CompileTimePure
    ) extends AnalysisState {
        var fieldLocalityDependees: Map[Field, (EOptionP[Field, FieldLocality], Set[(Expr[V], Purity)])] = Map.empty

        var fieldMutabilityDependees: Map[Field, (EOptionP[Field, FieldMutability], Set[Option[Expr[V]]])] = Map.empty

        var classImmutabilityDependees: Map[ObjectType, (EOptionP[ObjectType, ClassImmutability], Set[Expr[V]])] = Map.empty
        var typeImmutabilityDependees: Map[ObjectType, (EOptionP[ObjectType, TypeImmutability], Set[Expr[V]])] = Map.empty

        var purityDependees: Map[Context, (EOptionP[Context, Purity], Set[Seq[Expr[V]]])] = Map.empty

        var calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None
        var callees: Option[Callees] = None

        var rvfDependees: Map[Context, (EOptionP[Context, ReturnValueFreshness], Set[(Option[Expr[V]], Purity)])] = Map.empty
        var rvfCallSites: IntMap[(Option[Expr[V]], Purity)] = IntMap.empty

        var staticDataUsage: Option[EOptionP[DeclaredMethod, StaticDataUsage]] = None

        var tacai: Option[EOptionP[Method, TACAI]] = None

        def dependees: Set[SomeEOptionP] =
            (fieldLocalityDependees.valuesIterator.map(_._1) ++
                fieldMutabilityDependees.valuesIterator.map(_._1) ++
                classImmutabilityDependees.valuesIterator.map(_._1) ++
                typeImmutabilityDependees.valuesIterator.map(_._1) ++
                purityDependees.valuesIterator.map(_._1) ++
                calleesDependee ++
                rvfDependees.valuesIterator.map(_._1) ++
                staticDataUsage ++
                tacai).toSet

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
            context: Context,
            eop:     EOptionP[Context, Purity],
            params:  Seq[Expr[V]]
        ): Unit = {
            if (purityDependees.contains(context)) {
                val (_, oldParams) = purityDependees(context)
                purityDependees += ((context, (eop, oldParams + params)))
            } else {
                purityDependees += ((context, (eop, Set(params))))
            }
        }

        def updateCalleesDependee(eps: EOptionP[DeclaredMethod, Callees]): Unit = {
            if (eps.isFinal) calleesDependee = None
            else calleesDependee = Some(eps)
            if (eps.hasUBP)
                callees = Some(eps.ub)
        }

        def addRVFDependee(
            context: Context,
            eop:     EOptionP[Context, ReturnValueFreshness],
            data:    (Option[Expr[V]], Purity)
        ): Unit = {
            if (rvfDependees.contains(context)) {
                val (_, oldValues) = rvfDependees(context)
                rvfDependees += ((context, (eop, oldValues + data)))
            } else {
                rvfDependees += ((context, (eop, Set(data))))
            }
        }

        def removeFieldLocalityDependee(f: Field): Unit = fieldLocalityDependees -= f
        def removeFieldMutabilityDependee(f: Field): Unit = fieldMutabilityDependees -= f
        def removeClassImmutabilityDependee(t: ObjectType): Unit = classImmutabilityDependees -= t
        def removeTypeImmutabilityDependee(t: ObjectType): Unit = typeImmutabilityDependees -= t
        def removePurityDependee(context: Context): Unit = purityDependees -= context
        def removeRVFDependee(context: Context): Unit = rvfDependees -= context

        def updateStaticDataUsage(eps: Option[EOptionP[DeclaredMethod, StaticDataUsage]]): Unit = {
            staticDataUsage = eps
        }

        def updateTacai(eps: EOptionP[Method, TACAI]): Unit = {
            if (eps.isFinal) tacai = None
            else tacai = Some(eps)
            if (eps.hasUBP && eps.ub.tac.isDefined) {
                tac = eps.ub.tac.get
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
            _ => CompileTimePure,
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
        onParameter:        Int => Purity,
        treatParamsAsFresh: Boolean,
        excludedDefSites:   IntTrieSet    = EmptyIntTrieSet
    )(implicit state: State): Boolean = {
        if (expr eq null) {
            // Expression is unknown due to an indirect call (e.g. reflection)
            atMost(otherwise)
            return false;
        }

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
        onParameter:        Int => Purity,
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

        val stmt = state.tac.stmts(defSite)
        assert(stmt.astID == Assignment.ASTID, "defSite should be assignment")

        val rhs = stmt.asAssignment.expr
        if (rhs.isConst)
            return true;

        (rhs.astID: @switch) match {
            case New.ASTID | NewArray.ASTID => true
            case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                VirtualFunctionCall.ASTID =>
                val oldPurityLevel =
                    state.rvfCallSites.get(stmt.pc).map(_._2).getOrElse(CompileTimePure)
                val data = (rhs.asFunctionCall.receiverOption, otherwise meet oldPurityLevel)
                if (state.callees.isDefined) {
                    checkFreshnessOfReturn(stmt.pc, data, state.callees.get)
                } else {
                    state.rvfCallSites += stmt.pc -> data
                    reducePurityLB(otherwise)
                }
                true
            case GetField.ASTID =>
                val GetField(_, declClass, name, fieldType, objRef) = rhs
                project.resolveFieldReference(declClass, name, fieldType) match {
                    case Some(field) =>
                        val locality = propertyStore(field, FieldLocality.key)
                        checkLocalityOfField(locality, (objRef, otherwise)) &&
                            isLocalInternal(
                                objRef,
                                otherwise,
                                onParameter,
                                treatParamsAsFresh,
                                excludedDefSites ++ defSites
                            )
                    case None => false
                }
            case _ => false
        }
    }

    def checkLocalityOfField(
        ep:   EOptionP[Field, FieldLocality],
        data: (Expr[V], Purity)
    )(implicit state: State): Boolean = {
        val isLocal = ep match {
            case FinalP(LocalField | LocalFieldWithGetter) =>
                true
            case FinalP(ExtensibleLocalField | ExtensibleLocalFieldWithGetter) =>
                if (data._1.isVar) {
                    val value = data._1.asVar.value.asReferenceValue
                    value.isPrecise &&
                        !classHierarchy.isSubtypeOf(value.asReferenceType, ObjectType.Cloneable)
                } else
                    false
            case UBP(NoLocalField) =>
                false
            case _ =>
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
        ep:   EOptionP[Context, Property],
        data: (Option[Expr[V]], Purity)
    )(implicit state: State): Unit = {
        import project.classHierarchy.isSubtypeOf
        ep match {
            case LBP(PrimitiveReturnValue | FreshReturnValue) =>
            case FinalP(Getter) =>
                if (data._2 meet state.ubPurity ne state.ubPurity)
                    isLocal(data._1.get, data._2)
            case FinalP(ExtensibleGetter) =>
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
            case UBP(NoFreshReturnValue) =>
                atMost(data._2)
            case _: SomeEOptionP =>
                reducePurityLB(data._2)
                if (data._2 meet state.ubPurity ne state.ubPurity) {
                    state.addRVFDependee(
                        ep.e,
                        ep.asInstanceOf[EOptionP[Context, ReturnValueFreshness]],
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
        callees.callees(state.context, pc).foreach { callee =>
            if (callee.method.descriptor.returnType.isReferenceType)
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
            case MonitorEnter.ASTID | MonitorExit.ASTID =>
                val objRef = stmt.asSynchronizationStmt.objRef
                isLocalInternal(
                    objRef,
                    ImpureByAnalysis,
                    param => ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)

            // Storing into non-escaping locally initialized arrays/objects is pure
            case ArrayStore.ASTID =>
                val arrayRef = stmt.asArrayStore.arrayRef
                isLocalInternal(
                    arrayRef,
                    ImpureByAnalysis,
                    param => ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)
            case PutField.ASTID =>
                val objRef = stmt.asPutField.objRef
                isLocalInternal(
                    objRef,
                    ImpureByAnalysis,
                    param => ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ) && stmt.forallSubExpressions(checkPurityOfExpr)

            case _ => super.checkPurityOfStmt(stmt)
        }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendees when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[Context, Purity],
        params: Seq[Expr[V]]
    )(implicit state: State): Boolean = ep match {
        case UBP(_: ClassifiedImpure) =>
            atMost(ImpureByAnalysis)
            false
        case eps @ LUBP(lb, ub) =>
            if (eps.isRefinable && ((lb meet state.ubPurity) ne state.ubPurity)) {
                // On conditional, keep dependence
                state.addPurityDependee(ep.e, ep, params)
                reducePurityLB(lb)
            }
            // Contextual/external purity is handled below
            atMost(ub.withoutContextual)
            ub.modifiedParams.forall(param =>
                isLocalInternal(
                    params(param),
                    ImpureByAnalysis,
                    param => ContextuallyPure(IntTrieSet(param)),
                    treatParamsAsFresh = true
                ))
        case _: SomeEOptionP =>
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
            case LBP(UsesNoStaticData | UsesConstantDataOnly) =>
                state.updateStaticDataUsage(None)
            case UBP(UsesVaryingData) =>
                state.updateStaticDataUsage(None)
                atMost(Pure)
            case _ =>
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
        for ((dependee, (eop, data)) <- state.fieldLocalityDependees) {
            val newData = data.filter(_._2 meet state.ubPurity ne state.ubPurity)
            if (newData.nonEmpty) newFieldLocalityDependees += ((dependee, (eop, newData)))
        }
        state.fieldLocalityDependees = newFieldLocalityDependees

        var newRVFCallsites: IntMap[(Option[Expr[V]], Purity)] = IntMap.empty
        for ((callsite, data) <- state.rvfCallSites) {
            if (data._2 meet state.ubPurity ne state.ubPurity) newRVFCallsites += ((callsite, data))
        }
        state.rvfCallSites = newRVFCallsites

        var newRVFDependees: Map[Context, (EOptionP[Context, ReturnValueFreshness], Set[(Option[Expr[V]], Purity)])] = Map.empty
        for ((dependee, (eop, data)) <- state.rvfDependees) {
            val newData = data.filter(_._2 meet state.ubPurity ne state.ubPurity)
            if (newData.nonEmpty) newRVFDependees += ((dependee, (eop, newData)))
        }
        state.rvfDependees = newRVFDependees

        var newPurityDependees: Map[Context, (EOptionP[Context, Purity], Set[Seq[Expr[V]]])] = Map.empty
        for ((dependee, eAndD) <- state.purityDependees) {
            if (eAndD._1.isEPK || (eAndD._1.lb meet state.ubPurity ne state.ubPurity))
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

        for ((eop, _) <- state.purityDependees.valuesIterator) {
            eop match {
                case LBP(lb) => newLowerBound = newLowerBound meet lb
                case _ =>
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
            (_, data) <- state.fieldLocalityDependees.valuesIterator
            (_, purity) <- data
        } {
            newLowerBound = newLowerBound meet purity
        }

        for {
            (_, purity) <- state.rvfCallSites.valuesIterator
        } {
            newLowerBound = newLowerBound meet purity
        }

        for {
            (_, data) <- state.rvfDependees.valuesIterator
            (_, purity) <- data
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
    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        val oldPurity = state.ubPurity
        eps.ub.key match {
            case Purity.key =>
                val e = eps.e.asInstanceOf[Context]
                val dependees = state.purityDependees(e)
                state.removePurityDependee(e)
                dependees._2.foreach { e =>
                    checkMethodPurity(eps.asInstanceOf[EOptionP[Context, Purity]], e)
                }
            case FieldMutability.key =>
                val e = eps.e.asInstanceOf[Field]
                val dependees = state.fieldMutabilityDependees(e)
                state.removeFieldMutabilityDependee(e)
                dependees._2.foreach { e =>
                    checkFieldMutability(eps.asInstanceOf[EOptionP[Field, FieldMutability]], e)
                }
            case ClassImmutability.key =>
                val e = eps.e.asInstanceOf[ObjectType]
                val dependees = state.classImmutabilityDependees(e)
                state.removeClassImmutabilityDependee(e)
                dependees._2.foreach { e =>
                    checkTypeMutability(
                        eps.asInstanceOf[EOptionP[ObjectType, ClassImmutability]],
                        e
                    )
                }
            case TypeImmutability.key =>
                val e = eps.e.asInstanceOf[ObjectType]
                val dependees = state.typeImmutabilityDependees(e)
                state.removeTypeImmutabilityDependee(e)
                dependees._2.foreach { e =>
                    checkTypeMutability(eps.asInstanceOf[EOptionP[ObjectType, TypeImmutability]], e)
                }
            case ReturnValueFreshness.key =>
                val e = eps.e.asInstanceOf[Context]
                val dependees = state.rvfDependees(e)
                state.removeRVFDependee(e)
                dependees._2.foreach { e =>
                    checkLocalityOfReturn(
                        eps.asInstanceOf[EOptionP[Context, ReturnValueFreshness]],
                        e
                    )
                }
            case FieldLocality.key =>
                val e = eps.e.asInstanceOf[Field]
                val dependees = state.fieldLocalityDependees(e)
                state.removeFieldLocalityDependee(e)
                dependees._2.foreach { e =>
                    checkLocalityOfField(eps.asInstanceOf[EOptionP[Field, FieldLocality]], e)
                }
            case Callees.key =>
                checkPurityOfCallees(eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]])
                state.rvfCallSites.foreach {
                    case (pc, data) => checkFreshnessOfReturn(pc, data, eps.ub.asInstanceOf[Callees])
                }
            case StaticDataUsage.key =>
                checkStaticDataUsage(eps.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]])
            case TACAI.key =>
                state.updateTacai(eps.asInstanceOf[EOptionP[Method, TACAI]])
                return determineMethodPurity(eps.ub.asInstanceOf[TACAI].tac.get.cfg);
        }

        if (state.ubPurity eq ImpureByAnalysis)
            return Result(state.context, ImpureByAnalysis);

        if (state.ubPurity ne oldPurity)
            cleanupDependees() // Remove dependees that we don't need anymore.
        adjustLowerBound()

        val dependees = state.dependees
        if (dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.context, state.ubPurity)
        } else {
            InterimResult(
                state.context,
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
    )(implicit state: State): ProperPropertyComputationResult = {
        // Special case: The Throwable constructor is `LBSideEffectFree`, but subtype constructors
        // may not be because of overridable fillInStackTrace method
        if (state.method.isConstructor && state.declClass.isSubtypeOf(ObjectType.Throwable)) {
            val candidate = org.opalj.control.find(project.instanceMethods(state.declClass)) { mdc =>
                mdc.method.compare(
                    "fillInStackTrace",
                    MethodDescriptor.withNoArgs(ObjectType.Throwable)
                )
            }
            candidate foreach { mdc =>
                if (mdc.method.classFile.thisType != ObjectType.Throwable) {
                    val fISTMethod = declaredMethods(mdc.method)
                    val fISTContext = typeProvider.expandContext(state.context, fISTMethod, 0)
                    val fISTPurity = propertyStore(fISTContext, Purity.key)
                    val self = UVar(
                        ASObjectValue(isNull = No, isPrecise = false, state.declClass),
                        SelfReferenceParameter
                    )
                    if (!checkMethodPurity(fISTPurity, Seq(self)))
                        // Early return for impure fillInStackTrace
                        return Result(state.context, state.ubPurity);
                }
            }
        }

        // Synchronized methods have a visible side effect on the receiver
        // Static synchronized methods lock the class which is potentially globally visible
        if (state.method.isSynchronized)
            if (state.method.isStatic) return Result(state.context, ImpureByAnalysis);
            else atMost(ContextuallyPure(IntTrieSet(0)))

        val stmtCount = state.tac.stmts.length
        var s = 0
        while (s < stmtCount) {
            if (!checkPurityOfStmt(state.tac.stmts(s))) { // Early return for impure statements
                assert(state.ubPurity.isInstanceOf[ClassifiedImpure])
                return Result(state.context, state.ubPurity);
            }
            s += 1
        }

        val callees = propertyStore(state.context.method, Callees.key)
        if (!checkPurityOfCallees(callees)) {
            assert(state.ubPurity.isInstanceOf[ClassifiedImpure])
            return Result(state.context, state.ubPurity)
        }

        if (callees.hasUBP)
            state.rvfCallSites.foreach {
                case (pc, data) =>
                    checkFreshnessOfReturn(pc, data, callees.ub)
            }

        // Creating implicit exceptions is side-effect free (because of fillInStackTrace)
        // but it may be ignored as domain-specific
        val bbsCausingExceptions = cfg.abnormalReturnNode.predecessors
        for {
            bb <- bbsCausingExceptions
        } {
            val pc = bb.asBasicBlock.endPC
            if (isSourceOfImmediateException(pc)) {
                val throwingStmt = state.tac.stmts(pc)
                val ratedResult = rater.handleException(throwingStmt)
                if (ratedResult.isDefined) atMost(ratedResult.get)
                else atMost(SideEffectFree)
            }
        }

        if (state.ubPurity eq CompileTimePure) // Check static data usage only if necessary
            checkStaticDataUsage(propertyStore(state.context.method, StaticDataUsage.key))
        else
            cleanupDependees() // Remove dependees we already know we won't need

        val dependees = state.dependees
        if (dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.context, state.ubPurity)
        } else {
            InterimResult(state.context, state.lbPurity, state.ubPurity, dependees, c)
        }
    }

    /**
     * Determines the purity of the given method.
     *
     * @param context A method call context
     */
    def determinePurity(context: Context): ProperPropertyComputationResult = {
        val definedMethod = context.method
        val method = definedMethod.definedMethod
        val declClass = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if ((declClass ne definedMethod.declaringClassType) && context.isInstanceOf[SimpleContext])
            return baseMethodPurity(context);

        implicit val state: State =
            new State(method, context, declClass)

        val tacaiO = getTACAI(method)

        if (tacaiO.isEmpty)
            return InterimResult(
                context,
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

trait L2PurityAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(Purity)

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, SimpleContextsKey, ConfiguredPurityKey)

    override def uses: Set[PropertyBounds] = {
        Set(
            PropertyBounds.lub(FieldMutability),
            PropertyBounds.lub(ClassImmutability),
            PropertyBounds.lub(TypeImmutability),
            PropertyBounds.lub(StaticDataUsage),
            PropertyBounds.lub(ReturnValueFreshness),
            PropertyBounds.ub(FieldLocality),
            PropertyBounds.ub(TACAI),
            PropertyBounds.ub(Callees),
            PropertyBounds.lub(Purity)
        )
    }

    final override type InitializationData = L2PurityAnalysis
    final override def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        new L2PurityAnalysis(p)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

object EagerL2PurityAnalysis extends L2PurityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation :+ CallGraphKey

    override def start(
        p: SomeProject, ps: PropertyStore, analysis: InitializationData
    ): FPCFAnalysis = {
        val cg = p.get(CallGraphKey)
        val methods = cg.reachableMethods().collect {
            case c @ Context(dm) if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined && !analysis.configuredPurity.wasSet(dm) && ps(dm, Callers.key).ub != NoCallers =>
                c
        }

        ps.scheduleEagerComputationsForEntities(methods)(
            analysis.determinePurity
        )

        analysis
    }

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.finalP(Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazyL2PurityAnalysis extends L2PurityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(
        p: SomeProject, ps: PropertyStore, analysis: InitializationData
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
