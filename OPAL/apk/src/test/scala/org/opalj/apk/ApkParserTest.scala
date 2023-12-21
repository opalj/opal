/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

import java.net.URL

import org.opalj.apk.parser.ApkParser
import org.opalj.br.analyses.Project
import org.opalj.ll.LLVMProjectKey

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite

/**
 * Tests that the [[ApkParser]] works.
 *
 * @author Nicolas Gross
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
@Ignore // TODO Does not run @github since docker is missing
class ApkParserTest extends AnyFunSuite with BeforeAndAfterAll {

    var project: Project[URL] = _

    override def beforeAll(): Unit = {
        project = ApkParser.createProject(
            "./OPAL/apk/src/test/resources/context-registered-receivers-and-native.apk",
            BaseConfig
        )
    }

    test("components / entry point parsing") {
        val components = project.get(ApkComponentsKey)
        assert(components.length == 5)
    }

    test("java packages parsing") {
        assert(project.packagesCount == 159)
    }

    test("native functions parsing") {
        val llvmProject = project.get(LLVMProjectKey)
        assert(llvmProject.functions.size == 886)
    }

    test("context registered broadcast receiver parsing") {
        val contextRegisteredReceivers = project.get(ApkContextRegisteredReceiversKey)
        assert(contextRegisteredReceivers.length == 4)
    }
}
