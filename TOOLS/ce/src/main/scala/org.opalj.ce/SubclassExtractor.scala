/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.net.URL
import scala.collection.mutable

import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.unapply
import org.opalj.br.analyses.Project

/**
 * The class subclassExtractor is a wrapper class around the bytecode representation project.
 * It will fetch all subclasses from the opal project and prepares the bytecode hierarchies for querying.
 * @param files accepts an array of files of the jar archives that should be included in the ClassHierarchies
 */
class SubclassExtractor(files: Array[File]) {
    val p: Project[URL] = Project.apply(files, Array(org.opalj.bytecode.RTJar))
    val classHierarchy: ClassHierarchy = p.classHierarchy

    /**
     * This method queries the extracted class hierarchies for a class.
     * @param root accepts a string class name in dot notation. E.g. "org.opalj.ce.ConfigNode".
     * @return returns a set of the names of all subclasses of the root subclass.
     */
    def extractSubclasses(root: String): Seq[String] = {
        val results = mutable.Set[String]()
        val unformattedresult = classHierarchy.subtypeInformation(ObjectType(root.replace(".", "/"))).orNull
        if (unformattedresult != null) {
            for (entry <- unformattedresult.classTypes) {
                results += unapply(entry).getOrElse("").replace("/", ".")
            }
        }
        results.toSeq
    }
}
