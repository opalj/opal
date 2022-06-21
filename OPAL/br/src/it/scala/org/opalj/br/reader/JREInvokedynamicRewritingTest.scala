/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

import org.opalj.bi.isCurrentJREAtLeastJava8
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.Instruction

/**
 * This test loads all classes found in the JRE and verifies that all [[INVOKEDYNAMIC]]
 * instructions can be resolved.
 *
 * @author Arne Lottmann
 */
class JREInvokedynamicRewritingTest extends InvokedynamicRewritingTest {

    test("rewriting of invokedynamic instructions in the JRE") {
        if (!isCurrentJREAtLeastJava8) {
            fail("the current JDK does not use invokedynamic or was not correctly recognized")
        }

        val project = load(org.opalj.bytecode.JRELibraryFolder)

        val invokedynamics = project.allMethodsWithBody.par.flatMap { method =>
            method.body.get.collect({
                case i: INVOKEDYNAMIC => i
            }: PartialFunction[Instruction, Instruction])
        }

        // if the test fails we want to know the invokedynamic instructions
        assert(invokedynamics.isEmpty, "all invokedynamics should have been removed")
    }
}
