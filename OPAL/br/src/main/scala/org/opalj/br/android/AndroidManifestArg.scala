/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package android

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.fileConverter

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.fpcf.cli.ProjectBasedArg
import org.opalj.cli.PlainArg

object AndroidManifestArg extends PlainArg[File] with ProjectBasedArg[File, File] {
    override val name: String = "manifest"
    override val argName: String = "manifestPath"
    override val description: String = "Path to Android manifest file"

    override def apply(opalConfig: Config, value: Option[File]): Config = {
        if (value.isDefined) {
            opalConfig.withValue(
                InitialEntryPointsKey.ConfigKey,
                ConfigValueFactory.fromAnyRef("org.opalj.br.android.AndroidEntryPointFinder")
            )
        } else opalConfig
    }

    override def apply(project: SomeProject, value: Option[File]): Unit = {
        if (value.isDefined) {
            project.updateProjectInformationKeyInitializationData(AndroidManifestKey) {
                _ => value.get.getCanonicalPath
            }
        }
    }
}
