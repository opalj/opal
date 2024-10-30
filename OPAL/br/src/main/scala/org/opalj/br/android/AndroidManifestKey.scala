/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
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
 * @author Julius Naeumann
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
        def parseComponentFromXml[T <: AndroidComponent](
            node:            Node,
            createComponent: (ClassFile, Seq[IntentFilter]) => T
        ): Option[T] = {
            val name = (node \ s"@{$androidURI}name").text
            var ot = name.replaceAll("\\.", "/")
            if (ot.startsWith("/")) {
                ot = packageName + ot
            }
            val intentFilters = (node \ "intent-filter").map(parseIntentFilter)
            project.classFile(ObjectType(ot)).map(cls => createComponent(cls, intentFilters))
        }

        // Parse the different component types using specific lambdas
        val activities = (xml \ "application" \ "activity").flatMap(parseComponentFromXml(_, Activity))
        val services = (xml \ "application" \ "service").flatMap(parseComponentFromXml(_, Service))
        val receivers = (xml \ "application" \ "receiver").flatMap(parseComponentFromXml(_, BroadcastReceiver))
        val providers = (xml \ "application" \ "provider").flatMap(parseComponentFromXml(_, ContentProvider))

        // Collect all components
        AndroidManifest(packageName, activities, services, receivers, providers)
    }
}
