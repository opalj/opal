/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.instructions.NEW
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.CreateNewArrayInstruction
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Updates InstantiatedTypes attached to a method for each constructor
 * call occurring within that method.
 *
 * This is a simple analysis which yields useful results for basic tests,
 * but it does not capture, e.g., indirect constructor calls through reflection.
 *
 * The analysis is triggered for a method once it becomes reachable, i.e., a
 * caller has been added. Thus, the property is not computed for unreachable methods.
 *
 * @author Andreas Bauer
 */
// TODO AB replace later with a more sophisticated analysis (based on the RTA one)
class SimpleInstantiatedTypesAnalysis( final val project: SomeProject) extends ReachableMethodAnalysis {

    override def processMethod(definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        val code = definedMethod.definedMethod.body.get

        val instantiatedObjectTypes = code.instructions.flatMap({
            case NEW(declType) ⇒ Some(declType)
            case _             ⇒ None
        })

        // We only care about arrays of reference types.
        val instantiatedArrays = code.instructions.flatMap({
            case arr: CreateNewArrayInstruction if arr.arrayType.elementType.isReferenceType ⇒ Some(arr.arrayType)
            case _ ⇒ None
        })

        PartialResult(
            definedMethod,
            InstantiatedTypes.key,
            update(definedMethod, UIDSet((instantiatedObjectTypes ++ instantiatedArrays).toSeq: _*))
        )
    }

    // TODO AB code duplication; something like this appears in many places
    def update(
        method:               DefinedMethod,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Option[EPS[DefinedMethod, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(method, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(method, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object SimpleInstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new SimpleInstantiatedTypesAnalysis(project)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set.empty
    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
    override def triggeredBy: PropertyKind = Callers.key
}
