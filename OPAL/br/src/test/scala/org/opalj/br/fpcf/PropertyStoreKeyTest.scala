/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.br.TestSupport.biProject
import org.opalj.br.analyses.SomeProject

/**
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyStoreKeyTest extends AnyFunSpec with Matchers {

    describe("using the default PropertyStoreKey") {
        val p: SomeProject = biProject("ai.jar")

        val ps = p.get(PropertyStoreKey)

        it("the context should always contain the project") {
            assert(p == ps.context(classOf[SomeProject]))
        }
    }
}
