/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

abstract class AbstractPKECPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging[PKECPropertyStore] {

    override def afterAll(ps: PKECPropertyStore): Unit = {
        // TODO Basic smoke test?
    }
}

class PKECPropertyStoreTestWithDebugging
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore()
        ps.suppressError = true
        ps
    }

}

// *************************************************************************************************
// ************************************* NO DEBUGGING **********************************************
// *************************************************************************************************

abstract class AbstractPKECPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging[PKECPropertyStore]

class PKECPropertyStoreTestWithoutDebugging
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore()
        ps.suppressError = true
        ps
    }

}