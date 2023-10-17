/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import scala.collection.immutable.ArraySeq

import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.APIBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.utility.Language
import ScriptEngineDetector.engineManager
import ScriptEngineDetector.getEngine
import ScriptEngineDetector.scriptEngine
import org.opalj.xl.utility.Language.Language
import ScriptEngineDetector.engineNames
import ScriptEngineDetector.extensions
import ScriptEngineDetector.mimetypes
import org.opalj.xl.javaanalyses.detector.scriptengine.ScriptEngineDetector.invocable

import org.opalj.br.ArrayType
import org.opalj.br.VoidType

/**
 * Detects calls of put, get, eval, on the Java ScriptEngine object.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 */
abstract class ScriptEngineDetector( final val project: SomeProject) extends PointsToAnalysisBase {
    self =>

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this] type ElementType = self.ElementType
        override protected[this] type PointsToSet = self.PointsToSet
        override protected[this] type DependerType = self.DependerType

        override protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet] =
            self.pointsToPropertyKey

        override protected[this] def emptyPointsToSet: PointsToSet = self.emptyPointsToSet

        override protected[this] def createPointsToSet(pc: Int, callContext: ContextType, allocatedType: ReferenceType,
                                                       isConstant: Boolean, isEmptyArray: Boolean): PointsToSet = {
            self.createPointsToSet(pc, callContext.asInstanceOf[self.ContextType],
                allocatedType, isConstant, isEmptyArray)
        }

        @inline override protected[this] def getTypeOf(element: ElementType): ReferenceType = {
            self.getTypeOf(element)
        }

        @inline override protected[this] def getTypeIdOf(element: ElementType): Int = {
            self.getTypeIdOf(element)
        }

        @inline override protected[this] def isEmptyArray(element: ElementType): Boolean = {
            self.isEmptyArray(element)
        }
    }

    def process(project: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new ScriptEngineAllocationAnalysis(
            project, declaredMethods(engineManager, "", engineManager, "getEngineByName", getEngine), engineNames
        ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                project, declaredMethods(engineManager, "", engineManager, "getEngineByExtension", getEngine), extensions
            ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                project, declaredMethods(engineManager, "", engineManager, "getEngineByMimeType", getEngine), mimetypes
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysisPut(
                project, declaredMethods(scriptEngine, "", scriptEngine, "put",
                MethodDescriptor(ArraySeq(ObjectType.String, ObjectType.Object), VoidType))
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysisEval(
                project,
                declaredMethods(scriptEngine, "", scriptEngine, "eval", MethodDescriptor(ObjectType.String, ObjectType.Object))
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysisGet(
                project,
                declaredMethods(scriptEngine, "", scriptEngine, "get", MethodDescriptor(ObjectType.String, ObjectType.Object))
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysisInvokeFunction(
                project,
                declaredMethods(invocable, "", invocable, "invokeFunction", MethodDescriptor(ArraySeq(ObjectType.String, ArrayType.ArrayOfObject), ObjectType.Object))
            ) with PointsToBase
        )
        Results(analyses.map(_.registerAPIMethod()))
    }
}

object ScriptEngineDetector {
    val scriptEngine: ObjectType = ObjectType("javax/script/ScriptEngine")
    val invocable: ObjectType = ObjectType("javax/script/Invocable")
    val engineManager: ObjectType = ObjectType("javax/script/ScriptEngineManager")
    val getEngine: MethodDescriptor = MethodDescriptor(ObjectType.String, scriptEngine)

    val engineNames: Map[String, Language] = Map.from(
        Set("nashorn", "rhino", "js", "javascript", "ecmascript", "graal.js").
            map(_ -> Language.JavaScript)
    )

    val extensions: Map[String, Language] = Map.from(Set("js").map(_ -> Language.JavaScript))

    val mimetypes: Map[String, Language] = Map.from(
        Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript").
            map(_ -> Language.JavaScript)
    )
}

trait ScriptEngineDetectorScheduler extends BasicFPCFEagerAnalysisScheduler {
    def propertyKind: PropertyMetaInformation

    def createAnalysis: SomeProject => ScriptEngineDetector

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callees, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind, CrossLanguageInteraction)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

object TypeBasedApiScriptEngineDetectorSchedulerScheduler extends ScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => ScriptEngineDetector =
        new ScriptEngineDetector(_) with TypeBasedAnalysis
}

object AllocationSiteBasedApiScriptEngineDetectorScheduler extends ScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => ScriptEngineDetector =
        new ScriptEngineDetector(_) with AllocationSiteBasedAnalysis
}
