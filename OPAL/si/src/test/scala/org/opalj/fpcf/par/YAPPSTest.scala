/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

abstract class YAPPSTestWithDebugging
    extends PropertyStoreTestWithDebugging[YAPPS] {

    override def afterAll(ps: YAPPS): Unit = {
        // TODO Basic smoke test?
    }
}

class YAPPSTestWithDebuggingMaxEvalDepthDefault
    extends YAPPSTestWithDebugging {

    def createPropertyStore(): YAPPS = {
        val ps = new YAPPS(Map.empty)
        ps.suppressError = true
        ps
    }

}

// *************************************************************************************************
// ************************************* NO DEBUGGING **********************************************
// *************************************************************************************************

abstract class YAPPSTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging[YAPPS]

class YAPPSTestWithoutDebuggingMaxEvalDepth128AndSeqTaskManager
    extends YAPPSTestWithoutDebugging {

    def createPropertyStore(): YAPPS = {
        val ps = new YAPPS(Map.empty)
        ps.suppressError = true
        ps
    }

}