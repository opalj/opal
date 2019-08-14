/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package par

class PKECPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKECPropertyStore(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents())
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKECPropertyStore()
        ps.suppressError = true
        ps
    }

}

