/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package par

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory.fromAnyRef

class PKECPropertyStoreTestWithDebuggingMaxEvalDepthDefault
    extends PropertyStoreTestWithDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKECPropertyStore(classOf[PropertyStoreTracer], new RecordAllPropertyStoreEvents())
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithDebuggingMaxEvalDepth0
    extends PropertyStoreTestWithDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
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

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth128
    extends PropertyStoreTestWithoutDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKECPropertyStore("Seq", 128)()
        ps.suppressError = true
        ps
    }

}

class PKECPropertyStoreTestWithoutDebuggingMaxEvalDepth0
    extends PropertyStoreTestWithoutDebugging(
        List(DefaultPropertyComputation, CheapPropertyComputation)
    ) {

    def createPropertyStore(): PropertyStore = {
        val ps = PKECPropertyStore("Seq", 0)()
        ps.suppressError = true
        ps
    }

}
