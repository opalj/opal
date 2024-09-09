/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package string_definition

import org.scalatest.funsuite.AnyFunSuite

import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.Constant
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.Dynamic
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.PartiallyConstant

/**
 * Tests for [[StringConstancyLevel]] methods.
 *
 * @author Maximilian Rüsch
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class StringConstancyLevelTests extends AnyFunSuite {

    test("tests that the more general string constancy level is computed correctly") {
        // Trivial cases
        assert(StringConstancyLevel.determineMoreGeneral(Dynamic, Dynamic) == Dynamic)
        assert(StringConstancyLevel.determineMoreGeneral(PartiallyConstant, PartiallyConstant) == PartiallyConstant)
        assert(StringConstancyLevel.determineMoreGeneral(Constant, Constant) == Constant)

        // Test all other cases, start with { Dynamic, Constant }
        assert(StringConstancyLevel.determineMoreGeneral(Constant, Dynamic) == Dynamic)
        assert(StringConstancyLevel.determineMoreGeneral(Dynamic, Constant) == Dynamic)

        // { Dynamic, PartiallyConstant }
        assert(StringConstancyLevel.determineMoreGeneral(PartiallyConstant, Dynamic) == Dynamic)
        assert(StringConstancyLevel.determineMoreGeneral(Dynamic, PartiallyConstant) == Dynamic)

        // { PartiallyConstant, Constant }
        assert(StringConstancyLevel.determineMoreGeneral(PartiallyConstant, Constant) == PartiallyConstant)
        assert(StringConstancyLevel.determineMoreGeneral(Constant, PartiallyConstant) == PartiallyConstant)
    }
}
