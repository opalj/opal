/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.IntegerType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

/**
 * Handles the effect of `java.lang.System.arraycopy*` to points-to sets.
 *
 * @author Dominik Helm
 */
abstract class ArraycopyPointsToAnalysis private[pointsto] (
        final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.System, "", ObjectType.System,
        "arraycopy",
        MethodDescriptor(
            ArraySeq(ObjectType.Object, IntegerType, ObjectType.Object, IntegerType, IntegerType),
            VoidType
        )
    )

    def process(p: SomeProject): PropertyComputationResult = {
        registerAPIMethod()
    }

    def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
            )

        val sourceArr = params.head
        val targetArr = params(2)

        if (sourceArr.isDefined && targetArr.isDefined) {
            val index = tac.properStmtIndexForPC(pc)

            handleArrayLoad(
                ArrayType.ArrayOfObject, pc, sourceArr.get.asVar.definedBy, checkForCast = false
            )
            handleArrayStore(
                ArrayType.ArrayOfObject, targetArr.get.asVar.definedBy, IntTrieSet(index)
            )
        }

        Results(createResults(state))
    }
}

trait ArraycopyPointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    val propertyKind: PropertyMetaInformation
    val createAnalysis: SomeProject => ArraycopyPointsToAnalysis

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}

object TypeBasedArraycopyPointsToAnalysisScheduler extends ArraycopyPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => ArraycopyPointsToAnalysis =
        new ArraycopyPointsToAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedArraycopyPointsToAnalysisScheduler extends ArraycopyPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => ArraycopyPointsToAnalysis =
        new ArraycopyPointsToAnalysis(_) with AllocationSiteBasedAnalysis
}