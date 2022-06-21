/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import net.ceedubs.ficus.Ficus._

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.fpcf.UBP
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.ClassifiedImpure
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FinalField
import org.opalj.br.fpcf.properties.ImmutableObject
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.fpcf.properties.VirtualMethodPurity
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
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
 *       VirtualMachineErrors and may be unsound in the presence of native code, reflection or
 *       `sun.misc.Unsafe`. Calls to native methods are generally handled soundly as they are
 *       considered [[org.opalj.br.fpcf.properties.ImpureByAnalysis]]. There are no soundness
 *       guarantees in the presence of load-time transformation. Soundness in general depends
 *       on the soundness of the analyses that compute properties used by this analysis, e.g.
 *       field mutability.
 * @note This analysis is sound even if the three address code hierarchy is not flat, it will
 *       produce better results for a flat hierarchy, though. This is because it will not assess the
 *       types of expressions other than [[org.opalj.tac.Var]]s.
 * @note This analysis derives all purity levels except for the `Externally` variants. A
 *       configurable [[DomainSpecificRater]] is used to identify calls, expressions and exceptions
 *       that are `LBDPure` instead of `LBImpure` or any `SideEffectFree` purity level.
 *       Compared to the `L0PurityAnalysis`, it deals with all methods, even if their reference type
 *       parameters are mutable. It can handle accesses of (effectively) final instance fields,
 *       array loads, array length and virtual/interface calls. Array stores and field writes as
 *       well as (useless) synchronization on locally created, non-escaping objects/arrays are also
 *       handled. Newly allocated objects/arrays returned from callees are not identified.
 * @author Dominik Helm
 */
class L1PurityAnalysis private[analyses] (val project: SomeProject) extends AbstractPurityAnalysis {

    /**
     * Holds the state of this analysis.
     * @param dependees The set of entities/properties the purity depends on
     * @param method The currently analyzed method
     * @param context The corresponding Context we report results for
     * @param declClass The declaring class of the currently analyzed method
     * @param tac The method's three address code
     * @param lbPurity The current minimum purity level for the method
     * @param ubPurity The current maximum purity level for the method that will be assigned by
     *                  checkPurityOfX methods to aggregrate the purity
     */
    class State(
            var dependees: Set[EOptionP[Entity, Property]],
            val method:    Method,
            val context:   Context,
            val declClass: ObjectType,
            var tac:       TACode[TACMethodParameter, V]   = null,
            var lbPurity:  Purity                          = Pure,
            var ubPurity:  Purity                          = Pure
    ) extends AnalysisState

    override type StateType = State

    val raterFqn: String = project.config.as[String](
        "org.opalj.fpcf.analyses.L1PurityAnalysis.domainSpecificRater"
    )

    val rater: DomainSpecificRater =
        L1PurityAnalysis.rater.getOrElse(resolveDomainSpecificRater(raterFqn))

