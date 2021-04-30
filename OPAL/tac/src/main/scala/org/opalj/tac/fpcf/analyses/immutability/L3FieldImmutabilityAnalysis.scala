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
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableType
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
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.PCs
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
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
import org.opalj.fpcf.InterimUBP
import org.opalj.br.fpcf.properties.ClassImmutability
import scala.collection.mutable
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.LBP
import org.opalj.fpcf.EUBP
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.br.fpcf.properties.Assignable
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.br.fpcf.properties.LazilyInitialized
import org.opalj.br.fpcf.properties.NonAssignable
import org.opalj.br.fpcf.properties.UnsafelyLazilyInitialized

/**
 * Analysis that determines the immutability of org.opalj.br.Field
 * @author Tobias Roth
 */
class L3FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    /**
     *  Describes the different kinds of dependent immutable fields, that depend on the concrete types that replace
     *  the generic parameters.
     *
     *  [[NotDependentImmutable]] Shallow or mutable types could still exist.
     *  Example: Foo<T, MutableClass> f
     *
     *  [[NotShallowOrMutable]] There are no shallow and no mutable types.
     *  Example: Foo<T,T> f
     *
     *  [[OnlyDeepImmutable]] There are only deep immutable types and no generic parameters left.
     *  Example: Foo<String, String> f
     */
    sealed trait DependentImmutabilityTypes
    case object NotDependentImmutable extends DependentImmutabilityTypes
    case object NotShallowOrMutable extends DependentImmutabilityTypes
    case object OnlyDeepImmutable extends DependentImmutabilityTypes

    sealed trait ImmutabilityTypes
    case object Mutable extends ImmutabilityTypes
    case object DependentImmutable extends ImmutabilityTypes
    case object DeepImmutable extends ImmutabilityTypes

    case class State(
            field:                             Field,
            var typeImmutability:              ImmutabilityTypes                                  = DeepImmutable,
            var classTypeImmutability:         ImmutabilityTypes                                  = DeepImmutable,
            var referenceIsImmutable:          Option[Boolean]                                    = None,
            var dependentImmutability:         DependentImmutabilityTypes                         = OnlyDeepImmutable,
            var tacDependees:                  Map[Method, (EOptionP[Method, TACAI], (PCs, PCs))] = Map.empty,
            var dependees:                     Set[EOptionP[Entity, Property]]                    = Set.empty,
            var innerArrayTypes:               Set[ObjectType]                                    = Set.empty,
            var concreteGenericTypes:          Set[ObjectType]                                    = Set.empty,
            var escapesStillDetermined:        Boolean                                            = false,
            var concreteClassTypeIsKnown:      Boolean                                            = false,
            var totalNumberOfFieldWrites:      Int                                                = 0,
            var fieldTypeIsDependentImmutable: Boolean                                            = false
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
            "org.opalj.fpcf.analyses.L3FieldImmutabilityAnalysis.considerGenericity"
        )

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineFieldImmutability(field)
        case _ ⇒
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

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
         * Determines the immutability of a field's type. Adjusts the state and registers the dependencies if necessary.
         */
        def handleTypeImmutability(state: State): Unit = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                state.typeImmutability = Mutable //false //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                // base types are by design deep immutable
                //state.typeImmutability = true // true is default
            } else if (objectType.isArrayType) {
                // Because the entries of an array can be reassigned we state it as not being deep immutable
                state.typeImmutability = Mutable //false
            } else {

                propertyStore(objectType, TypeImmutability.key) match {

                    case LBP(TransitivelyImmutableType) ⇒ // deep immutable type is set as default
                    case FinalEP(t, DependentImmutableType) ⇒
                        state.typeImmutability = DependentImmutable //false
                        if (t == field.fieldType)
                            state.fieldTypeIsDependentImmutable = true
                    case UBP(MutableType | NonTransitivelyImmutableType) ⇒
                        state.typeImmutability = Mutable
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
                    noRelevantAttributesFound = false
                    onlyDeepImmutableTypesInGenericTypeFound = false
                    state.fieldTypeIsDependentImmutable = true //generic type T is dependent immutable
                //  if (!isInClassesGenericTypeParameters(t))
                //      noShallowOrMutableTypesInGenericTypeFound = false

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) ⇒
                    noRelevantAttributesFound = false

                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(identifier)) ⇒
                            onlyDeepImmutableTypesInGenericTypeFound = false
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
                    case UBP(DependentImmutableType | NonTransitivelyImmutableType | MutableType) ⇒
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
                    state.dependentImmutability != NotDependentImmutable) {
                    state.dependentImmutability = NotShallowOrMutable
                } else {
                    state.dependentImmutability = NotDependentImmutable
                }
            } else {
                state.dependentImmutability = NotDependentImmutable
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
                    val (reads, writes) =
                        if (state.tacDependees.contains(method))
                            (state.tacDependees(method)._2._1, state.tacDependees(method)._2._2)
                        else
                            (IntTrieSet.empty, IntTrieSet.empty)
                    if (isRead) state.tacDependees += method -> ((epk, (reads ++ pcs, writes)))
                    if (!isRead) state.tacDependees += method -> ((epk, (reads, writes ++ pcs)))
                    None
            }
        }

        /**
         * Handles the influence of an escape property on the field mutability.
         * @return true if the object - on which a field write occurred - escapes, false otherwise.
         * @note (Re-)Adds dependees as necessary.
         */
        def doesEscapeViaMethod(
            ep: EOptionP[DefinitionSite, EscapeProperty]
        )(implicit state: State): Boolean = {
            ep match {
                case FinalP(NoEscape) ⇒ false

                case InterimUBP(NoEscape) ⇒
                    state.dependees += ep
                    false

                case UBP(EscapeInCallee | EscapeViaReturn) ⇒ true
                case FinalP(AtMost(_))                     ⇒ true
                case _: FinalEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return
                case InterimUBP(AtMost(_)) ⇒ true

                case epk ⇒
                    state.dependees += epk
                    false
            }
        }

        /**
         * Determines if the referenced object can escape either via field reads or writes.
         */
        def determineEscapeOfReferencedObjectOrValue()(implicit state: State): Unit = {
            //val tmp = state.noEscapePossibilityViaReference
            val writes = fieldAccessInformation.writeAccesses(state.field)

            //has to be determined before the following foreach loop because in this the information is still needed
            state.totalNumberOfFieldWrites = writes.foldLeft(0)(_ + _._2.size)

            writes.foreach { write ⇒
                val (method, pcs) = write
                val taCodeOption = getTACAI(method, pcs, isRead = false)
                if (taCodeOption.isDefined)
                    determineEscapeViaFieldWritesWithKnownTAC(method, pcs, taCodeOption.get)
            }
            //TODO state.noEscapePossibilityViaReference = true
        }

        /**
         * Determine if the referenced object can escape via field writes
         */
        def determineEscapeViaFieldWritesWithKnownTAC(
            method: Method,
            pcs:    PCs,
            taCode: TACode[TACMethodParameter, V]
        )(implicit state: State): Unit = {

            //Required because of cyclic calls of the inner functions - to prevent infinite cycles
            val seen: mutable.Set[Stmt[V]] = mutable.Set.empty

            /**
             * Checks if the parameters of a static function call are no parameters from an outer
             * function and are constants
             */
            /*def doesStaticFunctionCallEnableEscape(
                staticFunctionCall: StaticFunctionCall[V]
            ): Boolean =
                staticFunctionCall.params.exists(
                    _.asVar.definedBy.exists(
                        definedByIndex ⇒
                            definedByIndex < 0 ||
                                !taCode.stmts(definedByIndex).asAssignment.expr.isConst
                    )
                ) */

            /**
             * In case of the concrete assigned classtype is known this method handles the immutability of it.
             * @note [[state.concreteClassTypeIsKnown]] must be set to true, when calling this function
             */
            def handleKnownClassType(objectType: ObjectType): Unit = {

                propertyStore(objectType, ClassImmutability.key) match {
                    case LBP(TransitivelyImmutableClass) ⇒ //nothing to do ; true ist default

                    case FinalEP(t, DependentImmutableClass) ⇒
                        state.classTypeImmutability = DependentImmutable
                        if (t == field.fieldType)
                            state.fieldTypeIsDependentImmutable = true
                    case UBP(MutableClass | NonTransitivelyImmutableClass) ⇒ state.classTypeImmutability = Mutable

                    case eps ⇒
                        state.dependees += eps
                }
            }

            /**
             * Checks if the referenced object or elements from it can escape via the nonvirtualmethod-call
             */
           /* def doesNonVirtualMethodCallEnablesEscape(
                nonVirtualMethodCall: NonVirtualMethodCall[V]
            )(implicit state: State): Boolean =
                nonVirtualMethodCall.params.exists { param ⇒
                    param.asVar.definedBy.exists { paramDefinedByIndex ⇒
                        if (paramDefinedByIndex < 0) {
                            true
                        } else {
                            val paramDefinitionStmt = taCode.stmts(paramDefinedByIndex)
                            if (paramDefinitionStmt.isAssignment) {
                                val assignmentExpression = paramDefinitionStmt.asAssignment.expr
                                if (assignmentExpression.isGetField ||
                                    assignmentExpression.isGetStatic) {
                                    val assignedField: Option[Field] =
                                        if (assignmentExpression.isGetField)
                                            assignmentExpression.asGetField.resolveField
                                        else if (assignmentExpression.isGetStatic)
                                            assignmentExpression.asGetStatic.resolveField
                                        else
                                            throw new Error(s"$assignmentExpression is not an assigned field")

                                    if (assignedField.isDefined && assignedField.get != state.field) {

                                        propertyStore(assignedField.get, FieldImmutability.key) match {

                                            case FinalP(TransitivelyImmutableField) ⇒ false //nothing to do here
                                            case UBP(MutableField | NonTransitivelyImmutableField | DependentImmutableField) ⇒
                                                true
                                            case ep ⇒
                                                state.dependees += ep
                                                false
                                        }
                                    } else false
                                } else if (assignmentExpression.isVirtualFunctionCall) {
                                    val virtualFunctionCall = assignmentExpression.asVirtualFunctionCall
                                    virtualFunctionCall.params.exists(
                                        param ⇒
                                            param.asVar.definedBy.head < 0 ||
                                                !taCode.stmts(param.asVar.definedBy.head).asAssignment.expr.isConst
                                    )
                                } else if (assignmentExpression.isStaticFunctionCall) {
                                    doesStaticFunctionCallEnableEscape(assignmentExpression.asStaticFunctionCall)
                                } else if (assignmentExpression.isNew) {
                                    val newStmt = assignmentExpression.asNew
                                    if (field.fieldType.isObjectType &&
                                        newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType /*&&
                                        state.totalNumberOfFieldWrites == 1*/ ) {
                                        state.concreteClassTypeIsKnown = true
                                        handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                    }
                                    /*
                                    paramDefinitionStmt.asAssignment.targetVar.asVar.usedBy.exists { usedSiteIndex ⇒
                                        val stmt = taCode.stmts(usedSiteIndex)
                                        if (stmt.isNonVirtualMethodCall) {
                                            if (!seen.contains(stmt)) {
                                                seen += stmt
                                                doesNonVirtualMethodCallEnablesEscape(stmt.asNonVirtualMethodCall)
                                            } else false
                                        } else true
                                    } */
                                    true
                                } else
                                    assignmentExpression.isConst
                            } else {
                                val definitionSitesOfParam = definitionSites(method, paramDefinedByIndex)
                                val stmt = taCode.stmts(taCode.pcToIndex(definitionSitesOfParam.pc))
                                if (stmt.isNonVirtualMethodCall) {
                                    if (!seen.contains(stmt)) {
                                        seen += stmt
                                        doesNonVirtualMethodCallEnablesEscape(stmt.asNonVirtualMethodCall)
                                    } else false
                                } else if (stmt.isPutField || stmt.isPutStatic) {
                                    /// if (!seen.contains(stmt)) {
                                        ///    seen += stmt
                                        ///      doesPutEnableEscape(stmt)
                                    ///  } else false
                                } else if (stmt.isArrayStore) {
                                    ///  !stmt.asArrayStore.value.asVar.isConst
                                    // true //TODO handling that case more precise
                                } else { // other cases that the purity analysis can not handle
                                  ///  doesEscapeViaMethod(propertyStore(definitionSitesOfParam, EscapeProperty.key))
                                }
                            } //TODO go further
                        }
                    }
                } */

            /**
             * Checks if a reference object can escape via a given putfield or putstatic
             */
            def doesPutEnableEscape(putStmt: Stmt[V]): Boolean = {

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

               /* if (putValue.value.isArrayValue.isYes) {
                    if (putValue.definedBy.forall(_ >= 0)) { //necessary
                        putValue.definedBy.exists { putValueDefinedByIndex ⇒
                            taCode.stmts(putValueDefinedByIndex).asAssignment.targetVar.usedBy.exists {
                                usedByIndex ⇒
                                    val arrayStmt = taCode.stmts(usedByIndex)
                                    if (arrayStmt != putStmt) {
                                        if (arrayStmt.isArrayStore) {

                                            val arrayStore = arrayStmt.asArrayStore
                                            val arrayStoreIndex = arrayStore.index
                                            val isArrayIndexConst =
                                                taCode.stmts(arrayStoreIndex.asVar.definedBy.head).asAssignment.expr.isConst
                                            val assignedValue = arrayStore.value
                                            if (assignedValue.asVar.definedBy.size == 1 &&
                                                assignedValue.asVar.definedBy.head >= 0) {
                                                val valueAssignment =
                                                    taCode.stmts(assignedValue.asVar.definedBy.head).asAssignment
                                                val assignedExpr = valueAssignment.expr
                                                val useSites = valueAssignment.targetVar.usedBy.map(taCode.stmts(_))
                                                if (assignedExpr.isConst)
                                                    false
                                                else if (useSites.exists { useSite ⇒
                                                    if (useSite.isNonVirtualMethodCall) {
                                                        val nonVirtualMethodCall = useSite.asNonVirtualMethodCall
                                                        nonVirtualMethodCall.params.exists(
                                                            param ⇒
                                                                !param.isConst && param.asVar.definedBy
                                                                    .forall(
                                                                        i ⇒ i >= 0 && taCode.stmts(i).asAssignment.expr.isConst
                                                                    )
                                                        )
                                                    } else if (useSite == arrayStore) {
                                                        false
                                                    } else if (useSite.isReturnValue) {
                                                        //assigned array-element escapes
                                                        true
                                                    } else {
                                                        true //to be sound
                                                    }
                                                })
                                                    true
                                                else if (!isArrayIndexConst) {
                                                    true
                                                } else if (assignedExpr.isStaticFunctionCall) {
                                                    doesStaticFunctionCallEnableEscape(assignedExpr.asStaticFunctionCall)
                                                } else if (assignedExpr.isNew) {
                                                    val newStmt = assignedExpr.asNew

                                                    if (field.fieldType.isObjectType &&
                                                        newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType &&
                                                        state.totalNumberOfFieldWrites == 1) {
                                                        state.concreteClassTypeIsKnown = true
                                                        handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                                    }

                                                    /*valueAssignment.targetVar.asVar.usedBy.exists { index ⇒
                                                        val tmpStmt = taCode.stmts(index)
                                                        if (tmpStmt.isArrayStore) {
                                                            false // can be ingored
                                                        } else if (tmpStmt.isNonVirtualMethodCall) {
                                                            val nonVirtualMethodcall = tmpStmt.asNonVirtualMethodCall
                     the generic parameters.                                       if (!seen.contains(tmpStmt)) {
                                                                seen += tmpStmt
                                                                doesNonVirtualMethodCallEnablesEscape(nonVirtualMethodcall)
                                                            }
                                                            false //nothing to do in the else case. Stmt has still been handled
                                                        } else true
                                                    } */ true
                                                } else true
                                            } else true
                                        } else true
                                    } else false
                            }
                        }
                    } else true
                }*/
            if(!putValue.value.isArrayValue.isYes) {

                    putDefinitionSites.exists { putDefinitionSite ⇒
                        if (putDefinitionSite >= 0) { //necessary
                            val definitionSiteStatement = taCode.stmts(putDefinitionSite)
                            val definitionSiteAssignment = definitionSiteStatement.asAssignment
                            if (definitionSiteAssignment.expr.isStaticFunctionCall) {
                                ///doesStaticFunctionCallEnableEscape(
                                ///    definitionSiteAssignment.expr.asStaticFunctionCall
                                ///)

                            } else if (definitionSiteAssignment.expr.isVar) {
                              ///  val definitionSiteVar = definitionSiteAssignment.expr.asVar
                              ///  definitionSiteVar.usedBy.exists { definitionSiteVarUseSite ⇒
                                ///     val definitionSiteVarUseSiteStmt = taCode.stmts(definitionSiteVarUseSite)
                              ///    if (definitionSiteVarUseSiteStmt.isNonVirtualMethodCall) {
                                ///         val nonVirtualMethodCall = definitionSiteVarUseSiteStmt.asNonVirtualMethodCall
                                ///        doesNonVirtualMethodCallEnablesEscape(nonVirtualMethodCall)
                                ///     } else true
                                ///    //TODO handle all cases
                                ///}
                            } else if (definitionSiteAssignment.expr.isNew) {

                                val newStmt = definitionSiteAssignment.expr.asNew
                                if (field.fieldType.isObjectType &&
                                    newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType /*&&
                                    state.totalNumberOfFieldWrites == 1*/ ) {
                                    state.concreteClassTypeIsKnown = true
                                    handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                }
                                if (!method.isConstructor && !method.isStaticInitializer) {
                                    definitionSiteAssignment.targetVar.asVar.usedBy.exists { x ⇒
                                        val tmpStmt = taCode.stmts(x)
                                        if (tmpStmt.isPutStatic || tmpStmt.isPutField) {
                                            if (!seen.contains(tmpStmt)) {
                                                seen += tmpStmt
                                                doesPutEnableEscape(tmpStmt)
                                            }
                                        }

                                        /// else if (tmpStmt.isNonVirtualMethodCall) {
                                        ///     if (!seen.contains(tmpStmt)) {
                                        ///        seen += tmpStmt
                                        ///        doesNonVirtualMethodCallEnablesEscape(tmpStmt.asNonVirtualMethodCall)
                                        ///    } else false
                                        ///} else true
                                    }
                                } else { //TODO check
                                    definitionSiteAssignment.targetVar.usedBy.exists { useSite ⇒
                                        val useSiteStmt = taCode.stmts(useSite)
                                        if (useSiteStmt.isNonVirtualMethodCall) {
                                            ///   doesNonVirtualMethodCallEnablesEscape(useSiteStmt.asNonVirtualMethodCall)
                                        } else if (useSiteStmt.isPutStatic || useSiteStmt.isPutField) {
                                            if (!seen.contains(useSiteStmt)) {
                                                seen += useSiteStmt
                                                doesPutEnableEscape(useSiteStmt)
                                            } else false
                                        }

                                        /// else if (useSiteStmt.isAssignment) {
                                        ///     true //conservative handling to reduce complexity
                                        ///} else {
                                        ///    true
                                        ///}
                                    }
                                }
                                //TODO handle all cases
                            } else !definitionSiteAssignment.expr.isConst
                        } else true
                    }
                }
            }

            //if (
            pcs.exists { pc ⇒
                val index = taCode.pcToIndex(pc)
                if (index >= 0) {
                    val stmt = taCode.stmts(index)
                    if (!seen.contains(stmt)) {
                        seen += stmt
                        doesPutEnableEscape(stmt)
                    } else false
                } else true
            } //)
            //state.noEscapePossibilityViaReference = false
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {

            // if (state.noEscapePossibilityViaReference && state.escapesStillDetermined && !state.field.isPrivate)
            //     state.noEscapePossibilityViaReference = false

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
                                    if (state.typeImmutability == DeepImmutable /*||
                                        state.noEscapePossibilityViaReference*/ ) {
                                        TransitivelyImmutableField
                                    } else if (state.typeImmutability == Mutable &&
                                        !state.fieldTypeIsDependentImmutable) {
                                        NonTransitivelyImmutableField
                                    } else {
                                        state.dependentImmutability match {
                                            case NotShallowOrMutable ⇒ DependentImmutableField
                                            case OnlyDeepImmutable   ⇒ TransitivelyImmutableField
                                            case _                   ⇒ NonTransitivelyImmutableField
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
                /////  if (!state.escapesStillDetermined)
                ////      state.noEscapePossibilityViaReference = false

                state.referenceIsImmutable match {

                    case Some(false) | None ⇒ Result(field, MutableField)

                    case Some(true) ⇒
                        if (!state.concreteClassTypeIsKnown && state.typeImmutability == DeepImmutable ||
                            state.concreteClassTypeIsKnown && state.classTypeImmutability == DeepImmutable) {
                            Result(field, TransitivelyImmutableField)
                        } else {
                            /*if (state.noEscapePossibilityViaReference) {
                                Result(field, DeepImmutableField)
                            } else */ {
                                if (state.fieldTypeIsDependentImmutable && field.fieldType == ObjectType.Object ||
                                    state.classTypeImmutability == DependentImmutable ||
                                    state.typeImmutability == DependentImmutable) {
                                    state.dependentImmutability match {
                                        case OnlyDeepImmutable     ⇒ Result(field, TransitivelyImmutableField)
                                        case NotShallowOrMutable   ⇒ Result(field, DependentImmutableField)
                                        case NotDependentImmutable ⇒ Result(field, NonTransitivelyImmutableField)
                                    }
                                } else {
                                    Result(field, NonTransitivelyImmutableField)
                                }
                            }
                        }
                }
            }
        }

        def handleMutableType(tpe: ObjectType)(implicit state: State): Unit = {
            //if (state.innerArrayTypes.contains(tpe))
            //    state.noEscapePossibilityViaReference = false

            if (state.concreteGenericTypes.contains(tpe)) {
                state.dependentImmutability = NotDependentImmutable
            }
        }

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
            import org.opalj.br.fpcf.properties.{Assignable, EffectivelyNonAssignable, LazilyInitialized, NonAssignable, UnsafelyLazilyInitialized}

            if (eps.asEPS.pk != TACAI.key)
                state.dependees = state.dependees.filter(ep ⇒ (ep.e != eps.e) || (ep.pk != eps.pk))
            eps match {
                case LBP(TransitivelyImmutableType) ⇒ //nothing to do -> is default

                case FinalEP(t, DependentImmutableType) ⇒

                    if (t.asInstanceOf[FieldType] == state.field.fieldType) {
                        state.fieldTypeIsDependentImmutable = true
                        state.typeImmutability = DependentImmutable
                    }
                    val tpe = t.asInstanceOf[ObjectType]
                    handleMutableType(tpe)

                case EUBP(t, MutableType | NonTransitivelyImmutableType) ⇒
                    import org.opalj.br.FieldType
                    if (t.asInstanceOf[FieldType] == state.field.fieldType)
                        state.typeImmutability = Mutable

                    val tpe = t.asInstanceOf[ObjectType]
                    handleMutableType(tpe)

                case UBP(Assignable | UnsafelyLazilyInitialized) ⇒
                    state.referenceIsImmutable = Some(false)
                    return Result(state.field, MutableField);

                case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) ⇒
                    state.referenceIsImmutable = Some(true)

                case LBP(TransitivelyImmutableField)                                             ⇒ // nothing to do

                case UBP(DependentImmutableField | NonTransitivelyImmutableField | MutableField) ⇒
                //state.noEscapePossibilityViaReference = false

                case epk if epk.asEPS.pk == TACAI.key ⇒
                    val newEP = epk.asInstanceOf[EOptionP[Method, TACAI]]
                    val method = newEP.e
                    val pcs = state.tacDependees(method)._2
                    state.tacDependees -= method
                    if (epk.isFinal) {
                        val tac = epk.asInstanceOf[FinalEP[Method, TACAI]].p.tac.get
                        determineEscapeViaFieldWritesWithKnownTAC(method, pcs._2, tac)(state)
                        //determineEscapeViaFieldReadsWithKnownTAC(method, pcs._1, tac)(state)
                    } else {
                        state.tacDependees += method -> ((newEP, pcs))
                    }

                //case epk if epk.isFinal && epk.asEPS.pk == EscapeProperty.key ⇒
                //    if (doesEscapeViaMethod(eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]))
                //        state.noEscapePossibilityViaReference = false

                case LBP(TransitivelyImmutableClass)                   ⇒ //nothing to do

                case UBP(MutableClass | NonTransitivelyImmutableClass) ⇒ state.classTypeImmutability = Mutable

                case FinalEP(t, DependentImmutableClass) ⇒
                    import org.opalj.br.FieldType
                    state.classTypeImmutability = DependentImmutable
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

            case FinalP(Assignable | UnsafelyLazilyInitialized) ⇒ return Result(field, MutableField);

            case ep                                             ⇒ state.dependees += ep
        }

        /**
         * Determines whether the field is dependent immutable if the flag [[considerGenericity]] is set
         */
        if (considerGenericity)
            handleGenericity()
        else {
            state.dependentImmutability = NotDependentImmutable
        }

        /**
         * Determines whether the reference object escapes or can be mutated if the flag [[considerEscape]] is set
         */
        // if (considerEscape) {
        //     state.noEscapePossibilityViaReference = true //state.field.isPrivate
        if (state.referenceIsImmutable.isEmpty || state.referenceIsImmutable.get)
            determineEscapeOfReferencedObjectOrValue()
        //   if (state.escapesStillDetermined && !state.field.isPrivate)
        //       state.noEscapePossibilityViaReference = false

        /*} else {
            state.noEscapePossibilityViaReference = false
            state.concreteClassTypeIsKnown = false
            state.escapesStillDetermined = true
        } */

        /**
         * In cases where we know the concrete class type assigned to the field we could use the immutability of this.
         */
        if (!state.concreteClassTypeIsKnown)
            handleTypeImmutability(state)
        else {
            state.typeImmutability = Mutable
        }

        createResult()
    }
}

trait L3FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

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
object EagerL3FieldImmutabilityAnalysis
    extends L3FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L3FieldImmutabilityAnalysis(p)
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
object LazyL3FieldImmutabilityAnalysis
    extends L3FieldImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L3FieldImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldImmutability.key,
            analysis.determineFieldImmutability
        )
        analysis
    }
}
