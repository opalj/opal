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
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.UBP
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.xl.analyses.java.adaptor.javatrash.JavaAdaptor
import org.opalj.xl.axa.bridge.function.ForeignFunction
import org.opalj.xl.axa.bridge.function.UniFTainted
import org.opalj.xl.axa.bridge.function.UniversalFunctionTaintLattice

import scala.collection.immutable

class JavaTaintAnalysis(val project: SomeProject) extends FPCFAnalysis {

  def analyzeTaints(method: Method): ProperPropertyComputationResult = {

    if(!method.name.contains("azbghik"))
      return Result(method, JavaUntainted);

    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty

    println(s"a0 analyze: $method")

    val tacai: Option[TACode[TACMethodParameter, V]] = getTACAI(method)
    var taints: Set[Int] = Set.empty

    tacai.get.instructions.zipWithIndex.foreach {

      case (PutStatic(pc, cl, n, tpe, value), index) =>
        println(value.asVar)
        if (value.isVar && value.asVar.definedBy.exists(_ < 0)) {
          //TODO globale variable tainten
        }
      case (Assignment(pc, lhs, rhs), index) =>
        if (lhs.isVar && rhs.isVar) {
          if (rhs.asVar.definedBy.forall(n => taints.contains(n)) || lhs.asVar.definedBy.exists(
                _ < 0
              ))
            lhs.definedBy.foreach(x => taints += x)
        } else if (rhs.isStaticFunctionCall) {
          val sfc = rhs.asStaticFunctionCall
          if (sfc.name == "ffc") {

            val (languageName, functionName, paramA, paramB) =
              JavaAdaptor.CrossLanguageAdaptor(tacai, sfc)

            if (paramA.asVar.definedBy.exists(x => taints.contains(x)) || paramB.asVar.definedBy
                  .exists(
                    x => taints.contains(x)
                  )
                || paramA.asVar.definedBy.forall(_ < 0) || paramB.asVar.definedBy.forall(_ < 0)) {
              val ff = ForeignFunction(languageName, functionName)
              val result = propertyStore(ff, UniversalFunctionTaintLattice.key)
              println(s"result: $result")
              result match {
                case UBP(UniFTainted) =>
                  taints += index
                case eps => dependencies += eps
              }
            }
          }
        }
      case (ReturnValue(pc, expr), index) =>
        if (expr.isVar && expr.asVar.definedBy.exists(taints.contains(_)) || expr.asVar.definedBy
              .forall(_ < 0))
          return Result(method, JavaTainted)

      case (ExprStmt(pc, sfc @ StaticFunctionCall(_, _, isInterface, "fvc", params, x)), index) =>
        println(
          s"params: ${params}" +
            s"x: ${x.tail.tail.head.asVar.definedBy}"
        )
        println(s"MMMMMMMMMM: ${method.classFile.thisType.simpleName}${method.name}")
        println(s".... ${tacai.get.stmts(sfc.params.tail.head.asVar.definedBy.head)}")

      case x => println(s"not matched $x")

    }

    println(s"taints: ${taints.toList.mkString("\n")}")

    Result(method, JavaUntainted)
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
      case _              => throw new IllegalArgumentException("can only process functions")
    }
  }
}

trait A0TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(JavaTaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: immutable.Set[PropertyBounds] =
    immutable.Set(
      PropertyBounds.ub(JavaTaintLattice),
      PropertyBounds.ub(UniversalFunctionTaintLattice)
    )
}

object EagerJavaTaintAnalysis
    extends A0TaintAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: immutable.Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new JavaTaintAnalysis(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.analyzeTaints)
    analysis
  }
}

object LazyJavaTaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new JavaTaintAnalysis(project)
    propertyStore.registerLazyPropertyComputation(
      JavaTaintLattice.key,
      analysis.lazilyAnalyzeTaints
    )
    analysis
  }
}
/*

object TriggertTaintAnalysis extends A0TaintAnalysisScheduler with BasicFPCFTriggertAnalysisScheduler {

  override def requiredProjectInformation: ProjectInformationKeys =
    Seq(DeclaredMethodsKey, InitialEntryPointsKey, TypeIteratorKey)

  override def uses: Set[PropertyBounds] =
    PropertyBounds.ubs(Callers, Callees, TACAI)

  override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = {
    p.get(TypeIteratorKey).usedPropertyKinds
  }

  override def derivesEagerly: Set[PropertyBounds] = Set.empty

  override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callers, Callees)

  /**
   * Updates the caller properties of the initial entry points
   * ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) to be called from an unknown context.
   * This will trigger the computation of the callees for these methods (see `processMethod`).
   */
  override def init(p: SomeProject, ps: PropertyStore): Null = {
    val declaredMethods = p.get(DeclaredMethodsKey)
    val entryPoints = p.get(InitialEntryPointsKey).map(declaredMethods.apply)

    if (entryPoints.isEmpty)
      OPALLogger.logOnce(
        Error("project configuration", "the project has no entry points")
      )(p.logContext)

    entryPoints.foreach { ep =>
      ps.preInitialize(ep, Callers.key) {
        case _: EPK[_, _] =>
          InterimEUBP(ep, OnlyCallersWithUnknownContext)
        case InterimUBP(ub: Callers) =>
          InterimEUBP(ep, ub.updatedWithUnknownContext())
        case eps =>
          throw new IllegalStateException(s"unexpected: $eps")
      }
    }

    null
  }

  override def register(p: SomeProject, ps: PropertyStore, unused: Null): CallGraphAnalysis = {
    val analysis = new CallGraphAnalysis(p)
    ps.registerTriggeredComputation(Callers.key, analysis.analyze)
    analysis
  }

  override def triggeredBy: PropertyKind = Callers
}
*/