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

case class State(f: Field) {
    var field: Field = f
    var typeIsImmutable: Option[Boolean] = Some(true)
    var referenceIsImmutable: Option[Boolean] = None
    var noEscapePossibilityViaReference: Boolean = true
    var dependentImmutability: Option[DependentImmutabilityKind] = Some(DependentImmutabilityKind.dependent)
    var genericTypeSetNotDeepImmutable = false
    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty
}

/**
 *  Enum that describes the different kinds of dependent immutable fields:
 *  [[DependentImmutabilityKind.dependent]] Shallow or mutable types could still exist
 *  [[DependentImmutabilityKind.notShallowOrMutable]] There are no shallow or mutable types
 *  [[DependentImmutabilityKind.onlyDeepImmutable]] There are no generic parameters left.
 *  All are replaced with deep immutable types.
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
        var classFormalTypeParameters: Option[Set[String]] = None
        def loadFormalTypeparameters(): Unit = {
            var result: Set[String] = Set.empty
            def CheckAttributeWithRegardToFormalTypeParameter: Attribute ⇒ Unit = {
                attribute ⇒
                    attribute match {
                        case SourceFile(_) ⇒
                        case ClassSignature(typeParameters, _, _) ⇒
                            for (parameter ← typeParameters) {
                                parameter match {
                                    case FormalTypeParameter(identifier, _, _) ⇒ result += identifier
                                    case _                                     ⇒
                                }
                            }
                        case _ ⇒
                    }
            }

            /**
             * If the genericity is nested in an inner class
             * collecting the generic type parameters from the fields outer class
             */
            if (field.classFile.outerType.isDefined) {
                val outerClassFile = project.classFile(field.classFile.outerType.get._1)
                if (outerClassFile.isDefined) {
                    outerClassFile.get.attributes.foreach(
                        CheckAttributeWithRegardToFormalTypeParameter
                    )
                }
            }

            /**
             * Collecting the generic type parameters from the fields class
             */
            field.classFile.attributes.foreach(
                CheckAttributeWithRegardToFormalTypeParameter
            )

            if (result.size > 0)
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
        def handleTypeImmutability(implicit state: State): Unit = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                state.typeIsImmutable = Some(false) //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                // we hardcode here the strings deep immutability
                // base types are by design deep immutable
                //state.typeImmutability = Some(true) // true is default
            } else if (objectType.isArrayType) {
                // Because the entries of an array can be reassigned we hardcode it as not being deep immutable
                state.typeIsImmutable = Some(false)
            } else {
                val typeImmutabilityPropertyStoreResult =
                    propertyStore(objectType, TypeImmutability_new.key)
                state.dependencies =
                    state.dependencies.iterator.filter(_.e ne typeImmutabilityPropertyStoreResult.e).toSet
                typeImmutabilityPropertyStoreResult match {
                    case FinalP(DeepImmutableType) ⇒ // type being deep immutable is default
                    case FinalP(DependentImmutableType) ⇒
                        state.typeIsImmutable = Some(false)
                        state.dependentImmutability =
                            Some(DependentImmutabilityKind.dependent)
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        state.typeIsImmutable = Some(false)
                        state.dependentImmutability = None
                        if (state.field.fieldType.isObjectType &&
                            state.field.fieldType.asObjectType != ObjectType.Object) {
                            state.dependentImmutability = None //state we are less then dependent immutable
                        }
                    }
                    case epk ⇒ state.dependencies += epk
                }
            }
        }

        def hasGenericType(state: State): Unit = {
            var noShallowOrMutableTypeInGenericTypeFound = true
            var onlyDeepTypesInGenericTypeFound = true
            var genericFields: List[ObjectType] = List()
            var noRelevantAttributesFound = true
            state.field.asField.attributes.foreach(
                _ match {
                    case RuntimeInvisibleAnnotationTable(_) ⇒
                    case SourceFile(_)                      ⇒
                    case TypeVariableSignature(t) ⇒
                        noRelevantAttributesFound = false
                        onlyDeepTypesInGenericTypeFound = false
                        if (!isInClassesGenericTypeParameters(t))
                            noShallowOrMutableTypeInGenericTypeFound = false
                    case ClassTypeSignature(
                        packageIdentifier,
                        SimpleClassTypeSignature(simpleName, typeArguments),
                        _
                        ) ⇒
                        noRelevantAttributesFound = false
                        typeArguments
                            .foreach({
                                _ match {
                                    case ProperTypeArgument(variance, TypeVariableSignature(identifier: String)) ⇒
                                        onlyDeepTypesInGenericTypeFound = false
                                        if (!isInClassesGenericTypeParameters(identifier))
                                            noShallowOrMutableTypeInGenericTypeFound = false
                                    case ProperTypeArgument(varianceIndicator,
                                        ClassTypeSignature(outerPackageIdentifier,
                                            SimpleClassTypeSignature(innerPackageIdentifier, typeArguments2), _)) ⇒ {
                                        val objectPath =
                                            outerPackageIdentifier match {
                                                case Some(prepackageIdentifier) ⇒
                                                    prepackageIdentifier + innerPackageIdentifier
                                                case _ ⇒ innerPackageIdentifier
                                            }
                                        genericFields = ObjectType(objectPath) :: genericFields
                                    }
                                    case _ ⇒
                                        noShallowOrMutableTypeInGenericTypeFound = false
                                        onlyDeepTypesInGenericTypeFound = false
                                        state.typeIsImmutable = Some(false)
                                }
                            })
                    case _ ⇒
                        state.typeIsImmutable = Some(false)
                        noShallowOrMutableTypeInGenericTypeFound = false
                        onlyDeepTypesInGenericTypeFound = false
                }
            )
            genericFields.foreach(objectType ⇒ {
                val typeImmutabilityPropertyStoreResult = propertyStore(objectType, TypeImmutability_new.key)
                state.dependencies =
                    state.dependencies.iterator.filter(_.e ne typeImmutabilityPropertyStoreResult.e).toSet
                typeImmutabilityPropertyStoreResult match {
                    case FinalP(DeepImmutableType) ⇒ //nothing to do here: default value is deep immutable
                    case FinalP(DependentImmutableType) ⇒
                        onlyDeepTypesInGenericTypeFound = false
                        state.typeIsImmutable = Some(false)
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        noShallowOrMutableTypeInGenericTypeFound = false
                        onlyDeepTypesInGenericTypeFound = false
                        state.typeIsImmutable = Some(false)
                    }
                    case ep ⇒
                        state.dependencies += ep
                }
            })
            //Prevents keeping the case of keeping the default values of these
            // flags only because of ne revelant attribute was found
            if (noRelevantAttributesFound) {
                noShallowOrMutableTypeInGenericTypeFound = false
                onlyDeepTypesInGenericTypeFound = false
            }

            if (state.dependentImmutability != None) {
                if (onlyDeepTypesInGenericTypeFound)
                    state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
                else if (noShallowOrMutableTypeInGenericTypeFound)
                    state.dependentImmutability = Some(DependentImmutabilityKind.notShallowOrMutable)
            }
        }

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
                case FinalP(NoEscape) ⇒ // | EscapeInCallee | EscapeViaReturn
                    false

                case FinalP(EscapeInCallee | EscapeViaReturn) ⇒ true
                case FinalP(AtMost(_)) ⇒
                    true

                case _: FinalEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return

                case InterimUBP(NoEscape) ⇒ // | EscapeInCallee | EscapeViaReturn) ⇒
                    state.dependencies += ep
                    false

                case InterimUBP(AtMost(_)) ⇒
                    true

                case _: InterimEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return

                case _ ⇒
                    state.dependencies += ep
                    false
            }
        }

        /*def isDeterministic(
            eop: EOptionP[DeclaredMethod, Purity]
        )(implicit state: State): Boolean = eop match {
            case FinalP(p: Purity) ⇒
                println("final p is det: "+p.isDeterministic); p.isDeterministic
            case LBP(p: Purity) if p.isDeterministic ⇒
                true
            case EUBP(e, p: Purity) if !p.isDeterministic ⇒
                false
            case _ ⇒
                state.dependencies += eop
                true
        } */
        def checkIfReferencedObjectCanEscape(implicit state: State): Unit = {

            def checkFieldReadsForEffImmutability(field: Field)(implicit state: State) = {

                for {
                    (method, pcs) ← fieldAccessInformation.readAccesses(field)
                    taCode ← getTACAI(method)
                    pc ← pcs
                } {
                    val index = taCode.pcToIndex(pc)
                    if (index > -1) {
                        val stmt = taCode.stmts(index)
                        if (stmt.isAssignment) {
                            val assignment = stmt.asAssignment
                            val useSites = assignment.targetVar.usedBy
                            val propStoreResult = propertyStore(definitionSites(method, assignment.pc), EscapeProperty.key)
                            val handleEscapePropertyResult = handleEscapeProperty(propStoreResult)
                            if (handleEscapePropertyResult) {

                                state.noEscapePossibilityViaReference = false
                            }
                            for {
                                index ← useSites
                            } {
                                val fieldsUseSiteStmt = taCode.stmts(index)

                                if (!fieldsUseSiteStmt.isMonitorEnter &&
                                    !fieldsUseSiteStmt.isMonitorExit &&
                                    !fieldsUseSiteStmt.isIf) {

                                    state.noEscapePossibilityViaReference = false
                                }
                            }
                        } else if (stmt.isExprStmt) {
                            //is ignored
                            // The value is only read but not assigned to another one
                        } else {

                            state.noEscapePossibilityViaReference = false
                        }
                    } else {

                        state.noEscapePossibilityViaReference = false
                    }
                }
            }

            var seen: Set[Stmt[V]] = Set.empty
            def checkFieldWritesForEffImmutability(field: Field)(implicit state: State): Unit = {
                import org.opalj.tac.StaticFunctionCall

                def handleStaticFunctionCall(
                    staticFunctionCall: StaticFunctionCall[V],
                    tacCode:            TACode[TACMethodParameter, V]
                ): Unit = {
                    if (staticFunctionCall.params.
                        exists(p ⇒
                            p.asVar.definedBy.size != 1 ||
                                p.asVar.definedBy.head < 0 ||
                                !tacCode.stmts(p.asVar.definedBy.head).asAssignment.expr.isConst)) {

                        state.noEscapePossibilityViaReference = false
                    }
                }

                def handleNonVirtualMethodCall(
                    method:               Method,
                    nonVirtualMethodCall: NonVirtualMethodCall[V],
                    tacCode:              TACode[TACMethodParameter, V]
                ): Unit = {
                    nonVirtualMethodCall.params.foreach(
                        param ⇒ {
                            param.asVar.definedBy.foreach(
                                index ⇒
                                    if (index < 0) {

                                        state.noEscapePossibilityViaReference = false
                                    } else {
                                        val paramDefinitionStmt = tacCode.stmts(index)
                                        if (paramDefinitionStmt.isAssignment) {
                                            val assignmentExpression = paramDefinitionStmt.asAssignment.expr
                                            if (assignmentExpression.isGetField) {
                                                val getField = assignmentExpression.asGetField
                                                val field = getField.resolveField
                                                val propertyStoreResult = propertyStore(field.get, FieldImmutability.key)
                                                propertyStoreResult match {
                                                    case FinalP(DeepImmutableField) ⇒ //nothing to do  here
                                                    case FinalP(_)                  ⇒ state.noEscapePossibilityViaReference = false
                                                    case ep                         ⇒ state.dependencies += ep
                                                }

                                            } else if (assignmentExpression.isNew) {
                                                //if (paramDefinitionStmt.isAssignment && //TODO.....
                                                //    paramDefinitionStmt.asAssignment.targetVar.isVar) {
                                                for (i ← paramDefinitionStmt.asAssignment.targetVar.asVar.usedBy) {
                                                    val stmt = tacCode.stmts(i)
                                                    if (stmt.isNonVirtualMethodCall) {
                                                        if (!seen.contains(stmt)) {
                                                            seen += stmt
                                                            handleNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                        }
                                                    } else {

                                                        state.noEscapePossibilityViaReference = false
                                                    }
                                                }
                                                //}
                                            } else {

                                                state.noEscapePossibilityViaReference = false
                                            }

                                        } else {
                                            val definitionSitesOfParam = definitionSites(method, index)
                                            val stmt = tacCode.stmts(tacCode.pcToIndex(definitionSitesOfParam.pc))
                                            if (stmt.isNonVirtualMethodCall) {
                                                if (!seen.contains(stmt)) {
                                                    seen += stmt
                                                    handleNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                }
                                            } else if (stmt.isPutField || stmt.isPutStatic) {
                                                if (!seen.contains(stmt)) {
                                                    seen += stmt
                                                    checkPuts(stmt, method, tacCode)
                                                }

                                            } else if (stmt.isArrayStore) {
                                                //val arrayStore = stmt.asArrayStore

                                                state.noEscapePossibilityViaReference = false //TODO handling that case more precise
                                            } //else if // other cases that the purity analysis can not handle
                                            else {
                                                val propertyStoreResult =
                                                    propertyStore(definitionSitesOfParam, EscapeProperty.key)
                                                if (handleEscapeProperty(propertyStoreResult)) {

                                                    state.noEscapePossibilityViaReference = false
                                                }
                                            }
                                        } //TODO go further
                                    }
                            )
                        }
                    )
                }

                def checkPuts(putStmt: Stmt[V], method: Method, tacCode: TACode[TACMethodParameter, V]): Unit = {
                    import org.opalj.Yes
                    import org.opalj.tac.Expr
                    var putDefinitionSites: IntTrieSet = IntTrieSet.empty
                    var value: Expr[V] = null
                    if (putStmt.isPutField) {
                        val putField = putStmt.asPutField
                        putDefinitionSites = putField.value.asVar.definedBy
                        value = putField.value
                    } else if (putStmt.isPutStatic) {
                        val putStatic = putStmt.asPutStatic
                        putDefinitionSites = putStatic.value.asVar.definedBy
                        value = putStatic.value
                    } else {

                        state.noEscapePossibilityViaReference = false
                    }
                    val index = value.asVar.definedBy.head
                    if (value.asVar.value.isArrayValue == Yes) {
                        tacCode.stmts(index).asAssignment.targetVar.usedBy.foreach(x ⇒ {
                            val arrayStmt = tacCode.stmts(x)
                            if (arrayStmt != putStmt)
                                if (arrayStmt.isArrayStore) {
                                    val arrayStore = arrayStmt.asArrayStore
                                    val arrayStoreIndex = arrayStore.index
                                    val isArrayIndexConst =
                                        tacCode.stmts(arrayStoreIndex.asVar.definedBy.head).asAssignment.expr.isConst
                                    val assignedValue = arrayStore.value
                                    val valueAssignment = tacCode.stmts(assignedValue.asVar.definedBy.head).asAssignment
                                    val assignedExpr = valueAssignment.expr
                                    if (!isArrayIndexConst) {
                                        state.noEscapePossibilityViaReference = false
                                    } else if (assignedExpr.isStaticFunctionCall) {
                                        handleStaticFunctionCall(assignedExpr.asStaticFunctionCall, tacCode)
                                    } else if (assignedExpr.isNew) {
                                        //val newExpression = assignedExpr.asNew
                                        valueAssignment.targetVar.asVar.usedBy.foreach(index ⇒ {
                                            val tmpStmt = tacCode.stmts(index)
                                            if (tmpStmt.isArrayStore) {
                                                // can be ingored
                                            } else if (tmpStmt.isNonVirtualMethodCall) {
                                                val nonVirtualMethodcall = tmpStmt.asNonVirtualMethodCall
                                                if (!seen.contains(tmpStmt)) {
                                                    seen += tmpStmt
                                                    handleNonVirtualMethodCall(method, nonVirtualMethodcall, tacCode)
                                                } //nothing to do in the else case. Stmt was still handled
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
                        })
                    } else
                        for {
                            i ← putDefinitionSites
                        } {
                            if (i > 0) {
                                val definitionSiteStatement = tacCode.stmts(i)
                                val definitionSiteAssignment = definitionSiteStatement.asAssignment
                                if (definitionSiteAssignment.expr.isStaticFunctionCall) {
                                    handleStaticFunctionCall(definitionSiteAssignment.expr.asStaticFunctionCall, tacCode)
                                } else if (definitionSiteAssignment.expr.isVar) {
                                    val definitionSiteVar = definitionSiteAssignment.expr.asVar
                                    for (definitionSiteVarUseSite ← definitionSiteVar.usedBy.iterator) {
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
                                                // can be ingored //TODO use seen
                                            } else if (tmpStmt.isNonVirtualMethodCall) {
                                                if (!seen.contains(tmpStmt))
                                                    handleNonVirtualMethodCall(method, tmpStmt.asNonVirtualMethodCall, tacCode)
                                            } else {

                                                state.noEscapePossibilityViaReference = false
                                            }
                                        })
                                        /*val propertyStoreResult = {
                                            propertyStore(
                                                definitionSites(method, definitionSiteAssignment.pc),
                                                EscapeProperty.key
                                            )

                                        }
                                        println("propertystoreresult: " + propertyStoreResult)
                                        if (handleEscapeProperty(propertyStoreResult)) {
                                            state.noEscapePossibilityViaReference = false
                                        }*/
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
                                                    checkPuts(useSiteStmt, method, tacCode)
                                                }
                                            } else if (useSiteStmt.isAssignment) {

                                                state.noEscapePossibilityViaReference = false //TODO
                                                //val assignment = stmt.asAssignment
                                            } else {

                                                state.noEscapePossibilityViaReference = false
                                            }
                                        }
                                    }
                                    //TODO alle Fälle abdecken
                                    //TODO escape analyse für Object
                                    // else ; aktuelles putfield bedenken
                                } else if (!definitionSiteAssignment.expr.isConst) {

                                    state.noEscapePossibilityViaReference = false
                                }
                            } else {

                                state.noEscapePossibilityViaReference = false
                            }
                        }
                }
                // start of method check field writes
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
                            checkPuts(stmt, method, taCode)
                        }

                    } else {

                        state.noEscapePossibilityViaReference = false
                    }
                }
            }
            if (state.noEscapePossibilityViaReference) {

                checkFieldReadsForEffImmutability(state.field)
            }
            if (state.noEscapePossibilityViaReference) {

                checkFieldWritesForEffImmutability(state.field)
            }

        }

        def createResult(state: State): ProperPropertyComputationResult = {
            /*
            println(
                s"""
                 | create result field: ${state.field}
                 | ref imm: ${state.referenceIsImmutable}
                 | type imm: ${state.typeIsImmutable}
                 | dep imm: ${state.dependentImmutability}
                 | not escape: ${state.noEscapePossibilityViaReference}
                 |
                 |""".stripMargin
            ) */

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
            eps match { //(: @unchecked)

                case x: InterimEP[_, _] ⇒ {
                    state.dependencies += eps
                    InterimResult(field, MutableField, DeepImmutableField, state.dependencies, c(state))
                }
                case FinalP(DeepImmutableType) ⇒ { //state.typeImmutability = Some(true)
                    if (!state.dependentImmutability.isDefined)
                        state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
                }
                case FinalEP(t, MutableType_new | ShallowImmutableType) ⇒
                    state.typeIsImmutable = Some(false)
                    if (t != ObjectType.Object) { // in case of generic fields
                        state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                    }
                case FinalEP(f, DependentImmutableType) ⇒ {
                    state.typeIsImmutable = Some(false)
                    if (!state.dependentImmutability.isDefined)
                        state.dependentImmutability = Some(DependentImmutabilityKind.notShallowOrMutable)
                }
                case FinalP(
                    MutableReference | LazyInitializedNotThreadSafeReference
                    ) ⇒ {
                    state.typeIsImmutable = Some(false)
                    return Result(field, MutableField);
                }
                case FinalP(
                    ImmutableReference |
                    LazyInitializedThreadSafeReference |
                    LazyInitializedNotThreadSafeButDeterministicReference
                    ) ⇒ { //TODO
                    state.referenceIsImmutable = Some(true)
                }
                case FinalP(DeepImmutableField) ⇒ // nothing to do
                case eps if eps.isFinal && eps.asEPS.pk == FieldImmutability.key ⇒ //else case
                    state.noEscapePossibilityViaReference = false
                case eps if eps.isFinal && eps.asEPS.pk == TACAI.key ⇒ checkIfReferencedObjectCanEscape(state)
                case eps if eps.isFinal && eps.asEPS.pk == EscapeProperty.key ⇒
                    checkIfReferencedObjectCanEscape(state)
                    if (handleEscapeProperty(eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]])(state)) {
                        state.noEscapePossibilityViaReference = false
                    }

                //case _: FinalEP[_, TACAI] ⇒
                //    checkIfReferencedObjectCanEscape(state)
                //TODO hier weiter machen

                //case EPS(,Te)⇒

                case ep ⇒
                    state.dependencies = state.dependencies + ep
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
        loadFormalTypeparameters()
        handleTypeImmutability(state)
        hasGenericType(state)
        //it is possible that the type immutability not already determined at this point
        if (!state.referenceIsImmutable.isDefined || state.referenceIsImmutable.get) {
            checkIfReferencedObjectCanEscape
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
        val fields = p.allFields // p.allProjectClassFiles.flatMap(_.fields) //TODO p.allFields
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