    /**
     * Checks if a reference was created locally, hence actions on it might not influence purity.
     *
     * @note Fresh references can be treated as non-escaping as the analysis result will be impure
     *       if anything escapes the method via parameters, static field assignments or calls.
     */
    override def isLocal(expr: Expr[V], otherwise: Purity, excludedDefSites: IntTrieSet = EmptyIntTrieSet)(implicit state: State): Boolean = {
        if (expr.isConst)
            true
        else if (expr.asVar.value.computationalType ne ComputationalTypeReference) {
            // Primitive values are always local (required for parameters of contextually pure calls)
            true
        } else if (expr.isVar) {
            val defSites = expr.asVar.definedBy -- excludedDefSites
            if (defSites.forall { defSite =>
                if (defSite >= 0) {
                    val rhs = state.tac.stmts(defSite).asAssignment.expr
                    if (rhs.isConst)
                        true
                    else {
                        val astID = rhs.astID
                        astID match {
                            case New.ASTID | NewArray.ASTID => true
                            case GetField.ASTID =>
                                val objRef = rhs.asGetField.objRef
                                isLocal(objRef, otherwise, excludedDefSites ++ defSites)
                            case ArrayLoad.ASTID =>
                                val arrayRef = rhs.asArrayLoad.arrayRef
                                isLocal(arrayRef, otherwise, excludedDefSites ++ defSites)
                            case _ => false
                        }
                    }
                } else if (isImmediateVMException(defSite)) {
                    true // immediate VM exceptions are freshly created
                } else {
                    // In initializers the self reference (this) is local
                    state.method.isConstructor && defSite == OriginOfThis
                }
            }) {
                true
            } else {
                atMost(otherwise)
                false
            }
        } else {
            // The expression could refer to further expressions in a non-flat representation.
            // In that case it could be, e.g., a GetStatic. In that case the reference is
            // not locally created and/or initialized. To avoid special handling, we just
            // fallback to false here as the analysis is intended to be used on flat
            // representations anyway.
            atMost(otherwise)
            false
        }
    }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendies when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[Context, Purity],
        params: Seq[Expr[V]]
    )(implicit state: State): Boolean = ep match {
        case UBP(_: ClassifiedImpure) =>
            atMost(ImpureByAnalysis)
            false
        case eps @ LUBP(lb, ub) =>
            if (ub.modifiesParameters) {
                atMost(ImpureByAnalysis)
                false
            } else {
                if (eps.isRefinable && ((lb meet state.ubPurity) ne state.ubPurity)) {
                    state.dependees += ep // On Conditional, keep dependence
                    reducePurityLB(lb)
                }
                atMost(ub)
                true
            }
        case _ =>
            state.dependees += ep
            reducePurityLB(ImpureByAnalysis)
            true
    }

    /**
     * If the given objRef is not local, adds the dependee necessary if the field mutability is not
     * known yet.
     */
    override def handleUnknownFieldMutability(
        ep:     EOptionP[Field, FieldMutability],
        objRef: Option[Expr[V]]
    )(implicit state: State): Unit = {
        if (objRef.isEmpty || !isLocal(objRef.get, Pure)) state.dependees += ep
    }

    /**
     * If the given expression is not local, adds the dependee necessary if the type mutability is
     * not known yet.
     */
    override def handleUnknownTypeMutability(
        ep:   EOptionP[ObjectType, Property],
        expr: Expr[V]
    )(implicit state: State): Unit = {
        if (!isLocal(expr, Pure)) state.dependees += ep
    }

    /**
     * If the callees property is not yet final, adds the necessary dependee.
     */
    override def handleCalleesUpdate(
        callees: EOptionP[DeclaredMethod, Callees]
    )(implicit state: StateType): Unit = {
        if (!callees.isFinal) state.dependees += callees
    }

    /**
     * Handles what to do if the TACAI is not yet final.
     */
    override def handleTACAI(ep: EOptionP[Method, TACAI])(implicit state: State): Unit = {
        if (ep.isRefinable)
            state.dependees += ep
        if (ep.hasUBP && ep.ub.tac.isDefined) {
            state.tac = ep.ub.tac.get
        }
    }

    def cleanupDependees()(implicit state: State): Unit = {
        // Remove unnecessary dependees
        if (!state.ubPurity.isDeterministic) {
            state.dependees = state.dependees.filter { ep =>
                ep.pk == Purity.key || ep.pk == VirtualMethodPurity.key || ep.pk == Callees.key ||
                    ep.pk == TACAI.key
            }
        }
        //IMPROVE: We could filter Purity/VPurity dependees with an lb not less than maxPurity
    }

    /**
     * Continuation to handle updates to properties of dependees.
     * Dependees may be
     *     - methods called (for their purity)
     *     - fields read (for their mutability)
     *     - classes files for class types returned (for their mutability)
     */
    def continuation(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        state.dependees = state.dependees.filter(_.e ne eps.e)
        val oldPurity = state.ubPurity

        (eps: @unchecked) match {
            case UBP(_: Callees) =>
                if (!checkPurityOfCallees(eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]]))
                    return Result(state.context, ImpureByAnalysis)

            case UBP(tacai: TACAI) =>
                handleTACAI(eps.asInstanceOf[EOptionP[Method, TACAI]])
                return determineMethodPurity(tacai.tac.get.cfg);

            // Cases dealing with other purity values
            case UBP(_: Purity) =>
                if (!checkMethodPurity(eps.asInstanceOf[EOptionP[Context, Purity]]))
                    return Result(state.context, ImpureByAnalysis)

            // Cases that are pure
            case FinalP(_: FinalField)                   => // Reading eff. final fields
            case FinalP(ImmutableType | ImmutableObject) => // Returning immutable reference

            // Cases resulting in side-effect freeness
            case FinalP(_: FieldMutability | // Reading non-final field
                _: TypeImmutability | _: ClassImmutability) => // Returning mutable reference
                atMost(SideEffectFree)

            case _: SomeInterimEP => state.dependees += eps
        }

        if (state.ubPurity ne oldPurity)
            cleanupDependees()

        if (state.dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.context, state.ubPurity)
        } else {
            InterimResult(
                state.context,
                state.lbPurity,
                state.ubPurity,
                state.dependees,
                continuation
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
                    if (!checkMethodPurity(fISTPurity, Seq.empty))
                        // Early return for impure fillInStackTrace
                        return Result(state.context, state.ubPurity);
                }
            }
        }

        val stmtCount = state.tac.stmts.length
        var s = 0
        while (s < stmtCount) {
            if (!checkPurityOfStmt(state.tac.stmts(s))) // Early return for impure statements
                return Result(state.context, state.ubPurity)
            s += 1
        }

        val callees = propertyStore(state.context.method, Callees.key)
        if (!checkPurityOfCallees(callees))
            return Result(state.context, state.ubPurity)

        // Creating implicit exceptions is side-effect free (because of fillInStackTrace)
        // but it may be ignored as domain-specific
        val bbsCausingExceptions = cfg.abnormalReturnNode.predecessors
        for {
            bb <- bbsCausingExceptions
            pc = bb.asBasicBlock.endPC
            if isSourceOfImmediateException(pc)
        } {
            val throwingStmt = state.tac.stmts(pc)
            val ratedResult = rater.handleException(throwingStmt)
            if (ratedResult.isDefined) atMost(ratedResult.get)
            else atMost(SideEffectFree)
        }

        // Remove unnecessary dependees
        if (state.ubPurity ne Pure) {
            cleanupDependees()
        }

        if (state.dependees.isEmpty || (state.lbPurity == state.ubPurity)) {
            Result(state.context, state.ubPurity)
        } else {
            InterimResult(
                state.context,
                state.lbPurity,
                state.ubPurity,
                state.dependees,
                continuation
            )
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

        // We treat all synchronized methods as impure
        if (method.isSynchronized)
            return Result(context, ImpureByAnalysis);

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if ((declClass ne definedMethod.declaringClassType) && context.isInstanceOf[SimpleContext])
            return baseMethodPurity(context);

        implicit val state: State =
            new State(Set.empty, method, context, declClass)

        val tacaiO = getTACAI(method)

        if (tacaiO.isEmpty)
            return InterimResult(
                context,
                ImpureByAnalysis,
                Pure,
                state.dependees,
                continuation
            );

        determineMethodPurity(tacaiO.get.cfg)
    }
}

object L1PurityAnalysis {
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

trait L1PurityAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(Purity)

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, SimpleContextsKey, ConfiguredPurityKey)

    override def uses: Set[PropertyBounds] = {
        Set(
            PropertyBounds.ub(TACAI),
            PropertyBounds.ub(Callees),
            PropertyBounds.lub(FieldMutability),
            PropertyBounds.lub(ClassImmutability),
            PropertyBounds.lub(TypeImmutability),
            PropertyBounds.lub(Purity)
        )
    }

    final override type InitializationData = L1PurityAnalysis
    final def init(p: SomeProject, ps: PropertyStore): InitializationData = new L1PurityAnalysis(p)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

object EagerL1PurityAnalysis extends L1PurityAnalysisScheduler with FPCFEagerAnalysisScheduler {

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

object LazyL1PurityAnalysis extends L1PurityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(
        p: SomeProject, ps: PropertyStore, analysis: InitializationData
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
