/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package par

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory.fromAnyRef

abstract class PKECPropertyStoreTestWithDebugging
    extends PropertyStoreTestWithDebugging[PKECPropertyStore](
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    override def afterAll(ps: PKECPropertyStore): Unit = {
        assert(ps.tracer.get.toTxt.size > 0) // basically just a smoke test
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

class PKECPropertyStoreTestWithDebuggingMaxEvalDepth0
    extends PKECPropertyStoreTestWithDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        import PKECPropertyStore.MaxEvaluationDepthKey
        val config = org.opalj.BaseConfig.withValue(MaxEvaluationDepthKey, fromAnyRef(0))
        val ps = PKECPropertyStore(
            PropertyStoreContext(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents()),
            PropertyStoreContext(classOf[Config], config)
        )
        ps.suppressError = true
        ps
    }

}

// *************************************************************************************************
// ************************************* NO DEBUGGING **********************************************
// *************************************************************************************************

abstract class PKECPropertyStoreTestWithoutDebugging
    extends PropertyStoreTestWithoutDebugging[PKECPropertyStore](
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

}

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth128
    extends PKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore("Seq", 128)()
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth0
    extends PKECPropertyStoreTestWithoutDebugging {

    def createPropertyStore(): PKECPropertyStore = {
        val ps = PKECPropertyStore("Seq", 0)()
        ps.suppressError = true
        ps
    }

}
