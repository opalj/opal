/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.EnumValue
import org.opalj.br.ElementValuePairs
import org.opalj.br.ElementValuePair
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.AnalysisModeConfigFactory

/**
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
        val project = Project(file)
        val testConfig = AnalysisModeConfigFactory.createConfig(analysisMode)
        Project.recreate(project, testConfig)
    }

    /*
     * PROPERTY VALIDATION
     */

    override def propertyExtraction(elementValuePairs: ElementValuePairs): Option[String] = {
        analysisMode match {
            case AnalysisModes.LibraryWithOpenPackagesAssumption ⇒
                elementValuePairs collectFirst {
                    case ElementValuePair("opa", EnumValue(_, property)) ⇒ property
                }

            case AnalysisModes.LibraryWithClosedPackagesAssumption ⇒
                elementValuePairs collectFirst {
                    case ElementValuePair("cpa", EnumValue(_, property)) ⇒ property
                }

            case AnalysisModes.DesktopApplication | AnalysisModes.JEE6WebApplication ⇒
                elementValuePairs collectFirst {
                    case ElementValuePair("application", EnumValue(_, property)) ⇒ property
                }
        }
    }
}
