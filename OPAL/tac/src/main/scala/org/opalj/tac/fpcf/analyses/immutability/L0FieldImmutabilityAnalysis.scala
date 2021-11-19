/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

import org.opalj.br.ClassTypeSignature
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.ProperTypeArgument
import org.opalj.br.RuntimeInvisibleAnnotationTable
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.SourceFile
import org.opalj.br.TypeVariableSignature
import org.opalj.br.fpcf.properties.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.Assignable
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.LBP
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.LazilyInitialized
import org.opalj.br.fpcf.properties.NonAssignable
import org.opalj.br.fpcf.properties.UnsafelyLazilyInitialized
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.br.fpcf.properties.DependentlyImmutableField
import org.opalj.br.ClassFile
import org.opalj.br.FormalTypeParameter
import org.opalj.br.Attribute
import org.opalj.br.ClassSignature
import org.opalj.tac.fpcf.analyses.cg.TypeProviderState
import org.opalj.br.fpcf.properties.DependentlyImmutableType
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableType
import org.opalj.br.ReferenceType

/**
 * Analysis that determines the immutability of org.opalj.br.Field
 * @author Tobias Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    /**
     *  Describes the different kinds of dependently immutable fields.
     *
     *  [[NotDependentlyImmutable]] non-transitively immutable or mutable types could still exist.
     *  Example: Generic<T, MutableClass> f
     *
     *  [[NotNonTransitivelyImmutableOrMutable]] There are no non-transitively immutable and no mutable types.
     *  Example: Generic<T,T> f
     *
     *  [[OnlyTransitivelyImmutable]] There are only transitively immutable types and no generic parameters left.
     *  Example: Generic<String, String> f
     */
    sealed trait DependentImmutabilityTypes
    case object NotDependentlyImmutable extends DependentImmutabilityTypes
    case object NotNonTransitivelyImmutableOrMutable extends DependentImmutabilityTypes
    case object OnlyTransitivelyImmutable extends DependentImmutabilityTypes

    case class State(
            field:                          Field,
            var upperBound:                 FieldImmutability               = TransitivelyImmutableField,
            var typeImmutability:           TypeImmutability                = TransitivelyImmutableType,
            var classImmutability:          ClassImmutability               = TransitivelyImmutableClass,
            var referenceIsImmutable:       Option[Boolean]                 = None,
            var dependentImmutability:      DependentImmutabilityTypes      = OnlyTransitivelyImmutable,
            var fieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = Set.empty,
            var genericTypeParameters:      Set[String]                     = Set.empty
    ) extends TypeProviderState {
        def hasFieldImmutabilityDependees: Boolean = fieldImmutabilityDependees.nonEmpty
        def getFieldImmutabilityDependees: Set[EOptionP[Entity, Property]] = fieldImmutabilityDependees
    }

    var considerGenericity =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis.considerGenericity"
        )

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineFieldImmutability(field)
        case _ ⇒
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {
        import org.opalj.tac.cg.CallGraphKey

        implicit val state: State = State(field)
        implicit val typeProvider: TypeProvider = CallGraphKey.typeProvider

        /**
         * Returns the formal type parameters from the class' and outer classes' signature
         */
        def getFormalTypeParameters: Set[String] = {

            /**
             *
             * Extract the formal type parameters if it exists of a class' attribute
             */
            def collectFormalTypeParameterFromClassAttribute(attribute: Attribute): Iterator[String] =
                attribute match {
                    case ClassSignature(typeParameters, _, _) ⇒
                        typeParameters.iterator.map { case FormalTypeParameter(identifier, _, _) ⇒ identifier }
                    case _ ⇒ Iterator.empty
                }

            /**
             * If the genericity is nested in an inner class
             * collect the generic type parameters from the field's outer classes
             */
            def getAllFormalParameters(classFile: ClassFile): Iterator[String] = {
                classFile.attributes.iterator.flatMap(collectFormalTypeParameterFromClassAttribute(_)) ++ {
                    if (classFile.outerType.isDefined) {
                        val outerClassFile = project.classFile(classFile.outerType.get._1)
                        if (outerClassFile.isDefined && outerClassFile.get != classFile) {
                            getAllFormalParameters(outerClassFile.get)
                        } else
                            Iterator.empty
                    } else
                        Iterator.empty
                }
            }
            getAllFormalParameters(field.classFile).toSet
        }

        val classFormalTypeParameters: Set[String] = getFormalTypeParameters

        /**
         * Returns, if a generic parameter like e.g. 'T' is in the class' or an outer class' signature
         * @param string The generic type parameter that should be looked for
         */
        def isInClassesGenericTypeParameters(string: String): Boolean =
            classFormalTypeParameters.contains(string)

        /**
         * Query the type provider for concrete class types.
         * Note: Does only work in a closed world assumption!!
         */
        def queryTypeProvider(implicit state: State, typeProvider: TypeProvider): Unit = {
            val actualTypes = typeProvider.typesProperty(state.field, typeProvider)

            typeProvider.foreachType(state.field, actualTypes) { actualType ⇒
                handleKnownClassType(actualType)
            }
        }

        def handleGenericity()(implicit state: State): Unit = {
            var noShallowOrMutableTypesInGenericTypeFound = true
            var onlyDeepImmutableTypesInGenericTypeFound = true
            var genericParameters: List[ObjectType] = List()
            var noRelevantAttributesFound = true

            state.field.attributes.foreach {

                case RuntimeInvisibleAnnotationTable(_) | SourceFile(_) ⇒ // no including generic parameter

                case TypeVariableSignature(t) ⇒
                    state.genericTypeParameters += t
                    noRelevantAttributesFound = false
                    onlyDeepImmutableTypesInGenericTypeFound = false
                    state.classImmutability = DependentImmutableClass(Set.empty).meet(state.classImmutability)
                    if (!isInClassesGenericTypeParameters(t))
                        noShallowOrMutableTypesInGenericTypeFound = false

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) ⇒
                    noRelevantAttributesFound = false

                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(identifier)) ⇒
                            onlyDeepImmutableTypesInGenericTypeFound = false
                            state.genericTypeParameters += identifier
                            if (!isInClassesGenericTypeParameters(identifier))
                                noShallowOrMutableTypesInGenericTypeFound = false

                        case ProperTypeArgument(
                            _,
                            ClassTypeSignature(
                                outerPackageIdentifier,
                                SimpleClassTypeSignature(innerPackageIdentifier, _),
                                _
                                )
                            ) ⇒
                            val objectPath = outerPackageIdentifier match {
                                case Some(prepackageIdentifier) ⇒ prepackageIdentifier + innerPackageIdentifier
                                case _                          ⇒ innerPackageIdentifier
                            }
                            genericParameters ::= ObjectType(objectPath)

                        case _ ⇒
                            noShallowOrMutableTypesInGenericTypeFound = false
                            onlyDeepImmutableTypesInGenericTypeFound = false
                    }
                case _ ⇒
                    noShallowOrMutableTypesInGenericTypeFound = false
                    onlyDeepImmutableTypesInGenericTypeFound = false
            }

            genericParameters.foreach(objectType ⇒ {

                propertyStore(objectType, TypeImmutability.key) match {

                    case FinalP(TransitivelyImmutableType) ⇒ //nothing to do here: default value is deep immutable

                    //no nested generic classes are allowed
                    case UBP(DependentlyImmutableType(_) | NonTransitivelyImmutableType | MutableType) ⇒
                        noShallowOrMutableTypesInGenericTypeFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false

                    case ep ⇒ state.fieldImmutabilityDependees += ep
                }
            })

            //Prevents the case of keeping the default values of these
            // flags only because of no relevant attribute has been found
            if (!noRelevantAttributesFound) {
                if (onlyDeepImmutableTypesInGenericTypeFound) {
                    //nothing to do...
                } else if (noShallowOrMutableTypesInGenericTypeFound &&
                    state.dependentImmutability != NotDependentlyImmutable) {
                    state.dependentImmutability = NotNonTransitivelyImmutableOrMutable
                } else
                    state.dependentImmutability = NotDependentlyImmutable
            } else
                state.dependentImmutability = NotDependentlyImmutable
        }

        /**
         * In case of the concrete assigned class-type is known this method handles the immutability of it.
         */
        def handleKnownClassType(referenceType: ReferenceType)(implicit state: State): Unit = {

            if (referenceType.isArrayType)
                state.classImmutability = MutableClass
            else {
                val result = propertyStore(referenceType, ClassImmutability.key)

                result match {
                    case LBP(TransitivelyImmutableClass) ⇒ //nothing to do ; transitively immutable is default

                    case FinalP(DependentImmutableClass(_)) ⇒
                        state.classImmutability = DependentImmutableClass(Set.empty).meet(state.classImmutability)

                    case UBP(MutableClass | NonTransitivelyImmutableClass) ⇒
                        state.classImmutability = MutableClass.meet(state.classImmutability)

                    case eps ⇒
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
                    if (state.referenceIsImmutable.isDefined && state.referenceIsImmutable.get)
                        NonTransitivelyImmutableField
                    else
                        MutableField
                state.upperBound =
                    state.referenceIsImmutable match {

                        case Some(false) ⇒ MutableField

                        case Some(true) | None ⇒
                            state.classImmutability match {

                                case TransitivelyImmutableClass                   ⇒ TransitivelyImmutableField.meet(state.upperBound)

                                case MutableClass | NonTransitivelyImmutableClass ⇒ NonTransitivelyImmutableField.meet(state.upperBound)

                                case DependentImmutableClass(_) ⇒ //TransitivelyImmutableField
                                    state.dependentImmutability match {
                                        case NotNonTransitivelyImmutableOrMutable ⇒
                                            DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                                        case OnlyTransitivelyImmutable ⇒ TransitivelyImmutableField.meet(state.upperBound)
                                        case NotDependentlyImmutable   ⇒ NonTransitivelyImmutableField.meet(state.upperBound)
                                    }
                            }
                    }

                if (lowerBound == state.upperBound)
                    Result(field, lowerBound)
                else
                    InterimResult(
                        field,
                        lowerBound,
                        state.upperBound,
                        state.getFieldImmutabilityDependees,
                        c
                    )
            } else {
                state.referenceIsImmutable match {

                    case Some(false) ⇒
                        Result(field, MutableField)

                    case Some(true) ⇒
                        state.classImmutability match {

                            case TransitivelyImmutableClass ⇒
                                Result(field, TransitivelyImmutableField.meet(state.upperBound))

                            case NonTransitivelyImmutableClass | MutableClass ⇒
                                Result(field, NonTransitivelyImmutableField.meet(state.upperBound))

                            case DependentImmutableClass(_) ⇒ state.dependentImmutability match {

                                case OnlyTransitivelyImmutable ⇒
                                    Result(field, TransitivelyImmutableField.meet(state.upperBound))

                                case NotNonTransitivelyImmutableOrMutable ⇒
                                    Result(field, DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound))

                                case NotDependentlyImmutable ⇒
                                    Result(field, NonTransitivelyImmutableField.meet(state.upperBound))
                            }
                        }
                    case None ⇒ throw new Error("Reference immutability is not determined yet!")
                }
            }
        }

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasDependee(eps.toEPK)) {
                if (eps.isFinal)
                    state.removeDependee(eps.toEPK)
                else
                    state.updateDependency(eps)
                typeProvider.continuation(state.field, eps) {
                    actualType ⇒ handleKnownClassType(actualType.asObjectType)
                }
            } else {

                state.fieldImmutabilityDependees =
                    state.fieldImmutabilityDependees.filter(ep ⇒ (ep.e != eps.e) || (ep.pk != eps.pk))

                eps match {
                    case LBP(TransitivelyImmutableClass | TransitivelyImmutableType) ⇒

                    case UBP(NonTransitivelyImmutableClass | MutableClass) ⇒
                        state.classImmutability = MutableClass

                    case UBP(Assignable | UnsafelyLazilyInitialized) ⇒
                        state.referenceIsImmutable = Some(false)
                        return Result(state.field, MutableField);

                    case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) ⇒
                        state.referenceIsImmutable = Some(true)

                    case FinalP(DependentImmutableClass(_)) ⇒
                        state.classImmutability = DependentImmutableClass(Set.empty)

                    case UBP(DependentlyImmutableType(_) | NonTransitivelyImmutableType | MutableType) ⇒
                        state.dependentImmutability = NotDependentlyImmutable

                    case ubp @ UBP(TransitivelyImmutableClass | TransitivelyImmutableType | DependentImmutableClass(_)) ⇒
                        state.fieldImmutabilityDependees += ubp

                    case ubp @ UBP(EffectivelyNonAssignable | NonAssignable) ⇒ state.fieldImmutabilityDependees += ubp

                    case ep                                                  ⇒ throw new Exception(s"$ep + was not covered")
                }
            }
            createResult
        }

        /**
         * Determines the assignability of the field and registers the dependees if necessary
         */
        propertyStore(field, FieldAssignability.key) match {

            case FinalP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) ⇒
                state.referenceIsImmutable = Some(true)

            case FinalP(Assignable | UnsafelyLazilyInitialized) ⇒
                return Result(field, MutableField);

            case ep ⇒ state.fieldImmutabilityDependees += ep
        }

        /**
         * Determines whether the field is dependent immutable if the flag [[considerGenericity]] is set
         */
        considerGenericity = true
        if (considerGenericity)
            handleGenericity()
        else //The analysis is optimistic so the default value must be adapted in this case
            state.dependentImmutability = NotDependentlyImmutable

        if (field.fieldType.isReferenceType && (!state.classImmutability.isInstanceOf[DependentImmutableClass] || state.dependentImmutability == NotDependentlyImmutable)) { //TODO
            queryTypeProvider
        } //else state.classImmutability = TransitivelyImmutable is default

        createResult()
    }
}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)
}

/**
 *
 * Executor for the eager field immutability analysis.
 *
 */
object EagerL0FieldImmutabilityAnalysis
    extends L0FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldImmutabilityAnalysis(p)
        val fields = p.allProjectClassFiles.flatMap(classfile ⇒ classfile.fields)
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
