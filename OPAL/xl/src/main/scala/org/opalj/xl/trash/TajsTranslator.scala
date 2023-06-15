/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package xl
package translator
package tajs

import dk.brics.tajs.flowgraph.SourceLocation
import dk.brics.tajs.flowgraph.jsnodes.JavaNode
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
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
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.xl.common.Constants
import org.opalj.xl.common.ForeignFunctionCall
import org.opalj.xl.common.Language
import org.opalj.xl.detector.JavaScriptFunctionCall
import org.opalj.xl.detector.JavaScriptInteraction
import org.opalj.xl.detector.NoJavaScriptCall
import org.opalj.xl.translator.opal

import java.io.File
import java.net.URL
import scala.collection.mutable

class TajsTranslator(val project: SomeProject) extends AbstractPointsToAnalysis with AllocationSiteBasedAnalysis {

  case class TajsTranslatorState(
      method: Method,
      project: SomeProject,
      var files: List[File] = null,
      var foreignFunctionCall: ForeignFunctionCall = null,
      var translatorDependees: Set[EOptionP[Entity, Property]] = Set.empty,
      var pointsToMapping: mutable.Map[DefinitionSite, String] = mutable.Map.empty,
      var propertyChanges: mutable.Map[PKey.StringPKey, Value] = mutable.Map.empty,
      var call: Option[JavaScriptInteraction] = None
  ) extends BaseAnalysisState with TypeIteratorState

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    implicit val state = TajsTranslatorState(method, project)

    def handlePointsToSet(
        variableName: String,
        newDependeePointsTo: AllocationSitePointsToSet
    )(implicit state: TajsTranslatorState): Unit = {
      var possibleTypes: Map[ReferenceType, Set[DefinitionSite]] = Map.empty
      //var defSites: Set[DefinitionSite] = Set.empty
      newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements)(id => {
        //possibleTypes += getTypeOf(id)
        val allocationSite = longToAllocationSite(id)
        //defSites += DefinitionSite(allocationSite._1.method.definedMethod, allocationSite._2)
        possibleTypes+= getTypeOf(id)-> Set(DefinitionSite(allocationSite._1.method.definedMethod, allocationSite._2))
      })
      //TODO
      state.propertyChanges.addOne((PKey.StringPKey.make(variableName),
        TajsOPALTranslatorFunctions.OpalToTajsValue(possibleTypes, project)))
    }

    def handleJavaScriptCall(
        code: String,
        ffc: ForeignFunctionCall,
        assignments: mutable.Map[String, (FieldType, Set[AnyRef])]
    )(implicit state:TajsTranslatorState) : Unit = {
      val functionCall =
        if(ffc!=null)
          s"var ${Constants.jsrResultVariableName} = ${ffc.functionName}(${ffc.actualParams.map(_._1).mkString(", ")});"
        else ""

      state.files = common.asFiles("JavaScript", ".js", List(functionCall, code))

      ffc.actualParams.foreach(ap=>{addAssignment(ap._1, ap._2.value.asReferenceValue.leastUpperType.get.asFieldType)})
      assignments.foreach(as=>{addAssignment(as._1, as._2._1)})
    }

    def addAssignment(vName: String, tpe: FieldType)(implicit state: TajsTranslatorState): Unit = {
      val url: URL = new URL("file", null, state.files.head.getPath)
      val sl: SourceLocation = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
      val javaNode = new JavaNode(sl, 1)
      javaNode.setIndex(1)
      val objectLabel = ObjectLabel.make(javaNode, ObjectLabel.Kind.JAVAOBJECT)
      objectLabel.javaName = tpe.toJava
      val v = Value.makeObject(objectLabel)
      v.setDontDelete().setDontEnum().setReadOnly()
      state.propertyChanges.addOne(
        (PKey.StringPKey.make(vName), v.setDontDelete().setDontEnum().setReadOnly())
      )
    }

    def c(eps: SomeEPS)(implicit state: TajsTranslatorState): ProperPropertyComputationResult = {
      state.translatorDependees=state.translatorDependees.filter(eps.e!=_.e)
      eps match {
        case FinalEP(e,newDependeePointsTo: AllocationSitePointsToSet @unchecked) => //PointsToSet
          handlePointsToSet(state.pointsToMapping(e.asInstanceOf[DefinitionSite]), newDependeePointsTo)
        case FinalP(NoJavaScriptCall) =>
          return Result(method, NoJavaScriptCall)
        case UBP(
            JavaScriptFunctionCall(Language.JavaScript, code, foreignFunctionCall, assignments)) =>
          handleJavaScriptCall(code, foreignFunctionCall, assignments)
        case ep => state.translatorDependees += ep
      }
      createResult
    }

    def createResult(implicit state: TajsTranslatorState): ProperPropertyComputationResult = {
          if (state.translatorDependees.isEmpty){
            Result(method,
          )
        JavaScriptCallTemp
          /** EndMarker */ (state.files, state.foreignFunctionCall, state.propertyChanges)(state.files, state.foreignFunctionCall, state.propertyChanges))
          } else {
            InterimResult(
              method,
              NoJavaScriptCall,
              JavaScriptCallTemp(state.files, state.foreignFunctionCall, state.propertyChanges),
              state.translatorDependees,
              c
            )
      }
    }

    val result = propertyStore(method, JavaScriptInteraction.key)
     result match {
      case UBP(JavaScriptFunctionCall(code, foreignFunctionCall, assignments)) =>
        handleJavaScriptCall(code, foreignFunctionCall, assignments)

      case LBP(NoJavaScriptCall) => return Result(method, NoJavaScriptCall);

      case x => throw new Exception(s" $x is not the right property")
    }
    createResult
  }

}

object TriggeredTajsTranslatorScheduler extends BasicFPCFTriggeredAnalysisScheduler {
  override def requiredProjectInformation: ProjectInformationKeys = Seq()

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptInteraction))

  override def triggeredBy: PropertyKey[JavaScriptInteraction] = JavaScriptInteraction
    /** EndMarker */ .key.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ): opal.TajsTranslator = {
    val analysis = new opal.TajsTranslator(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptInteractionTemp))

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
*/ 