/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.IntegerType
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.ReferenceType

class AllocationSiteBasedTamiFlexPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToAnalysisBase with AllocationSiteBasedAnalysis {

    //private[this] val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    private[this] val ArrayT = ObjectType("java/lang/reflect/Array")
    //private[this] val FieldT = ObjectType("java/lang/reflect/Field")
    //private[this] val MethodT = ObjectType("java/lang/reflect/Method")
    //private[this] val UnsafeT = ObjectType("sun/misc/Unsafe")

    val declaredMethods = project.get(DeclaredMethodsKey)

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses = List(
            new TamiFlexPointsToNewInstanceAnalysis[ElementType, PointsToSet](
                project,
                declaredMethods(
                    ArrayT,
                    "",
                    ArrayT,
                    "newInstance",
                    MethodDescriptor.apply(
                        RefArray(ObjectType.Class, IntegerType), ObjectType.Object
                    )
                ),
                pointsToPropertyKey,
                emptyPointsToSet,
                createPointsToSet
            ),
            new TamiFlexPointsToNewInstanceAnalysis[ElementType, PointsToSet](
                project,
                declaredMethods(
                    ArrayT,
                    "",
                    ArrayT,
                    "newInstance",
                    MethodDescriptor.apply(
                        RefArray(ObjectType.Class, ArrayType(IntegerType)), ObjectType.Object
                    )
                ),
                pointsToPropertyKey,
                emptyPointsToSet,
                createPointsToSet
            )
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

object AllocationSiteBasedTamiFlexPointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        AllocationSitePointsToSet
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        AllocationSitePointsToSet
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AllocationSiteBasedTamiFlexPointsToAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}

class TamiFlexPointsToNewInstanceAnalysis[E, P >: Null <: PointsToSetLike[E, _, P]](
        final val project:                                SomeProject,
        override val apiMethod:                           DeclaredMethod,
        override protected[this] val pointsToPropertyKey: PropertyKey[P],
        override protected val emptyPointsToSet:          P,
        val createPTS:                                    (Int, DeclaredMethod, ReferenceType, Boolean, Boolean) ⇒ P
) extends PointsToAnalysisBase with APIBasedAnalysis {
    override type ElementType = E
    override type PointsToSet = P

    override def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean
    ): PointsToSet =
        createPTS(pc, declaredMethod, allocatedType, isConstant, isEmptyArray)

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def handleNewCaller(
        caller:   DefinedMethod,
        pc:       Int,
        isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val allocatedTypes = tamiFlexLogData.classes(caller, line)
        var pts = emptyPointsToSet
        for (allocatedType ← allocatedTypes)
            pts = pts.included(createPointsToSet(pc, caller, allocatedType, isConstant = false))

        val defSite = definitionSites(caller.definedMethod, pc)

        if (pts ne emptyPointsToSet) {
            PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                defSite,
                pointsToPropertyKey,
                {
                    case UBP(ub: PointsToSet @unchecked) ⇒
                        val newPointsToSet = ub.included(pts)

                        if (newPointsToSet ne ub) {
                            Some(InterimEUBP(defSite, newPointsToSet))
                        } else {
                            None
                        }

                    case _: EPK[Entity, _] ⇒
                        Some(InterimEUBP(defSite, pts))

                    case eOptP ⇒
                        throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                }
            )
        } else
            Results()
    }
}