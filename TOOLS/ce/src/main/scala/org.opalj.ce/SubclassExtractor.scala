package org.opalj
package ce

import java.net.URL
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br._
import org.opalj.br.ObjectType.unapply
import org.opalj.br.analyses._

class SubclassExtractor(val f: FileLocator) {
    var classHierarchies: ListBuffer[ClassHierarchy] = new ListBuffer[ClassHierarchy]
    this.initialize()

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

    def initialize(): Unit = {
        val files = f.FindJarArchives()
        for (file <- files) {
            val p: Project[URL] = Project(file.toFile, org.opalj.bytecode.RTJar)
            this.classHierarchies += p.classHierarchy
        }
        this.classHierarchies = classHierarchies
    }
}
