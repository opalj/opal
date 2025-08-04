/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.cli.EscapeArg
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaHeapObject
import org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameter
import org.opalj.br.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.EscapeViaStaticField
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.cli.AnalysisLevelArg
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.log.OPALLogger.info
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time

/**
 * A runner for the escape analyses.
 *
 * @author Florian Kübler
 */
object EscapeAnalysis extends ProjectsAnalysisApplication {

    protected class EscapeConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {
        val description = "Determines escape information for every allocation site and every formal parameter"

        private val analysisLevelArg = new AnalysisLevelArg(EscapeArg.description, EscapeArg.levels: _*) {
            override val defaultValue: Option[String] = Some("L1")
            override val withNone = false
        }

        args(analysisLevelArg !)
        init()

        val analysis: FPCFAnalysisScheduler = getScheduler(apply(analysisLevelArg), eager = true)
            .asInstanceOf[FPCFAnalysisScheduler]
    }

    protected type ConfigType = EscapeConfig

    protected def createConfig(args: Array[String]): EscapeConfig = new EscapeConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: EscapeConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (propertyStore, _) = analysisConfig.setupPropertyStore(project)

        time {
            val manager = project.get(FPCFAnalysesManagerKey)
            if (analysisConfig.analysis eq EagerSimpleEscapeAnalysis) {
                manager.runAll(EagerSimpleEscapeAnalysis, LazyL0BaseAIAnalysis, TACAITransformer)
            } else {
                analysisConfig.setupCallGaph(project)
                manager.runAll(EagerInterProceduralEscapeAnalysis)
            }
        } { t => info("progress", s"escape analysis took ${t.toSeconds}")(project.logContext) }

        val escapeEntities = propertyStore.entities(EscapeProperty.key).toSeq

        def countAS(kind: EscapeProperty) = {
            escapeEntities.count {
                case FinalEP((_, _: DefinitionSite), `kind`) => true
                case _                                       => false
            }
        }

        // we are only interested in the "this" locals of  constructors
        def countFP(kind: EscapeProperty) = {
            escapeEntities.count {
                case FinalEP((_, _ @VirtualFormalParameter(dm: DefinedMethod, -1)), `kind`)
                    if dm.definedMethod.isConstructor => true
                case _ => false
            }
        }

        val message =
            s"""|ALLOCATION SITES:
                |# of local objects: ${countAS(NoEscape)}
                |# of objects escaping in a callee: ${countAS(EscapeInCallee)}
                |# of escaping objects via return: ${countAS(EscapeViaReturn)}
                |# of escaping objects via abnormal return: ${countAS(EscapeViaAbnormalReturn)}
                |# of escaping objects via parameter: ${countAS(EscapeViaParameter)}
                |# of escaping objects via normal and abnormal return: ${countAS(EscapeViaNormalAndAbnormalReturn)}
                |# of escaping objects via parameter and normal return: ${countAS(EscapeViaParameterAndReturn)}
                |# of escaping objects via parameter and abnormal return: ${countAS(
                    EscapeViaParameterAndAbnormalReturn
                )}
                |# of escaping objects via parameter and normal and abnormal return: ${countAS(
                    EscapeViaParameterAndNormalAndAbnormalReturn
                )}
                |# of escaping objects via static field: ${countAS(EscapeViaStaticField)}
                |# of escaping objects via heap objects: ${countAS(EscapeViaHeapObject)}
                |# of global escaping objects: ${countAS(GlobalEscape)}
                |# of at most local object: ${countAS(AtMost(NoEscape))}
                |# of escaping object at most in callee: ${countAS(AtMost(EscapeInCallee))}
                |# of escaping object at most via return: ${countAS(AtMost(EscapeViaReturn))}
                |# of escaping object at most via abnormal return: ${countAS(AtMost(EscapeViaAbnormalReturn))}
                |# of escaping object at most via parameter: ${countAS(AtMost(EscapeViaParameter))}
                |# of escaping object at most via normal and abnormal return: ${countAS(
                    AtMost(EscapeViaNormalAndAbnormalReturn)
                )}
                |# of escaping object at most via parameter and normal return: ${countAS(
                    AtMost(EscapeViaParameterAndReturn)
                )}
                |# of escaping object at most via parameter and abnormal return: ${countAS(
                    AtMost(EscapeViaParameterAndAbnormalReturn)
                )}
                |# of escaping object at most via parameter and normal and abnormal return: ${countAS(
                    AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)
                )}
                |
                |
                |FORMAL PARAMETERS:
                |# of local objects: ${countFP(NoEscape)}
                |# of objects escaping in a callee: ${countFP(EscapeInCallee)}
                |# of escaping objects via return: ${countFP(EscapeViaReturn)}
                |# of escaping objects via abnormal return: ${countFP(EscapeViaAbnormalReturn)}
                |# of escaping objects via parameter: ${countFP(EscapeViaParameter)}
                |# of escaping objects via normal and abnormal return: ${countFP(EscapeViaNormalAndAbnormalReturn)}
                |# of escaping objects via parameter and normal return: ${countFP(EscapeViaParameterAndReturn)}
                |# of escaping objects via parameter and abnormal return: ${countFP(
                    EscapeViaParameterAndAbnormalReturn
                )}
                |# of escaping objects via parameter and normal and abnormal return: ${countFP(
                    EscapeViaParameterAndNormalAndAbnormalReturn
                )}
                |# of escaping objects via static field: ${countFP(EscapeViaStaticField)}
                |# of escaping objects via heap objects: ${countFP(EscapeViaHeapObject)}
                |# of global escaping objects: ${countFP(GlobalEscape)}
                |# of at most local object: ${countFP(AtMost(NoEscape))}
                |# of escaping object at most in callee: ${countFP(AtMost(EscapeInCallee))}
                |# of escaping object at most via return: ${countFP(AtMost(EscapeViaReturn))}
                |# of escaping object at most via abnormal return: ${countFP(AtMost(EscapeViaAbnormalReturn))}
                |# of escaping object at most via parameter: ${countFP(AtMost(EscapeViaParameter))}
                |# of escaping object at most via normal and abnormal return: ${countFP(
                    AtMost(EscapeViaNormalAndAbnormalReturn)
                )}
                |# of escaping object at most via parameter and normal return: ${countFP(
                    AtMost(EscapeViaParameterAndReturn)
                )}
                |# of escaping object at most via parameter and abnormal return: ${countFP(
                    AtMost(EscapeViaParameterAndAbnormalReturn)
                )}
                |# of escaping object at most via parameter and normal and abnormal return: ${countFP(
                    AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)
                )}
            }"""

        (project, BasicReport(message.stripMargin('|')))
    }

}
