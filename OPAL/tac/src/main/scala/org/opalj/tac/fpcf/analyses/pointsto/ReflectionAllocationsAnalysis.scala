/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.pointsto

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.BooleanType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.APIBasedAnalysis

/**
 * Introduces additional allocation sites for reflection methods.
 *
 * @author Dominik Helm
 */
class ReflectionAllocationsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(ObjectType.String, ObjectType.Class)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(
                        RefArray(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")),
                        ObjectType.Class
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "forName",
                    MethodDescriptor(
                        RefArray(ObjectType("java/lang/Module"), ObjectType.String),
                        ObjectType.Class
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getMethod",
                    MethodDescriptor(RefArray(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(RefArray(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            )
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

class ReflectionMethodAllocationsAnalysis(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with AllocationSiteBasedAnalysis with APIBasedAnalysis {

    override def handleNewCaller(
        callContext: ContextType,
        pc:          Int,
        isDirect:    Boolean
    ): ProperPropertyComputationResult = {

        val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callContext, null)

        val defSite = definitionSites(callContext.method.definedMethod, pc)
        state.includeSharedPointsToSet(
            defSite,
            createPointsToSet(
                pc,
                callContext,
                apiMethod.descriptor.returnType.asReferenceType,
                isConstant = false
            ),
            PointsToSetLike.noFilter
        )

        Results(createResults(state))
    }
}

object ReflectionAllocationsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, AllocationSitePointsToSet)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(AllocationSitePointsToSet)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReflectionAllocationsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}
