/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package analyses

import org.opalj.br.ObjectType
import org.opalj.AnalysisModes
import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys
import org.opalj.fpcf.properties.ProjectAccessibility

/**
 * @author Michael Reif
 */
abstract class MethodAccessibilityTest extends AbstractFixpointAnalysisAssumptionTest {

    def analysisName = "MethodVisibilityAnalysis"

    override def testFileName = "classfiles/methodVisibilityTest.jar"

    override def testFilePath = "ai"

    override def analysisRunners = {
        Seq(CallableFromClassesInOtherPackagesAnalysis, MethodAccessibilityAnalysis)
    }

    override def propertyKey: PropertyKey[ProjectAccessibility] = ProjectAccessibility.Key

    override def propertyAnnotation: ObjectType = {
        ObjectType("org/opalj/fpcf/test/annotations/ProjectAccessibilityProperty")
    }

    def defaultValue = ProjectAccessibilityKeys.Global.toString
}

class MethodAccessibilityCPATest extends MethodAccessibilityTest {

    override def analysisMode = AnalysisModes.LibraryWithClosedPackagesAssumption
}

class MethodAccessibilityOPATest extends MethodAccessibilityTest {

    override def analysisMode = AnalysisModes.LibraryWithOpenPackagesAssumption
}
