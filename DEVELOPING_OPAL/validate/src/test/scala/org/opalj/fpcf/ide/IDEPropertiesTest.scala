/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.ide.ConfigKeyDebugLog
import org.opalj.ide.ConfigKeyTraceLog
import org.opalj.tac.cg.RTACallGraphKey

/**
 * Specialized test for IDE analyses preparing the configuration.
 *
 * @author Robin Körkemeier
 */
abstract class IDEPropertiesTest extends PropertiesTest {
    override def withRT: Boolean = false

    override def createConfig(): Config = {
        super.createConfig()
            .withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix + "AllInstantiatedTypesFinder.projectClassesOnly",
                ConfigValueFactory.fromAnyRef(false)
            )
            .withValue(ConfigKeyDebugLog, ConfigValueFactory.fromAnyRef(true))
            .withValue(ConfigKeyTraceLog, ConfigValueFactory.fromAnyRef(false))
    }

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(_ =>
            Set[Class[? <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        )
        p.get(RTACallGraphKey)
    }
}
