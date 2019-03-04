/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package par

class PKEFJPoolPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKEFJPoolPropertyStore()
        ps.suppressError = true
        ps
    }

}

class PKEFJPoolPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKEFJPoolPropertyStore()
        ps.suppressError = true
        ps
    }

}
