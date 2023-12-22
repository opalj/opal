/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.DeclaredField
import org.opalj.br.DefinedMethod
import org.opalj.br.DoubleType
import org.opalj.br.Field
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.fpcf.UBP
import org.opalj.tac.DUVar
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

trait AbstractFieldAssignabilityAnalysis extends FPCFAnalysis {

    trait AbstractFieldAssignabilityAnalysisState {

        val field: Field
        var fieldAssignability: FieldAssignability = NonAssignable
        var fieldAccesses: Map[DefinedMethod, Set[(PC, AccessReceiver)]] = Map.empty
        var escapeDependees: Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty
        var fieldWriteAccessDependee: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] = None
        var tacDependees: Map[DefinedMethod, EOptionP[Method, TACAI]] = Map.empty
        var callerDependees: Map[DefinedMethod, EOptionP[DefinedMethod, Callers]] = Map.empty.withDefault { dm => propertyStore(dm, Callers.key) }

        def hasDependees: Boolean = {
            escapeDependees.nonEmpty || fieldWriteAccessDependee.exists(_.isRefinable) ||
                tacDependees.valuesIterator.exists(_.isRefinable) || callerDependees.valuesIterator.exists(_.isRefinable)
        }

        def dependees: Set[SomeEOptionP] = {
            escapeDependees ++ fieldWriteAccessDependee.filter(_.isRefinable) ++
                callerDependees.valuesIterator.filter(_.isRefinable) ++ tacDependees.valuesIterator.filter(_.isRefinable)
        }
    }

    type V = DUVar[ValueInformation]

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    final val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit final val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def doDetermineFieldAssignability(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case field: Field =>
                determineFieldAssignability(field)
            case _ =>
                val m = entity.getClass.getSimpleName + " is not an org.opalj.br.Field"
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

        implicit val state: AnalysisState = createState(field)

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

        val fwaiEP = propertyStore(declaredFields(field), FieldWriteAccessInformation.key)

        if (handleFieldWriteAccessInformation(fwaiEP))
            return Result(field, Assignable);

        createResult()
    }

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method:   DefinedMethod,
        taCode:   TACode[TACMethodParameter, V],
        callers:  Callers,
        pc:       PC,
        receiver: AccessReceiver
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
        method:  DefinedMethod,
        callers: Callers
    )(implicit state: AnalysisState): Boolean = {
        ref.definedBy.forall { defSite =>
            if (defSite < 0) false // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) true
                else if (!definition.expr.isNew) false
                else {
                    var hasEscaped = false
                    callers.forNewCalleeContexts(null, method) { context =>
                        val entity = (context, definitionSites(method.definedMethod, definition.pc))
                        val escapeProperty = propertyStore(entity, EscapeProperty.key)
                        hasEscaped ||= handleEscapeProperty(escapeProperty)
                    }
                    !hasEscaped
                }
            }
        }
    }

    protected[this] def handleFieldWriteAccessInformation(
        newEP: EOptionP[DeclaredField, FieldWriteAccessInformation]
    )(implicit state: AnalysisState): Boolean = {
        val assignable = if (newEP.hasUBP) {
            val newFai = newEP.ub
            val (seenDirectAccesses, seenIndirectAccesses) = state.fieldWriteAccessDependee match {
                case Some(UBP(fai)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _              => (0, 0)
            }
            state.fieldWriteAccessDependee = Some(newEP)

            newFai.getNewestAccesses(
                newFai.numDirectAccesses - seenDirectAccesses,
                newFai.numIndirectAccesses - seenIndirectAccesses
            ) exists { writeAccess =>
                    val method = contextProvider.contextFromId(writeAccess._1).method.asDefinedMethod
                    state.fieldAccesses += method -> (state.fieldAccesses.getOrElse(method, Set.empty) +
                        ((writeAccess._2, writeAccess._3)))

                    val tacEP = state.tacDependees.get(method) match {
                        case Some(tacEP) => tacEP
                        case None =>
                            val tacEP = propertyStore(method.definedMethod, TACAI.key)
                            state.tacDependees += method -> tacEP
                            tacEP
                    }

                    val callersEP = state.callerDependees.get(method) match {
                        case Some(callersEP) => callersEP
                        case None =>
                            val callersEP = propertyStore(method, Callers.key)
                            state.callerDependees += method -> callersEP
                            callersEP
                    }

                    if (tacEP.hasUBP && callersEP.hasUBP)
                        methodUpdatesField(
                            method, tacEP.ub.tac.get, callersEP.ub, writeAccess._2, writeAccess._3
                        )
                    else
                        false
                }
        } else {
            state.fieldWriteAccessDependee = Some(newEP)
            false
        }

        assignable
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: AnalysisState): ProperPropertyComputationResult = {
        val isNonFinal = eps.pk match {
            case EscapeProperty.key =>
                val newEP = eps.asInstanceOf[EOptionP[(Context, DefinitionSite), EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e != newEP.e)
                handleEscapeProperty(newEP)
            case TACAI.key =>
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = declaredMethods(newEP.e)
                val accesses = state.fieldAccesses.get(method)
                state.tacDependees += method -> newEP
                val callersProperty = state.callerDependees(method)
                if (callersProperty.hasUBP && accesses.isDefined)
                    accesses.get.exists(access =>
                        methodUpdatesField(method, newEP.ub.tac.get, callersProperty.ub, access._1, access._2))
                else false
            case Callers.key =>
                val newEP = eps.asInstanceOf[EOptionP[DefinedMethod, Callers]]
                val method = newEP.e
                val accesses = state.fieldAccesses.get(method)
                state.callerDependees += newEP.e -> newEP
                val tacProperty = state.tacDependees(method)
                if (tacProperty.hasUBP && tacProperty.ub.tac.isDefined && accesses.isDefined)
                    accesses.get.exists(access =>
                        methodUpdatesField(method, tacProperty.ub.tac.get, newEP.ub, access._1, access._2))
                else false
            case FieldWriteAccessInformation.key =>
                val newEP = eps.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]]
                handleFieldWriteAccessInformation(newEP)
        }

        if (isNonFinal)
            Result(state.field, Assignable)
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
            Result(state.field, state.fieldAssignability)
        }
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

trait AbstractFieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        TACAI,
        EscapeProperty,
        FieldWriteAccessInformation,
        Callers
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey,
        ContextProviderKey,
        DeclaredFieldsKey
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.ub(FieldAssignability)
}
