/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.immutable.ArraySeq

import org.opalj.br.ArrayType
import org.opalj.br.BooleanType
import org.opalj.br.DeclaredMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results

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
                        ArraySeq(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")),
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
                        ArraySeq(ObjectType("java/lang/Module"), ObjectType.String),
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
                    "getConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "getMethod",
                    MethodDescriptor(ArraySeq(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(ArraySeq(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "getField",
                    MethodDescriptor(ArraySeq(ObjectType.String), ObjectType.Field)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "getDeclaredField",
                    MethodDescriptor(ArraySeq(ObjectType.String), ObjectType.Field)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles,
                    "",
                    ObjectType.MethodHandles,
                    "lookup",
                    MethodDescriptor.withNoArgs(ObjectType.MethodHandles$Lookup)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findStatic",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.MethodType),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findVirtual",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.MethodType),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findConstructor",
                    MethodDescriptor(ArraySeq(ObjectType.Class, ObjectType.MethodType), ObjectType.MethodHandle)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findSpecial",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.MethodType, ObjectType.Class),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findGetter",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.Class),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findStaticGetter",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.Class),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findSetter",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.Class),
                        ObjectType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ObjectType.MethodHandles$Lookup,
                    "",
                    ObjectType.MethodHandles$Lookup,
                    "findStaticSetter",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.String, ObjectType.Class),
                        ObjectType.MethodHandle
                    )
                )
            )
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

class ReflectionMethodAllocationsAnalysis(
    final val project:            SomeProject,
    override final val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with AllocationSiteBasedAnalysis with APIBasedAnalysis {

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {

        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)

        val defSite = getDefSite(pc)
        state.includeSharedPointsToSet(
            defSite,
            createPointsToSet(
                pc,
                callerContext,
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
        AbstractPointsToBasedAnalysis.requiredProjectInformation :+ DeclaredMethodsKey

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
