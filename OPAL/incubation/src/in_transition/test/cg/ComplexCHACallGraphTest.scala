/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm

/**
 * Tests the CHA based `CallGraph` algorithm using a more complex (explicitly
 * annotated) project.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
class ComplexCHACallGraphTest extends AbstractCallGraphTest {

    override def testFileName = "classfiles/callgraph.jar"

    override def testFilePath = "ai"

    override def testCallGraphConfiguration = new CHACallGraphAlgorithmConfiguration(project)

    override def testCallGraphAlgorithm = CallGraphAlgorithm.CHA

}
