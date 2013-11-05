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
package de.tud.cs.st
package bat
package resolved
package analyses

import util.graphs.{ Node, toDot }

import reader.Java7Framework

import java.net.URL

/**
 * Primary abstraction of a Java project. This class is basically just a container
 * for `ClassFile`s. Additionally, it makes project wide information available such as
 * the class hierarchy.
 *
 * ==Initialization==
 * To create a representation of a project use the `++` and `+` method.
 *
 * ==Thread Safety==
 * This class is immutable.
 *
 * @tparam S The type of the source of the class file. E.g., a `URL`, a `File` object,
 *    a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *    presenting users meaningful messages w.r.t. the location of issues.
 *    We abstract over the type of the resource to facilitate the embedding in existing
 *    tools such as IDEs. E.g., in Eclipse "Resources" are used to identify the
 *    location of a resource (e.g., a source or class file.)
 * @param classes A mapping of `ObjectType`s to `ClassFile`s. When the analysis does not
 *    load or keep all classes related to a project, it is possible that no class file is
 *    associated with a specific `ObjectType`.
 * @param sources A mapping of an `ObjectType` to its defining source.
 * @param classHierarchy This project's class hierarchy.
 *
 * @author Michael Eichberg
 */
class Project[+Source](
    val classes: Map[ObjectType, ClassFile],
    val sources: Map[ObjectType, Source],
    val classHierarchy: ClassHierarchy)
        extends (ObjectType ⇒ Option[ClassFile]) {

    def this(classHierarchy: ClassHierarchy = ClassHierarchy.empty) {
        this(
            Map.empty[ObjectType, ClassFile],
            Map.empty[ObjectType, Source],
            classHierarchy
        )
    }

    /**
     * Tries to lookup the class file for the given `objectType`.
     */
    def apply(objectType: ObjectType): Option[ClassFile] = classes.get(objectType)

    /**
     * Adds the class files to this project by calling the simple "+" method
     * for each class file.
     */
    def ++[NewS >: Source](classFiles: Traversable[(ClassFile, NewS)]): Project[NewS] =
        ((this: Project[NewS]) /: classFiles)(_ + _)

    /**
     * Adds the given class file to this project.
     *
     * If the class defines an object type that was previously added, the old class file
     * will be replaced by the given one.
     */
    def +[NewS >: Source](cs: (ClassFile, NewS)): Project[NewS] = {
        val (classFile, source) = cs
        new Project(
            classes + ((classFile.thisClass, classFile)),
            sources + ((classFile.thisClass, source)),
            classHierarchy + classFile)
    }

    /**
     * This project's class files.
     */
    def classFiles: Iterable[ClassFile] = classes.values
}

/**
 * Factory for [[de.tud.cs.st.bat.resolved.analyses.Project]] objects.
 *
 * @author Michael Eichberg
 */
object Project {

    /**
     * Creates a project that contains no class files, but where the class hierarchy
     * already contains the information about the exceptions thrown by JVM
     * instructions.
     */
    def initial[Source]() =
        new Project[Source](
            classHierarchy = ClassHierarchy.preInitializedClassHierarchy
        )

}
