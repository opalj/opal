package org.opalj.fpcf
package analysis

import org.opalj.br.ObjectType
import org.opalj.AnalysisModes
import org.opalj.fpcf.test.annotations.EntryPointKeys

/**
 *
 * @author Michael Reif
 */
abstract class EntryPointAnalysisTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "EntryPointAnalysis"

    override def testFileName = "classfiles/entryPointTest.jar"

    override def testFilePath = "fpcfa"

    override def analysisRunner = EntryPointsAnalysis

    override def propertyKey: PropertyKey = EntryPoint.key

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