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

import java.net.URL
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.{Set, Map}
import scala.collection.mutable.{AnyRefMap, OpenHashMap}
import scala.collection.parallel.mutable.ParArray
import scala.collection.parallel.immutable.ParVector
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import scala.collection.SortedMap
import scala.collection.mutable.LinkedHashMap
import scala.collection.SortedMap
import scala.collection.generic.FilterMonadic
import scala.reflect.ClassTag
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

import net.ceedubs.ficus.Ficus._

import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.Java8LibraryFrameworkWithCaching
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.OPALExecutionContext
import org.opalj.concurrent.parForeachArrayElement
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.DefaultLogContext
import org.opalj.collection.mutable.ArrayMap

/**
 * Primary abstraction of a Java project; i.e., a set of classes that constitute a
 * library, framework or application as well as the libraries or frameworks used by
 * the former.
 *
 * This class has several purposes:
 *
 *  1. It is a container for `ClassFile`s.
 *  1. It directly gives access to the project's class hierarchy.
 *  1. It serves as a container for project-wide information (e.g., a call graph,
 *     information about the mutability of classes, constant values,...) that can
 *     be queried using [[org.opalj.br.analyses.ProjectInformationKey]]s.
 *     The list of project wide information that can be made available is equivalent
 *     to the list of (concrete/singleton) objects implementing the trait
 *     [[org.opalj.br.analyses.ProjectInformationKey]].
 *
 * ==Thread Safety==
 * This class is thread-safe.
 *
 * ==Prototyping Analyses/Querying Projects==
 * Projects can easily be created and queried using the Scala `REPL`. For example,
 * to create a project, you can use:
 * {{{
 * val JRE = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/jre/lib"
 * val project = org.opalj.br.analyses.Project(new java.io.File(JRE))
 * }}}
 * Now, to determine the number of methods that have at least one parameter of type
 * `int`, you can use:
 * {{{
 * project.methods.filter(_.parameterTypes.exists(_.isIntegerType)).size
 * }}}
 *
 * @tparam Source The type of the source of the class file. E.g., a `URL`, a `File`,
 *      a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *      presenting users meaningful messages w.r.t. the location of issues.
 *      We abstract over the type of the resource to facilitate the embedding in existing
 *      tools such as IDEs. E.g., in Eclipse `IResource`'s are used to identify the
 *      location of a resource (e.g., a source or class file.)
 *
 * @param logContext The logging context associated with this project. Using the logging
 *      context after the project is no longer referenced (garbage collected) is not
 *      possible.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
class Project[Source] private (
        private[this] val projectClassFiles:              Array[ClassFile],
        private[this] val libraryClassFiles:              Array[ClassFile],
        private[this] val methods:                        Array[Method], // the concrete methods, sorted by size in descending order
        private[this] val projectTypes:                   Set[ObjectType], // the types defined by the class files belonging to the project's code
        private[this] val fieldToClassFile:               AnyRefMap[Field, ClassFile],
        private[this] val methodToClassFile:              AnyRefMap[Method, ClassFile],
        private[this] val objectTypeToClassFile:          OpenHashMap[ObjectType, ClassFile],
        private[this] val sources:                        OpenHashMap[ObjectType, Source],
        private[this] val methodsWithClassFilesAndSource: Array[(Source, ClassFile, Method)], // the concrete methods, sorted by size in descending order
        val projectClassFilesCount:                       Int,
        val projectMethodsCount:                          Int,
        val projectFieldsCount:                           Int,
        val libraryClassFilesCount:                       Int,
        val libraryMethodsCount:                          Int,
        val libraryFieldsCount:                           Int,
        val codeSize:                                     Long,
        val classHierarchy:                               ClassHierarchy,
        val analysisMode:                                 AnalysisMode
)(
        implicit
        val logContext: LogContext,
        val config:     Config
) extends ClassFileRepository {

    OPALLogger.debug("progress", s"project created (${logContext.logContextId})")

    /**
     * Creates a new `Project` which also includes the given class files.
     */
    def extend(
        projectClassFilesWithSources: Iterable[(ClassFile, Source)],
        libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = Iterable.empty
    ): Project[Source] = {
        Project.extend[Source](
            this,
            projectClassFilesWithSources,
            libraryClassFilesWithSources
        )
    }

    /**
     * Creates a new `Project` which also includes this as well as the other project's
     * class files.
     */
    def extend(otherProject: Project[Source]): Project[Source] = {
        Project.extend[Source](
            this,
            otherProject.projectClassFilesWithSources,
            otherProject.libraryClassFilesWithSources
        )
    }

    /**
     * The number of classes (including inner and annoymous classes as well as
     * interfaces, annotations, etc.) defined in libraries and in the analyzed project.
     */
    final val classFilesCount: Int = projectClassFilesCount + libraryClassFilesCount

    /**
     * The number of methods defined in libraries and in the analyzed project.
     */
    final val methodsCount: Int = projectMethodsCount + libraryMethodsCount

    /**
     * The number of field defined in libraries and in the analyzed project.
     */
    final val fieldsCount: Int = projectFieldsCount + libraryFieldsCount

    /**
     * The number of all source elements (fields, methods and class files).
     */
    def sourceElementsCount = fieldsCount + methodsCount + classFilesCount

    /**
     * Returns a new `Iterable` over all source elements of the project. The set
     * of all source elements consists of (in this order): all methods + all fields +
     * all class files.
     */
    def allSourceElements: Iterable[SourceElement] = methods() ++ fields() ++ allClassFiles

    val allProjectClassFiles: Iterable[ClassFile] = projectClassFiles

    private[this] def doParForeachClassFile[T](
        classFiles: Array[ClassFile], isInterrupted: () ⇒ Boolean
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        val classFilesCount = classFiles.length
        if (classFilesCount == 0)
            return Nil;

        val parallelizationLevel = Math.min(NumberOfThreadsForCPUBoundTasks, classFilesCount)
        parForeachArrayElement(classFiles, parallelizationLevel, isInterrupted)(f)
    }

    def parForeachProjectClassFile[T](
        isInterrupted: () ⇒ Boolean
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        doParForeachClassFile(this.projectClassFiles, isInterrupted)(f)
    }

    val allLibraryClassFiles: Iterable[ClassFile] = libraryClassFiles

    def parForeachLibraryClassFile[T](
        isInterrupted: () ⇒ Boolean = () ⇒ Thread.currentThread().isInterrupted()
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        doParForeachClassFile(this.libraryClassFiles, isInterrupted)(f)
    }

    val allClassFiles: Iterable[ClassFile] = allProjectClassFiles ++ allLibraryClassFiles

    def parForeachClassFile[T](
        isInterrupted: () ⇒ Boolean = () ⇒ Thread.currentThread().isInterrupted()
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        parForeachProjectClassFile(isInterrupted)(f) :::
            parForeachLibraryClassFile(isInterrupted)(f)
    }

    /**
     * The set of all method names of the given types.
     */
    def methodNames(objectTypes: Traversable[ObjectType]): Set[String] = {
        objectTypes.map(classFile(_)).flatten.map(_.methods.map(_.name)).flatten.toSet
    }

    /**
     * Returns the list of all packages that contain at least one class.
     *
     * For example, in case of the JDK the package `java` does not directly contain
     * any class – only its subclasses. This package is, hence, not returned by this
     * function, but the package `java.lang` is.
     *
     * @note This method's result is not cached.
     */
    def packages: Set[String] = projectPackages ++ libraryPackages

    /**
     * Returns the list of all project packages that contain at least one class.
     *
     * For example, in case of the JDK the package `java` does not directly contain
     * any class – only its subclasses. This package is, hence, not returned by this
     * function, but the package `java.lang` is.
     *
     * @note This method's result is not cached.
     */
    def projectPackages: Set[String] = {
        projectClassFiles.foldLeft(Set.empty[String])(_ + _.thisType.packageName)
    }

    /**
     * Returns the list of all library packages that contain at least one class.
     *
     * For example, in case of the JDK the package `java` does not directly contain
     * any class – only its subclasses. This package is, hence, not returned by this
     * function, but the package `java.lang` is.
     *
     * @note This method's result is not cached.
     */
    def libraryPackages: Set[String] = {
        libraryClassFiles.foldLeft(Set.empty[String])(_ + _.thisType.packageName)
    }

    def methodsWithBody: Iterable[Method] = methods

    /**
     * Iterates over all methods with a body in parallel.
     *
     * This method maximizes utilization by allowing each thread to pick the next
     * unanalyzed method as soon as the thread has finished analyzing the previous method.
     * I.e., each thread is not assigned a fixed batch of methods. Additionally, the
     * methods are analyzed ordered by their length (longest first).
     */
    def parForeachMethodWithBody[T](
        isInterrupted: () ⇒ Boolean = () ⇒ Thread.currentThread().isInterrupted()
    )(
        f: Function[(Source, ClassFile, Method), T]
    ): List[Throwable] = {
        val methods = this.methodsWithClassFilesAndSource
        val methodCount = methods.length
        if (methodCount == 0)
            return Nil;

        val parallelizationLevel = Math.min(NumberOfThreadsForCPUBoundTasks, methodCount)
        parForeachArrayElement(methods, parallelizationLevel, isInterrupted)(f)
    }

    /**
     * Determines for all packages of this project that contain at least one class
     * the "root" packages and stores the mapping between the package and its root package.
     *
     * For example, let's assume that we have project which has the following packages
     * that contain at least one class:
     *  - org.opalj
     *  - org.opalj.ai
     *  - org.opalj.ai.domain
     *  - org.apache.commons.io
     *  - java.lang
     * Then the map will be:
     *  - org.opalj => org.opalj
     *  - org.opalj.ai => org.opalj
     *  - org.opalj.ai.domain => org.opalj
     *  - org.apache.commons.io => org.apache.commons.io
     *  - java.lang => java.lang
     *
     * In other words the set of rootPackages can then be determined using:
     * {{{
     * <Project>.rootPackages().values.toSet
     * }}}
     *
     * @note This method's result is not cached.
     *
     * @return a Map which contains for each package name the root package name.
     */
    def rootPackages: Map[String, String] = {
        val allPackages = packages.toSeq.sorted
        if (allPackages.isEmpty)
            Map.empty
        else if (allPackages.tail.isEmpty)
            Map((allPackages.head, allPackages.head))
        else {
            allPackages.tail.foldLeft(SortedMap((allPackages.head, allPackages.head))) {
                (rootPackages, nextPackage) ⇒
                    // java is not a root package of "javax"...
                    val (_, lastPackage) = rootPackages.last
                    if (nextPackage.startsWith(lastPackage) &&
                        nextPackage.charAt(lastPackage.size) == '/')
                        rootPackages + ((nextPackage, lastPackage))
                    else
                        rootPackages + ((nextPackage, nextPackage))
            }
        }
    }

    /**
     * Number of packages.
     *
     * @note The result is (re)calculated for each call.
     */
    def packagesCount = packages.size

    def groupedClassFilesWithMethodsWithBody(groupsCount: Int): Array[Buffer[ClassFile]] = {
        var nextGroupId = 0
        val groups = Array.fill[Buffer[ClassFile]](groupsCount) {
            new ArrayBuffer[ClassFile](methodsCount / groupsCount)
        }
        for {
            classFile ← projectClassFiles
            if classFile.methods.exists(_.body.isDefined)
        } {
            // we distribute the classfiles among the different bins
            // to avoid that one bin accidentally just contains
            // interfaces
            groups(nextGroupId) += classFile
            nextGroupId = (nextGroupId + 1) % groupsCount
        }
        groups
    }

    def classFilesWithSources: Iterable[(ClassFile, Source)] = {
        projectClassFiles.view.map(cf ⇒ (cf, sources(cf.thisType))) ++
            libraryClassFiles.view.map(cf ⇒ (cf, sources(cf.thisType)))
    }

    /**
     * All methods defined by this project as well as the visible methods defined by the libraries.
     */
    def methods(): Iterable[Method] = methodToClassFile.keys

    /**
     * All fields defined by this project as well as the visible fields defined by the libraries.
     */
    def fields(): Iterable[Field] = fieldToClassFile.keys

    def projectClassFilesWithSources: Iterable[(ClassFile, Source)] = {
        projectClassFiles.view.map { classFile ⇒
            (classFile, sources(classFile.thisType))
        }
    }

    def libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = {
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
    def isLibraryType(classFile: ClassFile): Boolean = isLibraryType(classFile.thisType)

    /**
     * Returns true if the given type file belongs to the library part of the project.
     * This is generally the case if no class file was loaded for the given type.
     */
    def isLibraryType(objectType: ObjectType): Boolean = !projectTypes.contains(objectType)

    /**
     * Returns the source (for example, a `File` object or `URL` object) from which
     * the class file was loaded that defines the given object type, if any.
     *
     * @param objectType Some object type. (This method is defined for all `ObjectType`s.)
     */
    def source(objectType: ObjectType): Option[Source] = sources.get(objectType)

    /**
     * Returns the class file that defines the given `objectType`; if any.
     *
     * @param objectType Some object type. (This method is defined for all `ObjectType`s.)
     */
    override def classFile(objectType: ObjectType): Option[ClassFile] = {
        objectTypeToClassFile.get(objectType)
    }

    /**
     * Returns the given method's class file. This method is only defined if
     * the method was previously added to this project. (I.e., the class file which
     * defines the method was added.)
     */
    def classFile(method: Method): ClassFile = methodToClassFile(method)

    /**
     * Returns the given field's class file. This method is only defined if
     * the field was previously added to this project. (I.e., the class file which
     * defines the field was added.)
     */
    def classFile(field: Field): ClassFile = fieldToClassFile(field)

    /**
     * Converts this project abstraction into a standard Java `HashMap`.
     *
     * @note This method should only be used by Java projects that want to interact
     *      with BAT.
     */
    def toJavaMap(): java.util.HashMap[ObjectType, ClassFile] = {
        val map = new java.util.HashMap[ObjectType, ClassFile]
        for (classFile ← allClassFiles) map.put(classFile.thisType, classFile)
        map
    }

    /**
     * Some basic statistics about this project.
     *
     * ((Re)Calculated on-demand.)
     */
    def statistics: Map[String, Int] = {
        Map(
            ("ProjectClassFiles" → projectClassFilesCount),
            ("LibraryClassFiles" → libraryClassFilesCount),
            ("ProjectMethods" → projectMethodsCount),
            ("ProjectFields" → projectFieldsCount),
            ("LibraryMethods" → libraryMethodsCount),
            ("LibraryFields" → libraryFieldsCount),
            ("ProjectPackages" → projectPackages.size),
            ("LibraryPackages" → libraryPackages.size),
            ("ProjectInstructions" →
                projectClassFiles.foldLeft(0)(_ + _.methods.filter(_.body.isDefined).
                    foldLeft(0)(_ + _.body.get.instructions.count(_ != null))))
        )
    }

    /**
     * Returns the number of (non-synthetic) methods per method length
     * (size in length of the method's code array).
     */
    def projectMethodsLengthDistribution: Map[Int, Int] = {
        val data = Array.fill(UShort.MaxValue) { new AtomicInteger(0) }

        parForeachMethodWithBody(() ⇒ Thread.currentThread().isInterrupted()) { entity ⇒
            val (_ /*source*/ , _ /*classFile*/ , method) = entity
            if (!method.isSynthetic) {
                data(method.body.get.instructions.length).incrementAndGet()
            }
        }
        val result = LinkedHashMap.empty[Int, Int]
        for (i ← 0 until UShort.MaxValue) {
            val count = data(i).get
            if (count > 0)
                result += ((i, count))
        }
        result
    }

    /**
     * Returns the number of (non-synthetic) source elements per method length
     * (size in length of the method's code array). The number of class members of
     * nested classes are also taken into consideration.
     */
    def projectClassMembersPerClassDistribution: Map[Int, (Int, Set[String])] = {
        val data = OpenHashMap.empty[String, Int]

        projectClassFiles.foreach { classFile ⇒
            // we want to collect the size in relation to the source code; 
            //i.e., across all nested classes 
            val count =
                classFile.methods.view.filterNot { _.isSynthetic }.size +
                    classFile.fields.view.filterNot { _.isSynthetic }.size

            var key = classFile.thisType.toJava
            if (classFile.isInnerClass) {
                val index = key.indexOf('$')
                if (index >= 0) {
                    key = key.substring(0, index)
                }
            }
            data.update(key, data.getOrElse(key, 0) + count + 1 /*+1 for the inner class*/ )

        }

        var result = SortedMap.empty[Int, (Int, Set[String])]
        for ((typeName, membersCount) ← data) {
            val (count, typeNames) = result.getOrElse(membersCount, (0, Set.empty[String]))
            result += ((membersCount, (count + 1, typeNames + typeName)))
        }
        result
    }

    /**
     * Returns all available `ClassFile` objects for the given `objectTypes` that
     * pass the given `filter`. `ObjectType`s for which no `ClassFile` is available
     * are ignored.
     */
    def lookupClassFiles(
        objectTypes: Traversable[ObjectType]
    )(
        filter: ClassFile ⇒ Boolean
    ): Traversable[ClassFile] = {
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
            val pi = thisProjectInformation.get(i)
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
     * @note (Development Time)
     * 		Every analysis using [[ProjectInformationKey]]s must list '''All
     * 		requirements; failing to specify a requirement can end up in a deadlock.'''
     *
     * @see [[ProjectInformationKey]] for further information.
     */
    def get[T <: AnyRef](pik: ProjectInformationKey[T]): T = {
        val pikUId = pik.uniqueId

        def derive(projectInformation: AtomicReferenceArray[AnyRef]): T =
            /* calls are externally synchronized! */ {
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
                    // It may be the case that the underlying array was replaced!
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
     * is available. If the information is not (yet) available, the information
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

    // ----------------------------------------------------------------------------------
    //
    // FINALIZATION
    //
    // ----------------------------------------------------------------------------------

    /**
     * Unregisters this project from the OPALLogger and then calls `super.finalize`.
     */
    override protected def finalize(): Unit = {
        OPALLogger.debug("project", "finalized ("+logContext+")")
        OPALLogger.unregister(logContext)

        super.finalize()
    }
}

/**
 * Definition of factory methods to create [[Project]]s.
 *
 * @author Michael Eichberg
 */
object Project {

    private[this] def cache = new BytecodeInstructionsCache

    lazy val Java8ClassFileReader = new Java8FrameworkWithCaching(cache)

    lazy val Java8LibraryClassFileReader = new Java8LibraryFrameworkWithCaching(cache)

    /**
     * Given a reference to a class file, jar file or a folder containing jar and class
     * files, all class files will be loaded and a project will be returned.
     *
     * The global logger will be used for logging messages.
     */
    def apply(file: File): Project[URL] = {
        Project.apply(file, OPALLogger.globalLogger())
    }

    def apply(file: File, projectLogger: OPALLogger): Project[URL] = {
        apply(Java8ClassFileReader.ClassFiles(file), projectLogger = projectLogger)
    }

    def apply[Source](
        projectClassFilesWithSources: Traversable[(ClassFile, Source)]
    ): Project[Source] = {
        Project.apply[Source](
            projectClassFilesWithSources,
            projectLogger = OPALLogger.globalLogger()
        )
    }

    def apply[Source](
        projectClassFilesWithSources: Traversable[(ClassFile, Source)],
        projectLogger:                OPALLogger
    ): Project[Source] = {
        Project.apply[Source](
            projectClassFilesWithSources,
            Traversable.empty,
            virtualClassFiles = Traversable.empty
        )(
            projectLogger = projectLogger
        )
    }

    def apply(
        projectFile: File,
        libraryFile: File
    ): Project[URL] = {
        apply(
            Java8ClassFileReader.ClassFiles(projectFile),
            Java8LibraryClassFileReader.ClassFiles(libraryFile),
            virtualClassFiles = Traversable.empty
        )
    }

    def apply(
        projectFiles: Array[File],
        libraryFiles: Array[File]
    ): Project[URL] = {
        apply(
            Java8ClassFileReader.AllClassFiles(projectFiles),
            Java8LibraryClassFileReader.AllClassFiles(libraryFiles),
            virtualClassFiles = Traversable.empty
        )
    }

    def extend(project: Project[URL], file: File): Project[URL] = {
        project.extend(Java8ClassFileReader.ClassFiles(file))
    }

    /**
     * Creates a new `Project` that consists of the source files of the previous
     * project and the newly given source files.
     */
    def extend[Source](
        project:                      Project[Source],
        projectClassFilesWithSources: Iterable[(ClassFile, Source)],
        libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = Iterable.empty
    ): Project[Source] = {

        apply(
            project.projectClassFilesWithSources ++ projectClassFilesWithSources,
            project.libraryClassFilesWithSources ++ libraryClassFilesWithSources,
            virtualClassFiles = Traversable.empty
        )(
                config = project.config,
                projectLogger = OPALLogger.logger(project.logContext.successor)
            )
    }

    def apply[Source](
        projectClassFilesWithSources: Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources: Traversable[(ClassFile, Source)]
    ): Project[Source] = {
        Project.apply[Source](
            projectClassFilesWithSources,
            libraryClassFilesWithSources,
            virtualClassFiles = Traversable.empty
        )
    }

    /**
     * Creates a new `Project` that consists of the source files of the previous
     * project and uses the (new) configuration. The old project
     * configuration is by default used as fallback, so not all values have to be updated.
     */
    def recreate[Source](
        project:                Project[Source],
        config:                 Config,
        useOldConfigAsFallback: Boolean         = true
    ) = {

        apply(
            project.projectClassFilesWithSources,
            project.libraryClassFilesWithSources,
            virtualClassFiles = Traversable.empty
        )(
            if (useOldConfigAsFallback)
                config.withFallback(project.config)
            else
                config,
            projectLogger = OPALLogger.logger(project.logContext.successor)
        )
    }

    /**
     * The type of the function that is called if an inconsistent project is detected.
     */
    type HandleInconsistenProject = (LogContext, InconsistentProjectException) ⇒ Unit

    /**
     * This default handler just "logs" inconsistent project exceptions at the
     * [[org.opalj.log.Warn]] level.
     */
    def defaultHandlerForInconsistentProjects(
        logContext: LogContext,
        ex:         InconsistentProjectException
    ): Unit = {

        OPALLogger.log(Warn("project configuration", ex.message))(logContext)

    }

    /**
     * Creates a new Project.
     *
     * @param classFiles The list of class files of this project that are considered
     *    to belong to the application/library that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     *
     * @param libraryClassFiles The list of class files of this project that make up
     *    the libraries used by the project that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     *
     * @param virtualClassFiles A list of virtual class files that have no direct
     *      representation in the project.
     * 	    Such declarations are created, e.g., to handle `invokedynamic`
     *      instructions.
     *      '''In general, such class files should be added using
     *      `projectClassFilesWithSources` and the `Source` should be the file that
     *      was the reason for the creation of this additional `ClassFile`.'''
     *      [Thread Safety] The underlying data structure has to support concurrent access.
     *
     * @param handleInconsistentProject A function that is called back if the project
     *      is not consistent. The default behavior
     *      ([[defaultHandlerForInconsistentProjects]]) is to write a warning
     *      message to the console. Alternatively it is possible to throw the given
     *      exception to cancel the loading of the project (which is the only
     *      meaningful option for several advanced analyses.)
     */
    def apply[Source](
        projectClassFilesWithSources: Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources: Traversable[(ClassFile, Source)],
        virtualClassFiles:            Traversable[ClassFile]           = Traversable.empty,
        handleInconsistentProject:    HandleInconsistenProject         = defaultHandlerForInconsistentProjects
    )(
        implicit
        config:        Config     = ConfigFactory.load(),
        projectLogger: OPALLogger = OPALLogger.globalLogger()
    ): Project[Source] = {

        implicit val logContext = new DefaultLogContext()
        OPALLogger.register(logContext, projectLogger)

        try {
            import scala.collection.mutable.{Set, Map}
            import scala.concurrent.{Future, Await, ExecutionContext}
            import scala.concurrent.duration.Duration
            import ExecutionContext.Implicits.global

            val classHierarchyFuture: Future[ClassHierarchy] = Future {
                ClassHierarchy(
                    projectClassFilesWithSources.view.map(_._1) ++
                        libraryClassFilesWithSources.view.map(_._1) ++
                        virtualClassFiles
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

            var codeSize: Long = 0l

            val methodToClassFile = AnyRefMap.empty[Method, ClassFile]
            val fieldToClassFile = AnyRefMap.empty[Field, ClassFile]
            val objectTypeToClassFile = OpenHashMap.empty[ObjectType, ClassFile]
            val sources = OpenHashMap.empty[ObjectType, Source]

            def processProjectClassFile(
                classFile: ClassFile,
                source:    Option[Source]
            ): Unit = {
                val projectType = classFile.thisType
                if (projectTypes.contains(projectType)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"${projectType.toJava} is defined by multiple class files: "+
                                sources.get(projectType).getOrElse("<VIRTUAL>")+" and "+
                                source.map(_.toString).getOrElse("<VIRTUAL>")+
                                "; keeping the first one."
                        )
                    )
                } else {
                    projectTypes += projectType
                    projectClassFiles = classFile :: projectClassFiles
                    projectClassFilesCount += 1
                    for (method ← classFile.methods) {
                        projectMethodsCount += 1
                        methodToClassFile.put(method, classFile)
                        method.body.foreach(codeSize += _.instructions.size)
                    }
                    for (field ← classFile.fields) {
                        projectFieldsCount += 1
                        fieldToClassFile.put(field, classFile)
                    }
                    objectTypeToClassFile.put(projectType, classFile)
                    source.foreach(sources.put(classFile.thisType, _))
                }
            }

            for ((classFile, source) ← projectClassFilesWithSources) {
                processProjectClassFile(classFile, Some(source))
            }

            for (classFile ← virtualClassFiles) {
                processProjectClassFile(classFile, None)
            }

            // The Set `libraryTypes` is only used to improve the identification of
            // inconsistent projects while loading libraries
            val libraryTypes = Set.empty[ObjectType]
            for ((libClassFile, source) ← libraryClassFilesWithSources) {
                val libraryType = libClassFile.thisType
                if (projectTypes.contains(libClassFile.thisType)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"${libraryType.toJava} is defined by the project and a library: "+
                                sources.get(libraryType).getOrElse("<VIRTUAL>")+" and "+
                                source.toString+"; keeping the project class file."
                        )
                    )
                } else if (libraryTypes.contains(libraryType)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"${libraryType.toJava} is defined multiple times in the project's lbraries: "+
                                sources.get(libraryType).getOrElse("<VIRTUAL>")+" and "+
                                source.toString+"; keeping the first one."
                        )
                    )
                } else {
                    libraryClassFiles = libClassFile :: libraryClassFiles
                    libraryTypes += libraryType
                    libraryClassFilesCount += 1
                    for (method ← libClassFile.methods) {
                        libraryMethodsCount += 1
                        methodToClassFile.put(method, libClassFile)
                        method.body.foreach(codeSize += _.instructions.size)
                    }
                    for (field ← libClassFile.fields) {
                        libraryFieldsCount += 1
                        fieldToClassFile.put(field, libClassFile)
                    }
                    objectTypeToClassFile.put(libraryType, libClassFile)
                    sources.put(libraryType, source)
                }
            }

            fieldToClassFile.repack()
            methodToClassFile.repack()

            val methodsSortedBySize =
                methodToClassFile.keysIterator.filter(_.body.isDefined).toList.sortWith { (m1, m2) ⇒
                    m1.body.get.instructions.size > m2.body.get.instructions.size
                }.toArray

            val methodsSortedBySizeWithClassFileAndSource =
                methodToClassFile.filter(_._1.body.isDefined).toList.sortWith { (v1, v2) ⇒
                    v1._1.body.get.instructions.size > v2._1.body.get.instructions.size
                }.map(e ⇒ (sources(e._2.thisType), e._2, e._1)).toArray

            new Project(
                projectClassFiles.toArray,
                libraryClassFiles.toArray,
                methodsSortedBySize,
                projectTypes,
                fieldToClassFile,
                methodToClassFile,
                objectTypeToClassFile,
                sources,
                methodsSortedBySizeWithClassFileAndSource,
                projectClassFilesCount,
                projectMethodsCount,
                projectFieldsCount,
                libraryClassFilesCount,
                libraryMethodsCount,
                libraryFieldsCount,
                codeSize,
                Await.result(classHierarchyFuture, Duration.Inf),
                AnalysisModes.withName(config.as[String](AnalysisMode.ConfigKey))
            )
        } catch {
            case t: Throwable ⇒
                OPALLogger.unregister(logContext)
                throw t
        }
    }
}
