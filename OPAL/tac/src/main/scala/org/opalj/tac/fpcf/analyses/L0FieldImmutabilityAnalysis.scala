/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.analyses

import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
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

case class State() {
  var typeImmutability: Option[Boolean] = None
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

    println("Field Attributes: " + field.asField.attributes.toList.collectFirst({
      case TypeVariableSignature(t) => t
    }))

    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty

    def handleTypeImmutability(objectType: FieldType)(state: State): Option[Boolean] = {
      if (objectType.isArrayType) return Some(true); //TODO
      if (objectType.isBaseType) return Some(true);
      val result = propertyStore(objectType, TypeImmutability_new.key)
      println("result: " + result)
      result match {
        case FinalEP(e, DeepImmutableType) => { //ImmutableType || t == ImmutableContainerType) ⇒ {
          println("has deep immutable type")
          return Some(true);
        }
        case FinalEP(e, DependentImmutableType) => {
          println("has dependent immutable type")
          //TODO under construction
          state.dependentTypeImmutability = Some(true)
          return Some(false);
        }

        case FinalEP(e, ShallowImmutableType) => {
          println("has shallow immutable type")
          return Some(false); //TODO mindstorm if this approch is appropriate
        }
        case FinalEP(e, t) if (t == MutableType_new) => { //MutableType) ⇒ {
          println("has mutable type")
          return Some(false);
        }
        case x @ _ => {
          dependencies += x
          println(x)
          println("immutability of type couldn't be determined")
          return None;
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
          println("has mutable reference")
          Some(false)
        }
        case FinalEP(_, ImmutableReference | LazyInitializedReference) => { //TODO
          println("has immutable Reference")
          Some(true)
        }
        case x @ _ => {
          dependencies += x
          println(x)
          println("immutability of reference couldn't be determined")
          None
        }
      }
    }

    val state: State = new State()

    def createResult(state: State): ProperPropertyComputationResult = {
      println("reference Immutability: " + state.referenceImmutability)
      println("type immutabiliy: " + state.typeImmutability)
      println("dependent immutability: " + state.dependentTypeImmutability)

      state.referenceImmutability match {
        case Some(false) => Result(field, MutableField)
        case Some(true) => {
          //If the field type is object. It is a generic field
          //Test Dependent... //TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          //if (field.fieldType == ObjectType("java/lang/Object")) {
          val genericString = field.asField.attributes.toList.collectFirst({
            case TypeVariableSignature(t) => t
          })
          if (!field.attributes.isEmpty && genericString != None) {

            //if(genericString!=None)
            //    println("Generic String: "+genericString)
            //    println(
            //        "test: "+DependentImmutableField(genericString).genericString
            //   }
            Result(field, DependentImmutableField(genericString)) //genericString))

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
          println(dependencies)
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
      println("c function called")
      dependencies = dependencies.filter(_.e ne eps.e)
      (eps: @unchecked) match {
        case x: InterimEP[_, _] => {
          println("interim in continuation function")
          println(x)
          dependencies += eps
          InterimResult(field, MutableField, DeepImmutableField, dependencies, c(state))
        }

        case x @ FinalEP(_, t)
            if (t == DeepImmutableType) => { // || t == ShallowImmutableType) ⇒ { // ImmutableContainerType || t == ImmutableType) ⇒ {
          println(x)
          println("has immutable type. Determined by continuation function.")
          state.typeImmutability = Some(true)
        }

        case x @ FinalEP(_, MutableType_new | ShallowImmutableType) => { //MutableType) ⇒ {
          println(x)
          println("has mutable type. Determined by continuation function.")
          state.typeImmutability = Some(false)
        }
        case x @ FinalEP(_, DependentImmutableType) => {
          println(x)
          println("has dependent immutable type. Determined by continuation function.")
          state.typeImmutability = Some(false)
          state.dependentTypeImmutability = Some(true)
        }

        case x @ FinalEP(_, MutableReference) => {
          println(x)
          println("has mutable reference. Determined by continuation function.")
          state.referenceImmutability = Some(false)
        }

        case x @ FinalEP(_, ImmutableReference | LazyInitializedReference) => { //TODO
          println(x)
          println("has immutable reference. Determined by continuation function.")
          state.referenceImmutability = Some(true)
        }

        case x @ _ => println("default value: " + x)
      }
      createResult(state)
    }

    //--
    state.depencenciesTypes = scala.collection.mutable.Set[ObjectType]()
    state.referenceImmutability = hasImmutableReference(field)
    state.typeImmutability = hasImmutableType(field)(state);
    println("after type immutability determination")
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
