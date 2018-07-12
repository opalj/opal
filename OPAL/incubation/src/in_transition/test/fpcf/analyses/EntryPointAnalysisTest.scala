/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package analyses

import org.opalj.AnalysisModes
import org.opalj.br.ObjectType
import org.opalj.fpcf.test.annotations.EntryPointKeys
import org.opalj.fpcf.properties.EntryPoint

/**
 *
 * @author Michael Reif
 */
abstract class EntryPointAnalysisTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "EntryPointAnalysis"

    override def testFileName = "classfiles/entryPointTest.jar"

    override def testFilePath = "ai"

    override def analysisRunners = Seq(EntryPointsAnalysis)

    override def propertyKey: PropertyKey[EntryPoint] = EntryPoint.Key

    override def propertyAnnotation: ObjectType =
        ObjectType("org/opalj/fpcf/test/annotations/EntryPointProperty")

    def defaultValue = EntryPointKeys.IsEntryPoint.toString
}

class EntryPointAnalysisCPATest extends EntryPointAnalysisTest {

    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class EntryPointAnalysisOPATest extends EntryPointAnalysisTest {

    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}
