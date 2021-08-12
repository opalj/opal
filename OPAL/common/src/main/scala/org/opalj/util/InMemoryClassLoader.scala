/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

/**
 * A simple `ClassLoader` that looks-up the available classes in a standard map.
 *
 * @param   classes a `Map` of classes where the key is the class name – using `.` as the package
 *          separator – and the value is the serialized class file.
 *
 * @author  Malte Limmeroth
 * @author  Michael Eichberg
 */
class InMemoryClassLoader(
        private[this] var rawClasses: Map[String, Array[Byte]],
        parent:                       ClassLoader              = ClassLoader.getSystemClassLoader
) extends ClassLoader(parent) {

    /**
     * @note Clients should call `loadClass`! Please, consult the documentation of
     *       `java.lang.ClassLoader` for further details!
     */
    @throws[ClassNotFoundException]
    override def findClass(name: String): Class[_] = {
        rawClasses.get(name) match {
            case Some(data) =>
                val clazz = defineClass(name, data, 0, data.length)
                rawClasses -= name
                clazz
            case None =>
                throw new ClassNotFoundException(name)
        }
    }
}
