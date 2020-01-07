/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package par

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory.fromAnyRef

abstract class PKECPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging[PKECPropertyStore] {

    override def afterAll(ps: PKECPropertyStore): Unit = {
        assert(ps.tracer.get.toTxt.nonEmpty) // basically just a smoke test
    }
}

class PKECPropertyStoreTestWithDebuggingMaxEvalDepthDefault
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents())
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingMaxEvalDepth32AndSeqTaskManager
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        import PKECPropertyStore.MaxEvaluationDepthKey
        import PKECPropertyStore.TasksManagerKey
        val config = org.opalj.BaseConfig
            .withValue(MaxEvaluationDepthKey, fromAnyRef(32))
            .withValue(TasksManagerKey, fromAnyRef("Seq"))
        val ps = PKECPropertyStore(
            PropertyStoreContext(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents()),
            PropertyStoreContext(classOf[Config], config)
        )
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingMaxEvalDepth0AndSeqTaskManager
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        import PKECPropertyStore.MaxEvaluationDepthKey
        import PKECPropertyStore.TasksManagerKey
        val config = org.opalj.BaseConfig
            .withValue(MaxEvaluationDepthKey, fromAnyRef(0))
            .withValue(TasksManagerKey, fromAnyRef("Seq"))
        val ps = PKECPropertyStore(
            PropertyStoreContext(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents()),
            PropertyStoreContext(classOf[Config], config)
        )
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingMaxEvalDepth32AndParTaskManager
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        import PKECPropertyStore.MaxEvaluationDepthKey
        import PKECPropertyStore.TasksManagerKey
        val config = org.opalj.BaseConfig
            .withValue(MaxEvaluationDepthKey, fromAnyRef(32))
            .withValue(TasksManagerKey, fromAnyRef("Par"))
        val ps = PKECPropertyStore(
            PropertyStoreContext(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents()),
            PropertyStoreContext(classOf[Config], config)
        )
        ps.suppressError = true
        ps
    }

}

// FIXME: PKECPropertyStore seems to be broken
/*class PKECPropertyStoreTestWithDebuggingMaxEvalDepth1AndParTaskManager
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        import PKECPropertyStore.MaxEvaluationDepthKey
        import PKECPropertyStore.TasksManagerKey
        val config = org.opalj.BaseConfig
            .withValue(MaxEvaluationDepthKey, fromAnyRef(1))
            .withValue(TasksManagerKey, fromAnyRef("Par"))
        val ps = PKECPropertyStore(
            PropertyStoreContext(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents()),
            PropertyStoreContext(classOf[Config], config)
        )
        ps.suppressError = true
        ps
    }

}*/

// *************************************************************************************************
// ************************************* NO DEBUGGING **********************************************
// *************************************************************************************************

abstract class PKECPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging[PKECPropertyStore]

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth128AndSeqTaskManager
    extends PKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore("Seq", 128)()
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth0AndSeqTaskManager
    extends PKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore("Seq", 0)()
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth1AndParTaskManager
    extends PKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore("Par", 1)()
        ps.suppressError = true
        ps
    }

}
