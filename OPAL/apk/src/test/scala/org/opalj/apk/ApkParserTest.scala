/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import org.opalj.ll.LLVMProjectKey
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApkParserTest extends AnyFunSpec with Matchers {
    describe("ApkParser Test") {
        ApkParser.logOutput = true
        val project = ApkParser.createProject(
            "./OPAL/apk/src/test/resources/opal-native-test.apk",
            BaseConfig,
        )

        // check if static entry point parsing worked
        val entries = project.get(ApkComponentsKey)
        println()
        println("------------------------------------------")
        println("static entry points:")
        println("------------------------------------------")
        println(entries)
        assert(entries.length == 5)

        // check if java code parsing worked
        println()
        println("------------------------------------------")
        println("java packages:")
        println("------------------------------------------")
        println(project.projectPackages)
        assert(project.packagesCount == 159)

        // check if native code parsing worked
        val llvmProject = project.get(LLVMProjectKey)
        println()
        println("------------------------------------------")
        println("native functions:")
        println("------------------------------------------")
        println(llvmProject.functions)

        // check if dynamically registered Intents for Broadcast Receivers are found
        println()
        println("------------------------------------------")
        println("context registered broadcast receivers:")
        println("------------------------------------------")
        val contextRegisteredReceivers = project.get(ApkContextRegisteredReceiversKey)
        println(contextRegisteredReceivers)
        assert(contextRegisteredReceivers.length == 4)

        println()
        println("DONE")
    }
}
