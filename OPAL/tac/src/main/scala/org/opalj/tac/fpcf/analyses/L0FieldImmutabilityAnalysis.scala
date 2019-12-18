/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.analyses

import org.opalj.br.ClassTypeSignature
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ProperTypeArgument
import org.opalj.br.SimpleClassTypeSignature
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
import org.opalj.tac.fpcf.properties.TACAI

case class State(f: Field) {
  var field: Field = f
  var typeImmutability: Option[Boolean] = Some(true)
  var referenceImmutability: Option[Boolean] = None
  var depencenciesTypes: scala.collection.mutable.Set[ObjectType] =
    scala.collection.mutable.Set[ObjectType]()
  var dependentTypeImmutability: Option[Boolean] = None
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
      case field: Field => determineFieldImmutability(field)
      case _ =>
        val m = entity.getClass.getName + "is not an org.opalj.br.Field"
        throw new IllegalArgumentException(m)
    }
  }

  private[analyses] def determineFieldImmutability(
      field: Field
  ): ProperPropertyComputationResult = {

    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty

    def determineGenericFieldImmutability(state: State): Option[Boolean] = {
      println(
        "determine generic field imm-------------------------------------------------------------------"
      )
      var genericFields: Set[String] = Set.empty
      state.field.fieldTypeSignature.head match {
        case ClassTypeSignature(_, SimpleClassTypeSignature(name, typeArguments), _) => {
          typeArguments.foreach(
            ta => {
              ta match {
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
                    ) => {
                  genericFields += packageIdentifier2
                }
                case _ =>
              }

            }
          )
        }
        case _ =>
      }
      genericFields.foreach(f => println("generic Field: " + f))
      //state.typeImmutability = Some(true)

      genericFields.toList.foreach(s => {
        val objectType = ObjectType(s)
        val result = propertyStore(objectType, TypeImmutability_new.key)
        println("Result generic field with objtype: " + objectType + " result: " + result)
        result match {
          case FinalP(DeepImmutableType) =>
          case FinalP(ShallowImmutableType | MutableType_new) => {
            return Some(false); //state.typeImmutability = Some(false)
          }
          case ep @ _ => {
            dependencies += ep
          }
        }
      })
      if (dependencies.size > 0) None;
      else Some(true);
    }
    def handleTypeImmutability(objectType: FieldType)(state: State): Option[Boolean] = {
      if (objectType.isArrayType) return Some(true); //TODO
      if (objectType.isBaseType) return Some(true);
      val result = propertyStore(objectType, TypeImmutability_new.key)
      println("Result: " + result)
      result match {
        case FinalEP(e, DeepImmutableType) => {
          return Some(true);
        }
        case FinalEP(f, DependentImmutableType) => {
          println(f + " has dependent imm type")
          //TODO under construction
          //---------------------------------------------------------------------------------------
          state.typeImmutability = determineGenericFieldImmutability(state)
          //---------------------------------------------------------------------------------------
          //state.dependentTypeImmutability = Some(true)
          return state.typeImmutability;
        }

        case FinalEP(e, ShallowImmutableType) => {
          return Some(false); //TODO mindstorm if this approch is appropriate
        }
        case FinalEP(e, t) if (t == MutableType_new) => { //MutableType) ⇒ {
          return Some(false);
        }
        case x @ _ => {
          dependencies += x
          return None; //TODO check!!!!! None;
        }
      }
    }

    def hasImmutableType(field: Field)(state: State): Option[Boolean] = {
      //val  hasFieldImmutableType =
      handleTypeImmutability(field.fieldType.asFieldType)(state)

      /**
     * hasFieldImmutableType match {
     * case Some(false) => return Some(false);
     * case _ => {
     * state.depencenciesTypes.foreach(
     * t => {
     * val isImmutable = handleTypeImmutability(t)(state)
     * isImmutable match {
     * case Some(false) => return Some(false);
     * case _           =>
     * }
     * }
     * )
     * Some(true)
     * }
     * } *
     */

    }
    def hasImmutableReference(field: Field): Option[Boolean] = {

      propertyStore(field, ReferenceImmutability.key) match {
        case FinalEP(_, MutableReference) => {
          Some(false)
        }
        case FinalEP(_, ImmutableReference | LazyInitializedReference) => { //TODO
          Some(true)
        }
        case x @ _ => {
          dependencies += x
          None
        }
      }
    }

    val state: State = new State(field)

    def createResult(state: State): ProperPropertyComputationResult = {
      state.referenceImmutability match {
        case Some(false) => Result(field, MutableField)
        case Some(true) => {
          //If the field type is object. It is a generic field
          //Test Dependent... //TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          //if (field.fieldType == ObjectType("java/lang/Object")) {
          println("field attributes: " + field.asField.attributes)
          val genericString = field.asField.attributes.toList.collectFirst({
            case TypeVariableSignature(t) => t
            case ClassTypeSignature(
                packageIdentifier,
                SimpleClassTypeSignature(simpleName, typeArguments),
                classTypeSignatureSuffix
                ) => {
              val tA = typeArguments
                .find(typeArgument => {
                  typeArgument match {
                    case ProperTypeArgument(variance, signature) => {
                      signature match {
                        case TypeVariableSignature(identifier) => true
                        case _                                 => false
                      }
                    }
                    case _ => false
                  }

                })
              if (tA.size > 0)
                tA.head match {
                  case ProperTypeArgument(variance, TypeVariableSignature(identifier)) => identifier
                  case _                                                               => ""
                } else ""

            }
          })
          if (!field.attributes.isEmpty && genericString != None && genericString != Some("")) {

            //if(genericString!=None)
            //    println("Generic String: "+genericString)
            //    println(
            //        "test: "+DependentImmutableField(genericString).genericString
            //   }
            Result(field, DependentImmutableField(genericString))

          } else
            state.typeImmutability match {
              case Some(true) => Result(field, DeepImmutableField)
              case Some(false) => {
                state.dependentTypeImmutability match {
                  case Some(true) => Result(field, DependentImmutableField())
                  case _          => Result(field, ShallowImmutableField)
                }
              }
              case None if (dependencies.isEmpty) => Result(field, ShallowImmutableField)
              case None => {
                InterimResult(
                  field,
                  MutableField,
                  DeepImmutableField,
                  dependencies,
                  c(state)
                )
              }
            }
        }
        case None if (dependencies.isEmpty) => Result(field, MutableField)
        case None => {
          InterimResult(
            field,
            MutableField,
            DeepImmutableField,
            dependencies,
            c(state)
          )
        }

      }
    }
    def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
      dependencies = dependencies.filter(_.e ne eps.e)
      (eps: @unchecked) match {
        case x: InterimEP[_, _] => {
          dependencies += eps
          InterimResult(field, MutableField, DeepImmutableField, dependencies, c(state))
        }

        case x @ FinalEP(_, t)
            if (t == DeepImmutableType) => { // || t == ShallowImmutableType) ⇒ { // ImmutableContainerType || t == ImmutableType) ⇒ {
          state.typeImmutability = Some(true)
        }

        case x @ FinalEP(_, MutableType_new | ShallowImmutableType) => { //MutableType) ⇒ {
          state.typeImmutability = Some(false)
        }
        case x @ FinalEP(f, DependentImmutableType) => {
          //-------------------------------------------------------------------------------------
          state.typeImmutability = determineGenericFieldImmutability(state)
          //-------------------------------------------------------------------------------------
          //state.typeImmutability = Some(false)
          //state.dependentTypeImmutability = Some(true)
        }

        case x @ FinalEP(_, MutableReference) => {
          state.referenceImmutability = Some(false)
        }

        case x @ FinalEP(_, ImmutableReference | LazyInitializedReference) => { //TODO
          state.referenceImmutability = Some(true)
        }

        case x @ _ => dependencies = dependencies + x
      }
      createResult(state)
    }

    //--
    state.depencenciesTypes = scala.collection.mutable.Set[ObjectType]()
    state.referenceImmutability = hasImmutableReference(field)
    state.typeImmutability = hasImmutableType(field)(state);
    createResult(state)
  }

}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

  final override def uses: Set[PropertyBounds] = Set(
    PropertyBounds.ub(TACAI),
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
      p: SomeProject,
      ps: PropertyStore,
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
