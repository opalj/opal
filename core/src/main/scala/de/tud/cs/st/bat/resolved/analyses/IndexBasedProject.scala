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
 * the class hierarchy and the list of all fields and methods.
 *
 * ==Initialization==
 * To create a representation of a project use the companion object's factory method.
 * 
 * ==Thread Safety==
 * This class is immutable. After creating a project, it is not possible to dynamically
 * add/remove any class files.
 *
 * ==Implementation Details==
 * This class relies on the property that `ObjectType`s are associated with consecutive,
 * unique ids larger than 0 and that a `ClassFile`'s `hashCode` is equivalent to the
 * `id`/`hashCode` of the `ObjectType` it defines.
 *
 * @tparam Source The type of the source of the class file. E.g., a `URL`, a `File` object,
 *    a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *    presenting users meaningful messages w.r.t. the location of issues.
 *    We abstract over the type of the resource to facilitate the embedding in existing
 *    tools such as IDEs. E.g., in Eclipse `IResource`s are used to identify the
 *    location of a resource (e.g., a source or class file.)
 * @param classHierarchy This project's class hierarchy.
 *
 * @author Michael Eichberg
 */
class IndexBasedProject[Source: reflect.ClassTag] private (
    val classFilesCount: Int,
    /* The arrays are private to avoid that clients accidentally mutate them! 
       I.e., this class' data structures are indeed mutable, but they are never
       mutated by this class and they are not exposed to clients either. */
    // Mapping between an ObjectType('s id) and the ClassFile object which defines the type
    private[this] val classesMap: Array[ClassFile],
    // Mapping between an ObjectType('s id) and its defining source file 
    private[this] val sourcesMap: Array[Source],
    val classHierarchy: ClassHierarchy)
        extends ProjectLike[Source] {

    private[this] val classFileOfMethod = {
        val lookupTable = new Array[ClassFile](Method.methodsCount)
        foreachClassFile { classFile: ClassFile ⇒
            classFile.methods foreach { method ⇒ lookupTable(method.id) = classFile }
        }
        lookupTable
    }

    private[this] val classFileOfField = {
        val lookupTable = new Array[ClassFile](Field.fieldsCount)
        foreachClassFile { classFile: ClassFile ⇒
            classFile.fields foreach { field ⇒ lookupTable(field.id) = classFile }
        }
        lookupTable
    }

    import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf

    /**
     * This project's class files.
     */
    override def classFiles: Iterable[ClassFile] = classesMap.view.filter(_ ne null)

    override def source(objectType: ObjectType): Option[Source] = {
        // It may be the case that – after loading all class files – 
        // additional "ObjectType"s are created by some analysis which
        // will then have higher ids that are larger than the array's size!
        val id = objectType.id
        if (id < sourcesMap.size) Option(sourcesMap(id)) else None
    }

    override def classFile(objectType: ObjectType): Option[ClassFile] = {
        // It may be the case that – after loading all class files – 
        // additional "ObjectType"s are created by some analysis which
        // will then have ids that are larger than the array's size!
        val id = objectType.id
        if (id < classesMap.size) Option(classesMap(id)) else None
    }

    private[this] lazy val methodsMap: Array[Method] = {
        val map = new Array[Method](methodsCount)
        foreachClassFile { classFile ⇒
            classFile.methods foreach { method ⇒ map(method.id) = method }
        }
        map
    }

    /**
     * Returns the method with the specified id. If the id is not valid,
     * if the id is not valid, the result is undetermined.(An exception may be
     * thrown or `null` may be returned.)
     */
    def method(methodID: Int): Method = methodsMap(methodID)

    def classFile(objectTypeID: Int): ClassFile = classesMap(objectTypeID)

    /**
     * Looks up the ClassFile that contains the given field.
     *
     * The complexity of this operation is O(1).
     */
    override def classFile(field: Field): ClassFile = classFileOfField(field.id)

    /**
     * Looks up the ClassFile that contains the given method.
     *
     * The complexity of this operation is O(1).
     */
    override def classFile(method: Method): ClassFile = classFileOfMethod(method.id)

    override def foreachClassFile[U](f: ClassFile ⇒ U): Unit =
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            f(classFile)
        }

    override def forallClassFiles[U](f: ClassFile ⇒ Boolean): Boolean = {
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            if (!f(classFile))
                return false
        }
        true
    }

    override def foreachMethod[U](f: Method ⇒ U): Unit =
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            classFile.methods.foreach(f)
        }

    override def forallMethods[U](f: Method ⇒ Boolean): Boolean = {
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            if (!classFile.methods.forall(f))
                return false
        }
        true
    }

    def statistics: String = {
        val classFiles = classesMap.filter(_ != null)
        "Project Statistics:"+
            "\n\tClasses: "+classesMap.count(_ != null)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.annotations.size)+
            "\n\tMethods: "+classFiles.foldLeft(0)(_ + _.methods.size)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.methods.foldLeft(0)((c, n) ⇒ c + n.annotations.size + n.parameterAnnotations.size))+
            "\n\tFields: "+classFiles.foldLeft(0)(_ + _.fields.size)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.fields.foldLeft(0)((c, n) ⇒ c + n.annotations.size))+
            "\n\tInstructions: "+classFiles.foldLeft(0)(_ + _.methods.filter(_.body.isDefined).foldLeft(0)(_ + _.body.get.instructions.count(_ != null)))
    }

    override def toString: String = {
        val classesAndSources =
            (classesMap.view zip sourcesMap.view).view.filter(_._1 ne null)
        val classDescriptions =
            classesAndSources.map(cs ⇒ cs._1.thisType.toJava+" « "+cs._2.toString+" »")

        "IndexBasedProject( "+classDescriptions.mkString("\n\t", "\n\t", "\n")+")"
    }
}

/**
 * Defines factory methods to create
 * [[de.tud.cs.st.bat.resolved.analyses.IndexBasedProject]]s.
 *
 * @author Michael Eichberg
 */
object IndexBasedProject {

    /**
     * Creates a new IndexBasedProject.
     *
     * @param allClassFiles The list of class files of this project.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     */
    def apply[Source: reflect.ClassTag](
        classFiles: Iterable[(ClassFile, Source)]): IndexBasedProject[Source] = {

        import concurrent.{ Future, Await, ExecutionContext, future }
        import concurrent.duration.Duration
        import ExecutionContext.Implicits.global

        val classHierarchyFuture: Future[ClassHierarchy] = future {
            ClassHierarchy(classFiles.view.map(_._1))
        }

        val classes = new Array[ClassFile](ObjectType.objectTypesCount)
        val sources = new Array[Source](ObjectType.objectTypesCount)

        var classFilesCount = 0
        for ((classFile, source) ← classFiles) {
            classFilesCount += 1
            val id = classFile.thisType.id
            classes(id) = classFile
            sources(id) = source
        }

        new IndexBasedProject(
            classFilesCount,
            classes,
            sources,
            Await.result(classHierarchyFuture, Duration.Inf)
        )
    }
}
