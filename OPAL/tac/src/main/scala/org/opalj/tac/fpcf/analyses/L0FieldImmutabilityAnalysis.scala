/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.analyses

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
import org.opalj.br.fpcf.analyses.DependentImmutabilityKind.DependentImmutabilityKind
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedReference
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

case class State(f: Field) {
    var field: Field = f
    var typeImmutability: Option[Boolean] = None
    var referenceImmutability: Option[Boolean] = None
    var dependentImmutability: Option[DependentImmutabilityKind] = None
    var genericTypeSetNotDeepImmutable = false
}

object DependentImmutabilityKind extends Enumeration {
    type DependentImmutabilityKind = Value
    val dependent, nowShallowOrMutable, onlyDeepImmutable = Value
}

/**
 * Analyses that determines the mutability of org.opalj.br.Field
 * Because it depends on the Field Immutability Lattice it combines the immutability of the fields reference and
 * it's type. Thus needs the information of the reference of the field from the [[L0ReferenceImmutabilityAnalysis]]
 * and the information of the type immutability determined by the type immutability analysis.
 * Till now it uses the old type immutability analysis.
 *
 * @author Tobias Peter Roth
 */
class L0FieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends FPCFAnalysis {

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    def doDetermineFieldImmutability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒
                determineFieldImmutability(field)
            case _ ⇒
                val m = entity.getClass.getName+"is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    private[analyses] def determineFieldImmutability(
        field: Field
    ): ProperPropertyComputationResult = {
        var dependencies: Set[EOptionP[Entity, Property]] = Set.empty
        var classFormalTypeParameters: Option[Set[String]] = None
        def loadFormalTypeparameters() = {
            var result: Set[String] = Set.empty
            field.classFile.attributes.foreach(
                x ⇒
                    x match {
                        case SourceFile(_) ⇒
                        case ClassSignature(typeParameters, _, _) ⇒
                            typeParameters.foreach(
                                y ⇒
                                    y match {
                                        case FormalTypeParameter(identifier, _, _) ⇒ result += identifier
                                        case _                                     ⇒
                                    }
                            )
                        case _ ⇒
                    }
            )
            if (result.size > 0)
                classFormalTypeParameters = Some(result)
        }
        def isInClassesGenericTypeParameters(string: String): Boolean = {
            if (classFormalTypeParameters == None)
                false
            else
                classFormalTypeParameters.head.contains(string)
        }

        def handleTypeImmutability(state: State) = {
            val objectType = field.fieldType.asFieldType
            if (objectType.isArrayType) {
                state.typeImmutability = Some(true)
            } else if (objectType.isBaseType) {
                state.typeImmutability = Some(true);
            } else {
                val result = propertyStore(objectType, TypeImmutability_new.key)
                dependencies = dependencies.filter(_.e ne result.e)
                result match {
                    case FinalEP(e, DeepImmutableType) ⇒ {
                        state.typeImmutability = Some(true);
                    }
                    case FinalEP(f, DependentImmutableType) ⇒ {
                        if (dependencies.size > 0)
                            state.typeImmutability = None
                        else
                            state.typeImmutability = Some(false)
                    }
                    case FinalEP(e, ShallowImmutableType | MutableType_new) ⇒ {
                        state.typeImmutability = Some(false)
                        if (state.field.fieldType.isObjectType &&
                            state.field.fieldType.asObjectType != ObjectType("java/lang/Object")) {
                            state.dependentImmutability = None //when the generic type is still final
                            state.genericTypeSetNotDeepImmutable = true
                        }
                    }
                    case ep: InterimEP[e, p] ⇒ dependencies += ep
                    case epk @ _             ⇒ dependencies += epk
                }
            }
        }

        def hasGenericType(state: State): Unit = {
            var flag_noShallow = true
            var flag_onlyDeep = true
            var genericFields: List[ObjectType] = List()
            state.field.asField.attributes.foreach(
                _ match {
                    case RuntimeInvisibleAnnotationTable(_) ⇒
                    case TypeVariableSignature(t) ⇒
                        flag_onlyDeep = false
                        if (!isInClassesGenericTypeParameters(t)) {
                            flag_noShallow = false
                        }

                    case ClassTypeSignature(
                        packageIdentifier,
                        SimpleClassTypeSignature(simpleName, typeArguments),
                        _
                        ) ⇒
                        typeArguments
                            .foreach({
                                _ match {
                                    case ProperTypeArgument(
                                        variance,
                                        TypeVariableSignature(identifier: String)
                                        ) ⇒
                                        flag_onlyDeep = false
                                        if (!isInClassesGenericTypeParameters(identifier)) {
                                            flag_noShallow = false
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
                                                case Some(pid1) ⇒ pid1 + packageIdentifier2
                                                case _          ⇒ packageIdentifier2
                                            }
                                        genericFields = ObjectType(oPath) :: genericFields
                                    }
                                    case _ ⇒
                                        flag_noShallow = false
                                        flag_onlyDeep = false
                                }
                            })

                    case _ ⇒
                        flag_noShallow = false
                        flag_onlyDeep = false
                }
            )
            genericFields.foreach(objectType ⇒ {

                val result = propertyStore(objectType, TypeImmutability_new.key)
                dependencies = dependencies.filter(_.e ne result.e)
                result match {
                    case FinalP(DeepImmutableType) ⇒
                    case FinalP(ShallowImmutableType | MutableType_new) ⇒ {
                        flag_noShallow = false
                        flag_onlyDeep = false
                    }
                    case ep: InterimEP[e, p] ⇒ dependencies += ep
                    case ep @ _ ⇒
                        dependencies += ep
                }

            })

            if (state.field.asField.attributes.size == state.field.asField.attributes
                .collect({ case x @ RuntimeInvisibleAnnotationTable(_) ⇒ x })
                .size) {
                flag_noShallow = false
                flag_onlyDeep = false
            }

            if (flag_onlyDeep)
                state.dependentImmutability = Some(DependentImmutabilityKind.onlyDeepImmutable)
            else if (flag_noShallow)
                state.dependentImmutability = Some(DependentImmutabilityKind.nowShallowOrMutable)
        }

        def createResult(state: State): ProperPropertyComputationResult = {
            state.referenceImmutability match {
                case Some(false) | None ⇒
                    Result(field, MutableField)
                case Some(true) ⇒ {
                    state.typeImmutability match {
                        case Some(true) ⇒
                            Result(field, DeepImmutableField)
                        case Some(false) | None ⇒ {
                            if (state.genericTypeSetNotDeepImmutable)
                                Result(field, ShallowImmutableField)
                            else
                                state.dependentImmutability match {
                                    case Some(DependentImmutabilityKind.nowShallowOrMutable) ⇒
                                        Result(field, DependentImmutableField)
                                    case Some(DependentImmutabilityKind.onlyDeepImmutable) ⇒
                                        Result(field, DeepImmutableField)
                                    case _ ⇒ Result(field, ShallowImmutableField)
                                }
                        }
                    }
                }
            }
        }

        def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
            dependencies = dependencies.filter(_.e ne eps.e)
            (eps: @unchecked) match {
                case x: InterimEP[_, _] ⇒ {
                    dependencies += eps
                    InterimResult(field, MutableField, DeepImmutableField, dependencies, c(state))
                }
                case FinalEP(_, DeepImmutableType) ⇒ state.typeImmutability = Some(true)
                case FinalEP(t, MutableType_new | ShallowImmutableType) ⇒
                    state.typeImmutability = Some(false)
                    if (t == state.field.fieldType &&
                        state.field.fieldType.isObjectType &&
                        state.field.fieldType.asObjectType != ObjectType("java/lang/Object")) {
                        state.dependentImmutability = None
                        state.genericTypeSetNotDeepImmutable = true
                    }
                case FinalEP(f, DependentImmutableType) ⇒ {
                    state.typeImmutability = Some(false)
                    if (state.dependentImmutability == None)
                        state.dependentImmutability = Some(DependentImmutabilityKind.dependent)
                }
                case x @ FinalEP(_, MutableReference) ⇒ {
                    state.referenceImmutability = Some(false)
                }
                case x @ FinalEP(_, ImmutableReference | LazyInitializedReference) ⇒ { //TODO
                    state.referenceImmutability = Some(true)
                }
                case x @ _ ⇒
                    dependencies = dependencies + x
            }
            if (dependencies.isEmpty) createResult(state)
            else
                InterimResult(
                    field,
                    MutableField,
                    DeepImmutableField,
                    dependencies,
                    c(state)
                )
        }
        val state: State = new State(field)

        val result = propertyStore(state.field, ReferenceImmutability.key)
        result match {
            case FinalEP(_, ImmutableReference | LazyInitializedReference) ⇒
                state.referenceImmutability = Some(true)
            case FinalP(MutableReference) ⇒ return Result(field, MutableField)
            case x @ _ ⇒ {
                dependencies += x
            }
        }

        loadFormalTypeparameters()
        hasGenericType(state)
        handleTypeImmutability(state)
        if (dependencies.isEmpty)
            createResult(state)
        else
            InterimResult(
                field,
                MutableField,
                DeepImmutableField,
                dependencies,
                c(state)
            )

    }

}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
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
        val fields = p.allFields
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
