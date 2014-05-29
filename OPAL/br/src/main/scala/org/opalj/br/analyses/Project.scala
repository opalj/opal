/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package br
package analyses

import scala.collection.{ Set, Map }
import scala.collection.mutable.{ AnyRefMap, OpenHashMap }

/**
 * Primary abstraction of a Java project; i.e., a set of classes that constitute a
 * library, framework or application as well as the libraries or frameworks used by
 * the former.
 * This class has several purposes:
 *
 *  1. It is a container for `ClassFile`s.
 *  1. It directly gives access to the project's class hierarchy.
 *  1. It serves as a container for project-wide information (e.g., a call graph,
 *     information about the mutability of classes, constant values,...) that can be queried
 *     using [[org.opalj.br.analyses.ProjectInformationKey]]s.
 *     The list of project wide information that can be made available is equivalent
 *     to the list of (concrete/singleton) objects implementing the trait
 *     [[org.opalj.br.analyses.ProjectInformationKey]].
 *
 * ==Thread Safety==
 * This class is thread-safe.
 *
 * @tparam Source The type of the source of the class file. E.g., a `URL`, a `File`,
 *      a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *      presenting users meaningful messages w.r.t. the location of issues.
 *      We abstract over the type of the resource to facilitate the embedding in existing
 *      tools such as IDEs. E.g., in Eclipse `IResource`'s are used to identify the
 *      location of a resource (e.g., a source or class file.)
 *
 * @author Michael Eichberg
 */
