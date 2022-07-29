/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.ClassTypeSignature
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.ProperTypeArgument
import org.opalj.br.RuntimeInvisibleAnnotationTable
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.SourceFile
import org.opalj.br.TypeVariableSignature
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.ReferenceType
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
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.EUBP
import org.opalj.br.analyses.cg.TypeExtensibilityKey

/**
 * Analysis that determines the immutability of org.opalj.br.Field.
 * @author Tobias Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    /**
     *  Describes the different kinds of dependently immutable fields.
     *
     *  [[NotDependentlyImmutable]] There are non-transitively immutable or mutable types.
     *  Example: Generic<T, MutableClass> f
     *
     *  [[NotNonTransitivelyImmutableOrMutable]] There are no non-transitively immutable and no mutable types.
     *  Example: Generic<T,String> f
     *
     *  [[OnlyTransitivelyImmutable]] There are only transitively immutable types and no generic parameters left.
     *  Example: Generic<String, String> f
     */
    sealed trait DependentImmutabilityTypes
    case object NotDependentlyImmutable extends DependentImmutabilityTypes
    case object NotNonTransitivelyImmutableOrMutable extends DependentImmutabilityTypes
    case object OnlyTransitivelyImmutable extends DependentImmutabilityTypes

    case class State(
            field:                             Field,
            var upperBound:                    FieldImmutability               = TransitivelyImmutableField,
            var typeImmutability:              TypeImmutability                = TransitivelyImmutableType,
            var classImmutability:             ClassImmutability               = TransitivelyImmutableClass,
            var fieldIsNotAssignable:          Option[Boolean]                 = None,
            var dependentImmutability:         DependentImmutabilityTypes      = OnlyTransitivelyImmutable,
            var fieldImmutabilityDependees:    Set[EOptionP[Entity, Property]] = Set.empty,
            var genericTypeParameters:         Set[String]                     = Set.empty,
            var fieldTypeIsDependentImmutable: Boolean                         = false
    ) extends BaseAnalysisState with TypeIteratorState {
        def hasFieldImmutabilityDependees: Boolean = fieldImmutabilityDependees.nonEmpty
        def getFieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = fieldImmutabilityDependees
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)

    var considerGenericity: Boolean =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis.considerGenericity"
        )

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field => determineFieldImmutability(field)
        case _            => throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {
        import org.opalj.br.FieldType

        implicit val state: State = State(field)
        implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)

        /**
         * Query type iterator for concrete class types.
         * Note: This only works precisely in a closed world assumption!!
         */
        def queryTypeIterator(implicit state: State, typeIterator: TypeIterator): Unit = {
            val actualTypes = typeIterator.typesProperty(state.field, typeIterator)

            typeIterator.foreachType(state.field, actualTypes) { actualType =>
                handleClassImmutability(actualType)
            }
        }

        def handleGenericity()(implicit state: State): Unit = {
            var noNonTransitivelyImmutableOrMutableType = true
            var onlyTransitivelyImmutableTypes = true
            var noRelevantAttributesFound = true
            var genericParameters: List[ObjectType] = List()

            state.field.attributes.foreach {

                case RuntimeInvisibleAnnotationTable(_) | SourceFile(_) => // no generic parameter

                case TypeVariableSignature(t) =>
                    state.genericTypeParameters += t
                    noRelevantAttributesFound = false
                    onlyTransitivelyImmutableTypes = false
                    state.classImmutability = DependentlyImmutableClass(state.genericTypeParameters).
                        meet(state.classImmutability)
                //if (!isInClassesGenericTypeParameters(t))
                //    noNonTransitivelyImmutableOrMutableType = false

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                    noRelevantAttributesFound = false

                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(identifier)) =>
                            onlyTransitivelyImmutableTypes = false // At least one generic paramter found
                            state.genericTypeParameters += identifier
                        //if (!isInClassesGenericTypeParameters(identifier))
                        //    noNonTransitivelyImmutableOrMutableType = false

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
                            genericParameters ::= ObjectType(objectPath)

                        case _ =>
                            noNonTransitivelyImmutableOrMutableType = false
                            onlyTransitivelyImmutableTypes = false
                    }
                case _ =>
                    noNonTransitivelyImmutableOrMutableType = false
                    onlyTransitivelyImmutableTypes = false
            }

            genericParameters.foreach(objectType => {

                propertyStore(objectType, TypeImmutability.key) match {

                    case LBP(TransitivelyImmutableType) => //nothing to do here; default value: transitively immutable

                    //nested generic classes are over-approximated as mutable
                    case UBP(DependentlyImmutableType(_) | NonTransitivelyImmutableType | MutableType) =>
                        noNonTransitivelyImmutableOrMutableType = false
                        onlyTransitivelyImmutableTypes = false

                    case ep => state.fieldImmutabilityDependees += ep
                }
            })

            //Prevents the case of keeping the default values of these flags
            // only because of no relevant attribute have been found
            if (!noRelevantAttributesFound) {
                if (onlyTransitivelyImmutableTypes) {
                    //nothing to do...
                } else if (noNonTransitivelyImmutableOrMutableType &&
                    state.dependentImmutability != NotDependentlyImmutable) {
                    state.dependentImmutability = NotNonTransitivelyImmutableOrMutable
                } else
                    state.dependentImmutability = NotDependentlyImmutable
            } else
                state.dependentImmutability = NotDependentlyImmutable
        }

        def handleTypeImmutability(objectType: FieldType)(implicit state: State): Unit = {
            if (objectType.isBaseType) {
                // base types are by design transitively immutable
                //state.typeImmutability = true // true is default
            } else if (objectType == ObjectType.Object || //handling generic fields
                objectType.isArrayType) { //Because the entries of an array can be reassigned we state it as mutable
                state.typeImmutability = MutableType
            } else {

                val result = propertyStore(objectType, TypeImmutability.key)
                result match {
                    case LBP(TransitivelyImmutableType) => // transitively immutable type is set as default
                    case FinalEP(t, DependentlyImmutableType(_)) =>
                        state.typeImmutability = DependentlyImmutableType(Set.empty).meet(state.typeImmutability)
                        if (t == field.fieldType)
                            state.fieldTypeIsDependentImmutable = true
                    case UBP(MutableType | NonTransitivelyImmutableType) =>
                        state.typeImmutability = MutableType
                    case epk => state.fieldImmutabilityDependees += epk
                }
            }
        }

        /**
         * In case of the concrete assigned class-type is known this method handles the immutability of it.
         */
        def handleClassImmutability(referenceType: ReferenceType)(implicit state: State): Unit = {

            if (referenceType.isArrayType)
                state.classImmutability = MutableClass
            else {
                propertyStore(referenceType, ClassImmutability.key) match {

                    case LBP(TransitivelyImmutableClass) => //nothing to do ; transitively immutable is default

                    case FinalP(DependentlyImmutableClass(_)) =>
                        state.classImmutability = DependentlyImmutableClass(Set.empty).meet(state.classImmutability)

                    case UBP(MutableClass | NonTransitivelyImmutableClass) =>
                        state.classImmutability = MutableClass.meet(state.classImmutability)

                    case eps =>
                        state.fieldImmutabilityDependees += eps
                }
            }
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasFieldImmutabilityDependees) {
                val lowerBound =
                    if (state.fieldIsNotAssignable.isDefined && state.fieldIsNotAssignable.get)
                        NonTransitivelyImmutableField
                    else
                        MutableField
                val upperBound =
                    if (state.fieldIsNotAssignable.isEmpty) TransitivelyImmutableField
                    else {
                        state.fieldIsNotAssignable match {
                            case Some(false) => MutableField
                            case Some(true) | None =>
                                if (state.typeImmutability == TransitivelyImmutableType) {
                                    TransitivelyImmutableField
                                } else if (state.typeImmutability == MutableType &&
                                    !state.fieldTypeIsDependentImmutable) {
                                    NonTransitivelyImmutableField
                                } else {
                                    state.dependentImmutability match {
                                        case NotNonTransitivelyImmutableOrMutable =>
                                            DependentlyImmutableField(state.genericTypeParameters)
                                        case OnlyTransitivelyImmutable => TransitivelyImmutableField
                                        case _                         => NonTransitivelyImmutableField
                                    }
                                }
                        }
                    }
                if (lowerBound == upperBound)
                    Result(field, lowerBound)
                else
                    InterimResult(
                        field,
                        lowerBound,
                        upperBound,
                        state.getFieldImmutabilityDependees,
                        c
                    )
            } else {

                state.fieldIsNotAssignable match {

                    case Some(false) | None => Result(field, MutableField)

                    case Some(true) =>
                        if (state.typeImmutability == TransitivelyImmutableType ||
                            state.classImmutability == TransitivelyImmutableClass &&
                            typeExtensibility(ObjectType.Object).isNo) {
                            Result(field, TransitivelyImmutableField)
                        } else {
                            if (state.fieldTypeIsDependentImmutable && field.fieldType == ObjectType.Object ||
                                state.classImmutability.isDependentlyImmutable ||
                                state.typeImmutability.isDependentlyImmutable) {
                                state.dependentImmutability match {
                                    case OnlyTransitivelyImmutable => Result(field, TransitivelyImmutableField)
                                    case NotNonTransitivelyImmutableOrMutable =>
                                        Result(field, DependentlyImmutableField(state.genericTypeParameters))
                                    case NotDependentlyImmutable => Result(field, NonTransitivelyImmutableField)
                                }
                            } else Result(field, NonTransitivelyImmutableField)
                        }
                }
            }
        }

        /**
         * Checks whether the type was a concretization of a generic type and adjusts the immutability information if
         * necessary.
         */
        //  def handleMutableType(tpe: ObjectType)(implicit state: State): Unit =
        //      if (state.concreteGenericTypes.contains(tpe))
        //          state.dependentImmutability = NotDependentlyImmutable

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasDependee(eps.toEPK)) {
                if (eps.isFinal)
                    state.removeDependee(eps.toEPK)
                else
                    state.updateDependency(eps)
                typeIterator.continuation(state.field, eps) {
                    actualType => handleClassImmutability(actualType.asObjectType)
                }
            } else {

                state.fieldImmutabilityDependees =
                    state.fieldImmutabilityDependees.filter(ep => (ep.e != eps.e) || (ep.pk != eps.pk))

                eps match {
                    case LBP(TransitivelyImmutableClass | TransitivelyImmutableType) =>

                    case UBP(NonTransitivelyImmutableClass | MutableClass)           => state.classImmutability = MutableClass

                    case UBP(Assignable | UnsafelyLazilyInitialized) =>
                        state.fieldIsNotAssignable = Some(false)
                        return Result(state.field, MutableField);

                    case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) =>
                        state.fieldIsNotAssignable = Some(true)

                    case FinalP(DependentlyImmutableClass(_)) =>
                        state.classImmutability = DependentlyImmutableClass(Set.empty)

                    case UBP(DependentlyImmutableType(_) | NonTransitivelyImmutableType | MutableType) =>
                        state.dependentImmutability = NotDependentlyImmutable

                    case ubp @ UBP(TransitivelyImmutableClass |
                        TransitivelyImmutableType | DependentlyImmutableClass(_)) =>
                        state.fieldImmutabilityDependees += ubp

                    case ubp @ UBP(EffectivelyNonAssignable | NonAssignable) => state.fieldImmutabilityDependees += ubp

                    case FinalEP(t, DependentlyImmutableType(_)) =>

                        import org.opalj.br.FieldType
                        if (t.asInstanceOf[FieldType] == state.field.fieldType) {
                            state.fieldTypeIsDependentImmutable = true
                            state.typeImmutability = DependentlyImmutableType(Set.empty)
                        }
                    // val tpe = t.asInstanceOf[ObjectType]
                    // handleMutableType(tpe)

                    case EUBP(t, MutableType | NonTransitivelyImmutableType) =>
                        import org.opalj.br.FieldType
                        if (t.asInstanceOf[FieldType] == state.field.fieldType)
                            state.typeImmutability = MutableType

                    //    val tpe = t.asInstanceOf[ObjectType]
                    //    handleMutableType(tpe)

                    case ep =>
                        throw new Exception(s"$ep + was not covered")
                }
            }
            createResult()
        }

        /**
         * Determines the assignability of the field and registers the dependees if necessary
         */
        propertyStore(field, FieldAssignability.key) match {

            case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) =>
                state.fieldIsNotAssignable = Some(true)

            case UBP(Assignable | UnsafelyLazilyInitialized) =>
                return Result(field, MutableField);

            case ep => state.fieldImmutabilityDependees += ep
        }

        /**
         * Determines whether the field is dependent immutable if the flag [[considerGenericity]] is set
         */
        if (considerGenericity)
            handleGenericity()
        else //The analysis is optimistic so the default value must be adapted in this case
            state.dependentImmutability = NotDependentlyImmutable

        if (field.fieldType.isReferenceType &&
            (!state.classImmutability.isInstanceOf[DependentlyImmutableClass] ||
                state.dependentImmutability == NotDependentlyImmutable)) {
            if (typeExtensibility(ObjectType.Object).isYesOrUnknown) {
                handleTypeImmutability(field.fieldType)
            } else {
                queryTypeIterator
            }
        } else
            state.classImmutability = TransitivelyImmutableClass
        createResult()
    }
}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey)
    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)
}

/**
 * Executor for the eager field immutability analysis.
 */
object EagerL0FieldImmutabilityAnalysis
    extends L0FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldImmutabilityAnalysis(p)
        val fields = p.allFields // allProjectClassFiles.flatMap(classfile => classfile.fields)
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldImmutability)
        analysis
    }
}

/**
 *
 * Executor for the lazy field immutability analysis.
 *
 */
object LazyL0FieldImmutabilityAnalysis
    extends L0FieldImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L0FieldImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldImmutability.key,
            analysis.determineFieldImmutability
        )
        analysis
    }
}
