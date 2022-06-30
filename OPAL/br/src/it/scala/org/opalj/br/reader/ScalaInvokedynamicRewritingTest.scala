/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.Instruction

/**
 * This test loads all classes found in the Scala 2.12.4 libraries and verifies that all
 * suported [[INVOKEDYNAMIC]] instructions can be resolved.
 *
 * @author Arne Lottmann
 * @author Andreas Amuttsch
 * @author Michael Eichberg
 */
class ScalaInvokedynamicRewritingTest extends InvokedynamicRewritingTest {

    test("rewriting of invokedynamic instructions in Scala 2.12.4 library") {
        val project = load(locateTestResources("classfiles/scala-2.12.4", "bi"))

        val invokedynamics = project.allMethodsWithBody.par.flatMap { method =>
            method.body.get.collect({
                case i: INVOKEDYNAMIC => i
            }: PartialFunction[Instruction, Instruction])
        }

        if (invokedynamics.nonEmpty) {
            fail(invokedynamics.mkString("Could not resolve:", "\n", "\n"))
        }
    }
}
