/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package analyses

import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.NotFactoryMethod
import org.opalj.fpcf.properties.FactoryMethod

/**
 * @author Michael Reif
 */
class FactoryMethodAnalysisTest extends AbstractFixpointAnalysisTest {

    override def analysisName = "FactoryMethodAnalysis"

    override def testFileName = "classfiles/factorymethodTest.jar"

    override def testFilePath = "ai"

    override def analysisRunners = Seq(FactoryMethodAnalysis)

    override def propertyKey: PropertyKey[FactoryMethod] = FactoryMethod.key

    override def propertyAnnotation: ObjectType =
        ObjectType("org/opalj/fpcf/test/annotations/FactoryMethodProperty")

    override def defaultValue = NotFactoryMethod.toString
}
