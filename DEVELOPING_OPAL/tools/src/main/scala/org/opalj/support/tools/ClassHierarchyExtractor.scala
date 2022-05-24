/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.tools

import java.io.File

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.ClassHierarchy
import org.opalj.log.GlobalLogContext

/**
 * Writes out (a subset of) the class hierarchy in the format used by
 * [[org.opalj.br.ClassHierarchy]] to create the pre-initialized
 * class hierarchy.
 *
 * @author Michael Eichberg
 */
object ClassHierarchyExtractor {

    def deriveSpecification(
        types: Iterable[ObjectType]
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): String = {
        val specLines = types.map { aType =>
            var specLine =
                (
                    if (classHierarchy.isInterface(aType).isYes)
                        "interface "
                    else
                        "class "
                ) + aType.fqn
            val superclassType = classHierarchy.superclassType(aType)
            if (superclassType.isDefined) {
                specLine += " extends "+superclassType.get.fqn
                val superinterfaceTypes = classHierarchy.superinterfaceTypes(aType)
                if (superinterfaceTypes.isDefined && superinterfaceTypes.get.nonEmpty) {
                    specLine += superinterfaceTypes.get.map(_.fqn).mkString(" implements ", ", ", "")
                }
            }
            specLine
        }
        specLines.mkString("\n")
    }

    def main(args: Array[String]): Unit = {

        import Console.err
        import org.opalj.br.reader.Java8Framework.ClassFiles

        if (args.length < 3 ||
            !args.drop(2).forall(arg => arg.endsWith(".jar") || arg.endsWith(".jmod"))) {
            println("Usage:     java …ClassHierarchy supertype filterprefix <JAR|JMOD file>+")
            println("Example:   … java.lang.Enum \"\" .../rt.jar")
            println("           lists all subclasses of java.lang.Enum in rt.jar; \"\" effectively disables the filter.")
            sys.exit(-1)
        }

        val supertypeName = args(0).replace('.', '/')
        val filterPrefix = args(1).replace('.', '/')

        val classFiles = args.foldLeft(Nil: List[ClassFile]) { (classFiles, filename) =>
            classFiles ++ ClassFiles(new File(filename)).iterator.map(_._1)
        }
        implicit val classHierarchy =
            if (classFiles.forall(cf => cf.thisType != ObjectType.Object)) {
                println("the class files do not contain java.lang.Object; adding default type hierarchy")
                // load pre-configured class hierarchy...
                ClassHierarchy(classFiles)(GlobalLogContext)
            } else {
                ClassHierarchy(classFiles, Seq.empty)(GlobalLogContext)
            }

        val supertype = ObjectType(supertypeName)
        if (classHierarchy.isUnknown(supertype)) {
            err.println(s"The type: $supertypeName is not defined in the specified jar(s).")
            sys.exit(-2)
        }

        println(
            "# Class hierarchy for: "+
                supertypeName+
                " limited to subclasses that start with: "+
                "\""+filterPrefix+"\""
        )
        val allRelevantSubtypes =
            classHierarchy.allSubtypes(supertype, true).filter { candidateType =>
                candidateType.fqn.startsWith(filterPrefix)
            }
        val spec = deriveSpecification(allRelevantSubtypes)
        println(spec)
    }
}
