/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package translator
package tajs

import dk.brics.tajs.lattice.PKey
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.longToAllocationSite
import org.opalj.xl.axa.common.Language
import org.opalj.xl.axa.detector.CrossLanguageCall
import org.opalj.xl.axa.detector.DetectorLattice
import org.opalj.xl.axa.detector.NoCrossLanguageCall
import org.opalj.xl.axa.translator.Tajs.TajsTranslatorState

import scala.collection.mutable

class TajsTranslator(val project: SomeProject)
    extends AbstractPointsToAnalysis
    with AllocationSiteBasedAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    implicit val state = TajsTranslatorState(method)

    def handlePointsToSet(
        variableName: String,
        newDependeePointsTo: AllocationSitePointsToSet
    )(implicit state: TajsTranslatorState): Unit = {
      var possibleTypes: Set[ReferenceType] = Set.empty
      var defSites: Set[DefinitionSite] = Set.empty
      newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements)(id => {
        possibleTypes += getTypeOf(id)
        val allocationSite = longToAllocationSite(id)
        defSites += DefinitionSite(allocationSite._1.method.definedMethod, allocationSite._2)
      })
      state.propertyChanges.addOne(
        (PKey.StringPKey.make(variableName), Tajs.OpalToTajs(possibleTypes, defSites, project))
      )
    }

    def handleJavaScriptCall(
        code: String,
        assignments: mutable.Map[String, (FieldType, Set[DefinitionSite])]
    )(implicit state:TajsTranslatorState) : Unit = {
      state.file = Tajs.asFile("JavaScript", code)
      assignments.foreach(assignment => {
        assignment._2._2.foreach(defSite => {
          propertyStore(defSite, AllocationSitePointsToSet.key) match {
            case FinalP(newDependeePointsTo: AllocationSitePointsToSet @unchecked) => //PointsToSet
              handlePointsToSet(assignment._1, newDependeePointsTo)
            case ep =>
              state.translatorDependees += ep
              state.pointsToMapping += defSite -> assignment._1
          }
        })
      })
    }

    def c(eps: SomeEPS)(implicit state: TajsTranslatorState): ProperPropertyComputationResult = {
      println("enter c")
      state.translatorDependees=state.translatorDependees.filter(eps.e!=_.e)
      eps match {
        case EUBP(e,newDependeePointsTo: AllocationSitePointsToSet @unchecked) => //PointsToSet
          handlePointsToSet(state.pointsToMapping(e.asInstanceOf[DefinitionSite]), newDependeePointsTo)
        case FinalP(NoCrossLanguageCall) =>
          println(s"$method, NoJavaScriptCall")
          return Result(method, NoJavaScriptCall)
        case UBP(CrossLanguageCall(Language.JavaScript, code, assignments)) =>
          handleJavaScriptCall(code, assignments)
        case ep => state.translatorDependees += ep
      }
      createResult
    }

    def createResult(implicit state: TajsTranslatorState): ProperPropertyComputationResult = {
          if (state.translatorDependees.isEmpty){
            println("property changes TajsTranslator: ")
            state.propertyChanges.foreach(println(_))
            println("--------------------^")
            println(s"$method, JavaScriptCall")
            Result(method, JavaScriptCall(state.file, state.propertyChanges))
          } else {
            InterimResult(
              method,
              NoJavaScriptCall,
              JavaScriptCall(state.file, state.propertyChanges),
              state.translatorDependees,
              c
            )
      }
    }



    propertyStore(method, DetectorLattice.key) match {
      case UBP(CrossLanguageCall(Language.JavaScript, code, assignments)) =>
        handleJavaScriptCall(code, assignments)

      case FinalP(NoCrossLanguageCall) =>
        println(s"$method, NoJavScriptCall")
        return Result(method, NoJavaScriptCall);

      case x => throw new Exception(s" $x is not the right property")
    }
    createResult
  }
}

object TriggeredTajsTranslatorScheduler extends BasicFPCFTriggeredAnalysisScheduler {
  override def requiredProjectInformation: ProjectInformationKeys = Seq()

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(DetectorLattice))

  override def triggeredBy: PropertyKey[DetectorLattice] = DetectorLattice.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ): TajsTranslator = {
    val analysis = new TajsTranslator(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set(PropertyBounds.ub(TajsInquiries))

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
