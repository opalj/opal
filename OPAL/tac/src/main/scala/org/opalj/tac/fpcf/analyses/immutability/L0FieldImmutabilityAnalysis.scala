/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.immutability

import org.opalj.br.Attribute
import org.opalj.br.ClassSignature
import org.opalj.br.ClassTypeSignature
import org.opalj.br.Field
import org.opalj.br.FormalTypeParameter
import org.opalj.br.ObjectType
import org.opalj.br.ProperTypeArgument
import org.opalj.br.RuntimeInvisibleAnnotationTable
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.SourceFile
import org.opalj.br.TypeVariableSignature
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.br.fpcf.properties.TypeImmutability_new
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.immutability.DependentImmutabilityKind.DependentImmutabilityKind
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.fpcf.InterimUBP
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.Stmt
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.tac.StaticFunctionCall
import org.opalj.Yes
import org.opalj.tac.Expr
import org.opalj.value.ASArrayValue

case class State(f: Field) {
    var field: Field = f
    var typeIsImmutable: Option[Boolean] = Some(true)
    var referenceIsImmutable: Option[Boolean] = None
    var noEscapePossibilityViaReference: Boolean = true
    var dependentImmutability: Option[DependentImmutabilityKind] = Some(DependentImmutabilityKind.onlyDeepImmutable)
    var genericTypeSetNotDeepImmutable = false
    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty
}

/**
 *  Describes the different kinds of dependent immutable fields:
 *  [[DependentImmutabilityKind.dependent]] Shallow or mutable types could still exist
 *  [[DependentImmutabilityKind.notShallowOrMutable]] There are no shallow or mutable types
 *  [[DependentImmutabilityKind.onlyDeepImmutable]] There are no generic parameters left.
 *  All have been replaced with deep immutable types.
 */
object DependentImmutabilityKind extends Enumeration {
    type DependentImmutabilityKind = Value
    val notShallowOrMutable, dependent, onlyDeepImmutable = Value
}

