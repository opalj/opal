/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli
package cg

import java.util
import scala.jdk.CollectionConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.stringListConverter

import org.opalj.cli.PlainArg
import org.opalj.fpcf.FPCFAnalysesRegistry

object EnabledCGModulesArg extends CGModulesArg {
    override val name: String = "enabledCGModules"
    override val argName: String = "modules"
    override val description: String = "Modules to be enabled for call-graph generation, e.g., FinalizerAnalysis"

    override def operation(currentModules: util.List[String], argumentModules: util.List[String]): Unit =
        currentModules.addAll(argumentModules)
}

object DisabledCGModulesArg extends CGModulesArg {
    override val name: String = "disabledCGModules"
    override val argName: String = "modules"
    override val description: String = "Modules to be disabled for call-graph generation, e.g., FinalizerAnalysis"

    override def operation(currentModules: util.List[String], argumentModules: util.List[String]): Unit =
        currentModules.removeAll(argumentModules)
}

trait CGModulesArg extends PlainArg[List[String]] {
    override def apply(config: Config, value: Option[List[String]]): Config = {
        if (value.isDefined) {
            val modules = config.getStringList("org.opalj.tac.cg.CallGraphKey.modules")
            val moduleFQNs = value.get.map { moduleName =>
                if (moduleName.contains('.')) moduleName
                else FPCFAnalysesRegistry.factory(moduleName).getClass.getName.replace("$", "")
            }
            operation(modules, moduleFQNs.asJava)
            config.withValue("org.opalj.tac.cg.CallGraphKey.modules", ConfigValueFactory.fromIterable(modules))
        } else config
    }

    def operation(currentModules: java.util.List[String], argumentModules: java.util.List[String]): Unit
}
