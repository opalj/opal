/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.scalatest.FunSuite

import org.opalj.fpcf.fixtures.NilProperty

class EOptionPTest extends FunSuite {

    test("we can always get the property key") {
        assert(FinalEP(new Object, NilProperty).pk == NilProperty.key)
    }

}
