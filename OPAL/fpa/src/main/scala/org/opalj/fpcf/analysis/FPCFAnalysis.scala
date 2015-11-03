/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

import net.ceedubs.ficus.Ficus._
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.AnalysisModes._

/**
 *
 *
 * @author Michael Reif
 */
trait FPCFAnalysis {

    def project: SomeProject

}

abstract class AbstractFPCFAnalysis[T <: Entity](
        val project:        SomeProject,
        val entitySelector: PartialFunction[Entity, T] = PropertyStore.entitySelector()
) extends FPCFAnalysis {

    /**
     * The implementation of the analysis. This method will  in general be executed concurrently
     * for multiple entities.
     *
     * @param entity The entity which should be analyzed.
     * @return The result of analyzing the given entity.
     */
    def determineProperty(entity: T): PropertyComputationResult

    implicit val propertyStore = project.get(SourceElementsPropertyStoreKey)

    propertyStore <||< (
        entitySelector,
        (determineProperty _).asInstanceOf[T ⇒ PropertyComputationResult]
    )
}

trait AnalysisMode extends FPCFAnalysis {
    lazy val analysisMode = AnalysisModes.withName(project.config.as[String]("org.opalj.analysisMode"))

    def isOpenLibrary = analysisMode eq OPA

    def isClosedLibrary = analysisMode eq CPA

    def isApplication = analysisMode eq APP
}

abstract class DefaultFPCFAnalysis[T <: Entity](
    project:        SomeProject,
    entitySelector: PartialFunction[Entity, T] = PropertyStore.entitySelector()
) extends AbstractFPCFAnalysis[T](project, entitySelector) with AnalysisMode
