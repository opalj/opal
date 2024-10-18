/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import scala.xml.Elem
import scala.xml.Node

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

/**
 * A ProjectInformationKey that is used to parse an AndroidManifest.xml. It returns a map of Android components and
 * a ListBuffer of all IntentFilters defined in the manifest.
 * The AndroidManifest.xml can be set as initialization data.
 *
 * @author Tom Nikisch
 *         Julius Naeumann
 */
object AndroidManifestKey extends ProjectInformationKey[Option[AndroidManifest], Elem] {
    // Constants for the IntentFilter
    val ACTION_MAIN = "android.intent.action.MAIN"
    val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"

    override def requirements(project: SomeProject): ProjectInformationKeys = Nil

    override def compute(project: SomeProject): Option[AndroidManifest] = {
        val manifest = project.getProjectInformationKeyInitializationData(AndroidManifestKey)
        manifest.map(parseManifest(project, _))
    }

    def parseManifest(project: SomeProject, xml: Elem): AndroidManifest = {
        val androidURI = "http://schemas.android.com/apk/res/android"
        val packageName = xml.attribute("package").get.toString().replaceAll("\\.", "/")

        // Function to parse IntentFilter elements
        def parseIntentFilter(node: Node): IntentFilter = {
            val actions = (node \ "action").map { actionNode => (actionNode \ s"@{$androidURI}name").text }
            val categories = (node \ "category").map { categoryNode => (categoryNode \ s"@{$androidURI}name").text }
            IntentFilter(actions, categories)
        }

        // Function to parse AndroidComponent elements (Activity, Service, etc.)
        def parseComponent(node: Node, componentType: String): Option[AndroidComponent] = {
            val name = (node \ s"@{$androidURI}name").text
            var ot = name.replaceAll("\\.", "/")
            if (ot.startsWith("/")) {
                ot = packageName + ot
            }
            val intentFilters = (node \ "intent-filter").map(parseIntentFilter)
            project.classFile(ObjectType(ot)).map(cls =>
                componentType match {
                    case "activity" => Activity(cls, intentFilters)
                    case "service"  => Service(cls, intentFilters)
                    case "receiver" => BroadcastReceiver(cls, intentFilters)
                    case "provider" => ContentProvider(cls, intentFilters)
                }
            )
        }

        // Parse the different component types
        val activities = (xml \ "application" \ "activity").flatMap(parseComponent(_, "activity"))
        val services = (xml \ "application" \ "service").flatMap(parseComponent(_, "service"))
        val receivers = (xml \ "application" \ "receiver").flatMap(parseComponent(_, "receiver"))
        val providers = (xml \ "application" \ "provider").flatMap(parseComponent(_, "provider"))

        // Collect all components
        AndroidManifest(packageName, activities ++ services ++ receivers ++ providers)
    }
}

sealed abstract class AndroidComponent(val cls: ClassFile, val intentFilters: Seq[IntentFilter])

case class AndroidManifest(
    packageName: String,
    components:  Seq[AndroidComponent]
)

case class IntentFilter(actions: Seq[String], categories: Seq[String])

case class Activity(override val cls: ClassFile, override val intentFilters: Seq[IntentFilter])
    extends AndroidComponent(cls, intentFilters) {
    def isLauncherActivity = intentFilters.exists(filter =>
        filter.actions.contains(AndroidManifestKey.ACTION_MAIN)
            && filter.categories.contains(AndroidManifestKey.CATEGORY_LAUNCHER)
    )
}

case class Service(override val cls: ClassFile, override val intentFilters: Seq[IntentFilter])
    extends AndroidComponent(cls, intentFilters)

case class BroadcastReceiver(override val cls: ClassFile, override val intentFilters: Seq[IntentFilter])
    extends AndroidComponent(cls, intentFilters)

case class ContentProvider(override val cls: ClassFile, override val intentFilters: Seq[IntentFilter])
    extends AndroidComponent(cls, intentFilters)
