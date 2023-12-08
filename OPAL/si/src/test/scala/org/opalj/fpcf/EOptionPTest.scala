/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.fixtures.NilProperty

import org.scalatest.funsuite.AnyFunSuite

class EOptionPTest extends AnyFunSuite {

    test("we can always get the property key") {
        assert(FinalEP(new Object, NilProperty).pk == NilProperty.key)
    }

}
