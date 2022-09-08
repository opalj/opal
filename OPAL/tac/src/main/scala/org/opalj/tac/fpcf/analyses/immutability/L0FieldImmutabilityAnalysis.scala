/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
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
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.FieldType
import org.opalj.br.ConstantFloat
import org.opalj.br.ConstantInteger
import org.opalj.br.ConstantString
import org.opalj.br.ConstantLong
import org.opalj.br.ArrayTypeSignature
import org.opalj.br.Wildcard
import org.opalj.br.ConstantDouble
import org.opalj.br.Synthetic
import org.opalj.br.RuntimeVisibleAnnotationTable
import org.opalj.br.Deprecated

/**
 * Analysis that determines the immutability of org.opalj.br.Field
 * @author Tobias Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    case class State(
            field:                          Field,
            var fieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = Set.empty,
            var fieldIsNotAssignable:       Option[Boolean]                 = None,
            var genericTypeParameters:      Set[String]                     = Set.empty,
            var innerType:                  Option[ObjectType]              = None,
            var upperBound:                 FieldImmutability               = TransitivelyImmutableField
    ) extends BaseAnalysisState with TypeIteratorState {
        def hasFieldImmutabilityDependees: Boolean = fieldImmutabilityDependees.nonEmpty || super.hasOpenDependencies
        def getFieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = fieldImmutabilityDependees
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field => determineFieldImmutability(field)
        case _ =>
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

        implicit val state: State = State(field)

        /**
         * Query type iterator for concrete class types.
         * Note: This only works precisely in a closed world assumption!!
         */
        def queryTypeIterator(implicit state: State, typeIterator: TypeIterator): Unit = {
            val actualTypes = typeIterator.typesProperty(state.field, typeIterator)

            typeIterator.foreachType(state.field, actualTypes) { actualType =>
                determineClassImmutability(actualType)
            }

        }

        def determineDependentImmutability()(implicit state: State): Unit = {

            def collectGenericIdentifierAndSetDependentImmutability(t: String) = {
                state.genericTypeParameters += t
                setDependentImmutability()
            }

            def setDependentImmutability() = {
                state.upperBound = DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
            }

            state.field.attributes.foreach {

                case RuntimeInvisibleAnnotationTable(_) | SourceFile(_) => // no generic parameter

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
                            propertyStore(objectType, TypeImmutability.key) match {

                                case LBP(TransitivelyImmutableType) => // transitively immutable is default

                                //nested generic classes are over-approximated as mutable
                                case UBP(DependentlyImmutableType(_) | NonTransitivelyImmutableType | MutableType) =>
                                    state.upperBound = NonTransitivelyImmutableField

                                case ep =>
                                    state.innerType = Some(objectType)
                                    state.fieldImmutabilityDependees += ep
                            }
                        case ProperTypeArgument(_, ArrayTypeSignature(_)) =>
                            state.upperBound = NonTransitivelyImmutableField
                        case Wildcard =>
                        case _        => state.upperBound = NonTransitivelyImmutableField
                    }
                case ConstantInteger(_) | ConstantFloat(_) | ConstantString(_) | ConstantLong(_) | ConstantDouble(_) |
                    Synthetic | Deprecated |
                    RuntimeVisibleAnnotationTable(_) =>
                case ArrayTypeSignature(_) => state.upperBound = NonTransitivelyImmutableField
                case _                     => state.upperBound = NonTransitivelyImmutableField

            }
        }

        def determineTypeImmutability(objectType: FieldType)(implicit state: State): Unit = {
            if (objectType == ObjectType.Object && state.field.fieldType == ObjectType.Object) {
                var tmpImmutability: FieldImmutability = NonTransitivelyImmutableField
                state.field.attributes.foreach {
                    case TypeVariableSignature(_) => tmpImmutability =
                        DependentlyImmutableField(state.genericTypeParameters)
                    case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                        typeArguments.foreach {
                            case ProperTypeArgument(_, TypeVariableSignature(_)) =>
                                tmpImmutability = DependentlyImmutableField(state.genericTypeParameters)
                            case _ =>
                        }
                    case _ =>
                }
                state.upperBound = tmpImmutability
                //handle generics -> potentially unsound
            } else if (objectType.isArrayType) { //Because the entries of an array can be reassigned we state it as mutable
                state.upperBound = NonTransitivelyImmutableField
            } else {
                propertyStore(objectType, TypeImmutability.key) match {
                    case LBP(TransitivelyImmutableType)          => // transitively immutable type is set as default
                    case FinalEP(t, DependentlyImmutableType(_)) =>
                    //Will be recognized in determineDependentImmutability
                    //Here the upper bound is not changed to recognize concretized transitively immutable fields
                    case UBP(MutableType | NonTransitivelyImmutableType) =>
                        state.upperBound = NonTransitivelyImmutableField
                    case epk => state.fieldImmutabilityDependees += epk
                }
            }
        }

        /**
         * In case of the concrete assigned class-type is known this method handles the immutability of it.
         */
        def determineClassImmutability(referenceType: ReferenceType)(implicit state: State): Unit = {
            if (referenceType.isArrayType)
                state.upperBound = NonTransitivelyImmutableField
            else {
                propertyStore(referenceType, ClassImmutability.key) match {

                    case LBP(TransitivelyImmutableClass)      => //transitively immutable is default

                    case FinalP(DependentlyImmutableClass(_)) =>
                    //Will be recognized in determineDependentImmutability
                    //Here the upper bound is not changed to recognize concretized transitively immutable fields

                    case EUBP(c, MutableClass) if (field.fieldType == ObjectType.Object && c == ObjectType.Object) => {

                        state.field.attributes.foreach {
                            case TypeVariableSignature(_) =>
                                state.upperBound = DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                            case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                                typeArguments.foreach {
                                    case ProperTypeArgument(_, TypeVariableSignature(_)) =>
                                        state.upperBound = DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                                    case _ =>
                                }
                            case _ =>
                        }
                        if (!state.upperBound.isInstanceOf[DependentlyImmutableField])
                            state.upperBound = NonTransitivelyImmutableField
                    }
                    case UBP(MutableClass | NonTransitivelyImmutableClass) =>
                        state.upperBound = NonTransitivelyImmutableField

                    case eps => state.fieldImmutabilityDependees += eps
                }
            }
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasFieldImmutabilityDependees) {
                val lowerBound =
                    if (state.fieldIsNotAssignable.getOrElse(false)) NonTransitivelyImmutableField
                    else MutableField
                val upperBound: FieldImmutability =
                    if (state.fieldIsNotAssignable.isEmpty) TransitivelyImmutableField
                    else state.fieldIsNotAssignable match {
                        case Some(false) | None => MutableField
                        case Some(true)         => state.upperBound
                    }
                if (lowerBound == upperBound)
                    Result(field, lowerBound)
                else
                    InterimResult(field, lowerBound, upperBound, state.getFieldImmutabilityDependees, c)
            } else state.fieldIsNotAssignable match {
                case Some(false) | None => Result(field, MutableField)
                case Some(true)         => Result(field, state.upperBound)
            }
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

                eps match {

                    case UBP(Assignable | UnsafelyLazilyInitialized) =>
                        return Result(state.field, MutableField);

                    case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) =>
                        state.fieldIsNotAssignable = Some(true)

                    case LBP(TransitivelyImmutableType | TransitivelyImmutableClass) =>

                    case EUBP(c, MutableClass) if (field.fieldType == ObjectType.Object && c == ObjectType.Object) =>
                        state.field.attributes.foreach {
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

                    case UBP(NonTransitivelyImmutableClass | NonTransitivelyImmutableType | MutableType | MutableClass) =>
                        state.upperBound = NonTransitivelyImmutableField

                    case EUBP(t, DependentlyImmutableType(_) | DependentlyImmutableClass(_)) =>
                        if (t.asInstanceOf[FieldType] != state.field.fieldType || state.innerType.contains(t))
                            state.upperBound = NonTransitivelyImmutableField

                    case ubp @ UBP(EffectivelyNonAssignable |
                        NonAssignable |
                        LazilyInitialized |
                        TransitivelyImmutableClass |
                        TransitivelyImmutableType) => state.fieldImmutabilityDependees += ubp

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
         * Determines whether the field is dependently immutable
         */
        determineDependentImmutability()

        if (field.fieldType.isReferenceType && typeExtensibility(ObjectType.Object).isNo)
            queryTypeIterator
        else if (field.fieldType.isReferenceType)
            determineTypeImmutability(field.fieldType)

        createResult()
    }
}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(ClassImmutability),
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
