/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.immutable.ArraySeq

import org.opalj.br.ArrayType
import org.opalj.br.BooleanType
import org.opalj.br.ClassType
import org.opalj.br.DeclaredMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.tac.fpcf.properties.NoTACAI

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
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "forName",
                    MethodDescriptor(ClassType.String, ClassType.Class)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "forName",
                    MethodDescriptor(
                        ArraySeq(ClassType.String, BooleanType, ClassType.ClassLoader),
                        ClassType.Class
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "forName",
                    MethodDescriptor(
                        ArraySeq(ClassType.Module, ClassType.String),
                        ClassType.Class
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getConstructor",
                    MethodDescriptor(ArrayType(ClassType.Class), ClassType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ClassType.Class), ClassType.Constructor)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getMethod",
                    MethodDescriptor(ArraySeq(ClassType.String, ArrayType(ClassType.Class)), ClassType.Method)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(ArraySeq(ClassType.String, ArrayType(ClassType.Class)), ClassType.Method)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getField",
                    MethodDescriptor(ArraySeq(ClassType.String), ClassType.Field)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getDeclaredField",
                    MethodDescriptor(ArraySeq(ClassType.String), ClassType.Field)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles,
                    "",
                    ClassType.MethodHandles,
                    "lookup",
                    MethodDescriptor.withNoArgs(ClassType.MethodHandles$Lookup)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findStatic",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.MethodType),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findVirtual",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.MethodType),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findConstructor",
                    MethodDescriptor(ArraySeq(ClassType.Class, ClassType.MethodType), ClassType.MethodHandle)
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findSpecial",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.MethodType, ClassType.Class),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findGetter",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.Class),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findStaticGetter",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.Class),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findSetter",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.Class),
                        ClassType.MethodHandle
                    )
                )
            ),
            new ReflectionMethodAllocationsAnalysis(
                project,
                declaredMethods(
                    ClassType.MethodHandles$Lookup,
                    "",
                    ClassType.MethodHandles$Lookup,
                    "findStaticSetter",
                    MethodDescriptor(
                        ArraySeq(ClassType.Class, ClassType.String, ClassType.Class),
                        ClassType.MethodHandle
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
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, FinalEP(null, NoTACAI))

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

        Results(createResults)
    }
}

object ReflectionAllocationsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler
    with PointsToBasedAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation :+ DeclaredMethodsKey

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
