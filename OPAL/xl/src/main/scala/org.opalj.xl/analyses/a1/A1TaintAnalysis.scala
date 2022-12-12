/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a1

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
import org.opalj.xl.analyses.a2.A2Tainted
import org.opalj.xl.bridge.UniFTainted
import org.opalj.xl.bridge.UniversalFunctionTaintLattice
import org.opalj.xl.languages.GlobalVariable
import org.opalj.xl.languages.L1

import scala.collection.mutable.HashMap

class A1TaintAnalysisFunctions(val project: SomeProject) extends FPCFAnalysis {
  val A1Project = project.get(A1ProjectKey)


  def analyzeTaints(function: L1.Function): ProperPropertyComputationResult = {

    println(s"a1 analyze: $function")

    val taints: HashMap[Entity, ProperPropertyComputationResult] =
      HashMap.empty[Entity, ProperPropertyComputationResult]
    val dependencies = scala.collection.immutable.Set.empty[SomeEOptionP]

    taints.addAll(function.params.map(v=>v->Result(v, A1Tainted)))

      def c(e: Entity)(updatedValue: SomeEPS): ProperPropertyComputationResult = {
        println(s"continuation: $updatedValue")
        updatedValue match {
          case FinalP(A0Tainted | A1Tainted | A2Tainted | UniFTainted) => Result(e, A1Tainted)
          case FinalP(_)                                 => Result(e, A1Untainted)
          case eps @ _                                   => InterimResult(eps, A1Untainted, A1Tainted, dependencies, c(e))
        }
      }

      //var state: A0TaintLattice = A0Untainted

      function.Body.foreach(assignment => {
        println(s"a1 analyze assignment: $assignment")
        assignment match {

          case assignment @ L1.Assignment(lhs, rhs) if !rhs.isInstanceOf[L1.ForeignFunctionCall] =>
            val name = lhs match {
              case L1.Variable(name)    => name
              case GlobalVariable(name) => name
            }
            if (name == "source" || taints.contains(rhs))
              taints.addOne(
                (
                  assignment.variable.asInstanceOf[Entity],
                  Result(assignment.variable.asInstanceOf[Entity], A1Tainted)
                )
              )
          if (name == "return" && taints.contains(rhs))
            taints.addOne((function, Result(function, A1Tainted)))
          else taints.addOne((function, Result(function, A1Untainted)))

          case L1.Assignment(
              lhs: L1.Variable,
              ffc@L1.ForeignFunctionCall(language, fName, params)
              ) =>
            if(params.exists(param => taints.exists(taint => taint._1==param  && taint._2==Result(taint._1, A1Tainted)))) {
              val result = propertyStore(ffc, UniversalFunctionTaintLattice.key)
              result match {
                case FinalP(UniFTainted) =>
                  taints.addOne((lhs, Result(lhs, A1Tainted)))

                case eps @ _ if result.isRefinable =>
                  val v: ProperPropertyComputationResult =
                    InterimResult(lhs, A1Untainted, A1Tainted, Set(eps), c(lhs))
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
      case function: L1.Function => analyzeTaints(function)
      case _            => throw new IllegalArgumentException("")
    }
  }
}

trait A1TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(A1TaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] =
    Set(PropertyBounds.ub(A1TaintLattice), PropertyBounds.ub(UniversalFunctionTaintLattice))
}

object EagerA1TaintAnalysis extends A1TaintAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

  override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A1TaintAnalysisFunctions(project)
    val A1Project = project.get(A1ProjectKey)
    propertyStore.scheduleEagerComputationsForEntities(A1Project.functions)(analysis.analyzeTaints)
    analysis
  }
}

object LazyA1TaintAnalysis extends A1TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new A1TaintAnalysisFunctions(project)
    propertyStore.registerLazyPropertyComputation(A1TaintLattice.key, analysis.lazilyAnalyzeFieldImmutability)
    analysis
  }
}
