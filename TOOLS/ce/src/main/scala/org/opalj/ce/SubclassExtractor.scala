/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable

import org.opalj.bi.ACC_ABSTRACT
import org.opalj.br.ClassHierarchy
import org.opalj.br.ClassType
import org.opalj.br.analyses.SomeProject

/**
 * The class subclassExtractor is a wrapper class around the bytecode representation project.
 * It will fetch all subclasses from the project and prepares the bytecode hierarchies for querying.
 */
class SubclassExtractor(project: SomeProject) {
    val classHierarchy: ClassHierarchy = project.classHierarchy

    /**
     * This method queries the extracted class hierarchies for a class.
     * @param root accepts a string class name in dot notation. E.g. "org.opalj.ce.ConfigNode".
     * @return returns a set of the names of all subclasses of the root subclass.
     */
    def extractSubclasses(root: String): Seq[String] = {
        val results = mutable.Set[String]()
        val rootClassType = ClassType(root.replace(".", "/"))
        val compatibleTypes = classHierarchy.allSubclassTypes(
            rootClassType,
            reflexive = classHierarchy.isInterface(ClassType(root.replace(".", "/"))).isNoOrUnknown
        )
        for {
            entry <- compatibleTypes
            if project.classFile(entry).forall(cf => !ACC_ABSTRACT.isSet(cf.accessFlags))
        } {
            results += entry.fqn
        }
        results.toSeq
    }
}
