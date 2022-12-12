/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.a2

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
import org.opalj.xl.analyses.a0.A0Tainted
import org.opalj.xl.analyses.a1.A1Tainted
import org.opalj.xl.bridge.UniFTainted
import org.opalj.xl.bridge.UniversalFunctionTaintLattice
import org.opalj.xl.languages.GlobalVariable
import org.opalj.xl.languages.L2

import scala.collection.immutable.Set
import scala.collection.mutable.HashMap

class A2TaintAnalysisFunctions(val project: SomeProject) extends FPCFAnalysis {
  val A2Project = project.get(A2ProjectKey)


  def analyzeTaints(function: L2.Function): ProperPropertyComputationResult = {

    println(s"a2 analyze: $function")

    val taints: HashMap[Entity, ProperPropertyComputationResult] =
      HashMap.empty[Entity, ProperPropertyComputationResult]
    val dependencies = scala.collection.immutable.Set.empty[SomeEOptionP]

    taints.addAll(function.params.map(v=>v->Result(v, A2Tainted)))

    def c(e: Entity)(updatedValue: SomeEPS): ProperPropertyComputationResult = {
      println(s"continuation: $updatedValue")
      updatedValue match {
        case FinalP(A0Tainted | A1Tainted | A2Tainted | UniFTainted) => Result(e, A2Tainted)
        case FinalP(_)                                 => Result(e, A2Untainted)
        case eps @ _                                   => InterimResult(e, A2Untainted, A2Tainted, dependencies, c(e))
      }
    }

    //var state: A0TaintLattice = A0Untainted

    function.Body.foreach(assignment => {
      assignment match {
        case assignment @ L2.Assignment(lhs, rhs) if !rhs.isInstanceOf[L2.ForeignFunctionCall] =>
          val name = lhs match {
            case L2.Variable(name)    => name
            case GlobalVariable(name) => name
          }
          if (name == "source" || taints.contains(rhs))
            taints.addOne(
              (
                assignment.variable.asInstanceOf[Entity],
                Result(assignment.variable.asInstanceOf[Entity], A2Tainted)
              )
            )
          if (name == "return" && taints.contains(rhs))
            taints.addOne((function, Result(function, A2Tainted)))
          else taints.addOne((function, Result(function, A2Untainted)))

        case L2.Assignment(
            lhs: L2.Variable,
            ffc@L2.ForeignFunctionCall(language, fName, params)
            ) =>
          if(params.exists(param => taints.exists(taint => taint._1==param  && taint._2==Result(taint._1, A2Tainted)))) {
             val result = propertyStore(ffc, UniversalFunctionTaintLattice.key)
            result match {
              case FinalP(UniFTainted) =>
                taints.addOne((lhs, Result(lhs, A2Tainted)))

              case eps @ _ if result.isRefinable =>
                val v: ProperPropertyComputationResult =
                  InterimResult(lhs, A2Untainted, A2Tainted, Set(eps), c(lhs))
                taints.addOne((lhs, v))
              case _ => throw new Exception(".....x_")
            }
          }
        case x @ _ => throw new Error(s"bla bli blu: $x")
      }
    })
    Results(taints.valuesIterator.toSet)
  }

  def lazilyAnalyzeFieldImmutability(function: Entity): ProperPropertyComputationResult = {
    function match {
      case function: L2.Function => analyzeTaints(function)
      case _            => throw new IllegalArgumentException("")
    }
  }
}

trait A2TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(A2TaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] =
    Set(PropertyBounds.ub(A2TaintLattice), PropertyBounds.ub(UniversalFunctionTaintLattice))
}

object EagerA2TaintAnalysis extends A2TaintAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

  override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A2TaintAnalysisFunctions(project)
    val A2Project = project.get(A2ProjectKey)
    propertyStore.scheduleEagerComputationsForEntities(A2Project.functions)(analysis.analyzeTaints)
    analysis
  }
}

object LazyA2TaintAnalysis extends A2TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A2TaintAnalysisFunctions(project)
    propertyStore.registerLazyPropertyComputation(A2TaintLattice.key, analysis.lazilyAnalyzeFieldImmutability)
    analysis
  }
}
