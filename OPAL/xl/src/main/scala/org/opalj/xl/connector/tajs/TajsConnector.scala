/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector
package tajs

import dk.brics.tajs.analysis.axa.connector.IConnector
import dk.brics.tajs.lattice.Value
import org.opalj.br.Method
import org.opalj.br.ObjectType
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
import org.opalj.tac.common.DefinitionSite
import org.opalj.xl.analyses.javascript.TAJS
import org.opalj.xl.common.AnalysisResult
import org.opalj.xl.common.FinalAnalysisResult
import org.opalj.xl.common.InterimAnalysisResult
import org.opalj.xl.common.NoAnalysisResult
import org.opalj.xl.detector.JavaScriptFunctionCall
import org.opalj.xl.detector.JavaScriptInteraction
import org.opalj.xl.detector.NoJavaScriptCall
import org.opalj.xl.translator.TajsOPALTranslatorFunctions
import org.opalj.xl.translator.TajsTranslator

class OpalTajsConnector(val project: SomeProject) extends TajsTranslator with FPCFAnalysis {

    def analyzeMethod(method: Method): ProperPropertyComputationResult = {

        implicit val state = TajsTranslatorState(method, project)

        object opalConnector extends IConnector {

            override def queryFunctionValue(javaFullClassName: String, javaFunctionName: String): Value = {
                val classFileOption = project.classFile(ObjectType(javaFullClassName))
                if (classFileOption.isDefined) {
                    val returnType = classFileOption.get.methods.filter(_.name == javaFunctionName).head.returnType
                    if (returnType.isNumericType)
                        Value.makeAnyNum()
                    else if (returnType.isBooleanType)
                        Value.makeAnyBool()
                    else if (returnType == ObjectType.String)
                        Value.makeAnyStr()
                    else if (returnType.isReferenceType) {
                        TajsOPALTranslatorFunctions.OpalToTajsValue(Map(returnType.asReferenceType -> Set.empty[DefinitionSite]), project)
                    }
                }
                Value.makeUndef()
            }

            override def queryPropertyValue(
                javaFullClassName: String,
                javaPropertyName:  String
            ): Value = {
                val classFileOption = project.classFile(ObjectType(javaFullClassName))
                if (classFileOption.isDefined) {
                    val fields = classFileOption.get.fields.filter(field => field.name == javaPropertyName)

                    val m = Map(fields(0).fieldType.asReferenceType -> Set.empty[DefinitionSite])
                    if (fields.size > 0)
                        return TajsOPALTranslatorFunctions.OpalToTajsValue(m, project)
                }
                Value.makeUndef()
            }
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees -= eps
            eps match {
                case UBP(JavaScriptFunctionCall(code, foreignFunctionCall, assignments)) =>
                    //TODO Translation Process
                    handleJavaScriptCall(code, foreignFunctionCall, assignments)
                    val tajsResult = TAJS.analyze(state.files, state.propertyChanges, opalConnector)
                    createResult(tajsResult)
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

        propertyStore(method, JavaScriptInteraction.key) match {
            case UBP(JavaScriptFunctionCall(code, foreignFunctionCall, assignments)) =>
                handleJavaScriptCall(code, foreignFunctionCall, assignments)
                //TODO Translation process

                val tajsResult = TAJS.analyze(state.files, state.propertyChanges, opalConnector)
                val translatedTajsResult = TajsOPALTranslatorFunctions.TajsToOpal(tajsResult)
                createResult(translatedTajsResult) //TODO
            case LBP(NoJavaScriptCall) =>
                Result(method, NoAnalysisResult)
            case ep =>
                dependees += ep
                createResult(null)
        }
    }
}

object TriggeredOpalTajsConnectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq()
    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptInteraction))
    override def triggeredBy: PropertyKey[JavaScriptInteraction] =
        JavaScriptInteraction.key

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): OpalTajsConnector = {
        val analysis = new OpalTajsConnector(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] =
        Set(PropertyBounds.ub(AnalysisResult))
}
