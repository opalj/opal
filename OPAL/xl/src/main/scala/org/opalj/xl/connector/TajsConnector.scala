/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import java.io.File

import scala.jdk.CollectionConverters.MapHasAsJava

import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.xl.translator.LocalTAJSAdapter
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
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.translator.JavaJavaScriptTranslator
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.util.CrossLanguageAnalysisException
import org.opalj.xl.utility.Constants
import org.opalj.xl.utility.JavaScriptFunctionCall
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.utility.Language

import org.opalj.fpcf.Results
import org.opalj.br.FieldType
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis

class TajsConnector(override val project: SomeProject) extends FPCFAnalysis with AbstractPointsToAnalysis with AllocationSiteBasedAnalysis {

    val PROTOCOL_NAME = "tajs-host-env"

    case class TajsConnectorState(
            scriptEngineInstance:      ScriptEngineInstance[AllocationSite],
            project:                   SomeProject,
            var code:                  List[String]                         = List.empty, //for debugging purposes
            var files:                 List[File]                           = null,
            var javaScriptInteraction: CrossLanguageInteraction             = null,
            var connectorDependees:    Set[EOptionP[Entity, Property]]      = Set.empty,
            var puts:                  Map[PKey.StringPKey, Value]          = Map.empty
    ) extends BaseAnalysisState with TypeIteratorState

    def analyzeScriptEngineInstance(scriptEngineInstance: ScriptEngineInstance[AllocationSite]): ProperPropertyComputationResult = {

        implicit val state = TajsConnectorState(scriptEngineInstance, project)

        def fillEmptyCode(possibleEmptyCode: List[String]) = {
            val nonEmptyCode = {
                if (possibleEmptyCode.size == 0)
                    List("(function(){})();")
                else {
                    possibleEmptyCode
                }
            }
            nonEmptyCode
        }

        def c(oldScriptEngineInteraction: ScriptEngineInteraction, oldTAJSanalysis: Option[Analysis])(eps: SomeEPS)(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            println(s"tajs connector c: eps: $eps")
            state.connectorDependees =
                state.connectorDependees.filter(dependee => dependee.e != eps.e)
            eps match {
                case UBP(javaScriptInteraction @ ScriptEngineInteraction(language, possibleEmptyCode, foreignFunctionCall, puts, gets)) =>
                    //TODO Translation Process
                    val nonEmptyCode = fillEmptyCode(possibleEmptyCode)
                    state.code = nonEmptyCode
                    handleJavaScriptCall(nonEmptyCode, foreignFunctionCall, puts)
                    state.javaScriptInteraction = javaScriptInteraction
                    val entities = Constants.javaScriptResultVariableName :: gets.values.toList
                    state.connectorDependees += eps
                    try {
                        val tajsAdapter = new TAJSAdapter(project)
                        val analysis = {
                            if (oldScriptEngineInteraction.code != javaScriptInteraction.code)
                                None
                            else oldTAJSanalysis
                        }
                        createResult(javaScriptInteraction, analyze(analysis, entities, tajsAdapter))
                    } catch {
                        case crossLanguageAnalysisException: CrossLanguageAnalysisException =>
                            createResult(javaScriptInteraction, Some(crossLanguageAnalysisException.getAnalysis()))
                    }

                case ep =>
                    state.connectorDependees += ep
                    createResult(ScriptEngineInteraction(), oldTAJSanalysis)
            }
        }

        def analyze(
            oldAnalysis: Option[Analysis],
            entities:    List[String],
            tajsAdapter: TAJSAdapter
        )(implicit state: TajsConnectorState): Option[Analysis] = {

            println(s">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>start tajs ${state.code.mkString("\n")}")
            LocalTAJSAdapter.setLocalTAJSAdapter(tajsAdapter)
            val analysis = {
                if (oldAnalysis.isDefined)
                    oldAnalysis.get
                else {
                    val arguments = state.files.map(_.getPath).toArray
                    dk.brics.tajs.Main.init(arguments, null)
                }
            }
            try {
                dk.brics.tajs.Main.run(analysis, state.puts.asJava);
            } catch {
                case cae: CrossLanguageAnalysisException =>
                    return Some(cae.getAnalysis)
                /* println("Cross Language Analysis Exception")
                    InterimResult(
                        scriptEngineInstance,
                        NoAnalysisResult,
                        InterimAnalysisResult(cae.getAnalysis),
                        state.connectorDependees,
                        c(None)
                    )*/
            } finally {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end tajs")
            }

            Some(analysis)
        }

        def handleJavaScriptCall(
            code:                    List[String],
            javaScriptFunctionCalls: List[JavaScriptFunctionCall],
            puts:                    Map[String, (FieldType, Set[AnyRef], Option[Double])]
        )(implicit state: TajsConnectorState): Unit = {
            val fileContents =
                if (javaScriptFunctionCalls.nonEmpty) {
                    code ++ javaScriptFunctionCalls.map(javaScriptFunctionCall => {
                        val params = if (javaScriptFunctionCall.actualParams.size > 0)
                            javaScriptFunctionCall.actualParams.keys.mkString(", ")
                        else ""
                        javaScriptFunctionCall.actualParams.map(actualParam => {
                            JavaJavaScriptTranslator.Java2JavaScript(actualParam._1, List(actualParam._2._1), actualParam._2._3)
                        })
                        s"var ${Constants.javaScriptResultVariableName} = ${javaScriptFunctionCall.functionName}(${params});"
                    })
                } else code

            state.files = utility.asFiles("JavaScript", ".js", fileContents)
            state.puts = puts.map(put => JavaJavaScriptTranslator.Java2JavaScript(put._1, List(put._2._1), put._2._3))
        }

        def createResult(scriptEngineInteraction: ScriptEngineInteraction, analysis: Option[Analysis])(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            InterimResult(
                scriptEngineInstance,
                InterimAnalysisResult(analysis),
                FinalAnalysisResult(analysis),
                state.connectorDependees,
                c(scriptEngineInteraction, analysis)
            )
        }

        propertyStore(scriptEngineInstance, CrossLanguageInteraction.key) match {
            case ubp @ UBP(sei @ ScriptEngineInteraction(Language.JavaScript, possibleEmptyCode, javaScriptFunctionCall, puts, gets)) =>
                println(s"ubp: $ubp")
                val nonEmptyCode = fillEmptyCode(possibleEmptyCode)
                state.code = nonEmptyCode
                handleJavaScriptCall(nonEmptyCode, javaScriptFunctionCall, puts)
                val entities = Constants.javaScriptResultVariableName :: gets.map(x => (x._1, x._2)).values.toList
                val tajsAdapter = new TAJSAdapter(project)
                state.connectorDependees += ubp
                createResult(sei, analyze(None, entities, tajsAdapter))
            case UBP(ScriptEngineInteraction(Language.Unknown, _, _, _, _)) => Results()
            case ep =>
                state.connectorDependees += ep
                createResult(ScriptEngineInteraction(), None)
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
