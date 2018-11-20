/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm

/**
 * Tests the generation of the call graph for a small project.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
class SimpleCHACallGraphTest extends AbstractCallGraphTest {

    override def testFileName = "classfiles/simpleCallgraph.jar"

    override def testFilePath = "ai"

    override def testCallGraphConfiguration = new CHACallGraphAlgorithmConfiguration(project)

    override def testCallGraphAlgorithm = CallGraphAlgorithm.CHA
}
