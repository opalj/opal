/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.br

import org.opalj.br.analyses.SomeProject
import org.opalj.graphs.Node

/**
 * A classloader that resolves classfiles which are available in byte array form.
 *
 * @param project The project where the classfiles are loaded from.
 *
 * @author Andreas Muttscheller
 */
class ByteArrayClassLoader(project: SomeProject) extends ClassLoader {

    loadGraph(project.classHierarchy.toGraph())

    def loadGraph(node: Node): Unit = {
        if (node.nodeId >= 0) {
            val ot = project.classHierarchy.getObjectType(node.nodeId)
            project.allProjectClassFiles.find(_.thisType == ot).foreach(c ⇒ resolveProjectClass(c))
        }

        if (node.hasSuccessors) {
            node.foreachSuccessor(loadGraph)
        }
    }

    def resolveProjectClass(classFile: ClassFile): Unit = {
        try {
            resolveClass(findClass(classFile))
        } catch {
            // Can be ignored. The class will most probably loaded at a later time, when all
            // superclasses are loaded.
            case _: Any ⇒
        }
    }

    def findClass(classFile: ClassFile): Class[_] = {
        val ba = ProjectSerializer.classToByte(
            classFile
        )

        defineClass(classFile.thisType.toJava, ba, 0, ba.length)
    }
}
