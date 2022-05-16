import java.net.URL

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.instructions.NEW
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler

/* LATTICE */

sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = InstantiatedTypes
}

case class InstantiatedTypes(classes: UIDSet[ObjectType]) extends InstantiatedTypesPropertyMetaInformation with OrderedProperty {
    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {
    final val key: PropertyKey[InstantiatedTypes] = {
        PropertyKey.create(
            "InstantiatedTypes",
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => InstantiatedTypes(UIDSet.empty)
                case _                                                => throw new IllegalStateException(s"No analysis is scheduled for property InstantiatedTypes")
            }
        )
    }
}

/* ANALYSIS */

class InstantiatedTypesAnalysis(val project: SomeProject) extends FPCFAnalysis {
    implicit private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
        if (method.name != "<init>")
            return NoResult

        val instantiatedType = method.declaringClassType

        def result(): ProperPropertyComputationResult = {
            PartialResult[SomeProject, InstantiatedTypes](
                project,
                InstantiatedTypes.key,
                (current: EOptionP[SomeProject, InstantiatedTypes]) => current match {
                    case InterimUBP(ub: InstantiatedTypes) =>
                        if (ub.classes.contains(instantiatedType))
                            None
                        else
                            Some(InterimEUBP(project, InstantiatedTypes(ub.classes + instantiatedType)))

                    case _: EPK[_, _] =>
                        Some(InterimEUBP(project, InstantiatedTypes(UIDSet(instantiatedType))))

                    case r => throw new IllegalStateException(s"unexpected previous result $r")
                }
            )
        }

        val callersProperty = propertyStore(method, Callers.key)

        def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = {
            val callers: Callers = callersProperty match {
                case FinalP(NoCallers) =>
                    return NoResult

                case UBP(v) => v

                case r      => throw new IllegalStateException(s"unexpected result for callers $r")
            }

            if (callers.hasCallersWithUnknownContext || callers.hasVMLevelCallers)
                return result()

            for ((caller, _, isDirect) <- callers.callers) {
                if (!isDirect)
                    return result()

                if (caller.name != "<init>")
                    return result()

                val callerClass = project.classFile(caller.declaringClassType)

                if (callerClass.isEmpty || callerClass.get.superclassType.isEmpty || callerClass.get.superclassType.get != instantiatedType)
                    return result()

                val body = caller.definedMethod.body.get

                if (body.exists((_, instruction) => instruction == NEW(instantiatedType)))
                    return result()
            }

            def continuation(updatedValue: SomeEPS): PropertyComputationResult = {
                checkCallers(updatedValue.asInstanceOf[EOptionP[DeclaredMethod, Callers]])
            }

            if (callersProperty.isFinal) {
                NoResult
            } else {
                InterimPartialResult(
                    Nil,
                    Set(callersProperty),
                    continuation
                )
            }
        }

        checkCallers(callersProperty)
    }
}

/* SCHEDULER */

object InstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def register(project: SomeProject, propertyStore: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(project)
        propertyStore.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
        analysis
    }
}

/* RUNNER */

object InstantiatedTypesRunner extends ProjectAnalysisApplication {
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
            CHACallGraphAnalysisScheduler,
            InstantiatedTypesAnalysisScheduler
        )

        val instantiatedTypes = propertyStore(project, InstantiatedTypes.key).asFinal.p.classes.size

        BasicReport(
            "Results of instantiated types analysis: \n"+
                s"Number of instantiated types: $instantiatedTypes"
        )
    }
}
