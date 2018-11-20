/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.opalj.br.TestSupport.biProject
import org.opalj.br.analyses.SomeProject
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyStoreKeyTest extends FunSpec with Matchers {

    describe("using the default PropertyStoreKey") {
        val p: SomeProject = biProject("ai.jar")

        val ps = p.get(PropertyStoreKey)

        it("the context should always contain the project") {
            assert(p == ps.context(classOf[SomeProject]))
        }
    }
}
