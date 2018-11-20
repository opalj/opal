/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm

/**
 * Tests the VTA based `CallGraph` algorithm using a more complex (explicitly
 * annotated) project.
 *
 * @author Michael Eichberg
 */
class ComplexBasicVTACallGraphTest extends AbstractCallGraphTest {

    override def testFileName = "classfiles/callgraph.jar"

    override def testFilePath = "ai"

    override def testCallGraphConfiguration = new BasicVTACallGraphAlgorithmConfiguration(project)

    override def testCallGraphAlgorithm = CallGraphAlgorithm.BasicVTA
}
