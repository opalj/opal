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

import java.io.File

import org.opalj.graphs.toDot
import org.opalj.log.GlobalLogContext
import org.opalj.io.writeAndOpen
import org.opalj.br.ClassFile
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.reader.Java7LibraryFramework.ClassFiles

/**
 * Creates a `dot` (Graphviz) based representation of the class hierarchy
 * of the specified jar file(s).
 *
 * @author Michael Eichberg
 */
object ClassHierarchyVisualizer {

    def main(args: Array[String]): Unit = {
        if (!args.forall(arg ⇒ arg.endsWith(".jar") || arg.endsWith(".jmod"))) {
            Console.err.println("Usage: java …ClassHierarchy <.jar|.jmod file>+")
            sys.exit(-1)
        }

        println("Extracting class hierarchy.")
        val classHierarchy =
            if (args.length == 0) {
                ClassHierarchy.PreInitializedClassHierarchy
            } else {
                val classFiles =
                    (List.empty[ClassFile] /: args) { (classFiles, filename) ⇒
                        classFiles ++ ClassFiles(new File(filename)).iterator.map(_._1)
                    }
                if (classFiles.forall(cf ⇒ cf.thisType != ObjectType.Object))
                    // load pre-configured class hierarchy...
                    ClassHierarchy(classFiles)(GlobalLogContext)
                else
                    ClassHierarchy(classFiles, Seq.empty)(GlobalLogContext)
            }

        println("Creating class hierarchy visualization.")
        val dotGraph = toDot(Set(classHierarchy.toGraph()), "back")
        val file = writeAndOpen(dotGraph, "ClassHierarchy", ".gv")
        println(s"Wrote class hierarchy graph to: $file.")
    }
}
