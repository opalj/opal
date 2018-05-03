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
            case Some(data) ⇒
                val clazz = defineClass(name, data, 0, data.length)
                rawClasses -= name
                clazz
            case None ⇒
                throw new ClassNotFoundException(name)
        }
    }
}
