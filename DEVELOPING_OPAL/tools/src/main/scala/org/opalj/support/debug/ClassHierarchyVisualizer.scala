/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
        if (!args.forall(arg => arg.endsWith(".jar") || arg.endsWith(".jmod"))) {
            Console.err.println("Usage: java …ClassHierarchy <.jar|.jmod file>+")
            sys.exit(-1)
        }

        println("Extracting class hierarchy.")
        val classHierarchy =
            if (args.length == 0) {
                ClassHierarchy.PreInitializedClassHierarchy
            } else {
                val classFiles =
                    args.foldLeft(List.empty[ClassFile]) { (classFiles, filename) =>
                        classFiles ++ ClassFiles(new File(filename)).iterator.map(_._1)
                    }
                if (classFiles.forall(cf => cf.thisType != ObjectType.Object))
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