/**
 * Analyses that determines the immutability of org.opalj.br.Field
 * The information of the reference of the field from the [[L0ReferenceImmutabilityAnalysis]]
 * and the information of the type immutability determined by the [[LxTypeImmutabilityAnalysis_new]]
 *
 * @author Tobias Peter Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒
                determineFieldImmutability(field)
            case _ ⇒
                val m = s"""${entity.getClass.getName} is not an org.opalj.br.Field"""
                throw new IllegalArgumentException(m)
        }
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

        //stores the formal type parameters of the fields class or outer class
        var classFormalTypeParameters: Option[Set[String]] = None

        /**
         * Loads the formal typeparameters from the classes and outer class signature
         */
        def loadFormalTypeParameter(): Unit = {
            var result: Set[String] = Set.empty

            /**
             *
             * Extract the formal type parameter if it exists of a class attribute
             */
            def collectFormalTypeParameterFromClassAttribute: Attribute ⇒ Unit = {
                attribute ⇒
                    attribute match {
                        case ClassSignature(typeParameters, _, _) ⇒ {
                            typeParameters.foreach(
                                _ match {
                                    case FormalTypeParameter(identifier, _, _) ⇒ result += identifier
                                    case _                                     ⇒
                                }
                            )
                        }
                        case _ ⇒
                    }
            }

            /**
             * If the genericity is nested in an inner class
             * collect the generic type parameters from the fields outer class
             */
            if (field.classFile.outerType.isDefined) {
                val outerClassFile = project.classFile(field.classFile.outerType.get._1)
                if (outerClassFile.isDefined) {
                    outerClassFile.get.attributes.foreach(
                        collectFormalTypeParameterFromClassAttribute
                    )
                }
            }

            /**
             * Collect the generic type parameters from the fields class
             */
            field.classFile.attributes.foreach(collectFormalTypeParameterFromClassAttribute)

            if (result.nonEmpty)
                classFormalTypeParameters = Some(result)

        }
        /**
         * Returns, if a generic parameter like e.g. 'T' is in the classes or the first outer classes Signature
         * @param string The generic type parameter that should be looked for
         */
        def isInClassesGenericTypeParameters(string: String): Boolean =
            classFormalTypeParameters.isDefined && classFormalTypeParameters.get.contains(string)

        /**
         * Checks the immutability of a fields type. Returns the Result and registers the dependencies if necessary.
         * @param state
         */
        def handleTypeImmutability()(implicit state: State): Unit = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                state.typeIsImmutable = Some(false) //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                // we state here the strings deep immutability
                // base types are by design deep immutable
                //state.typeImmutability = Some(true) // true is default
            } else if (objectType.isArrayType) {
                // Because the entries of an array can be reassigned we state it as not being deep immutable
                state.typeIsImmutable = Some(false)
            } else {
                propertyStore(objectType, TypeImmutability_new.key) match {
                    case FinalP(DeepImmutableType) ⇒ // deep immutable type is set as default
                    case FinalP(DependentImmutableType) ⇒
                        state.typeIsImmutable = Some(false)
                    /*state.dependentImmutability =
                            Some(DependentImmutabilityKind.dependent)*/
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒
                        state.typeIsImmutable = Some(false)
                        state.dependentImmutability = None
                    case epk ⇒ state.dependencies += epk
                }
            }
        }

        def hasGenericType()(implicit state: State): Unit = {
            var noShallowOrMutableTypesInGenericTypeFound = true
            var onlyDeepImmutableTypesInGenericTypeFound = true
            var genericParameters: List[ObjectType] = List()
            var noRelevantAttributesFound = true
            state.field.asField.attributes.foreach(
                _ match {
                    case RuntimeInvisibleAnnotationTable(_) ⇒
                    case SourceFile(_)                      ⇒
                    case TypeVariableSignature(t) ⇒
                        noRelevantAttributesFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        if (!isInClassesGenericTypeParameters(t))
                            noShallowOrMutableTypesInGenericTypeFound = false

                    //state.typeIsImmutable = Some(false)
                    case ClassTypeSignature(
                        packageIdentifier,
                        SimpleClassTypeSignature(simpleName, typeArguments),
                        _
                        ) ⇒
                        //state.typeIsImmutable = Some(false)
                        noRelevantAttributesFound = false
                        typeArguments
                            .foreach({
                                _ match {
                                    case ProperTypeArgument(variance, TypeVariableSignature(identifier: String)) ⇒
                                        onlyDeepImmutableTypesInGenericTypeFound = false
                                        if (!isInClassesGenericTypeParameters(identifier))
                                            noShallowOrMutableTypesInGenericTypeFound = false
                                    case ProperTypeArgument(varianceIndicator,
                                        ClassTypeSignature(outerPackageIdentifier,
                                            SimpleClassTypeSignature(innerPackageIdentifier, typeArguments2), _)) ⇒ {
                                        val objectPath =
                                            outerPackageIdentifier match {
                                                case Some(prepackageIdentifier) ⇒
                                                    prepackageIdentifier + innerPackageIdentifier
                                                case _ ⇒ innerPackageIdentifier
                                            }
                                        genericParameters ::= ObjectType(objectPath)
                                    }
                                    case _ ⇒
                                        noShallowOrMutableTypesInGenericTypeFound = false
                                        onlyDeepImmutableTypesInGenericTypeFound = false
                                }
                            })
                    case _ ⇒
                        //state.typeIsImmutable = Some(false)
                        noShallowOrMutableTypesInGenericTypeFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                }
            )
            genericParameters.foreach(objectType ⇒ {
                propertyStore(objectType, TypeImmutability_new.key) match {
                    case FinalP(DeepImmutableType) ⇒ //nothing to do here: default value is deep immutable
                    case FinalP(DependentImmutableType) ⇒
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        state.typeIsImmutable = Some(false)
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        noShallowOrMutableTypesInGenericTypeFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        state.typeIsImmutable = Some(false)
                    }
                    case ep ⇒
                        state.dependencies += ep
                }
            })
            //Prevents the case of keeping the default values of these
            // flags only because of no relevant attribute has been found
            if (!noRelevantAttributesFound) {
                /**
                 * The above defined functions are called
                 */
                if (state.dependentImmutability.isDefined) {
                    if (onlyDeepImmutableTypesInGenericTypeFound) {
                        //nothing to do...
                        //state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
                    } else if (noShallowOrMutableTypesInGenericTypeFound)
                        state.dependentImmutability = Some(DependentImmutabilityKind.notShallowOrMutable)
                    else state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                }
            } else state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
        }

        /**
         * Returns the TAC to a given method when it still exists.
         * Otherwise collect dependencies.
         */
        def getTACAI(
            method: Method
        )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
            propertyStore(method, TACAI.key) match {
                case finalEP: FinalEP[Method, TACAI] ⇒
                    finalEP.ub.tac
                case epk ⇒
                    state.dependencies += epk
                    None
            }
        }

        /**
         * Handles the influence of an escape property on the field mutability.
         * @return true if the object - on which a field write occurred - escapes, false otherwise.
         * @note (Re-)Adds dependees as necessary.
         */
        def handleEscapeProperty(
            ep: EOptionP[DefinitionSite, EscapeProperty]
        )(implicit state: State): Boolean = {
            ep match {
                case FinalP(NoEscape) ⇒ false
                case InterimUBP(NoEscape) ⇒
                    false
                case FinalP(EscapeInCallee | EscapeViaReturn)   ⇒ true
                case FinalP(AtMost(_))                          ⇒ true
                case _: FinalEP[DefinitionSite, EscapeProperty] ⇒ true // Escape state is worse than via return
                case InterimUBP(AtMost(_)) ⇒
                    true
                case _: InterimEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return
                case _ ⇒
                    state.dependencies += ep
                    false
            }
        }

        /**
         * Determine if the referenced object can escape either via field reads or writes.
         */
        def determineEscapePossibilityOfReferencedObjectOrValue(implicit state: State): Unit = {

            /**
             * Determine if the referenced object can escape via field reads.
             */
            def determineEscapePossibilityViaFieldReads(implicit state: State): Unit = {
                for {
                    (method, pcs) ← fieldAccessInformation.readAccesses(state.field)
                    taCode ← getTACAI(method)
                    pc ← pcs
                } {
                    val readIndex = taCode.pcToIndex(pc)
                    // This if-statement is necessary, because there are -1 elements in the array
                    //TODO verify if this is a bug

                    //This if-statement is necessary because readAccesses can also be simple Expression-statements
                    if (readIndex != -1) {
                        val stmt = taCode.stmts(readIndex)
                        if (stmt.isAssignment) {
                            val assignment = stmt.asAssignment
                            if (handleEscapeProperty(
                                propertyStore(definitionSites(method, assignment.pc), EscapeProperty.key)
                            )) {
                                state.noEscapePossibilityViaReference = false
                            } else for {
                                useSite ← assignment.targetVar.usedBy
                            } {
                                val fieldsUseSiteStmt = taCode.stmts(useSite)
                                if (fieldsUseSiteStmt.isAssignment) {
                                    val assignment = fieldsUseSiteStmt.asAssignment
                                    if (assignment.expr.isArrayLoad) {
                                        val arrayLoad = assignment.expr.asArrayLoad
                                        arrayLoad.arrayRef.asVar.value.toCanonicalForm match {
                                            case value: ASArrayValue ⇒
                                                val innerArrayType = value.theUpperTypeBound.componentType
                                                if (innerArrayType.isBaseType) {
                                                    // nothing to do, because it can not be mutated
                                                } else if (innerArrayType.isArrayType) {
                                                    state.noEscapePossibilityViaReference = false // to be sound
                                                } else if (innerArrayType.isObjectType) {
                                                    //If a deep immutable object escapes, it can not be mutated
                                                    propertyStore(innerArrayType, TypeImmutability_new.key) match {
                                                        case FinalP(DeepImmutableType) ⇒ //nothing to to
                                                        case FinalP(_) ⇒
                                                            state.noEscapePossibilityViaReference = false
                                                        case ep ⇒
                                                            state.dependencies += ep
                                                    }
                                                } else state.noEscapePossibilityViaReference
                                            case _ ⇒ state.noEscapePossibilityViaReference = false
                                        }
                                    } else state.noEscapePossibilityViaReference = false
                                } else if (fieldsUseSiteStmt.isMonitorEnter ||
                                    fieldsUseSiteStmt.isMonitorExit ||
                                    fieldsUseSiteStmt.isIf) {
                                    //nothing to do
                                } else {
                                    state.noEscapePossibilityViaReference = false
                                }
                            }
                        } else if (stmt.isExprStmt) {
                            //nothing to do here, because the value is only read but not assigned to another one
                        } else {
                            state.noEscapePossibilityViaReference = false
                        }
                    } else {
                        //nothing to do
                        // -1 means nothing
                        //state.noEscapePossibilityViaReference = false
                    }
                }
            }

            /**
             * Determine if the referenced object can escape via field writes
             */
            def checkFieldWritesForEffImmutability(field: Field)(implicit state: State): Unit = {
                //Needed because of cyclic calls of the functions - to prevent infinite cycles
                var seen: Set[Stmt[V]] = Set.empty

                /**
                 * Checks if the parameters of a static function call are no parameters from an outer
                 * function and are constants
                 */
                def handleStaticFunctionCall(
                    staticFunctionCall: StaticFunctionCall[V],
                    tacCode:            TACode[TACMethodParameter, V]
                ): Unit = {
                    if (staticFunctionCall.params.exists(p ⇒
                        p.asVar.definedBy.size != 1 ||
                            p.asVar.definedBy.head < 0 ||
                            !tacCode.stmts(p.asVar.definedBy.head).asAssignment.expr.isConst)) {
                        state.noEscapePossibilityViaReference = false
                    }
                }

                /**
                 * Checks if the referenced object or elements from it can escape via the nonvirtualmethod-call
                 */
                def handleNonVirtualMethodCall(
                    method:               Method,
                    nonVirtualMethodCall: NonVirtualMethodCall[V],
                    tacCode:              TACode[TACMethodParameter, V]
                ): Unit = {
                    nonVirtualMethodCall.params.foreach(
                        param ⇒ {
                            param.asVar.definedBy.foreach(
                                paramDefinedByIndex ⇒
                                    if (paramDefinedByIndex < 0) {
                                        state.noEscapePossibilityViaReference = false
                                    } else {
                                        val paramDefinitionStmt = tacCode.stmts(paramDefinedByIndex)
                                        if (paramDefinitionStmt.isAssignment) {
                                            val assignmentExpression = paramDefinitionStmt.asAssignment.expr
                                            if (assignmentExpression.isGetField ||
                                                assignmentExpression.isGetStatic) {
                                                var field: Option[Field] = None
                                                if (assignmentExpression.isGetField)
                                                    field = assignmentExpression.asGetField.resolveField
                                                else if (assignmentExpression.isGetStatic)
                                                    field = assignmentExpression.asGetStatic.resolveField
                                                propertyStore(field.get, FieldImmutability.key) match {
                                                    case FinalP(DeepImmutableField) ⇒ //nothing to do here
                                                    case FinalP(_) ⇒
                                                        state.noEscapePossibilityViaReference = false
                                                    case ep ⇒ state.dependencies += ep
                                                }
                                            } else if (assignmentExpression.isVirtualFunctionCall) {
                                                val virtualFunctionCall = assignmentExpression.asVirtualFunctionCall
                                                virtualFunctionCall.params.exists(
                                                    param ⇒ param.asVar.definedBy.head < 0 ||
                                                        !tacCode.stmts(param.asVar.definedBy.head).asAssignment.expr.isConst
                                                )
                                            } else if (assignmentExpression.isStaticFunctionCall) {
                                                handleStaticFunctionCall(assignmentExpression.asStaticFunctionCall, tacCode)
                                            } else if (assignmentExpression.isNew) {
                                                for (usedSiteIndex ← paramDefinitionStmt.asAssignment.targetVar.asVar.usedBy) {
                                                    val stmt = tacCode.stmts(usedSiteIndex)
                                                    if (stmt.isNonVirtualMethodCall) {
                                                        if (!seen.contains(stmt)) {
                                                            seen += stmt
                                                            handleNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                        }
                                                    } else {
                                                        state.noEscapePossibilityViaReference = false
                                                    }
                                                }
                                            } else if (assignmentExpression.isConst) {
                                                //nothing to do
                                            } else {
                                                state.noEscapePossibilityViaReference = false
                                            }
                                        } else {
                                            val definitionSitesOfParam = definitionSites(method, paramDefinedByIndex)
                                            val stmt = tacCode.stmts(tacCode.pcToIndex(definitionSitesOfParam.pc))
                                            if (stmt.isNonVirtualMethodCall) {
                                                if (!seen.contains(stmt)) {
                                                    seen += stmt
                                                    handleNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                }
                                            } else if (stmt.isPutField || stmt.isPutStatic) {
                                                if (!seen.contains(stmt)) {
                                                    seen += stmt
                                                    handlePut(stmt, method, tacCode)
                                                }
                                            } else if (stmt.isArrayStore) {
                                                state.noEscapePossibilityViaReference = false //TODO handling that case more precise
                                            } //else if // other cases that the purity analysis can not handle
                                            else {
                                                if (handleEscapeProperty(
                                                    propertyStore(definitionSitesOfParam, EscapeProperty.key)
                                                )) {
                                                    state.noEscapePossibilityViaReference = false
                                                }
                                            }
                                        } //TODO go further
                                    }
                            )
                        }
                    )
                }

                /**
                 * Checks if a reference object can escape via a given putfield or putstatic
                 */
                def handlePut(putStmt: Stmt[V], method: Method, tacCode: TACode[TACMethodParameter, V]): Unit = {
                    var putDefinitionSites: IntTrieSet = IntTrieSet.empty
                    var putValue: Expr[V] = null
                    if (putStmt.isPutField) {
                        val putField = putStmt.asPutField
                        putDefinitionSites = putField.value.asVar.definedBy
                        putValue = putField.value
                    } else if (putStmt.isPutStatic) {
                        val putStatic = putStmt.asPutStatic
                        putDefinitionSites = putStatic.value.asVar.definedBy
                        putValue = putStatic.value
                    } else {
                        state.noEscapePossibilityViaReference = false
                    }
                    val putValueDefinedByIndex = putValue.asVar.definedBy.head
                    if (putValue.asVar.value.isArrayValue == Yes) {

                        if (putValueDefinedByIndex >= 0) { //necessary
                            tacCode.stmts(putValueDefinedByIndex).asAssignment.targetVar.usedBy.foreach(x ⇒ {
                                val arrayStmt = tacCode.stmts(x)
                                if (arrayStmt != putStmt)
                                    if (arrayStmt.isArrayStore) {
                                        val arrayStore = arrayStmt.asArrayStore
                                        val arrayStoreIndex = arrayStore.index
                                        val isArrayIndexConst =
                                            tacCode.stmts(arrayStoreIndex.asVar.definedBy.head).asAssignment.expr.isConst
                                        val assignedValue = arrayStore.value
                                        if (assignedValue.asVar.definedBy.head >= 0) {
                                            val valueAssignment = tacCode.stmts(assignedValue.asVar.definedBy.head).asAssignment
                                            val assignedExpr = valueAssignment.expr
                                            val useSites = valueAssignment.targetVar.usedBy.map(tacCode.stmts(_))
                                            for (useSite ← useSites) {
                                                if (useSite.isNonVirtualMethodCall) {
                                                    val nonVirtualMethodCall = useSite.asNonVirtualMethodCall
                                                    nonVirtualMethodCall.params.foreach(param ⇒ {
                                                        if (!param.isConst &&
                                                            param.asVar.definedBy.head > -1 &&
                                                            !tacCode.stmts(param.asVar.definedBy.head).asAssignment.expr.isConst)
                                                            state.noEscapePossibilityViaReference = false
                                                    })
                                                } else if (useSite == arrayStore) {
                                                    //nothing to do
                                                } else if (useSite.isReturnValue) {
                                                    //assigned array-element escapes
                                                    state.noEscapePossibilityViaReference = false
                                                } else {
                                                    state.noEscapePossibilityViaReference = false
                                                }
                                            }
                                            if (!isArrayIndexConst) {
                                                state.noEscapePossibilityViaReference = false
                                            } else if (assignedExpr.isStaticFunctionCall) {
                                                handleStaticFunctionCall(assignedExpr.asStaticFunctionCall, tacCode)
                                            } else if (assignedExpr.isNew) {
                                                valueAssignment.targetVar.asVar.usedBy.foreach(index ⇒ {
                                                    val tmpStmt = tacCode.stmts(index)
                                                    if (tmpStmt.isArrayStore) {
                                                        // can be ingored
                                                    } else if (tmpStmt.isNonVirtualMethodCall) {
                                                        val nonVirtualMethodcall = tmpStmt.asNonVirtualMethodCall
                                                        if (!seen.contains(tmpStmt)) {
                                                            seen += tmpStmt
                                                            handleNonVirtualMethodCall(method, nonVirtualMethodcall, tacCode)
                                                        } //nothing to do in the else case. Stmt has still been handled
                                                    } else {
                                                        state.noEscapePossibilityViaReference = false
                                                    }
                                                })
                                            } else {
                                                state.noEscapePossibilityViaReference = false
                                            }
                                        } else {
                                            state.noEscapePossibilityViaReference = false
                                        }
                                    } else {
                                        state.noEscapePossibilityViaReference = false
                                    }
                            })
                        } else state.noEscapePossibilityViaReference = false
                    } else
                        for {
                            i ← putDefinitionSites
                        } {
                            if (i > 0) { //necessary
                                val definitionSiteStatement = tacCode.stmts(i)
                                val definitionSiteAssignment = definitionSiteStatement.asAssignment
                                if (definitionSiteAssignment.expr.isStaticFunctionCall) {
                                    handleStaticFunctionCall(definitionSiteAssignment.expr.asStaticFunctionCall, tacCode)
                                } else if (definitionSiteAssignment.expr.isVar) {
                                    val definitionSiteVar = definitionSiteAssignment.expr.asVar
                                    for (definitionSiteVarUseSite ← definitionSiteVar.usedBy) {
                                        val definitionSiteVarUseSiteStmt = tacCode.stmts(definitionSiteVarUseSite)
                                        if (definitionSiteVarUseSiteStmt.isNonVirtualMethodCall) {
                                            val nonVirtualMethodCall = definitionSiteVarUseSiteStmt.asNonVirtualMethodCall
                                            handleNonVirtualMethodCall(method, nonVirtualMethodCall, tacCode)
                                        } else {
                                            state.noEscapePossibilityViaReference = false
                                        }
                                        //TODO andere Fälle bedenken
                                    }
                                } else if (definitionSiteAssignment.expr.isNew) {
                                    if (!method.isConstructor) {
                                        definitionSiteAssignment.targetVar.asVar.usedBy.foreach(x ⇒ {
                                            val tmpStmt = tacCode.stmts(x)
                                            if (tmpStmt.isPutStatic || tmpStmt.isPutField) {
                                                if (!seen.contains(tmpStmt)) {
                                                    seen += tmpStmt
                                                    handlePut(tmpStmt, method, tacCode)
                                                }
                                            } else if (tmpStmt.isNonVirtualMethodCall) {
                                                if (!seen.contains(tmpStmt)) {
                                                    seen += tmpStmt
                                                    handleNonVirtualMethodCall(method, tmpStmt.asNonVirtualMethodCall, tacCode)
                                                }
                                            } else {
                                                state.noEscapePossibilityViaReference = false
                                            }
                                        })
                                    } else {
                                        val useSites =
                                            definitionSiteAssignment.targetVar.usedBy
                                        for (i ← useSites) {
                                            val useSiteStmt = tacCode.stmts(i)
                                            if (useSiteStmt.isNonVirtualMethodCall) {
                                                handleNonVirtualMethodCall(method, useSiteStmt.asNonVirtualMethodCall, tacCode)
                                            } else if (useSiteStmt.isPutStatic || useSiteStmt.isPutField) {
                                                if (!seen.contains(useSiteStmt)) {
                                                    seen += useSiteStmt
                                                    handlePut(useSiteStmt, method, tacCode)
                                                }
                                            } else if (useSiteStmt.isAssignment) {
                                                state.noEscapePossibilityViaReference = false //TODO
                                            } else {
                                                state.noEscapePossibilityViaReference = false
                                            }
                                        }
                                    }
                                    //TODO alle Fälle abdecken
                                    //TODO escape analyse für Object
                                } else if (!definitionSiteAssignment.expr.isConst) {
                                    state.noEscapePossibilityViaReference = false
                                }
                            } else {
                                state.noEscapePossibilityViaReference = false
                            }
                        }
                }

                /**
                 * Begin of method check field writes
                 */
                state.noEscapePossibilityViaReference = field.isPrivate
                for {
                    (method, pcs) ← fieldAccessInformation.writeAccesses(field)
                    taCode ← getTACAI(method)
                    pc ← pcs
                } {
                    val index = taCode.pcToIndex(pc)
                    val staticAddition = if (method.isStatic) 1 else 0
                    if (index > (-1 + staticAddition)) {
                        val stmt = taCode.stmts(index)
                        if (!seen.contains(stmt)) {
                            seen += stmt
                            handlePut(stmt, method, taCode)
                        }
                    } else {
                        state.noEscapePossibilityViaReference = false
                    }
                }
            }
            /**
             * Begin of method determineEscapePossibilityOfReferencedObjectOrValue
             */
            if (state.noEscapePossibilityViaReference)
                determineEscapePossibilityViaFieldReads(state)
            if (state.noEscapePossibilityViaReference)
                checkFieldWritesForEffImmutability(state.field)
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult(state: State): ProperPropertyComputationResult = {

            state.referenceIsImmutable match {
                case Some(false) | None ⇒
                    Result(field, MutableField)
                case Some(true) ⇒ {
                    state.typeIsImmutable match {
                        case Some(true) ⇒
                            Result(field, DeepImmutableField)
                        case Some(false) | None ⇒ {
                            if (state.noEscapePossibilityViaReference)
                                Result(field, DeepImmutableField)
                            else
                                state.dependentImmutability match {
                                    case Some(DependentImmutabilityKind.notShallowOrMutable) ⇒
                                        Result(field, DependentImmutableField)
                                    case Some(DependentImmutabilityKind.onlyDeepImmutable) ⇒
                                        Result(field, DeepImmutableField)
                                    case _ ⇒ {
                                        Result(field, ShallowImmutableField)
                                    }
                                }
                        }
                    }
                }
            }
        }

        def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
            state.dependencies = state.dependencies.iterator.filter(_.e ne eps.e).toSet
            eps match {
                case _: InterimEP[_, _] ⇒ {
                    state.dependencies += eps
                    InterimResult(field, MutableField, DeepImmutableField, state.dependencies, c(state))
                }
                case FinalP(DeepImmutableType) ⇒ {
                    if (state.dependentImmutability.isEmpty)
                        state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
                }
                case FinalEP(t, MutableType_new | ShallowImmutableType) ⇒
                    state.typeIsImmutable = Some(false)
                    if (t != ObjectType.Object) { // in case of generic fields
                        state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                    }
                    if (field.fieldType.isArrayType && t == field.fieldType.asArrayType.componentType)
                        state.noEscapePossibilityViaReference = false
                case FinalEP(t, DependentImmutableType) ⇒ {
                    state.typeIsImmutable = Some(false)
                    if (state.dependentImmutability.isEmpty)
                        state.dependentImmutability = Some(DependentImmutabilityKind.notShallowOrMutable)
                    if (field.fieldType.isArrayType && t == field.fieldType.asArrayType.componentType)
                        state.noEscapePossibilityViaReference = false
                }
                case FinalP(
                    MutableReference | LazyInitializedNotThreadSafeReference
                    ) ⇒ {
                    state.typeIsImmutable = Some(false)
                    state.referenceIsImmutable = Some(false)
                    return Result(field, MutableField);
                }
                case FinalP(
                    ImmutableReference |
                    LazyInitializedThreadSafeReference |
                    LazyInitializedNotThreadSafeButDeterministicReference
                    ) ⇒ {
                    state.referenceIsImmutable = Some(true)
                }
                case FinalP(DeepImmutableField) ⇒ // nothing to do
                case FinalP(DependentImmutableField | ShallowImmutableField | MutableField) ⇒
                    state.noEscapePossibilityViaReference = false
                case eps if eps.isFinal && eps.asEPS.pk == TACAI.key ⇒
                    determineEscapePossibilityOfReferencedObjectOrValue(state)
                case eps if eps.isFinal && eps.asEPS.pk == EscapeProperty.key ⇒
                    determineEscapePossibilityOfReferencedObjectOrValue(state)
                    if (handleEscapeProperty(eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]])(state)) {
                        state.noEscapePossibilityViaReference = false
                    }
                case eps ⇒
                    state.dependencies += eps
            }
            if (state.dependencies.isEmpty) createResult(state)
            else
                InterimResult(
                    field,
                    MutableField,
                    DeepImmutableField,
                    state.dependencies,
                    c(state)
                )
        }

        /**
         * Begin of determine field immutability function
         */
        implicit val state: State = State(field)
        val referenceImmutabilityPropertyStoreResult = propertyStore(state.field, ReferenceImmutability.key)
        referenceImmutabilityPropertyStoreResult match {
            case FinalP(ImmutableReference) ⇒ state.referenceIsImmutable = Some(true)
            case FinalP(LazyInitializedThreadSafeReference | LazyInitializedNotThreadSafeButDeterministicReference) ⇒
                state.referenceIsImmutable = Some(true)
            case FinalP(MutableReference | LazyInitializedNotThreadSafeReference) ⇒
                return Result(field, MutableField);
            case ep @ _ ⇒ {
                state.dependencies += ep
            }
        }
        loadFormalTypeParameter()
        handleTypeImmutability()
        hasGenericType()
        //!! attention it is possible that the type immutability has not already been determined at this point
        if (state.referenceIsImmutable.isEmpty || state.referenceIsImmutable.get) {
            determineEscapePossibilityOfReferencedObjectOrValue
        }

        if (state.dependencies.isEmpty) {
            createResult(state)
        } else {
            InterimResult(
                field,
                MutableField,
                DeepImmutableField,
                state.dependencies,
                c(state)
            )
        }
    }
}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.finalP(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.lub(ReferenceImmutability),
        PropertyBounds.lub(TypeImmutability_new),
        PropertyBounds.lub(FieldImmutability)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)

}

/**
 * Executor for the field immutability analysis.
 */
object EagerL0FieldImmutabilityAnalysis
    extends L0FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldImmutabilityAnalysis(p)
        val fields = p.allFields // p.allProjectClassFiles.flatMap(_.fields) // p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldImmutability)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

/**
 * Executor for the lazy field immutability analysis.
 */
object LazyL0FieldImmutabilityAnalysis
    extends L0FieldImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

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
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