class Project[Source] private (
        val projectClassFiles: List[ClassFile],
        val libraryClassFiles: List[ClassFile],
        private[this] val projectTypes: Set[ObjectType],
        private[this] val fieldToClassFile: AnyRefMap[Field, ClassFile],
        private[this] val methodToClassFile: AnyRefMap[Method, ClassFile],
        private[this] val objectTypeToClassFile: OpenHashMap[ObjectType, ClassFile],
        private[this] val sources: OpenHashMap[ObjectType, Source],
        val projectClassFilesCount: Int,
        val projectMethodsCount: Int,
        val projectFieldsCount: Int,
        val libraryClassFilesCount: Int,
        val libraryMethodsCount: Int,
        val libraryFieldsCount: Int,
        val classHierarchy: ClassHierarchy) extends ClassFileRepository {

    val classFilesCount: Int =
        projectClassFilesCount + libraryClassFilesCount

    val methodsCount: Int =
        projectMethodsCount + libraryMethodsCount

    val fieldsCount: Int =
        projectFieldsCount + libraryFieldsCount

    val classFiles: Iterable[ClassFile] =
        projectClassFiles.toIterable ++ libraryClassFiles.toIterable

    def classFilesWithSources: Iterable[(Source, ClassFile)] = {
        projectClassFiles.view.map(cf ⇒ (sources(cf.thisType), cf)) ++
            libraryClassFiles.view.map(cf ⇒ (sources(cf.thisType), cf))
    }

    def methods: Iterable[Method] = methodToClassFile.keys

    def fields: Iterable[Field] = fieldToClassFile.keys

    private[analyses] def projectClassFilesWithSources: Iterable[(ClassFile, Source)] = {
        projectClassFiles.view.map { classFile ⇒
            (classFile, sources(classFile.thisType))
        }
    }

    private[analyses] def libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = {
        libraryClassFiles.view.map { classFile ⇒
            (classFile, sources(classFile.thisType))
        }
    }

    /**
     * Returns true if the given class file belongs to the library part of the project.
     * This is only the case if the class file was explicitly identified as being
     * part of the library. By default all class files are considered to belong to the
     * code base that will be analyzed.
     */
    def isLibraryType(classFile: ClassFile): Boolean =
        !projectTypes.contains(classFile.thisType)

    /**
     * Returns true if the given type file belongs to the library part of the project.
     * This is generally the case if no class file was loaded for the given type.
     */
    def isLibraryType(objectType: ObjectType): Boolean =
        !projectTypes.contains(objectType)

    /**
     * Returns the source (for example, a `File` object or `URL` object) from which
     * the class file was loaded that defines the given object type, if any.
     *
     * @param objectType Some object type. (This method is defined for all `ObjectType`s.)
     */
    def source(objectType: ObjectType): Option[Source] =
        sources.get(objectType)

    /**
     * Returns the class file that defines the given `objectType`; if any.
     *
     * @param objectType Some object type. (This method is defined for all `ObjectType`s.)
     */
    override def classFile(objectType: ObjectType): Option[ClassFile] =
        objectTypeToClassFile.get(objectType)

    /**
     * Returns the given method's class file. This method is only defined if
     * the method was previously added to this project. (I.e., the class file which
     * defines the method was added.)
     */
    def classFile(method: Method): ClassFile =
        methodToClassFile(method)

    /**
     * Returns the given field's class file. This method is only defined if
     * the field was previously added to this project. (I.e., the class file which
     * defines the field was added.)
     */
    def classFile(field: Field): ClassFile =
        fieldToClassFile(field)

    /**
     * Converts this project abstraction into a standard Java `HashMap`.
     *
     * @note This method should only be used by Java projects that want to interact
     *      with BAT.
     */
    def toJavaMap(): java.util.HashMap[ObjectType, ClassFile] = {
        val map = new java.util.HashMap[ObjectType, ClassFile]
        for (classFile ← classFiles) map.put(classFile.thisType, classFile)
        map
    }

    /**
     * Some basic statistics about this project.
     *
     * ((Re)Calculated on-demand.)
     */
    def statistics: Map[String, Int] =
        Map(
            ("ProjectClassFiles" -> projectClassFilesCount),
            ("LibraryClassFiles" -> libraryClassFilesCount),
            ("ProjectMethods" -> projectMethodsCount),
            ("ProjectFields" -> projectFieldsCount),
            ("LibraryMethods" -> libraryMethodsCount),
            ("LibraryFields" -> libraryFieldsCount),
            ("ProjectInstructions" -> classFiles.foldLeft(0)(_ + _.methods.filter(_.body.isDefined).foldLeft(0)(_ + _.body.get.instructions.count(_ != null))))
        )

    /**
     * Returns all available `ClassFile` objects for the given `objectTypes` that
     * pass the given `filter`. `ObjectType`s for which no `ClassFile` is available
     * are ignored.
     */
    def lookupClassFiles(
        objectTypes: Traversable[ObjectType])(
            filter: ClassFile ⇒ Boolean): Traversable[ClassFile] = {
        objectTypes.view.flatMap(classFile(_)) filter { someClassFile: ClassFile ⇒
            filter(someClassFile)
        }
    }

    override def toString: String = {
        val classDescriptions =
            sources map { (entry) ⇒
                val (ot, source) = entry
                ot.toJava+" « "+source.toString
            }
        "Project( "+classDescriptions.mkString("\n\t", "\n\t", "\n")+")"
    }

    // ----------------------------------------------------------------------------------
    //
    // CODE TO MAKE IT POSSIBLE TO ATTACH SOME INFORMATION TO A PROJECT (ON DEMAND)
    //
    // ----------------------------------------------------------------------------------

    import java.util.concurrent.atomic.AtomicReferenceArray

    // Note that the referenced array will never shrink!
    @volatile
    private[this] var projectInformation = new AtomicReferenceArray[AnyRef](32)

    /**
     * Returns the additional project information that is ''currently'' available.
     *
     * If some analyses are still running it may be possible that additional
     * information will be made available as part of the execution of those
     * analyses.
     *
     * @note This method redetermines the available project information on each call.
     */
    def availableProjectInformation: List[AnyRef] = {
        var pis = List.empty[AnyRef]
        val thisProjectInformation = this.projectInformation
        for (i ← (0 until thisProjectInformation.length())) {
            var pi = thisProjectInformation.get(i)
            if (pi != null) {
                pis = pi :: pis
            }
        }
        pis
    }

    /**
     * Returns the information attached to this project that is identified by the
     * given `ProjectInformationKey`.
     *
     * If the information was not yet required the information is computed and
     * returned. Subsequent calls will directly return the information.
     *
     * @see [[ProjectInformationKey]] for further information.
     */
    def get[T <: AnyRef](pik: ProjectInformationKey[T]): T = {
        val pikUId = pik.uniqueId

        def derive(projectInformation: AtomicReferenceArray[AnyRef]): T = { // calls are externally synchronized!
            for (requiredProjectInformationKey ← pik.getRequirements) {
                get(requiredProjectInformationKey)
            }
            val pi = pik.doCompute(this)
            projectInformation.set(pikUId, pi)
            pi
        }

        val thisProjectInformation = this.projectInformation
        if (pikUId < thisProjectInformation.length()) {
            val pi = thisProjectInformation.get(pikUId)
            if (pi != null) {
                pi.asInstanceOf[T]
            } else {
                this.synchronized {
                    // It may be the case that the underlying array was updated!
                    val thisProjectInformation = this.projectInformation
                    // double-checked locking (works with Java >=6)
                    val pi = thisProjectInformation.get(pikUId)
                    if (pi != null) {
                        pi.asInstanceOf[T]
                    } else {
                        derive(thisProjectInformation)
                    }
                }
            }
        } else {
            // We have to synchronize w.r.t. "this" object on write accesses
            // to make sure that we do not loose a concurrent update or
            // derive an information more than once.
            this.synchronized {
                val thisProjectInformation = this.projectInformation
                if (pikUId < thisProjectInformation.length()) {
                    get(pik)
                } else {
                    val newLength = Math.max(thisProjectInformation.length * 2, pikUId * 2)
                    val newProjectInformation = new AtomicReferenceArray[AnyRef](newLength)
                    for (i ← 0 until thisProjectInformation.length()) {
                        newProjectInformation.set(i, thisProjectInformation.get(i))
                    }
                    this.projectInformation = newProjectInformation
                    derive(newProjectInformation)
                }
            }
        }
    }

    /**
     * Tests if the information identified by the given [[ProjectInformationKey]]
     * is available. If the information is not available, the information
     * will not be computed and `None` will be returned.
     *
     * @see [[ProjectInformationKey]] for further information.
     */
    def has[T <: AnyRef](pik: ProjectInformationKey[T]): Option[T] = {
        val pikUId = pik.uniqueId

        if (pikUId < this.projectInformation.length())
            Option(this.projectInformation.get(pikUId).asInstanceOf[T])
        else
            None
    }
}

