/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a0

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.xl.analyses.a1.A1Tainted
import org.opalj.xl.analyses.a2.A2Tainted
import org.opalj.xl.bridge.UniFTainted
import org.opalj.xl.bridge.UniversalFunctionTaintLattice
import org.opalj.xl.languages.GlobalVariable
import org.opalj.xl.languages.L.ForeignFunctionCall
import org.opalj.xl.languages.L0

import scala.collection.immutable
import scala.collection.mutable.HashMap

class A0TaintAnalysis(val project: SomeProject) extends FPCFAnalysis {
  val a0Project = project.get(A0ProjectKey)

  def analyzeTaints(function: L0.Function): ProperPropertyComputationResult = {

    println(s"a0 analyze: $function")

    val taints: HashMap[Entity, ProperPropertyComputationResult] = HashMap.empty[Entity, ProperPropertyComputationResult]
    val dependencies = scala.collection.immutable.Set.empty[SomeEOptionP]

    def c(e:Entity)(updatedValue: SomeEPS): ProperPropertyComputationResult = {
      println(s"continuation: $updatedValue")
      updatedValue match {
        case FinalP(A0Tainted | A1Tainted | A2Tainted | UniFTainted) => Result(e, A0Tainted)
        case FinalP(_)                                 => Result(e, A0Untainted)
        case eps@_                                     => InterimResult(e, A0Untainted, A0Tainted, dependencies, c(e))
      }
    }

    //var state: A0TaintLattice = A0Untainted

    function.Body.foreach(
      assignment =>{
        assignment match {
        case assignment @ L0.Assignment(lhs, rhs) if !rhs.isInstanceOf[ForeignFunctionCall]=>
        val name =  lhs match {
            case L0.Variable(name)=> name
            case GlobalVariable(name) => name
          }
          if (name == "source" || taints.contains(rhs))
            taints.addOne((assignment.variable.asInstanceOf[Entity], Result(assignment.variable.asInstanceOf[Entity], A0Tainted)))
          if(name=="return" && taints.contains(rhs))
            taints.addOne((function, Result(function, A0Tainted)))
          else taints.addOne((function, Result(function, A0Untainted)))

         case L0.Assignment(
              lhs:L0.Variable,
              ffc@L0.ForeignFunctionCall(language, fName, params)
              ) =>
          if(params.exists(param => taints.exists(taint => taint._1==param  && taint._2==Result(taint._1, A0Tainted))))
            {
              val result = propertyStore(ffc, UniversalFunctionTaintLattice.key)
              result match {
                case FinalP(UniFTainted) => taints.addOne((lhs, Result(lhs, A0Tainted)))

                case eps@_ if result.isRefinable =>
                  val v:ProperPropertyComputationResult =
                    InterimResult(lhs, A0Untainted, A0Tainted, Set(eps), c(lhs))
                  taints.addOne((lhs, v))
                case _                       => throw new Exception(".....x_")
              }}
          case x@_ => throw new Error(s"bla bli blu: $x")
        }
  })

    Results(taints.valuesIterator.toSet)
  }

  def lazilyAnalyzeTaints(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case function: L0.Function => analyzeTaints(function)
      case _            => throw new IllegalArgumentException("can only process functions")
    }
  }
}

trait A0TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(A0TaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: immutable.Set[PropertyBounds] =
    immutable.Set(PropertyBounds.ub(A0TaintLattice), PropertyBounds.ub(UniversalFunctionTaintLattice))
}

object EagerA0TaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: immutable.Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A0TaintAnalysis(project)
    val a0Project = project.get(A0ProjectKey)
    propertyStore.scheduleEagerComputationsForEntities(a0Project.functions)(analysis.analyzeTaints)
    analysis
  }
}

object LazyA0TaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A0TaintAnalysis(project)
    propertyStore.registerLazyPropertyComputation(A0TaintLattice.key, analysis.lazilyAnalyzeTaints)
    analysis
  }
}
