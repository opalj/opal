/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import java.io.File

import dk.brics.tajs.analysis.xl.translator.JavaTranslator
import dk.brics.tajs.analysis.xl.translator.LocalTAJSJavaTranslatorCopy
import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.lattice.Value

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.FinalAnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.utility.NoAnalysisResult
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.translator.JavaJavaScriptTranslator
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.util.CrossLanguageAnalysisException
import org.opalj.xl.utility.Constants
import org.opalj.xl.utility.JavaScriptFunctionCall
import org.opalj.xl.Coordinator.ScriptEngineInstance

import org.opalj.br.FieldType
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.Type
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis

class TajsConnector(override val project: SomeProject) extends FPCFAnalysis with AbstractPointsToAnalysis with AllocationSiteBasedAnalysis {

    val PROTOCOL_NAME = "tajs-host-env"

    case class TajsConnectorState(
                                     scriptEngineInstance: ScriptEngineInstance[AllocationSite],
                                     project: SomeProject,
                                     var code: List[String] = List.empty, //for debugging purposes
                                     var files: List[File] = null,
                                     var javaScriptInteraction: CrossLanguageInteraction = null,
                                     var connectorDependees: Set[EOptionP[Entity, Property]] = Set.empty,
                                     var presetValues: Map[PKey.StringPKey, Value] = Map.empty,
                             ) extends BaseAnalysisState with TypeIteratorState

