/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package fpcf
package par

abstract class DHTPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging[DHTPropertyStore] {

    override def afterAll(ps: DHTPropertyStore): Unit = {
        // TODO Basic smoke test?
    }
}

class DHTPropertyStoreTestWithDebuggingMaxEvalDepthDefault
    extends DHTPropertyStoreTestWithDebugging {

    def createPropertyStore(): DHTPropertyStore = {
        val ps = new DHTPropertyStore(Map.empty)
        ps.suppressError = true
        ps
    }

}

// *************************************************************************************************
// ************************************* NO DEBUGGING **********************************************
// *************************************************************************************************

abstract class DHTPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging[DHTPropertyStore]

class DHTPropertyStoreTestWithoutDebuggingMaxEvalDepth128AndSeqTaskManager
    extends DHTPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): DHTPropertyStore = {
        val ps = new DHTPropertyStore(Map.empty)
        ps.suppressError = true
        ps
    }

}