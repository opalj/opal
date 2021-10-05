/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._

/**
 * This analysis reports setter/getter pairs where the set method is synchronized and the
 * get method is not synchronized.
 *
 * This indicates a bug because in a multi-threaded environment, the JVM may cache a
 * field's value instead of reading it from memory everytime the getter is called. This
 * means the getter may return a cached value which may differ from the value in memory.
 * This must be prevented by using a synchronized getter, which guarantees to return the
 * proper value.
 *
 * @author Michael Eichberg
 * @author Daniel Klauer
 */
object SyncSetUnsyncGet {

    override def description: String =
        "Reports getters that are unsynchronized while the setter is synchronized."

    /**
     * Runs this analysis on the given project. Reports unsynced getter methods.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def apply(
        project:       SomeProject,
        parameters:    Seq[String]  = List.empty,
        isInterrupted: () => Boolean
    ): Iterable[MethodBasedReport[Source]] = {

        // Look through non-static methods of all classes, collecting lists of
        // unsynchronized getters and synchronized setters.
        var unSyncedGetters = Map[String, Method]()
        var syncedSetters = Map[String, (ClassFile, Method)]()
        for {
            classFile <- project.allProjectClassFiles
            if !classFile.isInterfaceDeclaration
            method <- classFile.methods
            if !method.isAbstract
            if !method.isStatic
            if !method.isNative
            if !method.isPrivate
        } {
            if (method.name.startsWith("get") &&
                !method.isSynchronized &&
                method.parameterTypes.length == 0 &&
                method.returnType != VoidType) {
                unSyncedGetters += ((classFile.thisType.fqn+"."+method.name.substring(3),
                    method
                ))
            } else if (method.name.startsWith("set") &&
                method.isSynchronized &&
                method.parameterTypes.length == 1 &&
                method.returnType == VoidType) {
                syncedSetters += ((classFile.thisType.fqn+"."+method.name.substring(3),
                    (classFile, method)))
            }
        }

        // Report only cases where both setter/getter for the same field were found.
        // setters/getters that do not belong together are ignored.
        for (field <- syncedSetters.keySet.intersect(unSyncedGetters.keySet)) yield {
            val classFile = syncedSetters(field)._1
            val syncSet = syncedSetters(field)._2
            val unsyncGet = unSyncedGetters(field)
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                unsyncGet,
                "Is not synchronized like "+syncSet.name
            )
        }
    }
}
