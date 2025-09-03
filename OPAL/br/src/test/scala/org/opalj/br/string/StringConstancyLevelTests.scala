/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package string

import org.scalatest.funsuite.AnyFunSuite

import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.Constant
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.Dynamic
import org.opalj.br.fpcf.properties.string.StringConstancyLevel.Invalid
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
        assert(StringConstancyLevel.meet(Invalid, Invalid) == Invalid)
        assert(StringConstancyLevel.meet(Constant, Constant) == Constant)
        assert(StringConstancyLevel.meet(PartiallyConstant, PartiallyConstant) == PartiallyConstant)
        assert(StringConstancyLevel.meet(Dynamic, Dynamic) == Dynamic)

        // <= Constant
        assert(StringConstancyLevel.meet(Constant, Invalid) == Constant)
        assert(StringConstancyLevel.meet(Invalid, Constant) == Constant)

        // <= PartiallyConstant
        assert(StringConstancyLevel.meet(PartiallyConstant, Invalid) == PartiallyConstant)
        assert(StringConstancyLevel.meet(Invalid, PartiallyConstant) == PartiallyConstant)
        assert(StringConstancyLevel.meet(PartiallyConstant, Constant) == PartiallyConstant)
        assert(StringConstancyLevel.meet(Constant, PartiallyConstant) == PartiallyConstant)

        // <= Dynamic
        assert(StringConstancyLevel.meet(Invalid, Dynamic) == Dynamic)
        assert(StringConstancyLevel.meet(Dynamic, Invalid) == Dynamic)
        assert(StringConstancyLevel.meet(Constant, Dynamic) == Dynamic)
        assert(StringConstancyLevel.meet(Dynamic, Constant) == Dynamic)
        assert(StringConstancyLevel.meet(PartiallyConstant, Dynamic) == Dynamic)
        assert(StringConstancyLevel.meet(Dynamic, PartiallyConstant) == Dynamic)
    }

    test("tests that the string constancy level for concatenation of two levels is computed correctly") {
        // Trivial cases
        assert(StringConstancyLevel.determineForConcat(Invalid, Invalid) == Invalid)
        assert(StringConstancyLevel.determineForConcat(Constant, Constant) == Constant)
        assert(StringConstancyLevel.determineForConcat(PartiallyConstant, PartiallyConstant) == PartiallyConstant)
        assert(StringConstancyLevel.determineForConcat(Dynamic, Dynamic) == Dynamic)

        // Invalid blocks everything
        assert(StringConstancyLevel.determineForConcat(Constant, Invalid) == Invalid)
        assert(StringConstancyLevel.determineForConcat(Invalid, Constant) == Invalid)
        assert(StringConstancyLevel.determineForConcat(PartiallyConstant, Invalid) == Invalid)
        assert(StringConstancyLevel.determineForConcat(Invalid, PartiallyConstant) == Invalid)
        assert(StringConstancyLevel.determineForConcat(Invalid, Dynamic) == Invalid)
        assert(StringConstancyLevel.determineForConcat(Dynamic, Invalid) == Invalid)

        // PartiallyConstant can be retained
        assert(StringConstancyLevel.determineForConcat(PartiallyConstant, Constant) == PartiallyConstant)
        assert(StringConstancyLevel.determineForConcat(Constant, PartiallyConstant) == PartiallyConstant)
        assert(StringConstancyLevel.determineForConcat(PartiallyConstant, Dynamic) == PartiallyConstant)
        assert(StringConstancyLevel.determineForConcat(Dynamic, PartiallyConstant) == PartiallyConstant)

        // PartiallyConstant can be constructed
        assert(StringConstancyLevel.determineForConcat(Constant, Dynamic) == PartiallyConstant)
        assert(StringConstancyLevel.determineForConcat(Dynamic, Constant) == PartiallyConstant)
    }
}
