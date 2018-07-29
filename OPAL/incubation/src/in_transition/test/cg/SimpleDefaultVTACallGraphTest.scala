/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm

/**
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
class SimpleDefaultVTACallGraphTest extends AbstractCallGraphTest {

    override def testFileName = "classfiles/simpleCallgraph.jar"

    override def testFilePath = "ai"

    override def testCallGraphConfiguration = new DefaultVTACallGraphAlgorithmConfiguration(project)

    override def testCallGraphAlgorithm = CallGraphAlgorithm.DefaultVTA
}
