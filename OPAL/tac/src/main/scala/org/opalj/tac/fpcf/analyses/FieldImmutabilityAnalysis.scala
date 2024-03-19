/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.collection.immutable.SortedSet

import org.opalj.br.ArrayTypeSignature
import org.opalj.br.ClassTypeSignature
import org.opalj.br.ConstantDouble
import org.opalj.br.ConstantFloat
import org.opalj.br.ConstantInteger
import org.opalj.br.ConstantLong
import org.opalj.br.ConstantString
import org.opalj.br.DeclaredField
import org.opalj.br.Deprecated
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ProperTypeArgument
import org.opalj.br.ReferenceType
import org.opalj.br.RuntimeInvisibleAnnotationTable
import org.opalj.br.RuntimeInvisibleTypeAnnotationTable
import org.opalj.br.RuntimeVisibleAnnotationTable
import org.opalj.br.RuntimeVisibleTypeAnnotationTable
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.Synthetic
import org.opalj.br.TypeVariableSignature
import org.opalj.br.Wildcard
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState

/**
 * Analysis that determines the immutability of org.opalj.br.Field
 * @author Tobias Roth
 */
class FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    case class State(
        field:                          DeclaredField,
        var fieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = Set.empty,
        var genericTypeParameters:      SortedSet[String]               = SortedSet.empty,
        var innerTypes:                 Set[ReferenceType]              = Set.empty,
        var lowerBound:                 FieldImmutability               = MutableField,
        var upperBound:                 FieldImmutability               = TransitivelyImmutableField
    ) extends BaseAnalysisState with TypeIteratorState {
        def hasFieldImmutabilityDependees: Boolean = fieldImmutabilityDependees.nonEmpty || super.hasOpenDependencies
        def getFieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = fieldImmutabilityDependees
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val declaredFields = project.get(DeclaredFieldsKey)
    implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)

    def doDetermineFieldImmutability(entity: Entity): ProperPropertyComputationResult = entity match {
        case field: Field => determineFieldImmutability(field)
        case _ =>
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

        implicit val state: State = State(declaredFields(field))

        /**
         * Query type iterator for concrete class types.
         * Note: This only works precisely in a closed world assumption!!
         */
        def queryTypeIterator(implicit state: State, typeIterator: TypeIterator): Unit = {
            val actualTypes = typeIterator.typesProperty(state.field, typeIterator)

            typeIterator.foreachType(state.field, actualTypes) { actualType => determineClassImmutability(actualType) }

        }

        def determineDependentImmutability()(implicit state: State): Unit = {

            def collectGenericIdentifierAndSetDependentImmutability(t: String): Unit = {
                state.genericTypeParameters += t
                setDependentImmutability()
            }

            def setDependentImmutability(): Unit = {
                state.upperBound = DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
            }

            state.field.definedField.attributes.foreach {

                case TypeVariableSignature(t) =>
                    collectGenericIdentifierAndSetDependentImmutability(t)

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(identifier)) =>
                            collectGenericIdentifierAndSetDependentImmutability(identifier)

                        case ProperTypeArgument(
                                _,
                                ClassTypeSignature(
                                    outerPackageIdentifier,
                                    SimpleClassTypeSignature(innerPackageIdentifier, _),
                                    _
                                )
                            ) =>
                            val objectPath = outerPackageIdentifier match {
                                case Some(prepackageIdentifier) => prepackageIdentifier + innerPackageIdentifier
                                case _                          => innerPackageIdentifier
                            }
                            val objectType = ObjectType(objectPath)
                            state.innerTypes += objectType
                            checkTypeImmutability(propertyStore(objectType, TypeImmutability.key))
                        case ProperTypeArgument(_, ArrayTypeSignature(_)) =>
                            state.upperBound = NonTransitivelyImmutableField
                        case Wildcard =>
                        case _        => state.upperBound = NonTransitivelyImmutableField
                    }
                case ConstantInteger(_) | ConstantFloat(_) | ConstantString(_) | ConstantLong(_) | ConstantDouble(_) |
                    RuntimeInvisibleAnnotationTable(_) | RuntimeVisibleAnnotationTable(_) |
                    RuntimeVisibleTypeAnnotationTable(_) | RuntimeInvisibleTypeAnnotationTable(_) |
                    Synthetic | Deprecated =>
                case ArrayTypeSignature(_) => state.upperBound = NonTransitivelyImmutableField
                case _                     => state.upperBound = NonTransitivelyImmutableField

            }
        }

        def determineTypeImmutability(implicit state: State): Unit = {
            if (state.field.fieldType == ObjectType.Object) {
                // in case of a field with type object: field immutability stays NonTransitivelyImmutable
                if (state.genericTypeParameters.isEmpty)
                    state.upperBound = NonTransitivelyImmutableField
                // handle generics -> potentially unsound
            } else if (state.field.fieldType.isArrayType) {
                // Because the entries of an array can be reassigned we state it mutable
                state.upperBound = NonTransitivelyImmutableField
            } else {
                checkTypeImmutability(propertyStore(state.field.fieldType, TypeImmutability.key))
            }
        }

        def checkTypeImmutability(result: EOptionP[FieldType, TypeImmutability]): Unit = result match {
            case LBP(TransitivelyImmutableType)                => // transitively immutable type is set as default
            case ep @ EUBPS(t, DependentlyImmutableType(_), _) =>
                // if the inner type of a generic field is dependently immutable
                if (state.innerTypes.contains(t.asReferenceType) ||
                    // or there are no generic information it is over-approximated to non-transitively immutable
                    (state.genericTypeParameters.isEmpty && state.innerTypes.isEmpty)
                )
                    state.upperBound = NonTransitivelyImmutableField
                else if (ep.isRefinable)
                    // if a field as a dep imm type that is refinable it could get worse and therefor dependencies are stored
                    state.fieldImmutabilityDependees += ep

            // Will be recognized in determineDependentImmutability
            // Here the upper bound is not changed to recognize concretized transitively immutable fields
            case UBP(MutableType | NonTransitivelyImmutableType) =>
                state.upperBound = NonTransitivelyImmutableField
            case ep => state.fieldImmutabilityDependees += ep
        }

        /**
         * In case of the concrete assigned class-type is known this method handles the immutability of it.
         */
        def determineClassImmutability(referenceType: ReferenceType)(implicit state: State): Unit = {
            if (referenceType.isArrayType)
                state.upperBound = NonTransitivelyImmutableField
            else
                checkClassImmutability(propertyStore(referenceType, ClassImmutability.key))
        }

        def checkClassImmutability(result: EOptionP[ReferenceType, ClassImmutability])(implicit state: State): Unit =
            result match {

                case LBP(TransitivelyImmutableClass)            => // transitively immutable is default
                case ep @ EUBP(t, DependentlyImmutableClass(_)) =>
                    // if the inner type of a generic field is dependently immutable
                    if (state.innerTypes.contains(t.asReferenceType) ||
                        // or there are no generic information it is over-approximated to non-transitively immutable
                        (state.genericTypeParameters.isEmpty && state.innerTypes.isEmpty)
                    )
                        state.upperBound = NonTransitivelyImmutableField
                    else if (ep.isRefinable)
                        // if a field as a dep imm type that is refinable it could get worse and therefor dependencies are stored
                        state.fieldImmutabilityDependees += ep

                case EUBP(c, MutableClass) if (field.fieldType == ObjectType.Object && c == ObjectType.Object) => {

                    state.field.definedField.attributes.foreach {
                        case TypeVariableSignature(_) =>
                            state.upperBound =
                                DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                        case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                            typeArguments.foreach {
                                case ProperTypeArgument(_, TypeVariableSignature(_)) =>
                                    state.upperBound =
                                        DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                                case _ =>
                            }
                        case _ =>
                    }
                    if (!state.upperBound.isInstanceOf[DependentlyImmutableField])
                        state.upperBound = NonTransitivelyImmutableField
                }
                case UBP(MutableClass | NonTransitivelyImmutableClass) =>
                    state.upperBound = NonTransitivelyImmutableField

                case ep => state.fieldImmutabilityDependees += ep
            }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {
            if (state.lowerBound == state.upperBound || !state.hasFieldImmutabilityDependees)
                Result(field, state.upperBound)
            else
                InterimResult(field, state.lowerBound, state.upperBound, state.getFieldImmutabilityDependees, c)
        }

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasDependee(eps.toEPK)) {
                if (eps.isFinal)
                    state.removeDependee(eps.toEPK)
                else
                    state.updateDependency(eps)
                typeIterator.continuation(state.field, eps) {
                    actualType => determineClassImmutability(actualType.asObjectType)
                }
            } else {

                state.fieldImmutabilityDependees =
                    state.fieldImmutabilityDependees.filter(ep => (ep.e != eps.e) || (ep.pk != eps.pk))

                eps.ub.key match {
                    case TypeImmutability.key =>
                        checkTypeImmutability(eps.asInstanceOf[EOptionP[ObjectType, TypeImmutability]])
                    case ClassImmutability.key =>
                        checkClassImmutability(eps.asInstanceOf[EOptionP[ObjectType, ClassImmutability]])
                    case FieldAssignability.key =>
                        checkFieldAssignability(eps.asInstanceOf[EOptionP[Field, FieldAssignability]])
                    case ep =>
                        throw new Exception(s"$ep + was not covered")
                }

            }
            createResult()
        }

        /**
         * Determines the assignability of the field and registers the dependees if necessary
         */
        def checkFieldAssignability(result: EOptionP[Field, FieldAssignability]): Unit = result match {

            case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) =>
                state.lowerBound = NonTransitivelyImmutableField

            case UBP(Assignable | UnsafelyLazilyInitialized) =>
                state.upperBound = MutableField

            case ep => state.fieldImmutabilityDependees += ep
        }

        checkFieldAssignability(propertyStore(field, FieldAssignability.key))
        if (state.upperBound == MutableField)
            return Result(field, MutableField);

        /**
         * Determines whether the field is dependently immutable
         */
        determineDependentImmutability()

        if (field.fieldType.isReferenceType) {
            if (typeExtensibility(ObjectType.Object).isNo)
                queryTypeIterator
            else
                determineTypeImmutability
        }

        createResult()
    }
}

trait FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override final def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(ClassImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(TypeIteratorKey, DeclaredFieldsKey, TypeExtensibilityKey)
    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)
}

/**
 * Executor for the eager field immutability analysis.
 */
object EagerFieldImmutabilityAnalysis
    extends FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override final def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new FieldImmutabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldImmutability)
        analysis
    }
}

/**
 * Executor for the lazy field immutability analysis.
 */
object LazyFieldImmutabilityAnalysis
    extends FieldImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override final def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new FieldImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldImmutability.key,
            analysis.doDetermineFieldImmutability
        )
        analysis
    }
}
