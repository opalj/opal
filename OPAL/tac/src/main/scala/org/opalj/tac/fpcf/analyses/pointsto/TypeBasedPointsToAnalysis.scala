/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.NoTypes
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.TACAI

/**
 * An andersen-style points-to analysis, i.e. points-to sets are modeled as subsets.
 * It uses [[TypeBasedPointsToSet]] as points-to sets, i.e. does not differentiate allocation sites for
 * the same types.
 * The analysis is field-based, array-based and context-insensitive.
 * As the analysis is build on top of the [[org.opalj.tac.TACAI]], it is (implicitly)
 * flow-sensitive (which is not the case for pure andersen-style).
 *
 * Points-to sets may be attached to the following entities:
 *  - [[org.opalj.tac.common.DefinitionSite]] for local variables.
 *  - [[org.opalj.br.Field]] for fields (either static of instance)
 *  - [[org.opalj.br.DeclaredMethod]] for the points-to set of the return values.
 *  - [[org.opalj.br.analyses.VirtualFormalParameter]] for the parameters of a method.
 *  - [[org.opalj.br.ObjectType]] for the element type of an array.
 *
 * @author Florian Kuebler
 */
class TypeBasedPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis[ObjectType, TypeBasedPointsToSet] {

    override protected[this] val pointsToPropertyKey: PropertyKey[TypeBasedPointsToSet] = {
        TypeBasedPointsToSet.key
    }

    override protected def emptyPointsToSet: TypeBasedPointsToSet = NoTypes

    override def createPointsToSet(
        pc: Int, declaredMethod: DeclaredMethod, allocatedType: ObjectType
    ): TypeBasedPointsToSet = TypeBasedPointsToSet(UIDSet(allocatedType))
}

object TypeBasedPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        TypeBasedPointsToSet,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.ub(TypeBasedPointsToSet))

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): TypeBasedPointsToAnalysis = {
        val analysis = new TypeBasedPointsToAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}
