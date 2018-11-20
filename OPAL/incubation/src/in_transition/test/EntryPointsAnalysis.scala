/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.fpcf.properties.NoEntryPoint
import org.opalj.fpcf.properties.IsEntryPoint
import org.opalj.fpcf.properties.EntryPoint
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ArrayType
import org.opalj.br.VoidType

/**
 * Determines the methods that are Entry Points into a given Program.
 *
 * @author Michael Reif
 */
class EntryPointsAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    val MainMethodDescriptor = MethodDescriptor(ArrayType(ObjectType.String), VoidType)

    /*
   * This method is only called in the corresponding analysis runner. Therefore it is guaranteed that
   * the analysisMode during the execution is always a desktop application.
   */
    def determineEntrypoints(method: Method): PropertyComputationResult = {
        if (method.isStatic && method.isPublic && method.descriptor == MainMethodDescriptor &&
            method.name == "main")
            Result(method, IsEntryPoint)
        else
            Result(method, NoEntryPoint)
    }
}

object EntryPointsAnalysis extends FPCFEagerAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(EntryPoint.Key)

    override def usedProperties: Set[PropertyKind] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused : Null): FPCFAnalysis = {
        import AnalysisModes._
        val ms = project.allMethods.filter(m ⇒ !m.isAbstract && !m.isNative)
        project.analysisMode match {
            case DesktopApplication ⇒
                val analysis = new EntryPointsAnalysis(project)
                propertyStore.scheduleEagerComputationsForEntities(ms)(analysis.determineEntrypoints)
                analysis
            case _ ⇒
                // In all other cases we simply fallback to treat the code base as a "library"
                // this should soundly overapproximate the set of entry points.
                SimpleInstantiabilityAnalysis(project)
                val analysisRunner = project.get(FPCFAnalysesManagerKey)
                analysisRunner.run(CallableFromClassesInOtherPackagesAnalysis)
                analysisRunner.run(MethodAccessibilityAnalysis)
                propertyStore.waitOnPropertyComputationCompletion(true, true)
                // STRICTLY REQUIRED OVER HERE - THE FOLLOWING ANALYSIS REQUIRES IT!
                val analysis = new LibraryEntryPointsAnalysis(project)
                propertyStore.scheduleEagerComputationsForEntities(ms)(analysis.determineEntrypoints)
                analysis
        }
    }
}
