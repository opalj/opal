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

import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.ClientCallable
import org.opalj.fpcf.properties.IsClientCallable

/**
 * @author Michael Reif
 */
abstract class ClientCallableAnalysisTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "CallableFromClassesInOtherPackagesAnalysis"

    override def testFileName = "classfiles/clientCallableTest.jar"

    override def testFilePath = "ai"

    override def analysisRunners = Seq(CallableFromClassesInOtherPackagesAnalysis)

    override def propertyKey: PropertyKey[ClientCallable] = ClientCallable.Key

    override def propertyAnnotation: ObjectType =
        ObjectType("org/opalj/fpcf/test/annotations/CallabilityProperty")

    def defaultValue = IsClientCallable.toString
}

class ClientCallableAnalysisCPATest    extends ClientCallableAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class ClientCallableAnalysisOPATest    extends ClientCallableAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}
