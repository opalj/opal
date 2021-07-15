/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

import org.opalj.br.FieldType
/*
import org.opalj.br.ClassFile
import org.opalj.br.FormalTypeParameter
import org.opalj.br.Attribute
import org.opalj.br.ClassSignature */
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
import org.opalj.br.fpcf.properties.DependentlyImmutableField
import org.opalj.br.fpcf.properties.DependentlyImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.Assignable
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.PCs
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
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.UBP
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.LBP
import org.opalj.fpcf.EUBP
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.LazilyInitialized
import org.opalj.br.fpcf.properties.NonAssignable
import org.opalj.br.fpcf.properties.UnsafelyLazilyInitialized
import org.opalj.value.ASObjectValue

/**
 * Analysis that determines the immutability of org.opalj.br.Field
 * @author Tobias Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    /**
     *  Describes the different kinds of dependent immutable fields, that depend on the concrete types that replace
     *  the generic parameters.
     *
     *  [[NotDependentlyImmutable]] non-transitively immutable or mutable types could still exist.
     *  Example: Foo<T, MutableClass> f
     *
     *  [[NotNonTransitivelyImmutableOrMutable]] There are no non-transitively immutable and no mutable types.
     *  Example: Foo<T,T> f
     *
     *  [[OnlyTransitivelyImmutable]] There are only transitively immutable types and no generic parameters left.
     *  Example: Foo<String, String> f
     */
    sealed trait DependentImmutabilityTypes
    case object NotDependentlyImmutable extends DependentImmutabilityTypes
    case object NotNonTransitivelyImmutableOrMutable extends DependentImmutabilityTypes
    case object OnlyTransitivelyImmutable extends DependentImmutabilityTypes

    case class State(
            field:                             Field,
            var typeImmutability:              TypeImmutability                            = TransitivelyImmutableType,
            var classImmutability:             ClassImmutability                           = TransitivelyImmutableClass,
            var referenceIsImmutable:          Option[Boolean]                             = None,
            var dependentImmutability:         DependentImmutabilityTypes                  = OnlyTransitivelyImmutable,
            var tacDependees:                  Map[Method, (EOptionP[Method, TACAI], PCs)] = Map.empty,
            var dependees:                     Set[EOptionP[Entity, Property]]             = Set.empty,
            var innerArrayTypes:               Set[ObjectType]                             = Set.empty,
            var concreteGenericTypes:          Set[ObjectType]                             = Set.empty,
            var concreteClassTypeIsKnown:      Boolean                                     = false,
            var nonConcreteObjectAssignments:  Boolean                                     = false,
            var concreterTypeIsKnown:          Boolean                                     = false,
            var totalNumberOfFieldWrites:      Int                                         = 0,
            var fieldTypeIsDependentImmutable: Boolean                                     = false,
            var genericTypeParameters:         Set[String]                                 = Set.empty
    ) {
        def hasDependees: Boolean = dependees.nonEmpty || tacDependees.nonEmpty
        def getDependees: Set[EOptionP[Entity, Property]] =
            dependees ++ tacDependees.valuesIterator.map(_._1)
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    val considerGenericity =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis.considerGenericity"
        )

    val defaultTransitivelyImmutableTypes = project.config.getStringList(
        "org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis.defaultTransitivelyImmutableTypes"
    ).toArray().toList.map(s ⇒ ObjectType(s.asInstanceOf[String])).toSet

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineFieldImmutability(field)
        case _ ⇒
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {
        // import org.opalj.br.ReferenceType
        //TODO determine bounds
        /**
         * Returns the formal type parameters from the class' and outer classes' signature
         */
        /*
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
        } */

        //val classFormalTypeParameters: Set[String] = getFormalTypeParameters

        /**
         * Returns, if a generic parameter like e.g. 'T' is in the class' or an outer class' signature
         * @param string The generic type parameter that should be looked for
         */
        // def isInClassesGenericTypeParameters(string: String): Boolean =
        //     classFormalTypeParameters.contains(string)

        /**
         * Determines the immutability of a fieldtype. Adjusts the state and registers the dependencies if necessary.
         */
        def handleTypeImmutability(objectType: FieldType)(implicit state: State): Unit = {

            if (objectType.isBaseType) {
                // base types are by design transitively immutable
                //state.typeImmutability = true // true is default
            } else if (objectType.isArrayType) {
                // Because the entries of an array can be reassigned we state it as mutable
                state.typeImmutability = MutableType
            } else if (defaultTransitivelyImmutableTypes.contains(objectType.asObjectType)) {
                //handles the configured transitively immutable types as transitively immutable
            } else if (objectType == ObjectType.Object) {
                state.typeImmutability = MutableType //handling generic fields
            } else {
                propertyStore(objectType, TypeImmutability.key) match {
                    case LBP(TransitivelyImmutableType) ⇒ // transitively immutable type is set as default
                    case FinalEP(t, DependentlyImmutableType(_)) ⇒
                        state.typeImmutability = DependentlyImmutableType(Set.empty).meet(state.typeImmutability)
                        if (t == field.fieldType)
                            state.fieldTypeIsDependentImmutable = true
                    case UBP(MutableType | NonTransitivelyImmutableType) ⇒
                        state.typeImmutability = MutableType
                    case epk ⇒ state.dependees += epk
                }
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
                    state.fieldTypeIsDependentImmutable = true //generic type T is dependent immutable
                //  if (!isInClassesGenericTypeParameters(t))
                //      noShallowOrMutableTypesInGenericTypeFound = false

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) ⇒
                    noRelevantAttributesFound = false

                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(t)) ⇒
                            onlyDeepImmutableTypesInGenericTypeFound = false
                            state.genericTypeParameters += t
                        //  if (!isInClassesGenericTypeParameters(identifier))
                        //      noShallowOrMutableTypesInGenericTypeFound = false

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

                    case ep ⇒
                        state.concreteGenericTypes += ep.e
                        state.dependees += ep
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
                } else {
                    state.dependentImmutability = NotDependentlyImmutable
                }
            } else {
                state.dependentImmutability = NotDependentlyImmutable
            }
        }

        /**
         * Returns the TACode for a method if available, registering dependencies as necessary.
         */
        def getTACAI(
            method: Method,
            pcs:    PCs,
            isRead: Boolean
        )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
            propertyStore(method, TACAI.key) match {

                case finalEP: FinalEP[Method, TACAI] ⇒ finalEP.ub.tac

                case epk ⇒
                    val writes =
                        if (state.tacDependees.contains(method))
                            state.tacDependees(method)._2
                        else
                            IntTrieSet.empty
                    state.tacDependees += method -> ((epk, writes ++ pcs))
                    None
            }
        }

        /**
         * Determines whether the the field write information can lead to a concretization of the immutability.
         */
        def examineFieldWritesForConcretizationOfImmutability()(implicit state: State): Unit = {
            val writes = fieldAccessInformation.writeAccesses(state.field)

            //has to be determined before the following foreach loop because in this the information is still needed
            state.totalNumberOfFieldWrites = writes.foldLeft(0)(_ + _._2.size)

            writes.foreach { write ⇒
                val (method, pcs) = write
                val taCodeOption = getTACAI(method, pcs, isRead = false)
                if (taCodeOption.isDefined)
                    searchForConcreteObjectInFieldWritesWithKnownTAC(pcs, taCodeOption.get)
            }
        }

        /**
         * Determine whether concrete objects or more precise class type can be recognized in the field writes.
         */
        def searchForConcreteObjectInFieldWritesWithKnownTAC(
            pcs:    PCs,
            taCode: TACode[TACMethodParameter, V]
        )(implicit state: State): Unit = {

            /**
             * In case of the concrete assigned classtype is known this method handles the immutability of it.
             * @note [[state.concreteClassTypeIsKnown]] must be set to true, before calling this function
             */
            def handleKnownClassType(objectType: ObjectType): Unit = {

                if (defaultTransitivelyImmutableTypes.contains(objectType))
                    state.classImmutability = TransitivelyImmutableClass
                else
                    propertyStore(objectType, ClassImmutability.key) match {
                        case LBP(TransitivelyImmutableClass) ⇒ //nothing to do ; transitively immutable ist default

                        case FinalEP(t, DependentImmutableClass(_)) ⇒
                            state.classImmutability = DependentImmutableClass(Set.empty).meet(state.classImmutability)
                            if (t == field.fieldType)
                                state.fieldTypeIsDependentImmutable = true

                        case UBP(MutableClass | NonTransitivelyImmutableClass) ⇒ state.classImmutability = MutableClass

                        case eps                                               ⇒ state.dependees += eps
                    }
            }

            /**
             * Analyzes the put fields for concrete object or more precise class type information and adjusts the
             * immutability information.
             */
            def analyzePutForConcreteObjectsOrMorePreciseClassType(putStmt: Stmt[V]): Unit = {
                val (putDefinitionSites, putValue) = {
                    if (putStmt.isPutField) {
                        val putField = putStmt.asPutField
                        (putField.value.asVar.definedBy, putField.value.asVar)
                    } else if (putStmt.isPutStatic) {
                        val putStatic = putStmt.asPutStatic
                        (putStatic.value.asVar.definedBy, putStatic.value.asVar)
                    } else
                        throw new Exception(s"$putStmt is not a  putStmt")
                }
                if (putValue.value.isArrayValue.isYesOrUnknown) {
                    state.classImmutability = MutableClass
                } else if (putValue.value.isArrayValue.isNoOrUnknown && putValue.value.isReferenceValue) {
                    putDefinitionSites.foreach { putDefinitionSite ⇒
                        if (putDefinitionSite < 0) {
                            state.nonConcreteObjectAssignments = true
                            if (putValue.asVar.value.isInstanceOf[ASObjectValue]) {
                                val oType = putValue.asVar.value.asInstanceOf[ASObjectValue].theUpperTypeBound
                                if (oType == field.fieldType.asObjectType ||
                                    oType.isSubtypeOf(field.fieldType.asObjectType)) {
                                    state.concreterTypeIsKnown = true
                                    handleTypeImmutability(oType)
                                }
                            }
                        } else {
                            val definitionSiteAssignment = taCode.stmts(putDefinitionSite).asAssignment
                            if (definitionSiteAssignment.expr.isNew) {
                                val newStmt = definitionSiteAssignment.expr.asNew
                                if (field.fieldType.isObjectType) {
                                    state.concreteClassTypeIsKnown = true
                                    handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                } else
                                    state.nonConcreteObjectAssignments = true
                            } else
                                state.nonConcreteObjectAssignments = true
                        }
                    }
                }
            }

            pcs.foreach { pc ⇒
                val index = taCode.pcToIndex(pc)
                if (index >= 0) {
                    val stmt = taCode.stmts(index)
                    analyzePutForConcreteObjectsOrMorePreciseClassType(stmt)
                }
            }
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {

            if (state.hasDependees) {
                val lowerBound =
                    if (state.referenceIsImmutable.isDefined && state.referenceIsImmutable.get)
                        NonTransitivelyImmutableField
                    else
                        MutableField
                val upperBound =
                    if (state.referenceIsImmutable.isEmpty) TransitivelyImmutableField
                    else {
                        state.referenceIsImmutable match {

                            case Some(false) ⇒ MutableField

                            case Some(true) | None ⇒
                                if (state.tacDependees.isEmpty && !state.concreteClassTypeIsKnown) {
                                    if (state.typeImmutability == TransitivelyImmutableType) {
                                        TransitivelyImmutableField
                                    } else if (state.typeImmutability == MutableType &&
                                        !state.fieldTypeIsDependentImmutable) {
                                        NonTransitivelyImmutableField
                                    } else {
                                        state.dependentImmutability match {
                                            case NotNonTransitivelyImmutableOrMutable ⇒
                                                DependentlyImmutableField(state.genericTypeParameters)
                                            case OnlyTransitivelyImmutable ⇒ TransitivelyImmutableField
                                            case _                         ⇒ NonTransitivelyImmutableField
                                        }
                                    }
                                } else
                                    TransitivelyImmutableField
                        }
                    }
                if (lowerBound == upperBound)
                    Result(field, lowerBound)
                else
                    InterimResult(
                        field,
                        lowerBound,
                        upperBound,
                        state.getDependees,
                        c
                    )
            } else {

                state.referenceIsImmutable match {

                    case Some(false) | None ⇒ Result(field, MutableField)

                    case Some(true) ⇒
                        if (!state.concreteClassTypeIsKnown && state.typeImmutability == TransitivelyImmutableType ||
                            state.concreteClassTypeIsKnown && !state.nonConcreteObjectAssignments &&
                            state.classImmutability == TransitivelyImmutableClass) {
                            Result(field, TransitivelyImmutableField)
                        } else {
                            if (state.fieldTypeIsDependentImmutable && field.fieldType == ObjectType.Object ||
                                !state.nonConcreteObjectAssignments &&
                                state.classImmutability.isDependentlyImmutable &&
                                state.concreteClassTypeIsKnown ||
                                state.typeImmutability.isDependentlyImmutable && !state.concreteClassTypeIsKnown) {
                                state.dependentImmutability match {
                                    case OnlyTransitivelyImmutable ⇒ Result(field, TransitivelyImmutableField)
                                    case NotNonTransitivelyImmutableOrMutable ⇒
                                        Result(field, DependentlyImmutableField(state.genericTypeParameters))
                                    case NotDependentlyImmutable ⇒ Result(field, NonTransitivelyImmutableField)
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
        def handleMutableType(tpe: ObjectType)(implicit state: State): Unit =
            if (state.concreteGenericTypes.contains(tpe))
                state.dependentImmutability = NotDependentlyImmutable

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

            if (eps.asEPS.pk != TACAI.key)
                state.dependees = state.dependees.filter(ep ⇒ (ep.e != eps.e) || (ep.pk != eps.pk))
            eps match {
                case LBP(TransitivelyImmutableType) ⇒ //nothing to do -> is default

                case FinalEP(t, DependentlyImmutableType(_)) ⇒

                    if (t.asInstanceOf[FieldType] == state.field.fieldType) {
                        state.fieldTypeIsDependentImmutable = true
                        state.typeImmutability = DependentlyImmutableType(Set.empty)
                    }
                    val tpe = t.asInstanceOf[ObjectType]
                    handleMutableType(tpe)

                case EUBP(t, MutableType | NonTransitivelyImmutableType) ⇒
                    if (t.asInstanceOf[FieldType] == state.field.fieldType)
                        state.typeImmutability = MutableType

                    val tpe = t.asInstanceOf[ObjectType]
                    handleMutableType(tpe)

                case UBP(Assignable | UnsafelyLazilyInitialized) ⇒
                    state.referenceIsImmutable = Some(false)
                    return Result(state.field, MutableField);

                case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) ⇒
                    state.referenceIsImmutable = Some(true)

                case epk if epk.asEPS.pk == TACAI.key ⇒
                    val newEP = epk.asInstanceOf[EOptionP[Method, TACAI]]
                    val method = newEP.e
                    val pcs = state.tacDependees(method)._2
                    state.tacDependees -= method
                    if (epk.isFinal) {
                        val tac = epk.asInstanceOf[FinalEP[Method, TACAI]].p.tac.get
                        searchForConcreteObjectInFieldWritesWithKnownTAC(pcs, tac)(state)
                    } else {
                        state.tacDependees += method -> ((newEP, pcs))
                    }

                case LBP(TransitivelyImmutableClass)                   ⇒ //nothing to do

                case UBP(MutableClass | NonTransitivelyImmutableClass) ⇒ state.classImmutability = MutableClass

                case FinalEP(t, DependentImmutableClass(_)) ⇒
                    state.classImmutability = DependentImmutableClass(Set.empty)
                    if (t.asInstanceOf[FieldType] == field.fieldType)
                        state.fieldTypeIsDependentImmutable = true

                case epk ⇒
                    state.dependees += epk
            }
            createResult
        }

        implicit val state: State = State(field)

        /**
         * Determines the immutability of the field reference and registers the dependees if necessary
         */
        propertyStore(field, FieldAssignability.key) match {

            case FinalP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) ⇒
                state.referenceIsImmutable = Some(true)

            case FinalP(Assignable | UnsafelyLazilyInitialized) ⇒
                return Result(field, MutableField);

            case ep ⇒ state.dependees += ep
        }

        /**
         * Determines whether the field is dependent immutable if the flag [[considerGenericity]] is set
         */
        if (considerGenericity)
            handleGenericity()
        else //The analysis is optimistic so the default value must be adapted in this case
            state.dependentImmutability = NotDependentlyImmutable

        /**
         * Determines whether the immutability information can be precised due to more precise class or type information
         */
        examineFieldWritesForConcretizationOfImmutability()

        /**
         * In cases where we know the concrete class type assigned to the field we could use the immutability of this.
         */

        if (!state.concreterTypeIsKnown) {
            if (!state.concreteClassTypeIsKnown)
                handleTypeImmutability(state.field.fieldType)
            else //The analysis is optimistic so the default value must be adapted in this case
                state.typeImmutability = MutableType
        }

        createResult()
    }
}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        FieldAccessInformationKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey
    )

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
        val fields = p.allFields // p.allProjectClassFiles.flatMap(classfile ⇒ classfile.fields) //p.allFields
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
