package org.opalj.fpa

import org.opalj.fp.PropertyKey
import org.opalj.br.ObjectType
import org.opalj.fpa.test.annotations.EntryPointKeys
import org.opalj.AnalysisModes

/**
 *
 * @author Michael Reif
 */
abstract class EntryPointAnalysisTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "EntryPointAnalysis"

    override def testFileName = "classfiles/entryPointTest.jar"

    override def testFilePath = "fpa"

    override def analysisType = EntryPointsAnalysis

    override def dependees =
        Seq(StaticMethodAccessibilityAnalysis,
            LibraryLeakageAnalysis,
            FactoryMethodAnalysis,
            InstantiabilityAnalysis,
            MethodAccessibilityAnalysis)

    override def propertyKey: PropertyKey = EntryPoint.Key

    override def propertyAnnotation: ObjectType =
        ObjectType("org/opalj/fpa/test/annotations/EntryPointProperty")

    def defaultValue = EntryPointKeys.IsEntryPoint.toString
}

class EntryPointAnalysisCPATest extends EntryPointAnalysisTest {

    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class EntryPointAnalysisOPATest extends EntryPointAnalysisTest {

    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}