/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.br.TestSupport.biProject
import org.opalj.br.TestSupport.biProjectWithJDK

import scala.io.Source
import scala.io.Codec.UTF8

import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * Reads the test configuration file, then creates the tac code according to the specification
 * and compares it with the expected result.
 *
 * @author Michael Eichberg
 */
class TACAITest extends AnyFunSpec with Matchers {

    describe("the data-flow based three-address code") {

        val configInputStream = this.getClass.getResourceAsStream("TACAITest.config")
        val configLines = Source.fromInputStream(configInputStream)(UTF8).getLines()
        val rawConfigs = configLines.filterNot(l => l.trim.isEmpty || l.trim.startsWith("#"))
        val configs = rawConfigs.map(c => /* get values: */ c.split(':')(1).trim).sliding(6, 6)
        for {
            ((projectName, jdk), tests) <- configs.toList.groupBy(c => (c(0), c(1)))
        } {
            describe(s"analysis of $projectName with jdk: $jdk") {
                val p = jdk match {
                    case "no"       => biProject(projectName)
                    case "complete" => biProjectWithJDK(projectName, jdkAPIOnly = false)
                    case "api"      => biProjectWithJDK(projectName, jdkAPIOnly = true)
                }
                for { Seq(_, _, className, methodName, test, domainName) <- tests } {
                    it(test + s" ($className.$methodName using $domainName)") {

                        val cfOption = p.classFile(ObjectType(className.replace('.', '/')))
                        if (cfOption.isEmpty)
                            fail(s"cannot find class: $className")
                        val cf = cfOption.get

                        val mChain = cf.findMethod(methodName)
                        if (mChain.size != 1)
                            fail(s"invalid method name: $className{ $methodName; found: $mChain }")
                        val m = mChain.head

                        if (m.body.isEmpty)
                            fail(s"method body is empty: ${m.toJava}")

                        val d: Domain with RecordDefUse =
                            Class.
                                forName(domainName).asInstanceOf[Class[Domain with RecordDefUse]].
                                getConstructor(classOf[Project[_]], classOf[Method]).
                                newInstance(p, m)
                        val aiResult = BaseAI(m, d)
                        val TACode(params, code, _, cfg, _) = TACAI(m, p.classHierarchy, aiResult, false)(Nil)
                        val actual = ToTxt(params, code, cfg, skipParams = false, indented = false, true)

                        val simpleDomainName = domainName.stripPrefix("org.opalj.ai.domain.")
                        val expectedFileName =
                            projectName.substring(0, projectName.indexOf('.')) +
                                s"-$jdk-$className-$methodName-$simpleDomainName.tac.txt"
                        val expectedInputStream = this.getClass.getResourceAsStream(expectedFileName)
                        if (expectedInputStream eq null)
                            fail(
                                s"missing expected 3-adddress code representation: $expectedFileName;"+
                                    s"current representation:\n${actual.mkString("\n")}"
                            )
                        val expected = Source.fromInputStream(expectedInputStream)(UTF8).getLines().toList

                        // check that both files are identical:
                        val actualIt = actual.iterator
                        val expectedIt = expected.iterator
                        while (actualIt.hasNext && expectedIt.hasNext) {
                            val actualLine = actualIt.next()
                            val expectedLine = expectedIt.next()
                            if (actualLine != expectedLine)
                                fail(
                                    s"comparison failed:\n$actualLine\n\t\tvs. (expected)\n"+
                                        s"$expectedLine\ncomputed representation:\n"+
                                        actual.mkString("\n")
                                )
                        }
                        if (actualIt.hasNext)
                            fail(
                                "actual is longer than expected - first line: "+actualIt.next()+
                                    "\n computed representation:\n"+actual.mkString("\n")
                            )
                        if (expectedIt.hasNext)
                            fail(
                                "expected is longer than actual - first line: "+expectedIt.next()+
                                    "\n computed representation:\n"+actual.mkString("\n")
                            )
                    }
                }
            }
        }
    }
}
