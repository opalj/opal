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
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
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

            //if the genericity is nested in an inner class
            // collect the generic type parameters from the fields outer class
            if (field.classFile.outerType.isDefined) {
                val outerClassFile = project.classFile(field.classFile.outerType.get._1)
                if (outerClassFile.isDefined) {
                    outerClassFile.get.attributes.foreach(
                        CheckAttributeWithRegardToFormalTypeParameter
                    )
                }
            }
            // collect the generic type parameters from the fields class
            field.classFile.attributes.foreach(
                CheckAttributeWithRegardToFormalTypeParameter
            )

            if (result.size > 0)
                classFormalTypeParameters = Some(result)

        }
        def isInClassesGenericTypeParameters(string: String): Boolean =
            classFormalTypeParameters.isDefined && classFormalTypeParameters.get.contains(string)

        def handleTypeImmutability(state: State) = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                state.typeIsImmutable = Some(false) //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                // we hardcode here the strings deep immutability
                //state.typeImmutability = Some(true) // true is default
            } else if (objectType.isArrayType) {
                state.typeIsImmutable = Some(false)
            } else {
                val typeImmutabilityPropertyStoreResult =
                    propertyStore(objectType, TypeImmutability_new.key)
                state.dependencies = state.dependencies.iterator.filter(_.e ne typeImmutabilityPropertyStoreResult.e).toSet
                typeImmutabilityPropertyStoreResult match {
                    case FinalP(DeepImmutableType) ⇒ // type is immutable is default
                    case FinalP(DependentImmutableType) ⇒
                        state.typeIsImmutable = Some(false)
                        state.dependentImmutability =
                            Some(DependentImmutabilityKind.dependent)
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        state.typeIsImmutable = Some(false)
                        state.dependentImmutability = None
                        if (state.field.fieldType.isObjectType &&
                            state.field.fieldType.asObjectType != ObjectType.Object) {
                            state.dependentImmutability = None //when the generic type is still final
                        }
                    }
                    //case ep: InterimEP[e, p] ⇒ state.dependencies += ep
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

        def checkIfReferencedObjectCanEscape(implicit state: State): Unit = {

            def checkFieldReadsForEffImmutability(field: Field)(implicit state: State) = {

                for {
                    (method, pcs) ← fieldAccessInformation.readAccesses(field)
                    taCode ← getTACAI(method)
                    pc ← pcs
                } {
                    val index = taCode.pcToIndex(pc)
                    val stmt = taCode.stmts(index)
                    if (stmt.isAssignment) {
                        val assignment = stmt.asAssignment
                        val useSites = assignment.targetVar.usedBy
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
                    } else {
                        state.noEscapePossibilityViaReference = false
                    }
                }
            }

            var seen: Set[Stmt[V]] = Set.empty
            def checkFieldWritesForEffImmutability(field: Field)(implicit state: State) = {

                def checkNonVirtualMethodCall(
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
                                                            checkNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                        }
                                                    } else
                                                        state.noEscapePossibilityViaReference = false
                                                }
                                                //}
                                            } else
                                                state.noEscapePossibilityViaReference = false

                                        } else {
                                            val definitionSitesOfParam = definitionSites(method, index)
                                            val stmt = tacCode.stmts(tacCode.pcToIndex(definitionSitesOfParam.pc))
                                            if (stmt.isNonVirtualMethodCall) {
                                                if (!seen.contains(stmt)) {
                                                    seen += stmt
                                                    checkNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
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

                def checkPuts(stmt: Stmt[V], method: Method, tacCode: TACode[TACMethodParameter, V]): Unit = {
                    var putDefinitionSites: IntTrieSet = IntTrieSet.empty
                    if (stmt.isPutField) {
                        val putField = stmt.asPutField
                        putDefinitionSites = putField.value.asVar.definedBy
                    } else if (stmt.isPutStatic) {
                        val putStatic = stmt.asPutStatic
                        putDefinitionSites = putStatic.value.asVar.definedBy
                    } else {
                        state.noEscapePossibilityViaReference = false
                    }
                    for {
                        i ← putDefinitionSites
                    } {
                        if (i > 0) {
                            val definitionSiteStatement = tacCode.stmts(i)
                            val definitionSiteAssignment = definitionSiteStatement.asAssignment
                            if (definitionSiteAssignment.expr.isVar) {
                                val definitionSiteVar = definitionSiteAssignment.expr.asVar
                                for (definitionSiteVarUseSite ← definitionSiteVar.usedBy.iterator) {
                                    val definitionSiteVarUseSiteStmt = tacCode.stmts(definitionSiteVarUseSite)
                                    if (definitionSiteVarUseSiteStmt.isNonVirtualMethodCall) {
                                        val nonVirtualMethodCall = definitionSiteVarUseSiteStmt.asNonVirtualMethodCall
                                        checkNonVirtualMethodCall(method, nonVirtualMethodCall, tacCode)
                                    } else {
                                        state.noEscapePossibilityViaReference = false
                                    }
                                    //TODO andere Fälle bedenken
                                }
                            } else if (definitionSiteAssignment.expr.isNew) {
                                if (!method.isConstructor) {
                                    val propertyStoreResult =
                                        propertyStore(
                                            definitionSites(method, definitionSiteAssignment.pc),
                                            EscapeProperty.key
                                        )
                                    if (handleEscapeProperty(propertyStoreResult)) {
                                        state.noEscapePossibilityViaReference = false
                                    }
                                } else {
                                    val useSites =
                                        definitionSiteAssignment.targetVar.usedBy
                                    for (i ← useSites) {
                                        val useSiteStmt = tacCode.stmts(i)
                                        if (useSiteStmt.isNonVirtualMethodCall) {
                                            checkNonVirtualMethodCall(method, useSiteStmt.asNonVirtualMethodCall, tacCode)
                                        } else if (useSiteStmt.isPutStatic || useSiteStmt.isPutField) {
                                            //checkPuts(stmt, method, tacCode)
                                            if (useSiteStmt != stmt) {
                                                state.noEscapePossibilityViaReference = false //TODO
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
                // start of method
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
            if (state.noEscapePossibilityViaReference)
                checkFieldWritesForEffImmutability(state.field)
            if (state.noEscapePossibilityViaReference)
                checkFieldReadsForEffImmutability(state.field)
        }

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
                    MutableReference | LazyInitializedNotThreadSafeOrNotDeterministicReference
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
        propertyStore(state.field, ReferenceImmutability.key) match {
            case FinalP(ImmutableReference) ⇒ state.referenceIsImmutable = Some(true)
            case FinalP(LazyInitializedThreadSafeReference | LazyInitializedNotThreadSafeButDeterministicReference) ⇒
                state.referenceIsImmutable = Some(true)
            case FinalP(MutableReference | LazyInitializedNotThreadSafeOrNotDeterministicReference) ⇒
                return Result(field, MutableField);
            case ep @ _ ⇒ {
                state.dependencies += ep
            }
        }

        loadFormalTypeparameters()
        handleTypeImmutability(state)
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

    import org.opalj.tac.fpcf.properties.TACAI

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
        val fields = p.allProjectClassFiles.toIterator.flatMap { _.fields }
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
