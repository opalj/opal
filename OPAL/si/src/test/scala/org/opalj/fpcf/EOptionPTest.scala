/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.scalatest.funsuite.AnyFunSuite

import org.opalj.fpcf.fixtures.NilProperty

class EOptionPTest extends AnyFunSuite {

    test("we can always get the property key") {
        assert(FinalEP(new Object, NilProperty).pk == NilProperty.key)
    }

}
