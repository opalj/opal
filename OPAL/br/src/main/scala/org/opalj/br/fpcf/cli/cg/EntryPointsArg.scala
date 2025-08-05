/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli
package cg

import scala.jdk.CollectionConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.stringConverter
import org.rogach.scallop.stringListConverter

import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.cli.PlainArg

object EntryPointsArg extends PlainArg[List[String]] with EntryPointsArgLike {

    override val name: String = "entryPoints"
    override val description: String =
        "Entry-point methods for the call graph, with or without descriptor, e.g., org.opalj.Main.main or org.opalj.Main.main([java.lang.String;)V"

    override def apply(config: Config, value: Option[List[String]]): Config = {
        if (value.isDefined) {
            apply(config, value.get)
        } else config
    }
}

object MainClassArg extends PlainArg[String] with EntryPointsArgLike {

    override val name: String = "mainClass"
    override val description: String = "Class with main method as entry point for the call graph, e.g., org.opalj.Main"

    override def apply(config: Config, value: Option[String]): Config = {
        if (value.isDefined) {
            apply(config, List(value.get.replace('.', '/') + ".main([java.lang.String;)V"))
        } else config
    }
}

trait EntryPointsArgLike {

    def apply(config: Config, entryPoints: List[String]): Config = {
        val key = s"${InitialEntryPointsKey.ConfigKeyPrefix}entryPoints"
        val currentValues = config.getList(key).unwrapped()
        entryPoints.foreach { entryPoint =>
            val descriptorIndex = entryPoint.indexOf('(')
            val (classAndMethod, descriptor) =
                if (descriptorIndex >= 0) entryPoint.splitAt(descriptorIndex) else (entryPoint, "")
            val methodNameIndex = classAndMethod.lastIndexOf('.')
            val configValue = Map(
                "declaringClass" -> classAndMethod.take(methodNameIndex).replace('.', '/'),
                "name" -> classAndMethod.drop(methodNameIndex + 1)
            ).asJava
            if (descriptor.nonEmpty)
                configValue.put("descriptor", descriptor)
            currentValues.add(ConfigValueFactory.fromMap(configValue))
        }
        var newConfig = config.withValue(key, ConfigValueFactory.fromIterable(currentValues))

        newConfig = newConfig.withValue(
            s"${InitialEntryPointsKey.ConfigKeyPrefix}analysis",
            ConfigValueFactory.fromAnyRef(
                "org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder"
            )
        )

        newConfig
    }
}
