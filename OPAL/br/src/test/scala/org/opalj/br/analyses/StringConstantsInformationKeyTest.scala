/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Tests the support for "project" related functionality.
 *
 * @author Michael Eichberg
 */
class StringConstantsInformationKeyTest extends AnyFlatSpec with Matchers {

    val stringsArchive = locateTestResources("strings.jar", "bi")
    val stringsProject = Project(ClassFiles(stringsArchive))

    val opal = locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
    val opalProject = Project(ClassFiles(opal), Iterable.empty, true)

    //
    //
    // Verify
    //
    //

    behavior of "StringConstantsInformationKey"

    it should "collect all Strings in the strings project" in {
        // expected is a lower bound.. more are in the project!
        val expected = Set("List(", "1,2,3", ")", "yes", "no", "0123456789")
        val found = stringsProject.get(StringConstantsInformationKey).keys.toSet
        assert(expected.forall { found.contains })
    }

    it should "collect the Strings in the OPAL-SNAPSHOT-0.3.jar project" in {
        // basically a smoke test
        assert(stringsProject.get(StringConstantsInformationKey).keys.nonEmpty)
    }

}
