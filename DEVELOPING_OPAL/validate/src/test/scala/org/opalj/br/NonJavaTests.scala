/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.Project
import org.scalatest.funspec.AnyFunSpec

/**
 * A test for testing behavior that is valid bytecode but can not be generated from valid Java code.
 *
 * @author Dominik Helm
 * @author Michael Eichberg
 */
class NonJavaTests extends AnyFunSpec {

    describe("Project.instanceMethods") {

        val sourceFolder = locateTestResources("StaticAndDefaultInterfaceMethods", "bc")
        val project = Project(sourceFolder)
        val superIntfType = ObjectType("mr/SuperIntf")
        val intfType = ObjectType("mr/Intf")
        val subIntfType = ObjectType("mr/SubIntf")

        it("should not contain the default method \"m\" from SuperIntf that is inaccesible in Intf") {
            assert(project.instanceMethods(superIntfType).size == 1)
            assert(project.instanceMethods(intfType).isEmpty)
            assert(project.instanceMethods(subIntfType).size == 1)
        }
    }

}
