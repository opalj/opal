/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package connector
package tajs

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.xl.analyses.javascriptAnalysis.TAJS
import org.opalj.xl.axa.common.AnalysisResults
import org.opalj.xl.axa.common.FinalAnalysisResult
import org.opalj.xl.axa.common.InterimAnalysisResult
import org.opalj.xl.axa.common.NoAnalysisResult
import org.opalj.xl.axa.translator.tajs.JavaScriptCall
import org.opalj.xl.axa.translator.tajs.NoJavaScriptCall
import org.opalj.xl.axa.translator.tajs.TajsInquiries

class OpalTajsConnector(val project: SomeProject) extends FPCFAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    var dependees: Set[EOptionP[Entity, Property]] = Set.empty

    def c(eps: SomeEPS): ProperPropertyComputationResult = {
      dependees -= eps
      eps match {
        case UBP(JavaScriptCall(file, propertyChanges)) =>
          println("propertyChanges: ")
          propertyChanges.foreach(println(_))
          createResult(TAJS.analyze(file, propertyChanges, new TajsOpalConnector(project))) //TODO figure out whether "resume" suits better
        case LBP(NoJavaScriptCall) => Result(method, NoAnalysisResult)
        case ep =>
          dependees += ep
          createResult(null)
      }
    }

    def createResult(o: Object): ProperPropertyComputationResult = {
      if (dependees.isEmpty) {
        if (o == null)
          Result(method, NoAnalysisResult)
        else
          Result(method, FinalAnalysisResult(o))
      } else
        InterimResult(
          method,
          NoAnalysisResult,
          InterimAnalysisResult(o),
          dependees,
          c
        )
    }

    propertyStore(method, TajsInquiries.key) match {
      case UBP(JavaScriptCall(file, propertyChanges)) =>
        println("propertyChanges: ")
        propertyChanges.foreach(println(_))
        createResult(TAJS.analyze(file, propertyChanges, new TajsOpalConnector(project))) //TODO
      case LBP(NoJavaScriptCall) =>
        Result(method, NoAnalysisResult)
      case ep =>
        dependees += ep
        createResult(null)
    }
  }
}
/*class JavaScriptAnalysisConnector(val project: SomeProject) extends FPCFAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    var dependees: Set[EOptionP[Entity, Property]] = Set.empty

    def c(eps: SomeEPS): ProperPropertyComputationResult = {
      dependees -= eps
      eps match {
        case UBP(JavaScriptCall(file, propertyChanges)) =>
          println("propertyChanges: ")
          propertyChanges.foreach(println(_))
            createResult(TAJS.analyze(file, propertyChanges, new TajsOpalConnector(project))) //TODO figure out whether "resume" suits better
        case LBP(NoJavaScriptCall) => Result(method, NoAnalysisResult)
        case ep =>
          dependees += ep
          createResult(null)
      }
    }

    def createResult(o: Object): ProperPropertyComputationResult = {
      if (dependees.isEmpty) {
        if (o == null)
          Result(method, NoAnalysisResult)
        else
          Result(method, FinalAnalysisResult(o))
      } else
        InterimResult(
          method,
          NoAnalysisResult,
          InterimAnalysisResult(o),
          dependees,
          c
        )
    }

    propertyStore(method, TajsInquiries.key) match {
      case UBP(JavaScriptCall(file, propertyChanges)) =>
        println("propertyChanges: ")
        propertyChanges.foreach(println(_))
       createResult(TAJS.analyze(file, propertyChanges, new TajsOpalConnector(project))) //TODO
      case LBP(NoJavaScriptCall) =>
        Result(method, NoAnalysisResult)
      case ep =>
        dependees += ep
        createResult(null)
    }
  }
} */

object TriggeredOpalTajsConnectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {

  override def requiredProjectInformation: ProjectInformationKeys = Seq()
  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(TajsInquiries))
  override def triggeredBy: PropertyKey[TajsInquiries] = TajsInquiries.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ): OpalTajsConnector = {
    val analysis = new OpalTajsConnector(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set.empty
  override def derivesCollaboratively: Set[PropertyBounds] =
    Set(PropertyBounds.ub(AnalysisResults))
}
