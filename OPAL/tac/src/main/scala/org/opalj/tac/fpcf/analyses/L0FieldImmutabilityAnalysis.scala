/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.analyses

import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.ImmutableContainerType
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.LazyInitializedReference
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.br.fpcf.properties.TypeImmutability
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

    def hasImmutableType(field: Field): Option[Boolean] = {
      println("Field:: " + field)
      println(field.fieldType)
      println(field.fieldType.isBaseType)
      if (field.fieldType.isArrayType) return Some(true); //TODO
      if (field.fieldType.isBaseType) return Some(true);
      val result = propertyStore(field.fieldType, TypeImmutability.key)
      println("result: " + result)
      result match {
        case FinalEP(e, t) if (t == ImmutableType || t == ImmutableContainerType) => {
          println("has immutable type")
          Some(true)
        }
        case FinalEP(e, t) if (t == MutableType) => {
          println("has mutable type")
          Some(false)
        }
        case x @ _ => {
          dependencies += x
          println(x)
          println("immutability of type couldn't be determined")
          None
        }
      }
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

      state.referenceImmutability match {
        case Some(false) => Result(field, MutableField)
        case Some(true) => {
          //If the field type is object. It is a generic field
          if (field.fieldType == ObjectType("java/lang/Object"))
            Result(field, DependentImmutableField)
          else
            state.typeImmutability match {
              case Some(true)                     => Result(field, DeepImmutableField)
              case Some(false)                    => Result(field, ShallowImmutableField)
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

        case x @ FinalEP(_, t) if (t == ImmutableContainerType || t == ImmutableType) => {
          println(x)
          println("has immutable type. Determined by continuation function.")
          state.typeImmutability = Some(true)
        }

        case x @ FinalEP(_, MutableType) => {
          println(x)
          println("has mutable type. Determined by continuation function.")
          state.typeImmutability = Some(false)
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
    state.referenceImmutability = hasImmutableReference(field)
    state.typeImmutability = hasImmutableType(field);
    println("after type immutability determination")
    createResult(state)
  }

}

trait L0FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

  final override def uses: Set[PropertyBounds] = Set(
    //PropertyBounds.lub(Purity),
    //PropertyBounds.lub(FieldPrematurelyRead),
    PropertyBounds.ub(TACAI),
    //PropertyBounds.lub(FieldMutability),
    //PropertyBounds.lub(EscapeProperty),
    PropertyBounds.lub(ReferenceImmutability),
    PropertyBounds.lub(TypeImmutability),
    PropertyBounds.lub(FieldImmutability)
    //PropertyBounds.ub(EscapeProperty)
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
