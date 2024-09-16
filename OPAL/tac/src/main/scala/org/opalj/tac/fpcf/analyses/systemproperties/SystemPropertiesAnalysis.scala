/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package systemproperties

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @author Maximilian Rüsch
 */
class SystemPropertiesAnalysis private[analyses] (
    final val project: SomeProject
) extends ReachableMethodAnalysis {

    type State = SystemPropertiesState[ContextType]
    private type Values = Set[StringTreeNode]

    def processMethod(callContext: ContextType, tacaiEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        // IMPROVE add initialization framework similar to the EntryPointFinder framework
        implicit val state: State = new SystemPropertiesState(callContext, tacaiEP, Map.empty)

        var values: Values = Set.empty
        for (stmt <- state.tac.stmts) stmt match {
            case VirtualFunctionCallStatement(call)
                if (call.name == "setProperty" || call.name == "put") &&
                    classHierarchy.isSubtypeOf(call.declaringClass, ObjectType("java/util/Properties")) =>
                values ++= getPossibleStrings(call.pc, call.params(1))

            case StaticFunctionCallStatement(call)
                if call.name == "setProperty" && call.declaringClass == ObjectType.System =>
                values ++= getPossibleStrings(call.pc, call.params(1))

            case StaticMethodCall(pc, ObjectType.System, _, "setProperty", _, params) =>
                values ++= getPossibleStrings(pc, params(1))

            case _ =>
        }

        returnResults(values)
    }

    def returnResults(values: Set[StringTreeNode])(implicit state: State): ProperPropertyComputationResult = {
        def update(currentVal: EOptionP[SomeProject, SystemProperties]): Option[InterimEP[SomeProject, SystemProperties]] = {
            currentVal match {
                case UBP(ub) =>
                    val newUB = ub.mergeWith(SystemProperties(values))
                    if (newUB eq ub) None
                    else Some(InterimEUBP(project, newUB))

                case _: EPK[SomeProject, SystemProperties] =>
                    Some(InterimEUBP(project, SystemProperties(values)))
            }
        }

        if (state.hasOpenDependencies) {
            InterimPartialResult(
                project,
                SystemProperties.key,
                update,
                state.dependees,
                continuation(state)
            )
        } else {
            PartialResult(project, SystemProperties.key, update)
        }
    }

    def continuation(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case _ if eps.pk == TACAI.key =>
                continuationForTAC(state.callContext.method)(eps)

            case eps @ EUBP(_: VariableContext, ub: StringConstancyProperty) =>
                state.updateStringDependee(eps.asInstanceOf[EPS[VariableContext, StringConstancyProperty]])
                returnResults(Set(ub.tree))(state)

            case _ =>
                throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }

    def getPossibleStrings(pc: Int, value: Expr[V])(implicit state: State): Set[StringTreeNode] = {
        ps(
            VariableContext(pc, value.asVar.toPersistentForm(state.tac.stmts), state.callContext),
            StringConstancyProperty.key
        ) match {
            case eps @ UBP(ub) =>
                state.updateStringDependee(eps)
                Set(ub.tree)

            case epk: EOptionP[VariableContext, StringConstancyProperty] =>
                state.updateStringDependee(epk)
                Set.empty
        }
    }
}

object TriggeredSystemPropertiesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        TACAI,
        StringConstancyProperty,
        SystemProperties
    )

    override def triggeredBy: PropertyKind = Callers

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): SystemPropertiesAnalysis = {
        val analysis = new SystemPropertiesAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(SystemProperties)
}
