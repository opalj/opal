/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
   * This method is only called in the corresponding analysis runner. Therefore it it guaranteed that
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

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
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
