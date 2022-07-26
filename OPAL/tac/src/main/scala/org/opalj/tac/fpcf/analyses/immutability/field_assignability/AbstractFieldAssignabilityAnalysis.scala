/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package field_assignability

import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.fpcf.EOptionP
import org.opalj.tac.DUVar
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.SomeInterimEP
import org.opalj.tac.Stmt
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.br.fpcf.properties.Purity
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.fpcf.LBP
import org.opalj.fpcf.UBP
import org.opalj.br.ObjectType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.br.PC
import org.opalj.fpcf.SomeEOptionP

trait AbstractFieldAssignabilityAnalysis extends FPCFAnalysis {

    trait AbstractFieldAssignabilityAnalysisState {

        val field: Field
        var tacDependees: Map[Method, EOptionP[Method, TACAI]] = Map.empty
        var fieldAssignability: FieldAssignability = NonAssignable
        var callerDependees: Map[DeclaredMethod, EOptionP[DeclaredMethod, Callers]] = Map.empty
        var tacPCs: Map[Method, PCs] = Map.empty
        var escapeDependees: Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty
        var fieldAssignabilityDependees: Set[EOptionP[Field, FieldAssignability]] = Set.empty
        var calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None
        var lazyInitInvocation: Option[(DeclaredMethod, PC)] = None
        var purityDependees: Set[EOptionP[Context, Purity]] = Set.empty

        def hasDependees: Boolean = {
            escapeDependees.nonEmpty || tacDependees.valuesIterator.exists(_.isRefinable) ||
              callerDependees.valuesIterator.exists(_.isRefinable) ||
              fieldAssignabilityDependees.nonEmpty || purityDependees.nonEmpty
        }

        def dependees: Set[SomeEOptionP] = {
            (escapeDependees ++ callerDependees.valuesIterator.filter(_.isRefinable) ++
              purityDependees ++ fieldAssignabilityDependees ++
              tacDependees.valuesIterator.filter(_.isRefinable)).toSet
        }
    }
    /*
    trait AbstractFieldAssignabilityAnalysisState {

        val field: Field
        var tacDependees: Map[Method, EOptionP[Method, TACAI]] = Map.empty
        var fieldAssignability: FieldAssignability = NonAssignable
        var callerDependees: Map[DeclaredMethod, EOptionP[DeclaredMethod, Callers]] = Map.empty
        var tacPCs: Map[Method, PCs] = Map.empty
        var escapeDependees: Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty
        var fieldAssignabilityDependees: Set[EOptionP[Field, FieldAssignability]] = Set.empty
        var calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None
        var lazyInitInvocation: Option[(DeclaredMethod, PC)] = None
        var purityDependees: Set[EOptionP[Context, Purity]] = Set.empty

        def hasDependees: Boolean = {
            escapeDependees.nonEmpty || tacDependees.valuesIterator.exists(_.isRefinable) ||
                callerDependees.valuesIterator.exists(_.isRefinable) ||
                fieldAssignabilityDependees.nonEmpty || purityDependees.nonEmpty
        }

        def dependees: Set[SomeEOptionP] = {
            (escapeDependees ++ callerDependees.valuesIterator.filter(_.isRefinable) ++
                purityDependees ++ fieldAssignabilityDependees ++
                tacDependees.valuesIterator.filter(_.isRefinable)).toSet
        }
    } */

    type V = DUVar[ValueInformation]

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit final val typeIterator: TypeIterator = project.get(TypeIteratorKey)

    def doDetermineFieldAssignability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field =>
                determineFieldAssignability(field)
            case _ =>
                val m = entity.getClass.getSimpleName+" is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    type AnalysisState <: AbstractFieldAssignabilityAnalysisState

    def createState(field: Field): AnalysisState

