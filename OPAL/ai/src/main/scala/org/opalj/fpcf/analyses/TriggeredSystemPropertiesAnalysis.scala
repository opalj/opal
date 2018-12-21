/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.properties.SystemProperties
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

class TriggeredSystemPropertiesAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
        propertyStore(declaredMethod, CallersProperty.key) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        val tacaiEP = propertyStore(method, TACAI.key)
        if (tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined) {
            processMethod(declaredMethod, tacaiEP.asEPS)
        } else {
            InterimPartialResult(
                None,
                Some(tacaiEP),
                continuation(declaredMethod)
            )
        }

    }

    def continuation(declaredMethod: DeclaredMethod)(eps: SomeEPS): PropertyComputationResult = {
        eps match {
            case UBP(_: TACAI) ⇒
                processMethod(declaredMethod, eps.asInstanceOf[EPS[Method, TACAI]])
        }
    }

    def processMethod(
        declaredMethod: DeclaredMethod, tacaiEP: EPS[Method, TACAI]
    ): PropertyComputationResult = {
        assert(tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined)
        val stmts = tacaiEP.ub.tac.get.stmts

        var propertyMap: Map[String, Set[String]] = Map.empty

        for (stmt ← stmts) stmt match {
            case VirtualFunctionCallStatement(call) if (call.name == "setProperty" || call.name == "put") && classHierarchy.isSubtypeOf(call.declaringClass, ObjectType("java/util/Properties")) ⇒
                propertyMap = computeProperties(propertyMap, call.params, stmts)
            case StaticMethodCall(_, ObjectType.System, _, "setProperty", _, params) ⇒
                propertyMap = computeProperties(propertyMap, params, stmts)
            case _ ⇒
        }

        if (propertyMap.isEmpty) {
            return NoResult;
        }

        def update(
            currentVal: EOptionP[SomeProject, SystemProperties]
        ): Option[EPS[SomeProject, SystemProperties]] = currentVal match {
            case UBP(ub) ⇒
                var oldProperties = ub.properties
                val noNewProperty = propertyMap.forall {
                    case (key, values) ⇒
                        oldProperties.contains(key) && {
                            val oldValues = oldProperties(key)
                            values.forall(oldValues.contains)
                        }
                }

                if (noNewProperty) {
                    None
                } else {
                    for ((key, values) ← propertyMap) {
                        val oldValues = oldProperties.getOrElse(key, Set.empty)
                        oldProperties = oldProperties.updated(key, oldValues ++ values)
                    }
                    Some(InterimEUBP(project, new SystemProperties(propertyMap)))
                }
            case _: EPK[SomeProject, SystemProperties] ⇒ Some(InterimEUBP(project, new SystemProperties(propertyMap)))
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
                Some(tacaiEP),
                continuation(declaredMethod)
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

        for (key ← possibleKeys) {
            val values = res.getOrElse(key, Set.empty)
            res = res.updated(key, values ++ possibleValues)
        }

        res
    }

    def getPossibleStrings(
        value: Expr[DUVar[ValueInformation]], stmts: Array[Stmt[DUVar[ValueInformation]]]
    ): Set[String] = {
        value.asVar.definedBy filter { index ⇒
            index >= 0 && stmts(index).asAssignment.expr.isStringConst
        } map { stmts(_).asAssignment.expr.asStringConst.value }
    }

}

object TriggeredSystemPropertiesAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty),
        PropertyBounds.ub(TACAI)
    )

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): TriggeredSystemPropertiesAnalysis = {
        val analysis = new TriggeredSystemPropertiesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(SystemProperties)
    )
}