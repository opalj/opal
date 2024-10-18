/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.net.URL
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.unapply
import org.opalj.br.analyses.Project

/**
 * The class subclassExtractor is a wrapper class around the bytecode representation project.
 * It will fetch all subclasses from the opal project and prepares the bytecode hierarchies for querying
 * @param f accepts any initialized FileLocator
 */
class SubclassExtractor(val f: FileLocator) {
    var classHierarchies: ListBuffer[ClassHierarchy] = new ListBuffer[ClassHierarchy]
    this.initialize()

    /**
     * This method queries the extracted class hierarchies for a class.
     * @param root accepts a string class name in dot notation. E.g. "org.opalj.ce.ConfigNode"
     * @return returns a set of the names of all subclasses of the root subclass
     */
    def extractSubclasses(root: String): mutable.Set[String] = {
        val results = mutable.Set[String]()
        for (classHierarchy <- this.classHierarchies) {
            val unformattedresult = classHierarchy.subtypeInformation(ObjectType(root.replace(".", "/"))).orNull
            if (unformattedresult != null) {
                for (entry <- unformattedresult.classTypes) {
                    results += unapply(entry).getOrElse("").replace("/", ".")
                }
            }
        }
        results
    }

    /**
     * Method for fetching the class Hierarchies.
     * It is run upon initialization and on demand for reloading the class hierarchies.
     */
    def initialize(): Unit = {
        val files = f.FindJarArchives()
        for (file <- files) {
            val p: Project[URL] = Project(file.toFile, org.opalj.bytecode.RTJar)
            this.classHierarchies += p.classHierarchy
        }
        this.classHierarchies = classHierarchies
    }
}
