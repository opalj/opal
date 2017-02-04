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
package analysis

import org.opalj.fpcf.properties.NoEntryPoint
import org.opalj.fpcf.properties.IsEntryPoint
import org.opalj.fpcf.properties.EntryPoint
import org.opalj.br.analyses.SomeProject
import org.opalj.br._

/**
 * Determines the methods that are Entry Points into a given Program.
 *
 *
 * @author Michael Reif
 */
class EntryPointsAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    private[this] val mainMethodDescriptor = MethodDescriptor(ArayType(ObjectType.String), VoidType)

    /*
   * This method is only called in the corresponding analysis runner. Therefore it it guaranteed that
   * the analysisMode during the execution is always a desktop application.
   */
    def determineEntrypoints(method: Method): PropertyComputationResult = {
        if (method.isStatic &&
            method.isPublic &&
            (method.descriptor eq mainMethodDescriptor) &&
            method.name == "main")
            ImmediateResult(method, IsEntryPoint)
        else
            ImmediateResult(method, NoEntryPoint)
    }
}

object EntryPointsAnalysis extends FPCFAnalysisRunner {

    final def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract && !m.isNative ⇒ m
    }

    override def derivedProperties: Set[PropertyKind] = Set(EntryPoint.Key)
    override def usedProperties: Set[PropertyKind] = Set.empty
    override def recommendations: Set[FPCFAnalysisRunner] = Set.empty

    protected[fpcf] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        import AnalysisModes._
        project.analysisMode match {
            case DesktopApplication ⇒
                val analysis = new EntryPointsAnalysis(project)
                propertyStore <||< (entitySelector, analysis.determineEntrypoints)
                analysis
            case JEE6WebApplication ⇒
                val analysis = new JavaEEEntryPointsAnalysis(project)
                propertyStore <||< (JavaEEEntryPointsAnalysis.entitySelector, analysis.determineEntrypoints)
                analysis
            case (CPA | OPA) ⇒
                val analysis = new LibraryEntryPointsAnalysis(project)
                val analysisRunner = project.get(FPCFAnalysesManagerKey)
                analysisRunner.run(SimpleInstantiabilityAnalysis)
                analysisRunner.run(CallableFromClassesInOtherPackagesAnalysis)
                analysisRunner.run(MethodAccessibilityAnalysis)
                propertyStore <||< (entitySelector, analysis.determineEntrypoints)
                analysis
        }
    }
}
