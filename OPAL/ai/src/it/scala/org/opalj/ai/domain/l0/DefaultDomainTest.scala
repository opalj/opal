/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import java.net.URL

import org.opalj.br.Method
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.Project

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods using the l1.DefaultDomain. It basically
 * tests if we can load and process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultDomainTest extends DomainTestInfrastructure("l0.DefaultDomain") {

    type AnalyzedDomain = l0.BaseDomain[URL]

    override def analyzeAIResult(
        project: Project[URL],
        method:  Method,
        result:  AIResult { val domain: AnalyzedDomain }
    ): Unit = {

        super.analyzeAIResult(project, method, result)

        implicit val code = result.code
        implicit val classHierarchy = project.classHierarchy
        val operandsArray = result.operandsArray
        val evaluatedInstructions = result.evaluatedInstructions
        for {
            PCAndInstruction(pc, instruction) <- result.code
            if evaluatedInstructions.contains(pc)
            operands = operandsArray(pc)
        } {
            instruction.nextInstructions(pc, regularSuccessorsOnly = true).foreach { nextPC =>
                if (evaluatedInstructions.contains(nextPC)) {
                    val nextOperands = operandsArray(nextPC)
                    val stackSizeBefore = operands.foldLeft(0)(_ + _.computationalType.operandSize)
                    val stackSizeAfter = nextOperands.foldLeft(0)(_ + _.computationalType.operandSize)
                    val popped = instruction.numberOfPoppedOperands { operands(_).computationalType.category }
                    val pushed = instruction.numberOfPushedOperands { operands(_).computationalType.category }
                    assert { (nextOperands.size - operands.size) == pushed - popped }

                    assert(
                        instruction.stackSlotsChange == (stackSizeAfter - stackSizeBefore),
                        s"the height of the stack is not as expected for $instruction: "+
                            s"${instruction.stackSlotsChange} <> ${stackSizeAfter - stackSizeBefore}"
                    )
                }
            }
        }

    }

    def Domain(project: Project[URL], method: Method): AnalyzedDomain = {
        new l0.BaseDomain(project, method)
    }

}
