/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

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
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.ImmutableFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableFieldReference
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.br.fpcf.properties.ShallowImmutableType
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
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.value.ASArrayValue
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
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

/**
 * Analyses that determines the immutability of org.opalj.br.Field
 * It uses the results of the [[L3FieldReferenceImmutabilityAnalysis]]
 * and of the [[L1TypeImmutabilityAnalysis]]
 *
 * @author Tobias Roth
 *
 */
class L3FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    /**
     *  Describes the different kinds of dependent immutable fields:
     *  [[DependentImmutabilityKind.Dependent]] Shallow or mutable types could still exist
     *  [[DependentImmutabilityKind.NotShallowOrMutable]] There are no shallow or mutable types
     *  [[DependentImmutabilityKind.OnlyDeepImmutable]] There are no generic parameters left.
     *  All have been replaced with deep immutable types.
     */
    object DependentImmutabilityKind extends Enumeration {
        type DependentImmutabilityKind = Value
        val NotShallowOrMutable, Dependent, OnlyDeepImmutable = Value
    }

    import DependentImmutabilityKind._

    case class State(
            val field:                           Field,
            var typeIsImmutable:                 Boolean                                            = true,
            var referenceIsImmutable:            Option[Boolean]                                    = None,
            var noEscapePossibilityViaReference: Boolean                                            = true,
            var dependentImmutability:           DependentImmutabilityKind                          = OnlyDeepImmutable,
            var genericTypeSetNotDeepImmutable:  Boolean                                            = false,
            var tacDependees:                    Map[Method, (EOptionP[Method, TACAI], (PCs, PCs))] = Map.empty,
            var dependees:                       Set[EOptionP[Entity, Property]]                    = Set.empty,
            var innerArrayTypes:                 Set[ObjectType]                                    = Set.empty,
            var escapesStillDetermined:          Boolean                                            = false,
            var concreteClassTypeIsKnown:        Boolean                                            = false,
            var totalNumberOfFieldWrites:        Int                                                = 0
    ) {

        def hasDependees: Boolean = {
            !dependees.isEmpty || tacDependees.nonEmpty
        }

        def getDependees: Traversable[EOptionP[Entity, Property]] = {
            dependees ++ tacDependees.valuesIterator.map(_._1)
        }
    }

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
                val m = s"${entity.getClass.getName}  is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

        var classFormalTypeParameters: Option[Set[String]] = None

        /**
         * Loads the formal type parameters from the classes and outer class signature
         */
        def loadFormalTypeParameter(): Unit = {
            import org.opalj.br.ClassFile
            var result: Set[String] = Set.empty

            /**
             *
             * Extract the formal type parameter if it exists of a class attribute
             */
            def collectFormalTypeParameterFromClassAttribute(attribute: Attribute): Unit = attribute match {
                case ClassSignature(typeParameters, _, _) ⇒ {
                    typeParameters.foreach(
                        _ match {
                            case FormalTypeParameter(identifier, _, _) ⇒
                                result += identifier
                            case parameter ⇒
                        }
                    )
                }
                case _ ⇒
            }

            /**
             * If the genericity is nested in an inner class
             * collect the generic type parameters from the field' outer classes
             */
            def collectAllFormalParametersFromOuterClasses(classFile: ClassFile): Unit = {
                classFile.attributes.foreach(collectFormalTypeParameterFromClassAttribute)
                if (classFile.outerType.isDefined) {
                    val outerClassFile = project.classFile(classFile.outerType.get._1)
                    if (outerClassFile.isDefined && outerClassFile.get != classFile) {
                        collectAllFormalParametersFromOuterClasses(outerClassFile.get)
                    }
                }
            }
            collectAllFormalParametersFromOuterClasses(field.classFile)

            if (result.nonEmpty) {
                classFormalTypeParameters = Some(result)
                /*println(
                    s"""
                       |field: ${field}
                       | parameters: ${classFormalTypeParameters.mkString("\n ")}
                       |""".stripMargin
                ) */
            }
        }

        /**
         * Returns, if a generic parameter like e.g. 'T' is in the class' or the first outer class' Signature
         * @param string The generic type parameter that should be looked for
         */
        def isInClassesGenericTypeParameters(string: String): Boolean =
            classFormalTypeParameters.isDefined && classFormalTypeParameters.get.contains(string)

        /**
         * Determines the immutability of a fields type. Adjusts the state and registers the dependencies if necessary.
         *
         */
        def handleTypeImmutability(state: State): Unit = {
            val objectType = field.fieldType.asFieldType
            if (objectType == ObjectType.Object) {
                // println("i1")
                state.typeIsImmutable = false //handling generic fields
            } else if (objectType.isBaseType || objectType == ObjectType.String) {
                // base types are by design deep immutable
                //state.typeImmutability = Some(true) // true is default
            } else if (objectType.isArrayType) {
                // println("i3")
                // Because the entries of an array can be reassigned we state it as not being deep immutable
                state.typeIsImmutable = false
            } else {
                // println("i4")
                propertyStore(objectType, TypeImmutability.key) match {
                    case FinalP(DeepImmutableType) ⇒ // deep immutable type is set as default
                    case FinalP(DependentImmutableType) ⇒
                        state.typeIsImmutable = false
                    case UBP(ShallowImmutableType | MutableType) ⇒
                        state.typeIsImmutable = false
                        if (field.fieldType != ObjectType.Object)
                            state.dependentImmutability = DependentImmutabilityKind.Dependent
                    case epk ⇒ state.dependees += epk
                }
            }
        }

        def hasGenericType()(implicit state: State): Unit = {
            var noShallowOrMutableTypesInGenericTypeFound = true
            var onlyDeepImmutableTypesInGenericTypeFound = true
            var genericParameters: List[ObjectType] = List()
            var noRelevantAttributesFound = true
            state.field.attributes.foreach(
                _ match {
                    case RuntimeInvisibleAnnotationTable(_) ⇒
                    case SourceFile(_)                      ⇒
                    case TypeVariableSignature(t) ⇒
                        noRelevantAttributesFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        if (!isInClassesGenericTypeParameters(t))
                            noShallowOrMutableTypesInGenericTypeFound = false
                    case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) ⇒
                        noRelevantAttributesFound = false
                        typeArguments.foreach({
                            _ match {
                                case ProperTypeArgument(_, TypeVariableSignature(identifier)) ⇒
                                    onlyDeepImmutableTypesInGenericTypeFound = false
                                    if (!isInClassesGenericTypeParameters(identifier))
                                        noShallowOrMutableTypesInGenericTypeFound = false
                                case ProperTypeArgument(_, ClassTypeSignature(outerPackageIdentifier,
                                    SimpleClassTypeSignature(innerPackageIdentifier, _), _)) ⇒ {
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
                        noShallowOrMutableTypesInGenericTypeFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                }
            )
            genericParameters.foreach(objectType ⇒ {
                propertyStore(objectType, TypeImmutability.key) match {
                    case FinalP(DeepImmutableType) ⇒ //nothing to do here: default value is deep immutable
                    case FinalP(DependentImmutableType) ⇒
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        state.typeIsImmutable = false
                    case UBP(ShallowImmutableType | MutableType) ⇒ {
                        noShallowOrMutableTypesInGenericTypeFound = false
                        onlyDeepImmutableTypesInGenericTypeFound = false
                        state.typeIsImmutable = false
                    }
                    case ep ⇒
                        state.dependees += ep
                }
            })

            //Prevents the case of keeping the default values of these
            // flags only because of no relevant attribute has been found
            if (!noRelevantAttributesFound) {
                if (onlyDeepImmutableTypesInGenericTypeFound) {
                    //nothing to do...
                } else if (noShallowOrMutableTypesInGenericTypeFound &&
                    state.dependentImmutability != DependentImmutabilityKind.Dependent) {
                    state.dependentImmutability = DependentImmutabilityKind.NotShallowOrMutable
                } else
                    state.dependentImmutability = DependentImmutabilityKind.Dependent

            } else
                state.dependentImmutability = DependentImmutabilityKind.Dependent
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
                case finalEP: FinalEP[Method, TACAI] ⇒
                    finalEP.ub.tac
                case epk ⇒
                    var reads = IntTrieSet.empty
                    var writes = IntTrieSet.empty
                    if (state.tacDependees.contains(method)) {
                        reads = state.tacDependees(method)._2._1
                        writes = state.tacDependees(method)._2._2
                    }
                    if (isRead) {
                        state.tacDependees += method -> ((epk, (reads ++ pcs, writes)))
                    }
                    if (!isRead) {
                        state.tacDependees += method -> ((epk, (reads, writes ++ pcs)))
                    }
                    None
            }
        }

        /**
         * Handles the influence of an escape property on the field mutability.
         * @return true if the object - on which a field write occurred - escapes, false otherwise.
         * @note (Re-)Adds dependees as necessary.
         */
        def doesItEscapeViaMethod(
            ep: EOptionP[DefinitionSite, EscapeProperty]
        )(implicit state: State): Boolean = {
            ep match {
                case FinalP(NoEscape) ⇒ false
                case InterimUBP(NoEscape) ⇒
                    state.dependees += ep
                    false
                case UBP(EscapeInCallee | EscapeViaReturn)      ⇒ true
                case FinalP(AtMost(_))                          ⇒ true
                case _: FinalEP[DefinitionSite, EscapeProperty] ⇒ true // Escape state is worse than via return
                case InterimUBP(AtMost(_)) ⇒
                    true
                case _: InterimEP[DefinitionSite, EscapeProperty] ⇒
                    true // Escape state is worse than via return
                case epk ⇒
                    state.dependees += epk
                    false
            }
        }

        /**
         * Determine if the referenced object can escape either via field reads or writes.
         */
        def determineEscapeOfReferencedObjectOrValue()(implicit state: State): Unit = {
            state.escapesStillDetermined = true
            state.noEscapePossibilityViaReference = state.field.isPrivate

            if (state.noEscapePossibilityViaReference) {
                val writes = fieldAccessInformation.writeAccesses(state.field)

                //has to be determined before the following foreach loop because the information is needed
                state.totalNumberOfFieldWrites = writes.foldLeft(0)(_ + _._2.size)
                //println("total amaount ouf writes: "+state.totalNumberOfFieldWrites)
                writes.foreach(writeAccess ⇒ {

                    val method = writeAccess._1
                    val pcs = writeAccess._2
                    checkFieldWritesForEffImmutability(method, pcs)
                })
                //println("state no escape 1: "+state.noEscapePossibilityViaReference)
                if (state.noEscapePossibilityViaReference) {
                    val reads = fieldAccessInformation.readAccesses(state.field)
                    reads.foreach(read ⇒ {
                        val method = read._1
                        val pcs = read._2
                        determineEscapePossibilityViaFieldReads(method, pcs)
                    })
                    //println("state no escape 2: "+state.noEscapePossibilityViaReference)
                }
            }
        }

        /**
         * Determine if the referenced object can escape via field reads.
         */
        def determineEscapePossibilityViaFieldReads(method: Method, pcs: PCs)(implicit state: State): Unit = {

            val taCodeOption = getTACAI(method, pcs, isRead = true)
            if (taCodeOption.isDefined) {
                val taCode = taCodeOption.get
                pcs.foreach(pc ⇒ {
                    val readIndex = taCode.pcToIndex(pc)
                    // This if-statement is necessary, because there are -1 elements in the array
                    //println("readIndex: "+readIndex)
                    if (readIndex != -1) {
                        val stmt = taCode.stmts(readIndex)
                        //println("read stmt: "+stmt)
                        //println("index: "+taCode.pcToIndex(stmt.pc))
                        if (stmt.isAssignment) {
                            val assignment = stmt.asAssignment

                            /*if (doesItEscapeViaMethod(
                                propertyStore(definitionSites(method, assignment.pc), EscapeProperty.key)
                            )) {
                                state.noEscapePossibilityViaReference = false
                                println("false1")
                                return ;
                            } else */ for {
                                useSite ← assignment.targetVar.usedBy
                            } {
                                val fieldsUseSiteStmt = taCode.stmts(useSite)
                                //println("fieldUseSiteStmt: "+fieldsUseSiteStmt)
                                if (fieldsUseSiteStmt.isAssignment) {
                                    val assignment = fieldsUseSiteStmt.asAssignment
                                    if (assignment.expr.isVirtualFunctionCall) {
                                        // import org.opalj.br.fpcf.properties.Purity
                                        val virtualFunctionCall = assignment.expr.asVirtualFunctionCall
                                        // println("virtualfunction call: "+virtualFunctionCall)
                                        // println("isconst: "+virtualFunctionCall.isIntConst)
                                        //println("field Type: "+field.fieldType)
                                        if (field.fieldType.isObjectType) {
                                            //val m = virtualFunctionCall.resolveCallTargets(field.fieldType.asObjectType)
                                            if (virtualFunctionCall.params.exists(!_.isConst)) {
                                                state.noEscapePossibilityViaReference = false

                                                //println("for all is const: "+)
                                                //println("...................................."+field.classFile.thisType.simpleName)
                                                //if (field.classFile.thisType.simpleName.contains("EffectivelyImmutableFields")) {
                                                //println("is const: "+virtualFunctionCall.isCompare)
                                                //m.foreach(m ⇒ println())
                                                //pri//
                                                // ntln("m: "+m)
                                                //throw new Exception("")
                                            }
                                        }

                                        // val propertyResult = propertyStore(declaredMethods(method), Purity.key)
                                        // println("propertyResult: "+propertyResult)
                                    } else if (assignment.expr.isArrayLoad) {

                                        val arrayLoad = assignment.expr.asArrayLoad
                                        arrayLoad.arrayRef.asVar.value.toCanonicalForm match {
                                            case value: ASArrayValue ⇒
                                                val innerArrayType = value.theUpperTypeBound.componentType
                                                if (innerArrayType.isBaseType) {
                                                    // nothing to do, because it can not be mutated
                                                } else if (innerArrayType.isArrayType) {
                                                    state.noEscapePossibilityViaReference = false // to be sound
                                                    // println("false2")
                                                    return ;
                                                } else if (innerArrayType.isObjectType) {
                                                    //If a deep immutable object escapes, it can not be mutated
                                                    propertyStore(innerArrayType, TypeImmutability.key) match {
                                                        case FinalP(DeepImmutableType) ⇒ //nothing to do
                                                        case UBP(DependentImmutableType |
                                                            ShallowImmutableType |
                                                            MutableType) ⇒
                                                            state.noEscapePossibilityViaReference = false
                                                            return ;
                                                        case ep ⇒ {
                                                            state.innerArrayTypes += innerArrayType.asObjectType
                                                            state.dependees += ep
                                                        }
                                                    }
                                                } else {
                                                    state.noEscapePossibilityViaReference = false
                                                    return ;
                                                }
                                            case _ ⇒ {
                                                state.noEscapePossibilityViaReference = false
                                                return ;
                                            }
                                        }
                                    } else {
                                        state.noEscapePossibilityViaReference = false
                                        return ;
                                    }
                                } else if (fieldsUseSiteStmt.isMonitorEnter ||
                                    fieldsUseSiteStmt.isMonitorExit ||
                                    fieldsUseSiteStmt.isIf) {
                                    //nothing to do
                                } else {
                                    state.noEscapePossibilityViaReference = false
                                    //   println("false7")
                                    return ;
                                }
                            }
                        } else if (stmt.isExprStmt) {
                            //nothing to do here, because the value is only read but not assigned to another one
                        } else {
                            state.noEscapePossibilityViaReference = false
                            //   println("false8")
                            return ;
                        }
                    } else {
                        //nothing to do
                        // -1 means NOTHING as a placeholder
                    }
                })
            }
        }

        /**
         * Determine if the referenced object can escape via field writes
         */
        def checkFieldWritesForEffImmutability(method: Method, pcs: PCs)(implicit state: State): Unit = {
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
                /*println(
                    s"""
                     | static function call
                     | params: ${staticFunctionCall.params}
                     | ${staticFunctionCall.params.map(x ⇒ tacCode.stmts(x.asVar.definedBy.head).asAssignment.expr.isConst)}
                     |""".stripMargin
                )*/
                if (staticFunctionCall.params.exists(p ⇒
                    p.asVar.definedBy.size != 1 ||
                        p.asVar.definedBy.head < 0 ||
                        !tacCode.stmts(p.asVar.definedBy.head).asAssignment.expr.isConst)) {
                    state.noEscapePossibilityViaReference = false
                    return ;
                } //else println("ELSE")
            }

            /**
             * In case of the concrete assigned classtype is known this method handles the immutability of it.
             * @note [[state.concreteClassTypeIsKnown]] must be set to true, when calling this function
             */
            def handleKnownClassType(objectType: ObjectType)(implicit state: State): Unit = {
                val result = propertyStore(objectType, ClassImmutability.key)
                //println(s"handle type imm result: $result")
                result match {
                    case UBP(MutableClass |
                        ShallowImmutableClass |
                        DependentImmutableClass) ⇒ state.typeIsImmutable = false
                    case FinalP(DeepImmutableClass) ⇒ state.typeIsImmutable = true
                    case eps                        ⇒ state.dependees += eps
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
                                    return ;
                                } else {
                                    val paramDefinitionStmt = tacCode.stmts(paramDefinedByIndex)
                                    if (paramDefinitionStmt.isAssignment) {
                                        val assignmentExpression = paramDefinitionStmt.asAssignment.expr
                                        if (assignmentExpression.isGetField ||
                                            assignmentExpression.isGetStatic) {
                                            var assignedField: Option[Field] = None
                                            if (assignmentExpression.isGetField)
                                                assignedField = assignmentExpression.asGetField.resolveField
                                            else if (assignmentExpression.isGetStatic)
                                                assignedField = assignmentExpression.asGetStatic.resolveField

                                            if (assignedField.isDefined && assignedField.get != state.field)
                                                propertyStore(assignedField.get, FieldImmutability.key) match {
                                                    case FinalP(DeepImmutableField) ⇒ //nothing to do here
                                                    case FinalP(_) ⇒
                                                        state.noEscapePossibilityViaReference = false
                                                        return ;
                                                    case ep ⇒ state.dependees += ep
                                                }
                                        } else if (assignmentExpression.isVirtualFunctionCall) {
                                            val virtualFunctionCall = assignmentExpression.asVirtualFunctionCall
                                            virtualFunctionCall.params.exists(
                                                param ⇒ param.asVar.definedBy.head < 0 ||
                                                    !tacCode.stmts(param.asVar.definedBy.head).asAssignment.expr.isConst
                                            )
                                        } else if (assignmentExpression.isStaticFunctionCall) {
                                            handleStaticFunctionCall(assignmentExpression.asStaticFunctionCall, tacCode)
                                            return ;
                                        } else if (assignmentExpression.isNew) {
                                            val newStmt = assignmentExpression.asNew
                                            if (field.fieldType.isObjectType &&

                                                newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType &&
                                                state.totalNumberOfFieldWrites == 1) {
                                                state.concreteClassTypeIsKnown = true
                                                handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                            }
                                            for (usedSiteIndex ← paramDefinitionStmt.asAssignment.targetVar.asVar.usedBy) {
                                                val stmt = tacCode.stmts(usedSiteIndex)
                                                if (stmt.isNonVirtualMethodCall) {
                                                    if (!seen.contains(stmt)) {
                                                        seen += stmt
                                                        handleNonVirtualMethodCall(method, stmt.asNonVirtualMethodCall, tacCode)
                                                    }
                                                } else {
                                                    state.noEscapePossibilityViaReference = false
                                                    return ;
                                                }
                                            }
                                        } else if (assignmentExpression.isConst) {
                                            //nothing to do
                                        } else {
                                            state.noEscapePossibilityViaReference = false
                                            return ;
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
                                            return ;
                                        } else if (stmt.isArrayStore) {
                                            state.noEscapePossibilityViaReference = false //TODO handling that case more precise
                                            return ;
                                        } else { // other cases that the purity analysis can not handle
                                            if (doesItEscapeViaMethod(
                                                propertyStore(definitionSitesOfParam, EscapeProperty.key)
                                            )) {
                                                state.noEscapePossibilityViaReference = false
                                                return ;
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

                val (putDefinitionSites, putValue) = {
                    if (putStmt.isPutField) {
                        val putField = putStmt.asPutField
                        (putField.value.asVar.definedBy, putField.value)
                    } else if (putStmt.isPutStatic) {
                        val putStatic = putStmt.asPutStatic
                        (putStatic.value.asVar.definedBy, putStatic.value)
                    } else {
                        state.noEscapePossibilityViaReference = false
                        return ;
                    }
                }

                val putValueDefinedByIndex = putValue.asVar.definedBy.head
                if (putValue.asVar.value.isArrayValue == Yes) {

                    if (putValueDefinedByIndex >= 0) { //necessary
                        tacCode.stmts(putValueDefinedByIndex).asAssignment.targetVar.usedBy.foreach(usedByIndex ⇒ {
                            val arrayStmt = tacCode.stmts(usedByIndex)
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
                                                        !tacCode.stmts(param.asVar.definedBy.head).asAssignment.expr.isConst) {
                                                        state.noEscapePossibilityViaReference = false
                                                        return ;
                                                    }
                                                })
                                            } else if (useSite == arrayStore) {
                                                //nothing to do
                                            } else if (useSite.isReturnValue) {
                                                //assigned array-element escapes
                                                state.noEscapePossibilityViaReference = false
                                                return ;
                                            } else {
                                                state.noEscapePossibilityViaReference = false
                                                return ;
                                            }
                                        }
                                        if (!isArrayIndexConst) {
                                            state.noEscapePossibilityViaReference = false
                                            return ;
                                        } else if (assignedExpr.isStaticFunctionCall) {
                                            handleStaticFunctionCall(assignedExpr.asStaticFunctionCall, tacCode)
                                        } else if (assignedExpr.isNew) {
                                            val newStmt = assignedExpr.asNew
                                            if (field.fieldType.isObjectType &&
                                                newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType &&
                                                state.totalNumberOfFieldWrites == 1) {
                                                state.concreteClassTypeIsKnown = true
                                                handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                            }
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
                                                    return ;
                                                }
                                            })
                                        } else {
                                            state.noEscapePossibilityViaReference = false
                                            return ;
                                        }
                                    } else {
                                        state.noEscapePossibilityViaReference = false
                                        return ;
                                    }
                                } else {
                                    state.noEscapePossibilityViaReference = false
                                    return ;
                                }
                        })
                    } else {
                        state.noEscapePossibilityViaReference = false
                    }
                } else
                    for {
                        i ← putDefinitionSites
                    } {
                        if (i >= 0) { //necessary
                            val definitionSiteStatement = tacCode.stmts(i)
                            //println("def site stmt: "+definitionSiteStatement)
                            val definitionSiteAssignment = definitionSiteStatement.asAssignment
                            //println("def site assignement: "+definitionSiteAssignment)
                            if (definitionSiteAssignment.expr.isStaticFunctionCall) {
                                // println("handle static function call")
                                handleStaticFunctionCall(definitionSiteAssignment.expr.asStaticFunctionCall, tacCode)
                                return ;
                            } else if (definitionSiteAssignment.expr.isVar) {
                                //  println("def site is var")
                                val definitionSiteVar = definitionSiteAssignment.expr.asVar
                                for (definitionSiteVarUseSite ← definitionSiteVar.usedBy) {
                                    val definitionSiteVarUseSiteStmt = tacCode.stmts(definitionSiteVarUseSite)
                                    if (definitionSiteVarUseSiteStmt.isNonVirtualMethodCall) {
                                        val nonVirtualMethodCall = definitionSiteVarUseSiteStmt.asNonVirtualMethodCall
                                        handleNonVirtualMethodCall(method, nonVirtualMethodCall, tacCode)
                                    } else {
                                        state.noEscapePossibilityViaReference = false
                                        return ;
                                    }
                                    //TODO andere Fälle bedenken
                                }
                            } else if (definitionSiteAssignment.expr.isNew) {
                                //println("is new")

                                val newStmt = definitionSiteAssignment.expr.asNew
                                /*println(
                                    s"""
                                     | newStmt: $newStmt
                                     | newStmt.tpe.mostPreciseObjectType: ${newStmt.tpe.mostPreciseObjectType}
                                     | fhandleKnownClassTypeieldType: ${field.fieldType.asObjectType}
                                     | state.totalNumberOfFieldWrites
                                     |""".stripMargin
                                ) */
                                if (field.fieldType.isObjectType &&
                                    newStmt.tpe.mostPreciseObjectType == field.fieldType.asObjectType &&
                                    state.totalNumberOfFieldWrites == 1) {
                                    state.concreteClassTypeIsKnown = true
                                    handleKnownClassType(newStmt.tpe.mostPreciseObjectType)
                                }
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
                                            return ;
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
                                            return ;
                                        } else {
                                            state.noEscapePossibilityViaReference = false
                                            return ;
                                        }
                                    }
                                }
                                //TODO alle Fälle abdecken
                            } else if (!definitionSiteAssignment.expr.isConst) {
                                state.noEscapePossibilityViaReference = false
                                return ;
                            }
                        } else {
                            state.noEscapePossibilityViaReference = false
                            return ;
                        }
                    }
            }

            /**
             * Begin of method check field writes
             */
            val tacCodeOption = getTACAI(method, pcs, isRead = false)
            if (tacCodeOption.isDefined) {
                val taCode = tacCodeOption.get
                pcs.foreach(pc ⇒ {
                    val index = taCode.pcToIndex(pc)
                    if (index >= 0) {
                        val stmt = taCode.stmts(index)
                        //println("stmt: "+stmt)
                        if (!seen.contains(stmt)) {
                            seen += stmt
                            handlePut(stmt, method, taCode)
                        }
                    } else {
                        state.noEscapePossibilityViaReference = false
                        return ;
                    }
                })
            }
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult(implicit state: State): ProperPropertyComputationResult = {
            /* println(
                s"""
                 | create result
                 | field: ${state.field}
                 | type is immutable: ${state.typeIsImmutable}
                 | dependent immutability: ${state.dependentImmutability}
                 | does not escape: ${state.noEscapePossibilityViaReference}
                 | concrete classtype is known: ${state.concreteClassTypeIsKnown}
                 |""".stripMargin
            ) */
            if (state.hasDependees) {
                val lowerBound =
                    if (state.referenceIsImmutable.isDefined && state.referenceIsImmutable.get)
                        ShallowImmutableField
                    else
                        MutableField
                val upperBound = {
                    if (!state.referenceIsImmutable.isDefined)
                        DeepImmutableField
                    else
                        state.referenceIsImmutable match {
                            case Some(false) | None ⇒
                                MutableField
                            case Some(true) ⇒ {
                                if (state.tacDependees.isEmpty && !state.concreteClassTypeIsKnown) {
                                    if (state.typeIsImmutable) {
                                        DeepImmutableField
                                    } else {
                                        if (state.noEscapePossibilityViaReference) {
                                            DeepImmutableField
                                        } else {
                                            state.dependentImmutability match {
                                                case NotShallowOrMutable ⇒
                                                    DependentImmutableField
                                                case OnlyDeepImmutable ⇒
                                                    DeepImmutableField
                                                case _ ⇒ {
                                                    ShallowImmutableField
                                                }
                                            }
                                        }
                                    }
                                } else DeepImmutableField
                            }
                        }

                } //TODO check
                DeepImmutableField
                InterimResult(
                    field,
                    lowerBound,
                    upperBound,
                    state.getDependees,
                    c
                )
            } else {
                if (!state.escapesStillDetermined)
                    state.noEscapePossibilityViaReference = false
                state.referenceIsImmutable match {
                    case Some(false) | None ⇒
                        Result(field, MutableField)
                    case Some(true) ⇒ {
                        if (state.typeIsImmutable) {
                            Result(field, DeepImmutableField)
                        } else {
                            if (state.noEscapePossibilityViaReference) {
                                Result(field, DeepImmutableField)
                            } else {
                                state.dependentImmutability match {
                                    case NotShallowOrMutable ⇒
                                        Result(field, DependentImmutableField)
                                    case OnlyDeepImmutable ⇒
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
        }

        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
            import org.opalj.fpcf.EUBP
            if (eps.asEPS.pk != TACAI.key)
                state.dependees = state.dependees.filter(_.e ne eps.e)
            eps match {
                case FinalP(DeepImmutableType) ⇒ //nothing to do -> is default
                case EUBP(t, MutableType | ShallowImmutableType) ⇒
                    state.typeIsImmutable = false
                    if (t != ObjectType.Object) { // in case of generic fields
                        state.dependentImmutability = DependentImmutabilityKind.Dependent
                    }
                    if (t.isInstanceOf[ObjectType] && state.innerArrayTypes.contains(t.asInstanceOf[ObjectType]))
                        state.noEscapePossibilityViaReference = false
                case FinalEP(t, DependentImmutableType) ⇒ {
                    state.typeIsImmutable = false
                    if (t != field.fieldType && state.dependentImmutability == OnlyDeepImmutable)
                        state.dependentImmutability = DependentImmutabilityKind.NotShallowOrMutable
                    if (t.isInstanceOf[ObjectType] && state.innerArrayTypes.contains(t.asInstanceOf[ObjectType]))
                        state.noEscapePossibilityViaReference = false
                }
                case UBP(
                    MutableFieldReference | LazyInitializedNotThreadSafeFieldReference
                    ) ⇒ {
                    state.typeIsImmutable = false
                    state.referenceIsImmutable = Some(false)
                    return Result(field, MutableField);
                }
                case FinalP(
                    ImmutableFieldReference |
                    LazyInitializedThreadSafeFieldReference |
                    LazyInitializedNotThreadSafeButDeterministicFieldReference
                    ) ⇒ {
                    state.referenceIsImmutable = Some(true)
                }
                case FinalP(DeepImmutableField) ⇒ // nothing to do
                case UBP(DependentImmutableField | ShallowImmutableField | MutableField) ⇒
                    state.noEscapePossibilityViaReference = false
                case eps if eps.asEPS.pk == TACAI.key ⇒
                    val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                    val method = newEP.e
                    state.tacDependees -= method
                    val pcs = state.tacDependees(method)._2
                    if (eps.isFinal) {
                        checkFieldWritesForEffImmutability(method, pcs._2)(state)
                        determineEscapePossibilityViaFieldReads(method, pcs._1)(state)
                    } else {
                        state.tacDependees += method -> ((newEP, pcs))
                    }

                case eps if eps.isFinal && eps.asEPS.pk == EscapeProperty.key ⇒
                    if (doesItEscapeViaMethod(eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]])(state))
                        state.noEscapePossibilityViaReference = false
                case FinalP(DeepImmutableClass)                                          ⇒ state.typeIsImmutable = true
                case UBP(ShallowImmutableClass | DependentImmutableClass | MutableClass) ⇒ state.typeIsImmutable = false
                case eps ⇒
                    state.dependees += eps
            }
            createResult
        }

        /**
         * Requests the immutability of the field reference and registers the dependees if necessary
         */
        implicit val state: State = State(field)
        propertyStore(field, FieldReferenceImmutability.key) match {
            case FinalP(ImmutableFieldReference) ⇒ state.referenceIsImmutable = Some(true)
            case FinalP(LazyInitializedThreadSafeFieldReference | LazyInitializedNotThreadSafeButDeterministicFieldReference) ⇒
                state.referenceIsImmutable = Some(true)
            case FinalP(MutableFieldReference | LazyInitializedNotThreadSafeFieldReference) ⇒
                return Result(field, MutableField);
            case ep @ _ ⇒ {
                state.dependees += ep
            }
        }
        // println("A")
        /**
         * Determines whether the field is dependent immutable
         */
        loadFormalTypeParameter()
        hasGenericType()
        // println("B")
        /**
         * Determines whether the reference object escapes or can be mutated.
         */
        if (state.referenceIsImmutable.isEmpty || state.referenceIsImmutable.get) {
            determineEscapeOfReferencedObjectOrValue()
        }
        // println("c")
        /**
         * In cases where we know the concrete class type assigned to the field we could use the immutabiltiy of this.
         */
        if (!state.concreteClassTypeIsKnown) {
            // println("C1")
            handleTypeImmutability(state)
        }
        // println("D")
        createResult
    }
}

trait L3FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    import org.opalj.fpcf.PropertyBounds

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.lub(FieldReferenceImmutability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)
}

/**
 *
 * Executor for the eager field immutability analysis.
 *
 */
object EagerL3FieldImmutabilityAnalysis extends L3FieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L3FieldImmutabilityAnalysis(p)
        val fields = p.allFields // p.allProjectClassFiles.flatMap(classfile ⇒ classfile.fields) // p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldImmutability)
        analysis
    }
}

/**
 *
 * Executor for the lazy field immutability analysis.
 *
 */
object LazyL3FieldImmutabilityAnalysis extends L3FieldImmutabilityAnalysisScheduler
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
