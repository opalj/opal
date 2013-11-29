/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
package de.tud.cs.st.bat.resolved
package analyses
package bug_patterns.ioc

import instructions._

/**
 * The set method is synchronized and the get method is not synchronized.
 *
 * @author Michael Eichberg
 */
object UG_SYNC_SET_UNSYNC_GET extends (Project[_] ⇒ Iterable[(ClassFile, Method, Method)]) {

    def apply(project: Project[_]) = {
        var unSyncedGetters = Map[String, Method]()
        var syncedSetters = Map[String, (ClassFile, Method)]()
        for {
            classFile ← project.classFiles if !classFile.isInterfaceDeclaration
            method ← classFile.methods
            if !method.isAbstract && !method.isStatic && !method.isNative && !method.isPrivate
        } {
            if (method.name.startsWith("get") &&
                !method.isSynchronized &&
                method.parameterTypes.length == 0 &&
                method.returnType != VoidType) {
                unSyncedGetters += ((classFile.thisClass.className+"."+method.name.substring(3), method))
            } else if (method.name.startsWith("set") &&
                method.isSynchronized &&
                method.parameterTypes.length == 1 &&
                method.returnType == VoidType) {
                syncedSetters += ((classFile.thisClass.className+"."+method.name.substring(3), (classFile, method)))
            }
        }
        for (property ← syncedSetters.keySet.intersect(unSyncedGetters.keySet))
            yield (syncedSetters(property)._1, syncedSetters(property)._2, unSyncedGetters(property))

    }
}
