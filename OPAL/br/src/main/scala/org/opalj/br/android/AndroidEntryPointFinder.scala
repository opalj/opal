/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package android

import scala.collection.mutable.ArrayBuffer

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.EntryPointFinder

/**
 * The AndroidEntryPointFinder considers specific methods of launcher Activity Classes as entry points.
 * An activity is a launcher activity if it contains an intent filter with action "android.intent.action.MAIN"
 * and category "android.intent.category.LAUNCHER". Requires Android Manifest to be loaded.
 *
 * @author Julius Naeumann
 */
object AndroidEntryPointFinder extends EntryPointFinder {

    import pureconfig.*

    val configKey = "org.opalj.fpcf.android.AndroidEntryPointFinder.entryPoints"

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        super.requirements(project) ++ Seq(AndroidManifestKey)
    }

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {
        val declaredMethods = project.get(DeclaredMethodsKey)

        val entryPointDescriptions = getConfiguredEntryPoints(project)
        val manifest: AndroidManifest = project.get(AndroidManifestKey)

        // get launcher activities
        val launchableClasses = manifest.activities.filter(_.isLauncherActivity).map(_.cls)
        val classHierarchy = project.classHierarchy
        val entryPoints = ArrayBuffer[Method]()

        // iterate over launchable classes, collect their respective entry point methods according to config
        for (componentClass <- launchableClasses) {
            for (epd <- entryPointDescriptions) {
                if (classHierarchy.isASubtypeOf(
                        ReferenceType(componentClass.fqn),
                        ReferenceType(epd.declaringClass)
                    ).isYesOrUnknown
                ) {
                    entryPoints ++= componentClass.findMethod(epd.name, MethodDescriptor(epd.descriptor))
                }
            }
        }

        entryPoints.map(declaredMethods.apply)
    }

    private def getConfiguredEntryPoints(project: SomeProject): List[EntryPointContainer] = {
        ConfigSource.fromConfig(project.config).at(configKey).loadOrThrow[List[EntryPointContainer]]
    }

    /* Required by pureconfig */
    private case class EntryPointContainer(
        declaringClass: String,
        name:           String,
        descriptor:     String
    ) derives ConfigReader
}
