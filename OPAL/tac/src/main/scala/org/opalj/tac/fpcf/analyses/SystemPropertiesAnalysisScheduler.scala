/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.UBP
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.TACAI

class SystemPropertiesAnalysisScheduler private[analyses] (
        final val project: SomeProject
) extends ReachableMethodAnalysis {

    final override implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    def processMethod(
        callContext: ContextType, tacaiEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        assert(tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined)
        val stmts = tacaiEP.ub.tac.get.stmts

        var propertyMap: Map[String, Set[String]] = Map.empty

        for (stmt <- stmts) stmt match {
            case VirtualFunctionCallStatement(call) if (call.name == "setProperty" || call.name == "put") && classHierarchy.isSubtypeOf(call.declaringClass, ObjectType("java/util/Properties")) =>
                propertyMap = computeProperties(propertyMap, call.params, stmts)
            case StaticMethodCall(_, ObjectType.System, _, "setProperty", _, params) =>
                propertyMap = computeProperties(propertyMap, params, stmts)
            case _ =>
        }

        if (propertyMap.isEmpty) {
            return Results()
        }

        def update(
            currentVal: EOptionP[SomeProject, SystemProperties]
        ): Option[InterimEP[SomeProject, SystemProperties]] = currentVal match {
            case UBP(ub) =>
                var oldProperties = ub.properties
                val noNewProperty = propertyMap.forall {
                    case (key, values) =>
                        oldProperties.contains(key) && {
                            val oldValues = oldProperties(key)
                            values.forall(oldValues.contains)
                        }
                }

                if (noNewProperty) {
                    None
                } else {
                    for ((key, values) <- propertyMap) {
                        val oldValues = oldProperties.getOrElse(key, Set.empty)
                        oldProperties = oldProperties.updated(key, oldValues ++ values)
                    }
                    Some(InterimEUBP(project, new SystemProperties(propertyMap)))
                }

            case _: EPK[SomeProject, SystemProperties] =>
                Some(InterimEUBP(project, new SystemProperties(propertyMap)))
        }

        if (tacaiEP.isFinal) {
            PartialResult[SomeProject, SystemProperties](
                project,
                SystemProperties.key,
                update
            )
        } else {
            InterimPartialResult(
                project,
                SystemProperties.key,
                update,
                Set(tacaiEP),
                continuationForTAC(callContext.method)
            )
        }
    }

    def computeProperties(
        propertyMap: Map[String, Set[String]],
        params:      Seq[Expr[DUVar[ValueInformation]]],
        stmts:       Array[Stmt[DUVar[ValueInformation]]]
    ): Map[String, Set[String]] = {
        var res = propertyMap

        assert(params.size == 2)
        val possibleKeys = getPossibleStrings(params.head, stmts)
        val possibleValues = getPossibleStrings(params(1), stmts)

        for (key <- possibleKeys) {
            val values = res.getOrElse(key, Set.empty)
            res = res.updated(key, values ++ possibleValues)
        }

        res
    }

    def getPossibleStrings(
        value: Expr[DUVar[ValueInformation]], stmts: Array[Stmt[DUVar[ValueInformation]]]
    ): Set[String] = {
        value.asVar.definedBy filter { index =>
            index >= 0 && stmts(index).asAssignment.expr.isStringConst
        } map { stmts(_).asAssignment.expr.asStringConst.value }
    }

}

object SystemPropertiesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(Callers),
        PropertyBounds.ub(TACAI)
    )

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): SystemPropertiesAnalysisScheduler = {
        val analysis = new SystemPropertiesAnalysisScheduler(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(SystemProperties)
    )
}
