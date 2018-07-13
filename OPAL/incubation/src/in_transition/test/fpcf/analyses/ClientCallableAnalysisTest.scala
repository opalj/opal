/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

class ClientCallableAnalysisCPATest extends ClientCallableAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class ClientCallableAnalysisOPATest extends ClientCallableAnalysisTest {
    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}
