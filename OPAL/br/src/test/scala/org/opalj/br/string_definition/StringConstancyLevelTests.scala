/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.string_definition

import org.scalatest.FunSuite

import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.CONSTANT
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.PARTIALLY_CONSTANT

/**
 * Tests for [[StringConstancyLevel]] methods.
 *
 * @author Patrick Mell
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StringConstancyLevelTests extends FunSuite {

    test("tests that the more general string constancy level is computed correctly") {
        // Trivial cases
        assert(StringConstancyLevel.determineMoreGeneral(DYNAMIC, DYNAMIC) == DYNAMIC)
        assert(StringConstancyLevel.determineMoreGeneral(
            PARTIALLY_CONSTANT, PARTIALLY_CONSTANT
        ) == PARTIALLY_CONSTANT)
        assert(StringConstancyLevel.determineMoreGeneral(CONSTANT, CONSTANT) == CONSTANT)

        // Test all other cases, start with { DYNAMIC, CONSTANT }
        assert(StringConstancyLevel.determineMoreGeneral(CONSTANT, DYNAMIC) == DYNAMIC)
        assert(StringConstancyLevel.determineMoreGeneral(DYNAMIC, CONSTANT) == DYNAMIC)

        // { DYNAMIC, PARTIALLY_CONSTANT }
        assert(StringConstancyLevel.determineMoreGeneral(PARTIALLY_CONSTANT, DYNAMIC) == DYNAMIC)
        assert(StringConstancyLevel.determineMoreGeneral(DYNAMIC, PARTIALLY_CONSTANT) == DYNAMIC)

        // { PARTIALLY_CONSTANT, CONSTANT }
        assert(StringConstancyLevel.determineMoreGeneral(
            PARTIALLY_CONSTANT, CONSTANT
        ) == PARTIALLY_CONSTANT)
        assert(StringConstancyLevel.determineMoreGeneral(
            CONSTANT, PARTIALLY_CONSTANT
        ) == PARTIALLY_CONSTANT)
    }

}
