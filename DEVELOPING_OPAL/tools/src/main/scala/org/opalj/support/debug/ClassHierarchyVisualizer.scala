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
package org.opalj.support.debug

import org.opalj.graphs.toDot
import org.opalj.log.GlobalLogContext
import org.opalj.io.writeAndOpen
import org.opalj.br.ClassFile
import org.opalj.br.ClassHierarchy
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Creates a `dot` (Graphviz) based representation of the class hierarchy
 * of the specified jar file(s).
 *
 * @author Michael Eichberg
 */
object ClassHierarchyVisualizer {

    def main(args: Array[String]): Unit = {
        if (!args.forall(_.endsWith(".jar"))) {
            println("Usage: java …ClassHierarchy <JAR file>+")
            println("(c) 2014 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
            sys.exit(-1)
        }

        val classHierarchy =
            if (args.length == 0) {
                ClassHierarchy.PreInitializedClassHierarchy
            } else {
                val classFiles =
                    (List.empty[(ClassFile, java.net.URL)] /: args) { (cfs, filename) ⇒
                        cfs ++ ClassFiles(new java.io.File(filename))
                    }
                ClassHierarchy(classFiles.view.map(_._1))(GlobalLogContext)
            }

        val dotGraph = toDot(Set(classHierarchy.toGraph()), "back")
        writeAndOpen(dotGraph, "ClassHierarchy", ".gv")
    }
}
