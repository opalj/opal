/* License (BSD Style License):
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.tud.cs.st
package bat
package findrealbugs
package analyses

import resolved._
import resolved.analyses._
import resolved.instructions._

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
class SyncSetUnsyncGet[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    def description: String =
        "Reports getters that are unsynchronized while the setter is synchronized."

    /**
     * Runs this analysis on the given project. Reports unsynced getter methods.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[MethodBasedReport[Source]] = {

        // Look through non-static methods of all classes, collecting lists of
        // unsynchronized getters and synchronized setters.
        var unSyncedGetters = Map[String, Method]()
        var syncedSetters = Map[String, (ClassFile, Method)]()
        for {
            classFile ← project.classFiles if !classFile.isInterfaceDeclaration
            if !project.isLibraryType(classFile)
            method ← classFile.methods
            if !method.isAbstract && !method.isStatic && !method.isNative &&
                !method.isPrivate
        } {
            if (method.name.startsWith("get") &&
                !method.isSynchronized &&
                method.parameterTypes.length == 0 &&
                method.returnType != VoidType) {
                unSyncedGetters += ((classFile.thisType.fqn+"."+method.name.substring(3),
                    method))
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
        for (field ← syncedSetters.keySet.intersect(unSyncedGetters.keySet)) yield {
            val classFile = syncedSetters(field)._1
            val syncSet = syncedSetters(field)._2
            val unsyncGet = unSyncedGetters(field)
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                unsyncGet,
                "Is not synchronized like "+syncSet.name)
        }
    }
}
