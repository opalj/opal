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
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 8, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingSingleThreaded
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 1, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 8, 0)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingSingleThreadedNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 1, 0)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebugging128Threads
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 128, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebugging128ThreadsNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 128, 0)
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
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 8, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingSingleThreaded
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 1, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 8, 0)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingSingleThreadedNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 1, 0)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebugging128Threads
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 128, 32)
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebugging128ThreadsNoLocalEvaluation
    extends AbstractPKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = new PKECPropertyStore(Map.empty, PKECNoPriorityTaskManager, 128, 0)
        ps.suppressError = true
        ps
    }

}