import java.net.URL
import java.io.File

/**
 * Factory for `Project`s.
 *
 * @author Michael Eichberg
 */
object Project {

    /**
     * Given a reference to a class file, jar file or a folder containing jar and class
     * files, all class files will be loaded and a project will be returned.
     */
    def apply(file: File): Project[URL] = {
        Project.apply[URL](reader.Java8Framework.ClassFiles(file))
    }

    /**
     * Creates a new `Project` that consists of the source files of the previous
     * project and the newly given source files.
     */
    def extend[Source: reflect.ClassTag](
        project: Project[Source],
        projectClassFilesWithSources: Iterable[(ClassFile, Source)],
        libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = Iterable.empty): Project[Source] = {

        apply(
            project.projectClassFilesWithSources ++ projectClassFilesWithSources,
            project.libraryClassFilesWithSources ++ libraryClassFilesWithSources
        )
    }

    def defaultHandlerForInconsistentProject(ex: InconsistentProjectException): Unit = {
        import Console._
        println(RED+"[warning] "+ex.message + RESET)
    }

    /**
     * Creates a new Project.
     *
     * @param classFiles The list of class files of this project that are considered
     *    to belong to the application/library that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     * @param libraryClassFiles The list of class files of this project that make up
     *    the libraries used by the project that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     * @param handleInconsistentProject A function that is called back if the project
     *      is not consistent. The default behavior
     *      ([[defaultHandlerForInconsistentProject]]) is to write a warning
     *      message to the console. Alternatively it is possible to throw the given
     *      exception to cancel the loading of the project (which is the only
     *      meaningful option for several advanced analyses.)
     */
    def apply[Source: reflect.ClassTag](
        projectClassFilesWithSources: Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources: Traversable[(ClassFile, Source)] = Iterable.empty,
        handleInconsistentProject: (InconsistentProjectException) ⇒ Unit = defaultHandlerForInconsistentProject): Project[Source] = {

        import scala.collection.mutable.{ Set, Map }
        import concurrent.{ Future, Await, ExecutionContext }
        import concurrent.duration.Duration
        import ExecutionContext.Implicits.global

        val classHierarchyFuture: Future[ClassHierarchy] = Future {
            ClassHierarchy(
                projectClassFilesWithSources.view.map(_._1) ++
                    libraryClassFilesWithSources.view.map(_._1)
            )
        }

        var projectClassFiles = List.empty[ClassFile]
        val projectTypes = Set.empty[ObjectType]
        var projectClassFilesCount: Int = 0
        var projectMethodsCount: Int = 0
        var projectFieldsCount: Int = 0

        var libraryClassFiles = List.empty[ClassFile]
        var libraryClassFilesCount: Int = 0
        var libraryMethodsCount: Int = 0
        var libraryFieldsCount: Int = 0

        val methodToClassFile = AnyRefMap.empty[Method, ClassFile]
        val fieldToClassFile = AnyRefMap.empty[Field, ClassFile]
        val objectTypeToClassFile = OpenHashMap.empty[ObjectType, ClassFile]
        val sources = OpenHashMap.empty[ObjectType, Source]

        for ((classFile, source) ← projectClassFilesWithSources) {
            projectClassFiles = classFile :: projectClassFiles
            projectClassFilesCount += 1
            val objectType = classFile.thisType
            projectTypes += objectType
            for (method ← classFile.methods) {
                projectMethodsCount += 1
                methodToClassFile.put(method, classFile)
            }
            for (field ← classFile.fields) {
                projectFieldsCount += 1
                fieldToClassFile.put(field, classFile)
            }
            if (objectTypeToClassFile.contains(objectType)) {
                handleInconsistentProject(
                    InconsistentProjectException(
                        "The type "+
                            objectType.toJava+
                            " is defined by multiple class files: "+
                            sources(objectType)+" and "+
                            source+"."
                    )
                )
            }
            objectTypeToClassFile.put(objectType, classFile)
            sources.put(objectType, source)
        }

        for ((classFile, source) ← libraryClassFilesWithSources) {
            libraryClassFiles = classFile :: libraryClassFiles
            libraryClassFilesCount += 1
            val objectType = classFile.thisType
            for (method ← classFile.methods) {
                libraryMethodsCount += 1
                methodToClassFile.put(method, classFile)
            }
            for (field ← classFile.fields) {
                libraryFieldsCount += 1
                fieldToClassFile.put(field, classFile)
            }
            objectTypeToClassFile.put(objectType, classFile)
            sources.put(objectType, source)
        }

        fieldToClassFile.repack()
        methodToClassFile.repack()

        new Project(
            projectClassFiles,
            libraryClassFiles,
            projectTypes,
            fieldToClassFile,
            methodToClassFile,
            objectTypeToClassFile,
            sources,
            projectClassFilesCount,
            projectMethodsCount,
            projectFieldsCount,
            libraryClassFilesCount,
            libraryMethodsCount,
            libraryFieldsCount,
            Await.result(classHierarchyFuture, Duration.Inf)
        )
    }
}
