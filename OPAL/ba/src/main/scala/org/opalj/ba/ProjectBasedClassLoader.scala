/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.bc.Assembler
import org.opalj.br.ClassFileRepository
import org.opalj.br.ObjectType

/**
 * A simple `ClassLoader` that looks-up the available classes from the given
 * [[org.opalj.br.analyses.Project]].
 *
 * @author Andreas Muttscheller
 */
class ProjectBasedInMemoryClassLoader(
    val project: ClassFileRepository,
    parent:      ClassLoader = classOf[ProjectBasedInMemoryClassLoader].getClassLoader
) extends ClassLoader(parent) {

    @throws[ClassNotFoundException]
    override def findClass(name: String): Class[?] = {
        project.classFile(ObjectType(name.replace('.', '/'))) match {

            case Some(cf) =>
                val bytes = Assembler(ba.toDA(cf))
                defineClass(name, bytes, 0, bytes.length)

            case None =>
                throw new ClassNotFoundException(name)
        }
    }
}
