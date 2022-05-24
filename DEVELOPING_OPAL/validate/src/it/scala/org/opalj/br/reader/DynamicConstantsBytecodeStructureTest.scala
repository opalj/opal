/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.log.GlobalLogContext
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.LDCDynamic
import org.opalj.br.instructions.WIDE
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.domain.l0.BaseDomain

/**
 * Test that code with dynamic constants is loaded without exceptions and after rewriting is still
 * valid bytecode.
 *
 * @author Dominik Helm
 */
@RunWith(classOf[JUnitRunner])
class DynamicConstantsBytecodeStructureTest extends AnyFunSpec with Matchers {

    def testMethod(
        method:        Method,
        domainFactory: Method => Domain
    ): Unit = {
        val instructions = method.body.get.instructions

        val domain = domainFactory(method)
        try {
            val result = BaseAI(method, domain)
            // the abstract interpretation succeed
            result should not be Symbol("wasAborted")
            // the layout of the instructions array is correct
            for {
                pc <- instructions.indices
                if instructions(pc) != null
            } {
                val modifiedByWide = pc != 0 && instructions(pc) == WIDE
                val nextPc = instructions(pc).indexOfNextInstruction(pc, modifiedByWide)
                instructions.slice(pc + 1, nextPc).foreach(_ should be(null))
            }
        } catch {
            case e: InterpretationFailedException =>
                val pc = e.pc
                val details =
                    if (pc == instructions.length) {
                        "post-processing failed"
                    } else {
                        e.operandsArray(pc).mkString(s"\tAt PC $pc\n\twith stack:\n", ", ", "")
                    }
                val msg = e.getMessage+"\n"+
                    (if (e.getCause != null) "\tcause: "+e.getCause.getMessage+"\n" else "") +
                    details+"\n"+
                    method.toJava +
                    instructions.zipWithIndex.map(_.swap).mkString("\n\t\t", "\n\t\t", "\n")
                Console.err.println(msg)
                fail(msg)
        }
    }

    describe("test interpretation of dynamic constants") {
        // Note: The bytecode for this test project was created using
        // [[org.opalj.test.fixtures.dynamicConstants.DynamicConstantsCreationTest]]
        val dynamicConstantsJar = locateTestResources("classfiles/dynamic_constants.jar", "bi")

        describe("testing the non-rewritten methods of the dynamic constants test project") {
            val config = DynamicConstantRewriting.defaultConfig(
                rewrite = false,
                logRewrites = false
            )
            val project = Project(dynamicConstantsJar, GlobalLogContext, config)
            info(project.statistics.toList.map(_.toString).filter(_.startsWith("(Project")).mkString(","))

            it("should be able to perform abstract interpretation of rewritten dynamic constants "+
                "in the dynamic constants test project") {
                project.allMethods.foreach(testMethod(_, m => BaseDomain(project, m)))
            }
        }

        describe("testing the rewritten methods of the dynamic constants test project") {
            val config = DynamicConstantRewriting.defaultConfig(
                rewrite = true,
                logRewrites = false
            )
            val project = Project(dynamicConstantsJar, GlobalLogContext, config)
            info(project.statistics.toList.map(_.toString).filter(_.startsWith("(Project")).mkString(","))

            it("should be able to rewrite all dynamic constants in the dynamic constants test "+
                "project") {
                val hasDynamicLoadsRemaining =
                    project.allMethods.exists { m =>
                        m.body.get.instructions.exists {
                            case LDCDynamic(_, _, _) => true
                            case _                   => false
                        }
                    }
                assert(!hasDynamicLoadsRemaining)
            }

            it("should be able to perform abstract interpretation of rewritten dynamic constants "+
                "in the dynamic constants test project") {
                project.allMethods.foreach(testMethod(_, m => BaseDomain(project, m)))
            }
        }
    }
}
