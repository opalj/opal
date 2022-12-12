/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package java
package analysis

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
//import org.opalj.tac.DVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.xl.bridge.UniversalFunctionTaintLattice
//import org.opalj.xl.languages.L.Assignment

import scala.collection.immutable

class JavaTaintAnalysis(val project: SomeProject) extends FPCFAnalysis {

  def analyzeTaints(method: Method): ProperPropertyComputationResult = {

    println(s"a0 analyze: $method")

    val tacai = getTACAI(method)

    tacai.get.instructions.foreach(instruction => instruction match {
      case x => println(x)
//      case Assignment(pc, DVar(origin, value)) => // (useSites, value, origin), _) =>
    })

/*
    val taints: HashMap[Entity, ProperPropertyComputationResult] = HashMap.empty[Entity, ProperPropertyComputationResult]
    val dependencies = scala.collection.immutable.Set.empty[SomeEOptionP]

    def c(e:Entity)(updatedValue: SomeEPS): ProperPropertyComputationResult = {
      println(s"continuation: $updatedValue")
      updatedValue match {
        case FinalP(JavaTainted | A1Tainted | A2Tainted | UniFTainted) => Result(e, JavaTainted)
        case FinalP(_)                                 => Result(e, JavaUntainted)
        case eps@_                                     => InterimResult(e, JavaUntainted, JavaTainted, dependencies, c(e))
      }
    }

    //var state: A0TaintLattice = A0Untainted


*/


    /*method.body.get.instructions.foreach(println(_)) /* .Body.foreach(
      assignment =>{
        assignment match {
        case assignment @ L0.Assignment(lhs, rhs) if !rhs.isInstanceOf[ForeignFunctionCall]=>
        val name =  lhs match {
            case L0.Variable(name)=> name
            case GlobalVariable(name) => name
          }
          if (name == "source" || taints.contains(rhs))
            taints.addOne((assignment.variable.asInstanceOf[Entity], Result(assignment.variable.asInstanceOf[Entity], JavaTainted)))
          if(name=="return" && taints.contains(rhs))
            taints.addOne((function, Result(function, JavaTainted)))
          else taints.addOne((function, Result(function, JavaUntainted)))

         case L0.Assignment(
              lhs:L0.Variable,
              ffc@L0.ForeignFunctionCall(language, fName, params)
              ) =>
          if(params.exists(param => taints.exists(taint => taint._1==param  && taint._2==Result(taint._1, JavaTainted))))
            {
              val result = propertyStore(ffc, UniversalFunctionTaintLattice.key)
              result match {
                case FinalP(UniFTainted) => taints.addOne((lhs, Result(lhs, JavaTainted)))

                case eps@_ if result.isRefinable =>
                  val v:ProperPropertyComputationResult =
                    InterimResult(lhs, JavaUntainted, JavaTainted, Set(eps), c(lhs))
                  taints.addOne((lhs, v))
                case _                       => throw new Exception(".....x_")
              }}
          case x@_ => throw new Error(s"bla bli blu: $x")
        }
  })
*/*/
    Result(method, JavaTainted)
  }

  def getTACAI(
      method: Method
  ): Option[TACode[TACMethodParameter, V]] = {
    propertyStore(method, TACAI.key) match {
      case finalEP: FinalEP[Method, TACAI] =>
        finalEP.ub.tac
      case eps @ InterimUBP(ub: TACAI) =>
        ub.tac
      case epk =>
        None
    }
  }

  def lazilyAnalyzeTaints(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case method: Method => analyzeTaints(method)
      case _            => throw new IllegalArgumentException("can only process functions")
    }
  }
}

trait A0TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(JavaTaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: immutable.Set[PropertyBounds] =
    immutable.Set(PropertyBounds.ub(JavaTaintLattice), PropertyBounds.ub(UniversalFunctionTaintLattice))
}

object EagerJavaTaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: immutable.Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new JavaTaintAnalysis(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.analyzeTaints)
    analysis
  }
}

object LazyJavaTaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new JavaTaintAnalysis(project)
    propertyStore.registerLazyPropertyComputation(JavaTaintLattice.key, analysis.lazilyAnalyzeTaints)
    analysis
  }
}
