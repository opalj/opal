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

case class State(f: Field) {

    var field: Field = f
    var typeImmutability: Option[Boolean] = Some(true)
    var referenceImmutability: Option[Boolean] = None
    var referenceNotEscapes: Boolean = true
    var dependentImmutability: Option[DependentImmutabilityKind] = Some(
        DependentImmutabilityKind.dependent
    )
    var genericTypeSetNotDeepImmutable = false
    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty

    def hasDependees: Boolean = {
        dependencies.nonEmpty
    }

    def dependees: Set[EOptionP[Entity, Property]] = {
        dependencies
    }
}

object DependentImmutabilityKind extends Enumeration {
    type DependentImmutabilityKind = Value
    val dependent, notShallowOrMutable, onlyDeepImmutable = Value
}

/**
 * Analyses that determines the immutability of org.opalj.br.Field
 * Because it depends on the Field Immutability Lattice it combines the immutability of the fields reference and
 * it's type. Thus needs the information of the reference of the field from the [[L0ReferenceImmutabilityAnalysis]]
 * and the information of the type immutability determined by the type immutability analysis.
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

        if (field.name.contains("hash") &&
            field.classFile.thisType.simpleName.contains("AclEntry"))
            println(s"field: ${field}")

        var classFormalTypeParameters: Option[Set[String]] = None
        def loadFormalTypeparameters() = {
            var result: Set[String] = Set.empty
            //TODO
            def CheckAttributeWithRegardToFormalTypeParameter: Attribute ⇒ Unit = {
                attribute ⇒
                    attribute match {
                        case SourceFile(_) ⇒
                        case ClassSignature(typeParameters, _, _) ⇒
                            for (parameter ← typeParameters.iterator) {
                                parameter match {
                                    case FormalTypeParameter(identifier, _, _) ⇒ result += identifier
                                    case _                                     ⇒
                                }
                            }
                        case _ ⇒
                    }
            }

            //if the genericity is nested in an inner class
            if (field.classFile.outerType.isDefined) {
                val outerClassFile = project.classFile(field.classFile.outerType.get._1)
                if (outerClassFile.isDefined) {
                    outerClassFile.get.attributes.iterator.foreach(
                        CheckAttributeWithRegardToFormalTypeParameter
                    )
                }
            }

            field.classFile.attributes.iterator.foreach(
                CheckAttributeWithRegardToFormalTypeParameter
            )

            if (result.size > 0) {
                println("result: "+result)
                classFormalTypeParameters = Some(result)
            }

        }
        def isInClassesGenericTypeParameters(string: String): Boolean =
            classFormalTypeParameters.isDefined && classFormalTypeParameters.get.contains(string)

        def handleTypeImmutability(state: State) = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                state.typeImmutability = Some(false) //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                //state.typeImmutability = Some(true) // true is default
            } else if (objectType.isArrayType) {
                state.typeImmutability = Some(false)
            } else {
                val result = propertyStore(objectType, TypeImmutability_new.key)
                state.dependencies = state.dependencies.iterator.filter(_.e ne result.e).toSet
                result match {
                    case FinalP(DeepImmutableType) ⇒
                    case FinalP(DependentImmutableType) ⇒ {
                        state.typeImmutability = Some(false)
                    }
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        state.typeImmutability = Some(false)
                        state.dependentImmutability = None
                        if (state.field.fieldType.isObjectType &&
                            state.field.fieldType.asObjectType != ObjectType.Object) {
                            state.dependentImmutability = None //when the generic type is still final
                        }
                    }
                    case ep: InterimEP[e, p] ⇒ state.dependencies += ep
                    case epk @ _             ⇒ state.dependencies += epk
                }
            }
        }

        def hasGenericType(state: State): Unit = {
            var flag_notShallow = true
            var flag_onlyDeep = true
            var genericFields: List[ObjectType] = List()
            println("Attributes: "+state.field.asField.attributes)
            state.field.asField.attributes.iterator.foreach(
                _ match {
                    case RuntimeInvisibleAnnotationTable(_) ⇒
                    case SourceFile(_)                      ⇒
                    case TypeVariableSignature(t) ⇒
                        println("T: "+t)
                        flag_onlyDeep = false
                        if (!isInClassesGenericTypeParameters(t))
                            flag_notShallow = false
                    case ClassTypeSignature(
                        packageIdentifier,
                        SimpleClassTypeSignature(simpleName, typeArguments),
                        _
                        ) ⇒
                        typeArguments.iterator
                            .foreach({
                                _ match {
                                    case ProperTypeArgument(
                                        variance,
                                        TypeVariableSignature(identifier: String)
                                        ) ⇒
                                        println("identifier: "+identifier)
                                        flag_onlyDeep = false
                                        if (!isInClassesGenericTypeParameters(identifier)) {
                                            flag_notShallow = false
                                        }
                                    case ProperTypeArgument(
                                        varianceIndicator,
                                        ClassTypeSignature(
                                            packageIdentifier1,
                                            SimpleClassTypeSignature(
                                                packageIdentifier2,
                                                typeArguments2
                                                ),
                                            _
                                            )
                                        ) ⇒ {

                                        val oPath =
                                            packageIdentifier1 match {
                                                case Some(packageIdentifier1) ⇒ packageIdentifier1 + packageIdentifier2
                                                case _                        ⇒ packageIdentifier2
                                            }
                                        genericFields = ObjectType(oPath) :: genericFields
                                    }
                                    case _ ⇒
                                        flag_notShallow = false
                                        flag_onlyDeep = false
                                        state.typeImmutability = Some(false)
                                }
                            })

                    case _ ⇒
                        state.typeImmutability = Some(false)
                        flag_notShallow = false
                        flag_onlyDeep = false
                }
            )
            genericFields.foreach(objectType ⇒ {
                val result = propertyStore(objectType, TypeImmutability_new.key)
                state.dependencies = state.dependencies.iterator.filter(_.e ne result.e).toSet
                println("result: "+result)
                result match {
                    case FinalP(DeepImmutableType) ⇒ //nothing to to here: default value is deep imm
                    case FinalP(ShallowImmutableType | DependentImmutableType | MutableType_new) ⇒ {
                        flag_notShallow = false
                        flag_onlyDeep = false
                        state.typeImmutability = Some(false)
                    }
                    case ep @ _ ⇒
                        state.dependencies += ep
                }
            })

            if (state.field.asField.attributes.size == state.field.asField.attributes
                .collect({ case x @ RuntimeInvisibleAnnotationTable(_) ⇒ x })
                .size) {
                flag_notShallow = false
                flag_onlyDeep = false
            }

            if (state.dependentImmutability != None) {
                if (flag_onlyDeep)
                    state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
                else if (flag_notShallow)
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
            println("ep: "+ep)
            ep match {
                case FinalP(NoEscape) ⇒ // | EscapeInCallee | EscapeViaReturn
                    false

                case FinalP(EscapeInCallee | EscapeViaReturn) ⇒ true
                case FinalP(AtMost(_)) ⇒
                    true

                case _: FinalEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return

                case InterimUBP(NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
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
                    println("stmt: "+taCode.stmts(index))
                    val stmt = taCode.stmts(index)
                    // if (stmt.isAssignment) {
                    val assignment = stmt.asAssignment
                    val useSites = assignment.targetVar.usedBy
                    for {
                        index ← useSites
                    } {
                        val fieldsUseSiteStmt = taCode.stmts(index)
                        if (!(fieldsUseSiteStmt.isMonitorEnter ||
                            fieldsUseSiteStmt.isMonitorExit ||
                            fieldsUseSiteStmt.isIfStmt)) {
                            state.referenceNotEscapes = false
                        }
                    }
                    // } else if (stmt.isExprStmt) {
                    //     //is ignored
                    //} else state.referenceNotEscapes = false

                }
            }

            def checkFieldWritesForEffImmutability(field: Field)(implicit state: State) = {
                import org.opalj.tac.Stmt
                def checkNonVirtualMethodCall( //TODO loswerden über escape analyse
                    method:               Method,
                    nonVirtualMethodCall: NonVirtualMethodCall[V],
                    tacCode:              TACode[TACMethodParameter, V]
                ): Unit = {
                    println("check nvmc parameters")
                    println(state.field.classFile)
                    println("nvmc: "+nonVirtualMethodCall)
                    println("params: "+nonVirtualMethodCall.params)
                    nonVirtualMethodCall.params.foreach(
                        param ⇒ {
                            println("param :"+param)

                            param.asVar.definedBy.foreach(
                                index ⇒
                                    if (index < 0)
                                        state.referenceNotEscapes = false
                                    else {
                                        println("A")
                                        val definitionSides = definitionSites(method, index)
                                        println("B")
                                        println("C")
                                        var flag = true
                                        val stmt = tacCode.stmts(tacCode.pcToIndex(definitionSides.pc))
                                        if (stmt.isNonVirtualMethodCall) {
                                            checkNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                            flag = false
                                        } else if (stmt.isPutField || stmt.isPutStatic) {
                                            flag = false
                                            //referenceNotEscapes = false
                                            checkPuts(stmt, method, tacCode)
                                        }
                                        if (flag) {
                                            val propertyStoreResult =
                                                propertyStore(definitionSides, EscapeProperty.key)
                                            if (handleEscapeProperty(propertyStoreResult)) {
                                                println("nvmc")
                                                state.referenceNotEscapes = false
                                            }
                                        }
                                    }
                            )
                        }
                    )
                }
                def checkPuts(stmt: Stmt[V], method: Method, tacCode: TACode[TACMethodParameter, V]): Unit = {
                    var defSites: IntTrieSet = IntTrieSet.empty
                    if (stmt.isPutField) {
                        val putField = stmt.asPutField
                        defSites = putField.value.asVar.definedBy
                    } else if (stmt.isPutStatic) {
                        val putStatic = stmt.asPutStatic
                        defSites = putStatic.value.asVar.definedBy
                    } else {
                        println("false1")
                        state.referenceNotEscapes = false
                    }
                    println("defsites: "+defSites)
                    for {
                        i ← defSites
                    } {
                        if (i > 0) {
                            val stmt2 = tacCode.stmts(i)
                            val assignment = stmt2.asAssignment
                            println(
                                s"""i: $i
                                   | assignment: $assignment
                                   |""".stripMargin
                            )
                            if (assignment.expr.isVar) {
                                val v2 = assignment.expr.asVar
                                for (x ← v2.usedBy.iterator) {
                                    val stmt3 = tacCode.stmts(x)
                                    if (stmt3.isNonVirtualMethodCall) {
                                        println("is nvmc")
                                        val nonVirtualMethodCall = stmt3.asNonVirtualMethodCall
                                        checkNonVirtualMethodCall(method, nonVirtualMethodCall, tacCode)
                                    } else { state.referenceNotEscapes = false }
                                    //TODO andere F#lle bedenken
                                }
                            } else if (assignment.expr.isNew) {
                                if (!method.isConstructor) {
                                    val propertyStoreResult =
                                        propertyStore(definitionSites(method, assignment.pc), EscapeProperty.key)
                                    if (handleEscapeProperty(propertyStoreResult)) {
                                        println("false3")
                                        state.referenceNotEscapes = false
                                    }
                                } else {
                                    val useSites =
                                        assignment.targetVar.usedBy

                                    for (i ← useSites) {
                                        val stmt = tacCode.stmts(i)
                                        if (stmt.isNonVirtualMethodCall) {
                                            checkNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                        }
                                        if (stmt.isPutStatic || stmt.isPutField) {
                                            //checkPuts(stmt, method, tacCode)
                                        } //else state.referenceNotEscapes = false
                                    }
                                }

                                /*taCode.stmts(i) match {
                                  case nvmc: NonVirtualMethodCall ⇒ checkNonVirtualMethodCall(method, nvmc)
                              } */
                                //else state.referenceNotEscapes = false
                                //)
                                //}

                                /*val constrUseSites = assignment.targetVar.usedBy
                                    for (us ← constrUseSites.iterator) {
                                        val stmt = taCode.stmts(us)
                                        if (stmt.isNonVirtualMethodCall) {
                                            checkNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall)
                                        }
                                        //TODO alle Fälle abdecken
                                        //TODO escape analyse für Object
                                        // else ; aktuelles putfield bedenken
                                    }*/
                            } else if (!assignment.expr.isConst) {
                                println("false5")
                                state.referenceNotEscapes = false
                            }
                        } else {
                            println("false6")
                            state.referenceNotEscapes = false
                        }
                    }
                }

                for {
                    (method, pcs) ← fieldAccessInformation.writeAccesses(field)
                    taCode ← getTACAI(method)
                    pc ← pcs
                } {
                    val index = taCode.pcToIndex(pc)
                    val staticAddition = if (method.isStatic) 1 else 0
                    if (index > (-1 + staticAddition)) {
                        val stmt = taCode.stmts(index)
                        checkPuts(stmt, method, taCode)

                    }
                }
            }
            println(s"""ne: ${state.referenceNotEscapes}""")
            state.referenceNotEscapes = field.isPrivate
            println(s"""ne: ${state.referenceNotEscapes}""")
            if (state.referenceNotEscapes)
                checkFieldWritesForEffImmutability(state.field)
            println(s"""ne: ${state.referenceNotEscapes}""")
            if (state.referenceNotEscapes)
                checkFieldReadsForEffImmutability(state.field)
            println(s"""ne: ${state.referenceNotEscapes}""")

        }

        def createResult(state: State): ProperPropertyComputationResult = {
            /*if (field.name.contains("hash") &&
                state.field.classFile.thisType.simpleName.contains("AclEntry")) */
            //". == ObjectType("java/nio/file/attribute/AclEntry"))
            println(
                s"""
                 | field: $field
                 | ref imm: ${state.referenceImmutability}
                 | type imm: ${state.typeImmutability}
                 | not escapes: ${state.referenceNotEscapes}
                 | dep imm: ${state.dependentImmutability}
                 |""".stripMargin
            )
            state.referenceImmutability match {
                case Some(false) | None ⇒
                    Result(field, MutableField)
                case Some(true) ⇒ {
                    state.typeImmutability match {
                        case Some(true) ⇒
                            Result(field, DeepImmutableField)
                        case Some(false) | None ⇒ {
                            if (state.referenceNotEscapes)
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

            if (state.field.name.contains("hash") && state.field.classFile.thisType.simpleName.contains("AclEntry"))
                println("continuation; eps: "+eps)

            eps match { //(: @unchecked)

                /*case InterimLUBP(MutableReference | LazyInitializedNotThreadSafeOrNotDeterministicReference, _) ⇒
                    state.typeImmutability = Some(false)
                    if (field.name.contains("hash") &&
                      field.classFile.thisType.simpleName.contains("AclEntry"))
                      println("here")
                    return Result(field, MutableField); */
                case x: InterimEP[_, _] ⇒ {
                    state.dependencies += eps
                    InterimResult(field, MutableField, DeepImmutableField, state.dependencies, c(state))
                }
                case FinalP(DeepImmutableType) ⇒ //state.typeImmutability = Some(true)
                case FinalEP(t, MutableType_new | ShallowImmutableType) ⇒
                    state.typeImmutability = Some(false)
                    if (t != ObjectType.Object) {
                        state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                    }
                case FinalEP(f, DependentImmutableType) ⇒ {
                    state.typeImmutability = Some(false)
                    if (state.dependentImmutability == None)
                        state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                }
                case FinalP(
                    MutableReference | LazyInitializedNotThreadSafeOrNotDeterministicReference
                    ) ⇒ {
                    state.typeImmutability = Some(false)
                    return Result(field, MutableField);
                }
                case FinalP(ImmutableReference) ⇒ {
                    state.referenceImmutability = Some(true)
                    //state.referenceNotEscapes = notEscapes
                }
                case FinalP(
                    LazyInitializedThreadSafeReference |
                    LazyInitializedNotThreadSafeButDeterministicReference
                    ) ⇒ { //TODO
                    state.referenceImmutability = Some(true)
                }

                case _ if eps.isFinal && eps.asEPK.pk == TACAI.key ⇒
                    checkIfReferencedObjectCanEscape(state)

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
            case FinalP(ImmutableReference) ⇒ {
                state.referenceImmutability = Some(true)
                //state.referenceNotEscapes = notEscapes
            }
            case FinalP(LazyInitializedThreadSafeReference | LazyInitializedNotThreadSafeButDeterministicReference) ⇒
                state.referenceImmutability = Some(true)
            case FinalP(MutableReference | LazyInitializedNotThreadSafeOrNotDeterministicReference) ⇒
                return Result(field, MutableField);
            case ep @ _ ⇒ {
                state.dependencies += ep
            }
        }

        loadFormalTypeparameters()

        handleTypeImmutability(state)

        hasGenericType(state)

        if (state.referenceImmutability == Some(true) && state.typeImmutability != Some(true)) {
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