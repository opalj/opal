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

import java.net.URL

import org.opalj.AnalysisModes
import org.opalj.br.ObjectType
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.FormalParametersKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape

class InterproceduralEscapeAnalysisTest extends AbstractFixpointAnalysisTest {

    def analysisName = "InterproceduralEscapeAnalysis"

    override def testFileName = "escape-1.8-g-parameters-genericsignature.jar"

    override def testFilePath = "bi"

    override def analysisRunner = InterproceduralEscapeAnalysis

    override def propertyKey: PropertyKey[EscapeProperty] = EscapeProperty.key

    override def propertyAnnotation: ObjectType = ObjectType("annotations/escape/Escapes")

    override def containerAnnotation: ObjectType = ObjectType("annotations/escape/EscapeProperties")
    /**
     * Add all AllocationsSites found in the project to the entities in the property
     * stores created with the PropertyStoreKey.
     */
    override def init(): Unit = {
        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.addEntityDerivationFunction(project)(FormalParametersKey.entityDerivationFunction)
    }

    override def loadProject: Project[URL] = {
        val project = org.opalj.br.analyses.Project(file)
        val testConfig = AnalysisModeConfigFactory.createConfig(AnalysisModes.OPA)
        Project.recreate(project, testConfig)
    }

    def defaultValue: String = NoEscape.toString
}
