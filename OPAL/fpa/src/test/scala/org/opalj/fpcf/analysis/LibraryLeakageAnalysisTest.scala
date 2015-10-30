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
package org.opalj.fpcf
package analysis

import org.opalj.br.ObjectType
import org.opalj.AnalysisModes
import org.opalj.fpcf.test.annotations.LibraryLeakageKeys

/**
 * @author Michael Reif
 */
abstract class LibraryLeakageAnalysisTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "LibraryLeakageAnalysis"

    override def testFileName = "classfiles/libraryLeakageTest.jar"

    override def testFilePath = "fpa"

    override def analysisRunner = LibraryLeakageAnalysis

    override def propertyKey: PropertyKey = LibraryLeakage.Key

    override def propertyAnnotation: ObjectType =
        ObjectType("org/opalj/fpcf/test/annotations/LibraryLeakageProperty")

    def defaultValue = LibraryLeakageKeys.Leakage.toString
}

class LibraryLeakageAnalysisCPATest extends LibraryLeakageAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class LibraryLeakageAnalysisOPATest extends LibraryLeakageAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}