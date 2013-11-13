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
 * This class relies on the property that `ObjectType`s are associated with consecutive,
 * unique ids larger than 0.
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
 * @param classes A mapping of the id of `ObjectType`s to `ClassFile`s. When the analysis
 *    does not load or keep all classes related to a project, it is possible that no
 *    class file is associated with a specific `ObjectType`.
 * @param sources A mapping of the id of an `ObjectType` to its defining source.
 * @param classHierarchy This project's class hierarchy.
 *
 * @author Michael Eichberg
 */
class IndexBasedProject[Source: reflect.ClassTag] private (
    private[this] val classes: Array[ClassFile],
    private[this] val sources: Array[Source],
    val classHierarchy: ClassHierarchy)
        extends ProjectLike[Source] {

    def source(objectType: ObjectType): Option[Source] = {
        if (sources.size <= objectType.id)
            None
        else
            Option(sources(objectType.id))
    }

    def classFile(objectType: ObjectType): Option[ClassFile] = {
        if (classes.size <= objectType.id)
            None
        else
            Option(classes(objectType.id))
    }

    private[this] val classFileOfMethod = {
        val lookupTable = new Array[ClassFile](Method.methodsCount)
        foreachClassFile { classFile: ClassFile ⇒
            classFile.methods.foreach { method ⇒
                lookupTable(method.id) = classFile
            }
        }
        lookupTable
    }
    
    /**
     * Looks up the ClassFile that contains the given method.
     * 
     * The complexity of this operation is O(1).
     */
    override def classFile(method: Method): ClassFile = {
        classFileOfMethod(method.id)
    }

    override def foreachClassFile(f: ClassFile ⇒ _): Unit = {
        var i = classes.length - 1
        while (i >= 0) {
            val classFile = classes(i)
            if (classFile ne null) f(classFile)
            i -= 1
        }
    }

    override def foreachMethod(f: Method ⇒ _): Unit = {
        var i = classes.length - 1
        while (i >= 0) {
            val classFile = classes(i)
            if (classFile ne null) classFile.methods.foreach(f)
            i -= 1
        }
    }

    /**
     * This project's class files.
     */
    override def classFiles: Iterable[ClassFile] = classes.view.filter(_ ne null)

    override def toString: String = {
        "Classes:\n\t"+classes.view.filter(_ ne null).map(_.thisClass.toJava).mkString("(", ",", ")")+"\n"
        "ClassFileOfMethod:\n\t"+classFileOfMethod.mkString("(", ",", ")")
    }
}

/**
 * Factory for [[de.tud.cs.st.bat.resolved.analyses.Project]] objects.
 *
 * @author Michael Eichberg
 */
object IndexBasedProject {

    def apply[Source: reflect.ClassTag](
        classFiles: Iterable[(ClassFile, Source)],
        classHierarchy: ClassHierarchy = ClassHierarchy.preInitializedClassHierarchy): IndexBasedProject[Source] = {

        val classFilesCount = classFiles.size
        val classes = new Array[ClassFile](ObjectType.objectTypesCount)
        val sources = new Array[Source](ObjectType.objectTypesCount)

        for ((classFile, source) ← classFiles) {
            val id = classFile.thisClass.id
            classes(id) = classFile
            sources(id) = source
        }

        new IndexBasedProject(classes, sources, classHierarchy)
    }
}
