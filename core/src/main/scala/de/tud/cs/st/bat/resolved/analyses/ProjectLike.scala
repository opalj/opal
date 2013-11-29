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
 * ==Thread Safety==
 * Instances of this trait are expected to be immutable.
 *
 * @tparam Source The type of the source of the class file. E.g., a `URL`, a `File` object,
 *    a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *    presenting users meaningful messages w.r.t. the location of issues.
 *    We abstract over the type of the resource to facilitate the embedding in existing
 *    tools such as IDEs. E.g., in Eclipse "Resources" are used to identify the
 *    location of a resource (e.g., a source or class file.)
 *
 * @author Michael Eichberg
 */
abstract class ProjectLike[Source] extends (ObjectType ⇒ Option[ClassFile]) {

    /**
     * Tries to lookup the class file for the given `objectType`.
     */
    final def apply(objectType: ObjectType): Option[ClassFile] = classFile(objectType)

    def source(objectType: ObjectType): Option[Source]

    def classFile(objectType: ObjectType): Option[ClassFile]

    def classFile(method: Method): ClassFile

    def foreachClassFile[U](f: ClassFile ⇒ U): Unit

    def foreachMethod[U](f: Method ⇒ U): Unit

    def method(methodID: Int): Method
    
    def classFile(classFileID : Int) : ClassFile

    final def objectTypesCount = ObjectType.objectTypesCount

    final def methodsCount = Method.methodsCount

    final def fieldCount = Field.fieldsCount

    /**
     * This project's class files.
     */
    def classFiles: Iterable[ClassFile]

    val classHierarchy: ClassHierarchy

    /**
     * Returns all available `ClassFile` objects for the given `objectTypes` that
     * pass the given `filter`. `ObjectType`s for which no `ClassFile` is available
     * are ignored.
     */
    def lookupClassFiles(
        objectTypes: Traversable[ObjectType])(
            filter: ClassFile ⇒ Boolean): Traversable[ClassFile] =
        objectTypes.view.map(apply(_)).filter(_.isDefined).map(_.get).filter(filter)
}

trait ProjectBuilder[Source, Project <: ProjectBuilder[Source, Project]] {
    this: Project ⇒

    /**
     * Adds the class files to this project by calling the simple "+" method
     * for each class file.
     */
    final def ++(classFiles: Traversable[(ClassFile, Source)]): Project =
        (this /: classFiles)(_ + _)

    /**
     * Adds the given class file to this project.
     *
     * If the class defines an object type that was previously added, the old class file
     * will be replaced by the given one.
     */
    def +(cs: (ClassFile, Source)): Project
}