    /**
     * Analyzes the field's assignability.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods and reflections.
     * Fields can be manipulated by any given native method.
     * Because the analysis cannot be aware of any given native method,
     * they are not considered as well as reflections.
     */
    private[analyses] def determineFieldAssignability(
        field: Field
    ): ProperPropertyComputationResult = {

        implicit val state = createState(field)

        if (field.isFinal)
            return Result(field, NonAssignable);
        else
            state.fieldAssignability = EffectivelyNonAssignable

        if (field.isPublic)
            return Result(field, Assignable);

        val thisType = field.classFile.thisType

        if (field.isPublic) {
            if (typeExtensibility(ObjectType.Object).isYesOrUnknown) {
                return Result(field, Assignable);
            }
        } else if (field.isProtected) {
            if (typeExtensibility(thisType).isYesOrUnknown) {
                return Result(field, Assignable);
            }
            if (!closedPackages(thisType.packageName)) {
                return Result(field, Assignable);
            }
        }
        if (field.isPackagePrivate) {
            if (!closedPackages(thisType.packageName)) {
                return Result(field, Assignable);
            }
        }
        for {
            (method, pcs) <- fieldAccessInformation.writeAccesses(field)
            (taCode, callers) <- getTACAIAndCallers(method, pcs) //TODO field accesses via this
        } {
            if (methodUpdatesField(method, taCode, callers, pcs))
                return Result(field, Assignable);
        }
        createResult()
    }

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method:  Method,
        taCode:  TACode[TACMethodParameter, V],
        callers: Callers,
        pcs:     PCs
    )(implicit state: AnalysisState): Boolean

    /**
     * Handles the influence of an escape property on the field immutability.
     *
     * @return true if the object - on which a field write occurred - escapes, false otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleEscapeProperty(
        ep: EOptionP[(Context, DefinitionSite), EscapeProperty]
    )(implicit state: AnalysisState): Boolean = ep match {
        case FinalP(NoEscape | EscapeInCallee | EscapeViaReturn) =>
            false

        case FinalP(AtMost(_)) =>
            true

        case _: FinalEP[(Context, DefinitionSite), EscapeProperty] =>
            true // Escape state is worse than via return

        case InterimUBP(NoEscape | EscapeInCallee | EscapeViaReturn) =>
            state.escapeDependees += ep
            false

        case InterimUBP(AtMost(_)) =>
            true

        case _: SomeInterimEP =>
            true // Escape state is worse than via return

        case _ =>
            state.escapeDependees += ep
            false
    }

    /**
     * Checks whether the object reference of a PutField does not escape (except for being returned).
     */
    def referenceHasNotEscaped(
        ref:     V,
        stmts:   Array[Stmt[V]],
        method:  Method,
        callers: Callers
    )(implicit state: AnalysisState): Boolean = {
        val dm = declaredMethods(method)
        ref.definedBy.forall { defSite =>
            if (defSite < 0) false // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) true
                else if (!definition.expr.isNew) false
                else {
                    var hasEscaped = false
                    callers.forNewCalleeContexts(null, dm) { context =>
                        val entity = (context, definitionSites(method, definition.pc))
                        val escapeProperty = propertyStore(entity, EscapeProperty.key)
                        hasEscaped ||= handleEscapeProperty(escapeProperty)
                    }
                    !hasEscaped
                }
            }
        }
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: AnalysisState): ProperPropertyComputationResult = {
        val isNonFinal = eps.pk match {
            case EscapeProperty.key =>
                val newEP = eps.asInstanceOf[EOptionP[(Context, DefinitionSite), EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e != eps.e)
                handleEscapeProperty(newEP)
            case TACAI.key =>
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = newEP.e
                val pcs = state.tacPCs(method)
                state.tacDependees += method -> newEP
                val callersProperty = state.callerDependees(declaredMethods(method))
                if (callersProperty.hasUBP)
                    methodUpdatesField(method, newEP.ub.tac.get, callersProperty.ub, pcs)
                else false
            case Callers.key =>
                val newEP = eps.asInstanceOf[EOptionP[DeclaredMethod, Callers]]
                val method = newEP.e.definedMethod
                val pcs = state.tacPCs(method)
                state.callerDependees += newEP.e -> newEP
                val tacProperty = state.tacDependees(method)
                if (tacProperty.hasUBP && tacProperty.ub.tac.isDefined)
                    methodUpdatesField(method, tacProperty.ub.tac.get, newEP.ub, pcs)
                else false
        }

        if (isNonFinal)
            Result(state.field, Assignable);
        else
            createResult()
    }

    def createResult()(implicit state: AnalysisState): ProperPropertyComputationResult = {

        if (state.hasDependees && (state.fieldAssignability ne Assignable)) {
            InterimResult(
                state.field,
                Assignable,
                state.fieldAssignability,
                state.dependees,
                c
            )
        } else {
            Result(state.field, state.fieldAssignability) //EffectivelyNonAssignable)
        }
    }

    /**
     * Returns TACode and Callers for a method if available, registering dependencies as necessary.
     */
    def getTACAIAndCallers(
        method: Method,
        pcs:    PCs
    )(implicit state: AnalysisState): Option[(TACode[TACMethodParameter, V], Callers)] = {
        val tacEOptP = propertyStore(method, TACAI.key)
        val tac = if (tacEOptP.hasUBP) tacEOptP.ub.tac else None
        state.tacDependees += method -> tacEOptP
        state.tacPCs += method -> pcs

        val declaredMethod: DeclaredMethod = declaredMethods(method)
        val callersEOptP = propertyStore(declaredMethod, Callers.key)
        val callers = if (callersEOptP.hasUBP) Some(callersEOptP.ub) else None
        state.callerDependees += declaredMethod -> callersEOptP

        if (tac.isDefined && callers.isDefined) {
            Some((tac.get, callers.get))
        } else None
    }

    def handleCalls(
        calleesEOP: EOptionP[DeclaredMethod, Callees]
    )(
        implicit
        state: AnalysisState
    ): Boolean = {
        calleesEOP match {
            case FinalP(callees) =>
                state.calleesDependee = None
                handleCallees(callees)
            case InterimUBP(callees: Callees) =>
                state.calleesDependee = Some(calleesEOP)
                handleCallees(callees)
            case _ =>
                state.calleesDependee = Some(calleesEOP)
                false
        }
    }

    def handleCallees(callees: Callees)(implicit state: AnalysisState): Boolean = {
        val pc = state.lazyInitInvocation.get._2
        callees.callerContexts.exists { callerContext =>
            if (callees.isIncompleteCallSite(callerContext, pc)) {
                state.fieldAssignability = Assignable
                true
            } else {
                val targets = callees.callees(callerContext, pc).to(Iterable)
                if (targets.exists(target => isNonDeterministic(propertyStore(target, Purity.key)))) {
                    state.fieldAssignability = Assignable
                    true
                } else false
            }
        }
    }

    /**
     * Checks if the method that defines the value assigned to a (potentially) lazily initialized
     * field is deterministic, ensuring that the same value is written even for concurrent
     * executions.
     */
    def isNonDeterministic(
        eop: EOptionP[Context, Purity]
    )(implicit state: AnalysisState): Boolean = eop match {
        case LBP(p: Purity) if p.isDeterministic  => false
        case UBP(p: Purity) if !p.isDeterministic => true
        case _ =>
            state.purityDependees += eop
            false
    }

    /**
     * Checkes whether the field the value is assigned to a (potentially) lazily initialized field is final,
     * ensuring that the same value is written even for concurrent executions.
     */
    def isNotAssignableField(
        eop: EOptionP[Field, FieldAssignability]
    )(implicit state: AnalysisState): Boolean = eop match {
        case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) => true
        case UBP(Assignable | UnsafelyLazilyInitialized)                       => false
        case _ =>
            state.fieldAssignabilityDependees += eop
            true
    }

    /**
     * Returns the initialization value of a given type.
     */
    def getDefaultValues()(implicit state: AnalysisState): Set[Any] = state.field.fieldType match {
        case FloatType | ObjectType.Float     => Set(0.0f)
        case DoubleType | ObjectType.Double   => Set(0.0d)
        case LongType | ObjectType.Long       => Set(0L)
        case CharType | ObjectType.Character  => Set('\u0000')
        case BooleanType | ObjectType.Boolean => Set(false)
        case IntegerType |
            ObjectType.Integer |
            ByteType |
            ObjectType.Byte |
            ShortType |
            ObjectType.Short => Set(0)
        case ObjectType.String => Set("", null)
        case _: ReferenceType  => Set(null)
    }

}
