/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ClassType
import org.opalj.br.DeclaredField
import org.opalj.br.DefinedMethod
import org.opalj.br.DoubleType
import org.opalj.br.Field
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.Method
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
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

trait AbstractFieldAssignabilityAnalysis extends FPCFAnalysis {

    trait AbstractFieldAssignabilityAnalysisState {

        val field: Field
        var fieldAssignability: FieldAssignability = NonAssignable
        var fieldAccesses: Map[DefinedMethod, Map[Int, Set[(PC, AccessReceiver)]]] = Map.empty
        var escapeDependees: Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty
        var fieldWriteAccessDependee: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] = None
        var tacDependees: Map[DefinedMethod, EOptionP[Method, TACAI]] = Map.empty

        def hasDependees: Boolean = {
            escapeDependees.nonEmpty || fieldWriteAccessDependee.exists(_.isRefinable) ||
            tacDependees.valuesIterator.exists(_.isRefinable)
        }

        def dependees: Set[SomeEOptionP] = {
            escapeDependees ++ fieldWriteAccessDependee.filter(_.isRefinable) ++
                tacDependees.valuesIterator.filter(_.isRefinable)
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

    private[analyses] def determineFieldAssignability(field: Field): ProperPropertyComputationResult = {
        implicit val state: AnalysisState = createState(field)

        val thisType = field.classFile.thisType
        if (!field.isFinal) {
            if (field.isPublic) {
                return Result(field, Assignable);
            } else if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown || !closedPackages(thisType.packageName)) {
                    return Result(field, Assignable);
                }
            }

            if (field.isPackagePrivate && !closedPackages(thisType.packageName)) {
                return Result(field, Assignable);
            }
        }

        state.fieldAssignability =
            if (field.isFinal) NonAssignable
            else EffectivelyNonAssignable

        handleWriteAccessInformation(propertyStore(declaredFields(field), FieldWriteAccessInformation.key))
    }

    protected def handleWriteAccessInformation(
        newEP: EOptionP[DeclaredField, FieldWriteAccessInformation]
    )(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (newEP.hasUBP) {
            val newFai = newEP.ub
            val (seenDirectAccesses, seenIndirectAccesses) = state.fieldWriteAccessDependee match {
                case Some(UBP(fai)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _              => (0, 0)
            }
            state.fieldWriteAccessDependee = Some(newEP)

            newFai.getNewestAccesses(
                newFai.numDirectAccesses - seenDirectAccesses,
                newFai.numIndirectAccesses - seenIndirectAccesses
                ) foreach { case (contextID, pc, receiver, _) =>
                val method = contextProvider.contextFromId(contextID).method.asDefinedMethod
                val access = (pc, receiver)
                state.fieldAccesses = state.fieldAccesses.updatedWith(method) {
                    case None => Some(Map((contextID, Set(access))))
                    case Some(map) => Some(map + (contextID -> (map.getOrElse(contextID, Set.empty) + access)))
                }
            }

            state.fieldAccesses.foreachEntry { (method, contextIDToAccesses) =>
                val tacEP = state.tacDependees.get(method) match {
                    case Some(tacEP) => tacEP
                    case None        =>
                        val tacEP = propertyStore(method.definedMethod, TACAI.key)
                        state.tacDependees += method -> tacEP
                        tacEP
                }

                if (tacEP.hasUBP) {
                    contextIDToAccesses.foreachEntry { (contextID, accesses) =>
                        val context = contextProvider.contextFromId(contextID)
                        if (state.fieldAssignability == Assignable ||
                            doesMethodUpdateFieldInContext(context, method, tacEP.ub.tac.get, accesses))
                            state.fieldAssignability = Assignable
                    }
                }
            }
        } else {
            state.fieldWriteAccessDependee = Some(newEP)
        }

        createResult()
    }

    /**
     * Returns true when the method in the given context definitely updates the field in a way that forces it to be
     * assignable, and false when it does not, or we are not yet sure.
     *
     * @note Callers must pass ALL write accesses of this method in this context discovered so far.
     */
    protected def doesMethodUpdateFieldInContext(
        context: Context,
        definedMethod: DefinedMethod,
        taCode: TACode[TACMethodParameter, V],
        writeAccesses: Iterable[(PC, AccessReceiver)]
    )(implicit state: AnalysisState): Boolean

    def createResult()(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees && (state.fieldAssignability ne Assignable))
            InterimResult(state.field, lb = Assignable, ub = state.fieldAssignability, state.dependees, continuation)
        else
            Result(state.field, state.fieldAssignability)
    }

    def continuation(eps: SomeEPS)(implicit state: AnalysisState): ProperPropertyComputationResult = {
        eps.pk match {
            case FieldWriteAccessInformation.key =>
                handleWriteAccessInformation(eps.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]])

            case EscapeProperty.key =>
                val newEP = eps.asInstanceOf[EOptionP[(Context, DefinitionSite), EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e != newEP.e)
                if (handleEscapeProperty(newEP))
                    Result(state.field, Assignable)
                else
                    createResult()

            case TACAI.key =>
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = declaredMethods(newEP.e)
                val accessesByContext = state.fieldAccesses(method)
                state.tacDependees += method -> newEP
                if (accessesByContext.exists(kv => doesMethodUpdateFieldInContext(contextProvider.contextFromId(kv._1), method, newEP.ub.tac.get, kv._2)))
                    Result(state.field, Assignable)
                else
                    createResult()
        }
    }

    /**
     * Handles the influence of an escape property on the field assignability.
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
     * Checks whether the object reference of a field access does not escape (except for being returned).
     */
    def referenceHasEscaped(
        ref:     V,
        stmts:   Array[Stmt[V]],
        method:  DefinedMethod,
        context: Context
    )(implicit state: AnalysisState): Boolean = {
        ref.definedBy.forall { defSite =>
            if (defSite < 0) true // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) false
                else if (!definition.expr.isNew) true
                else {
                    val entity = (context, definitionSites(method.definedMethod, definition.pc))
                    val escapeProperty = propertyStore(entity, EscapeProperty.key)
                    handleEscapeProperty(escapeProperty)
                }
            }
        }
    }

    /**
     * Returns the initialization value of a given type.
     */
    def getDefaultValues()(implicit state: AnalysisState): Set[Any] = state.field.fieldType match {
        case FloatType | ClassType.Float     => Set(0.0f)
        case DoubleType | ClassType.Double   => Set(0.0d)
        case LongType | ClassType.Long       => Set(0L)
        case CharType | ClassType.Character  => Set('\u0000')
        case BooleanType | ClassType.Boolean => Set(false)
        case IntegerType |
            ClassType.Integer |
            ByteType |
            ClassType.Byte |
            ShortType |
            ClassType.Short => Set(0)
        case ClassType.String => Set("", null)
        case _: ReferenceType => Set(null)
    }
}

trait AbstractFieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        TACAI,
        EscapeProperty,
        FieldWriteAccessInformation
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
