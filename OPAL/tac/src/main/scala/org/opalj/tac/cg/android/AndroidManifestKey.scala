/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.cg.android

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.cg.IntentFilter

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.Elem

/**
 * A ProjectInformationKey that is used to parse an AndroidManifest.xml. It returns a map of Android components and
 * a ListBuffer of all IntentFilters defined in the manifest.
 * The AndroidManifest.xml can be set as initialization data.
 *
 * @author Tom Nikisch
 */
object AndroidManifestKey extends ProjectInformationKey[Option[(Map[String, ListBuffer[ClassFile]], ListBuffer[IntentFilter])], Elem] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Nil

    override def compute(project: SomeProject): Option[(Map[String, ListBuffer[ClassFile]], ListBuffer[IntentFilter])] = {
        val manifest = project.getProjectInformationKeyInitializationData(AndroidManifestKey)
        manifest.map(parseManifest(project, _))
    }

    /**
     * Analyses the AndroidManifest.xml of the project to find intent filters and relevant components to generate
     * lifecycle callbacks.
     */
    def parseManifest(project: SomeProject, manifest: Elem): (Map[String, ListBuffer[ClassFile]], ListBuffer[IntentFilter]) = {
        val componentMap: mutable.Map[String, ListBuffer[ClassFile]] = mutable.Map(
            "activity" -> ListBuffer.empty[ClassFile],
            "service" -> ListBuffer.empty[ClassFile]
        )
        val intentFilters = ListBuffer.empty[IntentFilter]
        val androidURI = "http://schemas.android.com/apk/res/android"
        val packageName = manifest.attribute("package").get.toString().replaceAll("\\.", "/")
        List("activity", "receiver", "service").foreach { comp ⇒
            val components = manifest \\ comp
            components.foreach { c ⇒
                var ot = c.attribute(androidURI, "name").head.toString().replaceAll("\\.", "/")
                if (ot.startsWith("/")) { ot = packageName + ot }
                val rec = project.classFile(ObjectType(ot))
                if (rec.isDefined) {
                    if (comp == "activity" || comp == "service") componentMap(comp) += rec.get
                    val filters = c \ "intent-filter"
                    if (filters.nonEmpty) {
                        filters.foreach { filter ⇒
                            val intentFilter = new IntentFilter(rec.get, comp)
                            intentFilter.actions = (filter \ "action").map(_.attribute(androidURI, "name").
                                get.head.toString()).to[ListBuffer]
                            intentFilter.categories = (filter \ "category").map(_.attribute(androidURI, "name").
                                get.head.toString()).to[ListBuffer]
                            val data = filter \ "data"
                            if (data.nonEmpty) {
                                data.foreach { d ⇒
                                    val t = d.attribute(androidURI, "mimeType")
                                    if (t.isDefined) {
                                        intentFilter.dataTypes += t.get.head.toString()
                                    }
                                    val s = d.attribute(androidURI, "scheme")
                                    if (s.isDefined) {
                                        intentFilter.dataSchemes += s.get.head.toString()
                                    }
                                    val h = d.attribute(androidURI, "host")
                                    if (h.isDefined) {
                                        var authority = h.get.head.toString()
                                        val port = d.attribute(androidURI, "port")
                                        if (port.isDefined) {
                                            authority = authority + port.get.head.toString()
                                        }
                                        intentFilter.dataAuthorities += authority
                                    }
                                    val p = d.attribute(androidURI, "path")
                                    if (p.isDefined) {
                                        intentFilter.dataPaths += p.get.head.toString()
                                    }
                                    val pp = d.attribute(androidURI, "pathPrefix")
                                    if (pp.isDefined) {
                                        intentFilter.dataPaths += pp.get.head.toString()
                                    }
                                    val pathPattern = d.attribute(androidURI, "pathPattern")
                                    if (pathPattern.isDefined) {
                                        intentFilter.dataPaths += pathPattern.get.head.toString()
                                    }
                                }
                            }
                            intentFilters += intentFilter
                        }
                    }
                }
            }
        }
        (componentMap.toMap, intentFilters)
    }
}