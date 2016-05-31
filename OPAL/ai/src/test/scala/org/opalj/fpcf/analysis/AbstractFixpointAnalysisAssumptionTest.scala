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

import org.opalj.br.Annotation
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.EnumValue
import org.opalj.br.ElementValuePair
import org.opalj.br.analyses.AnalysisModeConfigFactory

/**
 *
 * Tests a fix-point analysis implementation using the classes in the configured
 * class file.
 *
 * @author Michael Reif
 */
abstract class AbstractFixpointAnalysisAssumptionTest extends AbstractFixpointAnalysisTest {

    def analysisMode: AnalysisMode

    /*
     * PROJECT SETUP
     */

    override def loadProject: Project[URL] = {
        val project = org.opalj.br.analyses.Project(file)
        val testConfig = AnalysisModeConfigFactory.createConfig(analysisMode)
        Project.recreate(project, testConfig)
    }

    /*
     * PROPERTY VALIDATION
     */

    override def propertyExtraction(annotation: Annotation): Option[String] = {
        analysisMode match {
            case AnalysisModes.LibraryWithOpenPackagesAssumption ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("opa", EnumValue(_, property)) ⇒ property }
                )
            case AnalysisModes.LibraryWithClosedPackagesAssumption ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("cpa", EnumValue(_, property)) ⇒ property }
                )
            case AnalysisModes.DesktopApplication | AnalysisModes.JEE6WebApplication ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("application", EnumValue(_, property)) ⇒ property }
                )
        }
    }
}