    def analyzeScriptEngineInstance(scriptEngineInstance: ScriptEngineInstance[AllocationSite]): ProperPropertyComputationResult = {

        implicit val state = TajsConnectorState(scriptEngineInstance, project)

        def c(oldTAJSanalysis: Option[Analysis])(eps: SomeEPS)(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            println(s"tajs connector c: eps: $eps")
            state.connectorDependees = state.connectorDependees.filter(dependee => dependee.e != eps.e || dependee.ub.key != eps.ub.key)
            eps match {
                case UBP(javaScriptInteraction@ScriptEngineInteraction(language, possibleEmptyCode, foreignFunctionCall, puts, gets)) => //if code.size>0 =>
                    //TODO Translation Process
                    val nonEmptyCode = {
                        if (possibleEmptyCode.size == 0)
                            List("(function(){})();")
                        else {
                            possibleEmptyCode
                        }
                    }
                    state.code=nonEmptyCode
                    handleJavaScriptCall(nonEmptyCode, foreignFunctionCall, puts)
                    state.javaScriptInteraction = javaScriptInteraction
                    val entities = Constants.javaScriptResultVariableName :: gets.map(x => (x._1, x._2)).values.toList
                    state.connectorDependees += eps
                    try{
                        val tajsAdapter = new TAJSAdapter(project)
                        val newTAJSAnalysis = analyze(oldTAJSanalysis, entities, tajsAdapter)
                        createResult(Some(newTAJSAnalysis))
                    }
                    catch {
                        case crossLanguageAnalysisException: CrossLanguageAnalysisException =>
                            createResult(Some(crossLanguageAnalysisException.getAnalysis()))
                    }

                case ep =>
                    state.connectorDependees += ep
                    createResult(oldTAJSanalysis)
            }
        }

        def analyze(oldAnalysis: Option[Analysis],
                    entities: List[String],
                    javaTranslator: JavaTranslator)
                   (implicit state:TajsConnectorState): Analysis = {

            println(s">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>start tajs ${state.code.mkString("\n")}")
            LocalTAJSJavaTranslatorCopy.setLocalTAJSJavaTranslatorCopy(javaTranslator)
            val  javaPropertyChanges = new java.util.HashMap[PKey.StringPKey, Value]()
            //TODO state.presetValues.foreach(kvp => javaPropertyChanges.put(kvp._1, kvp._2))
            val analysis = {
                if(oldAnalysis.isDefined)
                    oldAnalysis.get
                else {
                    val arguments = state.files.map(_.getPath).toArray
                    dk.brics.tajs.Main.init(arguments, null)
                }
            }
            try{
                dk.brics.tajs.Main.run(analysis, javaPropertyChanges);
            }
            catch{
                case cae: CrossLanguageAnalysisException =>
                    println("Cross Language Analysis Exception")
                    InterimResult(
                        scriptEngineInstance,
                        NoAnalysisResult,
                        InterimAnalysisResult(cae.getAnalysis),
                        state.connectorDependees,
                        c(None)
                    )
            }
            finally {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end tajs")
            }

            analysis
        }

        def handleJavaScriptCall(
                                    code: List[String],
                                    javaScriptFunctionCalls: List[JavaScriptFunctionCall],
                                    puts: Map[String, (FieldType, Set[AnyRef], Option[Double])]
                                )(implicit state: TajsConnectorState): Unit = {
            val fileContents: List[String] =
                if (javaScriptFunctionCalls.nonEmpty) {
                    code ++ javaScriptFunctionCalls.map(javaScriptFunctionCall => {
                        val params = if (javaScriptFunctionCall.actualParams != null && javaScriptFunctionCall.actualParams.size > 0)
                            javaScriptFunctionCall.actualParams.map(_._1).mkString(", ")
                        else ""
                        javaScriptFunctionCall.actualParams.foreach(actualParam => {
                            addAssignment(actualParam._1, Map(actualParam._2._1->actualParam._2._2.asInstanceOf[Set[DefinitionSite]]), actualParam._2._3)
                        })
                        s"var ${Constants.javaScriptResultVariableName} = ${javaScriptFunctionCall.functionName}(${params});"
                    })
                }
                else code

            state.files = utility.asFiles("JavaScript", ".js", fileContents)

            puts.foreach(put => {
                addAssignment(put._1, Map(put._2._1->put._2._2.asInstanceOf[Set[DefinitionSite]]), put._2._3)
            })
        }

        def addAssignment(variableName: String, possibleTypes: Map[Type, Set[DefinitionSite]], value: Option[Double])
                         (implicit state: TajsConnectorState): Unit =
            state.presetValues+=
                JavaJavaScriptTranslator.Java2JavaScript(variableName, possibleTypes,value, project)


        def createResult(analysis: Option[Analysis])(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            InterimResult(
                    scriptEngineInstance,
                    InterimAnalysisResult(analysis),
                    FinalAnalysisResult(analysis),
                    state.connectorDependees,
                    c(analysis)
                )
        }

       val result = propertyStore(scriptEngineInstance, CrossLanguageInteraction.key)
        println(s"first result $result")
       result match {
            case ubp@UBP(ScriptEngineInteraction(language, possibleEmptyCode, javaScriptFunctionCall, puts, gets)) => // if(possibleEmptyCode.size>0)=>
                println(s"ubp: $ubp")
                val nonEmptyCode = {
                    if (possibleEmptyCode.size==0)
                        List("(function(){})();")
                    else {
                        possibleEmptyCode
                    }
                }
                state.code = nonEmptyCode
                handleJavaScriptCall(nonEmptyCode, javaScriptFunctionCall, puts)
                val entities = Constants.javaScriptResultVariableName :: gets.map(x=>(x._1, x._2)).values.toList
                val translator = new TAJSAdapter(project)
                state.connectorDependees += result
                val tajsAnalysis = analyze(None, entities, translator)
                createResult(Some(tajsAnalysis)) //TODO
            case ep =>
                state.connectorDependees += ep
                createResult(None)
        }
    }
}

object TriggeredTajsConnectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq()
    override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(CrossLanguageInteraction))
    override def triggeredBy: PropertyKey[CrossLanguageInteraction] =
        CrossLanguageInteraction.key

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): TajsConnector = {
        val analysis = new TajsConnector(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyzeScriptEngineInstance)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] =
        Set(PropertyBounds.ub(AnalysisResult))
}
