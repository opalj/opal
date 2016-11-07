/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
import java.util.Arrays.{sort => sortArray}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicIntegerArray

import scala.collection.JavaConverters._
import scala.collection.Set
import scala.collection.Map
import scala.collection.SortedMap
import scala.collection.immutable
import scala.collection.mutable.{AnyRefMap, OpenHashMap}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.concurrent.Tasks
import org.opalj.concurrent.OPALExecutionContext
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.parForeachArrayElement
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.DefaultLogContext
import org.opalj.log.Error
import org.opalj.log.GlobalLogContext
import org.opalj.collection.immutable.ConstArray
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithLambdaExpressionsSupportAndCaching
import org.opalj.br.reader.Java9LibraryFramework
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEINTERFACE

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
 *     One of the most important project information keys is the
 *     `SourceElementsPropertyStoreKey` which gives access to the property store.
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
 * @tparam  Source The type of the source of the class file. E.g., a `URL`, a `File`,
 *          a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *          presenting users meaningful messages w.r.t. the location of issues.
 *          We abstract over the type of the resource to facilitate the embedding in existing
 *          tools such as IDEs. E.g., in Eclipse `IResource`'s are used to identify the
 *          location of a resource (e.g., a source or class file.)
 *
 * @param   logContext The logging context associated with this project. Using the logging
 *          context after the project is no longer referenced (garbage collected) is not
 *          possible.
 *
 * @param   libraryClassFilesAreInterfacesOnly If `true` then only the public interface
 *         of the methods of the library's classes is available.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
class Project[Source] private (
        private[this] val projectClassFiles:          Array[ClassFile],
        private[this] val libraryClassFiles:          Array[ClassFile],
        private[this] val methodsWithBody:            Array[Method], // methods with bodies sorted by size
        private[this] val projectTypes:               Set[ObjectType], // the types defined by the class files belonging to the project's code
        private[this] val fieldToClassFile:           AnyRefMap[Field, ClassFile],
        private[this] val methodToClassFile:          AnyRefMap[Method, ClassFile],
        private[this] val objectTypeToClassFile:      OpenHashMap[ObjectType, ClassFile],
        private[this] val sources:                    OpenHashMap[ObjectType, Source],
        private[this] val methodsWithBodyAndContext:  Array[MethodInfo[Source]], // the concrete methods, sorted by size in descending order
        final val projectClassFilesCount:             Int,
        final val projectMethodsCount:                Int,
        final val projectFieldsCount:                 Int,
        final val libraryClassFilesCount:             Int,
        final val libraryMethodsCount:                Int,
        final val libraryFieldsCount:                 Int,
        final val codeSize:                           Long,
        final val classHierarchy:                     ClassHierarchy,
        final val analysisMode:                       AnalysisMode,
        final val libraryClassFilesAreInterfacesOnly: Boolean
)(
        implicit
        final val logContext: LogContext,
        final val config:     Config
) extends ProjectLike {

    private[this] final implicit val thisProject: this.type = this

    assert(
        !libraryClassFilesAreInterfacesOnly || libraryClassFiles.forall(_.methods.forall(_.body.isEmpty)),
        "the library's methods contain bodies though libraryClassFilesAreInterfacesOnly is true"
    )

    /* ------------------------------------------------------------------------------------------ *\
    |                                                                                              |
    |                                                                                              |
    |                                     PROJECT STATE                                            |
    |                                                                                              |
    |                                                                                              |
    \* ------------------------------------------------------------------------------------------ */

    final val ObjectClassFile = classFile(ObjectType.Object)

    final val MethodHandleClassFile = classFile(ObjectType.MethodHandle)

    final val MethodHandleSubtypes = {
        classHierarchy.allSubtypes(ObjectType.MethodHandle, reflexive = true)
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

    final val allProjectClassFiles: Iterable[ClassFile] = projectClassFiles.toIterable

    final val allLibraryClassFiles: Iterable[ClassFile] = libraryClassFiles.toIterable

    final val allClassFiles: Iterable[ClassFile] = allProjectClassFiles ++ allLibraryClassFiles

    final val virtualMethodsCount: Int = {
        methodToClassFile.keysIterator.count(m ⇒ m.isVirtualMethodDeclaration)
    }

    final val instanceMethods: Map[ObjectType, ConstArray[MethodDeclarationContext]] = time {

        // IMPROVE Instead of an Array/Chain use a sorted trie (set) or something similar which is always sorted.

        // IDEA
        // Process the type hierarchy starting with the root type(s) to ensure that all method
        // information about all super types is available (already stored in instanceMethods)
        // when we process the subtype. If not all information is already available, which
        // can happen in the following case if the processing of C would be scheduled before B:
        //      interface A; interface B extends A; interface C extends A, B,
        // we postpone the processing of C until the information is available.

        val methods: ConcurrentHashMap[ObjectType, Chain[MethodDeclarationContext]] = {
            new ConcurrentHashMap(ObjectType.objectTypesCount)
        }

        /* Returns `true` if the potentially available information is actually available. */
        @inline def isAvailable(
            objectType: ObjectType,
            methods:    Chain[MethodDeclarationContext]
        ): Boolean = {
            (methods ne null) || !objectTypeToClassFile.contains(objectType)
        }

        def computeDefinedMethods(tasks: Tasks[ObjectType], objectType: ObjectType): Unit = {
            // Due to the fact that we may inherit from multiple interfaces,
            // the computation may have been scheduled multiple times.
            if (methods.get(objectType) ne null)
                return ;

            var inheritedClassMethods: Chain[MethodDeclarationContext] = null
            val superclassType = classHierarchy.superclassType(objectType)
            if (superclassType.isDefined) {
                val theSuperclassType = superclassType.get
                val superclassTypeMethods = methods.get(theSuperclassType)
                if (!isAvailable(theSuperclassType, superclassTypeMethods)) {
                    // let's postpone the processing of this object type
                    // because we will get some result in the future
                    tasks.submit(objectType)
                    return ;
                }
                inheritedClassMethods = superclassTypeMethods
            }
            if (inheritedClassMethods eq null) {
                inheritedClassMethods = Naught
            }

            var inheritedInterfacesMethods: Chain[Chain[MethodDeclarationContext]] = Naught
            for {
                superinterfaceTypes ← classHierarchy.superinterfaceTypes(objectType)
                superinterfaceType ← superinterfaceTypes
                superinterfaceTypeMethods = methods.get(superinterfaceType)
            } {
                if (!isAvailable(superinterfaceType, superinterfaceTypeMethods)) {
                    tasks.submit(objectType)
                    return ;
                }
                if ((superinterfaceTypeMethods ne null) && superinterfaceTypeMethods.nonEmpty) {
                    inheritedInterfacesMethods :&:= superinterfaceTypeMethods
                }
            }

            // When we reach this point, we have collected all methods inherited by the
            // current type.

            // We now have to select the most maximally specific methods, recall that:
            //  -   methods defined by a class have precedence over concrete methods defined
            //      by interfaces (e.g., default methods).
            //  -   we assume that the project is valid; i.e., there is
            //      always at most one maximally specific method and if not, then
            //      the subclass resolves the conflict by defining the method.
            var definedMethods: Chain[MethodDeclarationContext] = inheritedClassMethods
            for {
                inheritedInterfaceMethods ← inheritedInterfacesMethods
                inheritedInterfaceMethod ← inheritedInterfaceMethods
            } {
                // The relevant interface methods are public, hence, the package
                // name is not relevant!
                if (!definedMethods.exists { definedMethod ⇒
                    definedMethod.descriptor == inheritedInterfaceMethod.descriptor &&
                        definedMethod.name == inheritedInterfaceMethod.name

                })
                    definedMethods :&:= inheritedInterfaceMethod
            }

            classFile(objectType) match {
                case Some(classFile) ⇒
                    for {
                        declaredMethod ← classFile.methods
                        if declaredMethod.isVirtualMethodDeclaration
                        declaredMethodContext = MethodDeclarationContext(declaredMethod, classFile)
                    } {
                        // We have to filter multiple methods when we inherit (w.r.t. the visibility)
                        // multiple conflicting methods!
                        definedMethods = definedMethods.filterNot(declaredMethodContext.directlyOverrides)

                        // Recall that it is possible to make a method "abstract" again...
                        if (declaredMethod.isNotAbstract) {
                            definedMethods :&:= declaredMethodContext
                        }
                    }
                case None ⇒
                // this point is only reached in case of a rather incomplete projects...
            }
            methods.put(objectType, definedMethods)
            classHierarchy.foreachDirectSubtypeOf(objectType)(tasks.submit)
        }

        val tasks = Tasks[ObjectType](computeDefinedMethods)(OPALExecutionContext)
        classHierarchy.rootTypes foreach { t ⇒ tasks.submit(t) }
        val exceptions = tasks.join()
        exceptions foreach { e ⇒
            OPALLogger.error("project configuration", "computing the defined methods failed", e)
        }

        val result = new AnyRefMap[ObjectType, ConstArray[MethodDeclarationContext]](methods.size)
        methods.asScala.foreach { e ⇒
            val (objectType, methods) = e
            val sortedMethods = methods.toArray
            sortArray(sortedMethods, MethodDeclarationContextOrdering)
            result.+=(objectType, ConstArray(sortedMethods))
        }
        result.repack
        result
        //new AnyRefMap[ObjectType, Chain[MethodDeclarationContext]](methods.size) ++ methods.asScala
    } { t ⇒
        OPALLogger.info("project setup", s"computing defined methods took ${t.toSeconds}")
    }

    /**
     * Returns for a given virtual method the set of all non-abstract virtual methods which
     * overrides it.
     *
     * This method takes the visibility of the methods and the defining context into consideration.
     *
     * @see     [[Method]]`.isVirtualMethodDeclaration` for further details.
     * @note    The map only contains those methods which have at least one concrete
     *          implementation.
     */
    final val overridingMethods: Map[Method, immutable.Set[Method]] = time {
        // IDEA
        // 0.   We start with the leaf nodes of the class hierarchy and store for each method
        //      the set of overriding methods (recall that the overrides relation is reflexive).
        //      Hence, initially the set contains the method it self.
        //
        // 1.   After that the direct superclass is scheduled to be analyzed if all subclasses
        //      are analyzed. The superclass then tests for each overridable method if it is
        //      overridden in the sublcasses and, if so, looks up the respective sets of overriding
        //      methods and joins them.
        //      A method is overridden by a subclass if the set of instance methods of the
        //      subclass does not contain the super class' method.
        //
        // 2.   Continue with 1.

        // Stores foreach type the number of subtypes that still need to be processed.
        val subtypesToProcessCounts = new AtomicIntegerArray(ObjectType.objectTypesCount)
        classHierarchy.foreachKnownType { objectType ⇒
            val oid = objectType.id
            subtypesToProcessCounts.set(oid, classHierarchy.directSubtypesCount(oid))
        }

        val methods = new ConcurrentHashMap[Method, immutable.HashSet[Method]](virtualMethodsCount)

        def computeOverridingMethods(tasks: Tasks[ObjectType], objectType: ObjectType): Unit = {
            val declaredMethodPackageName = objectType.packageName

            // If we don't know anything about the methods, we just do nothing;
            // instanceMethods will also just reuse the information derived from the superclasses.
            try {
                for {
                    cf ← classFile(objectType)
                    declaredMethod ← cf.methods
                    if declaredMethod.isVirtualMethodDeclaration
                } {
                    if (declaredMethod.isFinal) { //... the method is necessarily not abstract...
                        methods.put(declaredMethod, immutable.HashSet(declaredMethod))
                    } else {
                        var overridingMethods = immutable.HashSet.empty[Method]
                        // let's join the results of all subtypes
                        classHierarchy.foreachSubtypeCF(objectType) { subtypeClassFile ⇒
                            subtypeClassFile.findDirectlyOverridingMethod(
                                declaredMethodPackageName,
                                declaredMethod
                            ) match {
                                case None ⇒ true
                                case Some(overridingMethod) ⇒
                                    if (overridingMethods.isEmpty) {
                                        overridingMethods = methods.get(overridingMethod)
                                    } else {
                                        overridingMethods ++= methods.get(overridingMethod)
                                    }
                                    false // we don't have to analyze subsequent subtypes.
                            }
                        }

                        if (declaredMethod.isNotAbstract) overridingMethods += declaredMethod

                        methods.put(declaredMethod, overridingMethods)
                    }
                }
            } finally {
                // The try-finally is a safety net to ensure that this method at least
                // terminates and that exceptions can be reported!
                classHierarchy.foreachDirectSupertype(objectType) { supertype ⇒
                    if (subtypesToProcessCounts.decrementAndGet(supertype.id) == 0) {
                        tasks.submit(supertype)
                    }
                }
            }
        }

        val tasks = Tasks[ObjectType](computeOverridingMethods)(OPALExecutionContext)
        classHierarchy.leafTypes foreach { t ⇒ tasks.submit(t) }
        val exceptions = tasks.join()
        exceptions foreach { e ⇒
            OPALLogger.error("project configuration", "computing the overriding methods failed", e)
        }

        val result = new AnyRefMap[Method, immutable.Set[Method]](methods.size)
        result ++= methods.asScala
        result.repack
        result
    } { t ⇒
        OPALLogger.info("project setup", s"computing overriding information took ${t.toSeconds}")
    }

    OPALLogger.debug("progress", s"project created (${logContext.logContextId})")

    /* ------------------------------------------------------------------------------------------ *\
    |                                                                                              |
    |                                                                                              |
    |                                    INSTANCE METHODS                                          |
    |                                                                                              |
    |                                                                                              |
    \* ------------------------------------------------------------------------------------------ */

    /**
     * Creates a new `Project` which also includes the given class files.
     */
    def extend(projectClassFilesWithSources: Iterable[(ClassFile, Source)]): Project[Source] = {
        Project.extend[Source](this, projectClassFilesWithSources)
    }

    /**
     * Creates a new `Project` which also includes this as well as the other project's
     * class files.
     */
    def extend(other: Project[Source]): Project[Source] = {
        if (this.analysisMode != other.analysisMode) {
            throw new IllegalArgumentException("the projects have different analysis modes");
        }

        if (this.libraryClassFilesAreInterfacesOnly != other.libraryClassFilesAreInterfacesOnly) {
            throw new IllegalArgumentException("the projects' libraries are loaded differently");
        }

        val otherClassFiles = other.projectClassFilesWithSources
        val otherLibraryClassFiles = other.libraryClassFilesWithSources
        Project.extend[Source](this, otherClassFiles, otherLibraryClassFiles)
    }

    /**
     * The number of all source elements (fields, methods and class files).
     */
    def sourceElementsCount = fieldsCount + methodsCount + classFilesCount

    /**
     * Returns a new `Iterable` over all source elements of the project. The set
     * of all source elements consists of (in this order): all methods + all fields +
     * all class files.
     */
    def allSourceElements: Iterable[SourceElement] = allMethods ++ allFields ++ allClassFiles

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
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        doParForeachClassFile(this.projectClassFiles, isInterrupted)(f)
    }

    def parForeachLibraryClassFile[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: ClassFile ⇒ T
    ): List[Throwable] = {
        doParForeachClassFile(this.libraryClassFiles, isInterrupted)(f)
    }

    def parForeachClassFile[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
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
        objectTypes.flatMap(ot ⇒ classFile(ot)).flatMap(cf ⇒ cf.methods.map(m ⇒ m.name)).toSet
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
     * Returns the set of all project packages that contain at least one class.
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
     * Returns the set of all library packages that contain at least one class.
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

    def allMethodsWithBody: Iterable[Method] = this.methodsWithBody.toIterable

    def allMethodsWithBodyWithContext: Iterable[MethodInfo[Source]] = {
        this.methodsWithBodyAndContext.toIterable
    }

    /**
     * Iterable of all methods along along with its defining class file.
     */
    def allMethodsWithContext: Iterable[(Method, ClassFile)] = this.methodToClassFile.toIterable

    /**
     * Iterates over all methods with a body in parallel.
     *
     * This method maximizes utilization by allowing each thread to pick the next
     * unanalyzed method as soon as the thread has finished analyzing the previous method.
     * I.e., each thread is not assigned a fixed batch of methods. Additionally, the
     * methods are analyzed ordered by their length (longest first).
     */
    def parForeachMethodWithBody[T](
        isInterrupted:        () ⇒ Boolean = defaultIsInterrupted,
        parallelizationLevel: Int          = NumberOfThreadsForCPUBoundTasks
    )(
        f: MethodInfo[Source] ⇒ T
    ): List[Throwable] = {
        val concreteMethods = this.methodsWithBodyAndContext
        if (concreteMethods.length == 0)
            return Nil;

        parForeachArrayElement(concreteMethods, parallelizationLevel, isInterrupted)(f)
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

    /**
     * Distributes all classes which define methods with bodies across a given number of
     * groups. Afterwards these groups can, e.g., be processed in parallel.
     */
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

    /**
     * All methods defined by this project as well as the visible methods defined by the libraries.
     */
    def allMethods: Iterable[Method] = methodToClassFile.keys

    /**
     * All fields defined by this project as well as the visible fields defined by the libraries.
     */
    def allFields: Iterable[Field] = fieldToClassFile.keys

    def projectClassFilesWithSources: Iterable[(ClassFile, Source)] = {
        projectClassFiles.view.map { classFile ⇒ (classFile, sources(classFile.thisType)) }
    }

    def libraryClassFilesWithSources: Iterable[(ClassFile, Source)] = {
        libraryClassFiles.view.map { classFile ⇒ (classFile, sources(classFile.thisType)) }
    }

    def classFilesWithSources: Iterable[(ClassFile, Source)] = {
        projectClassFilesWithSources ++ libraryClassFilesWithSources
    }

    /**
     * Returns `true` if the given class file belongs to the library part of the project.
     * This is only the case if the class file was explicitly identified as being
     * part of the library. By default all class files are considered to belong to the
     * code base that will be analyzed.
     */
    def isLibraryType(classFile: ClassFile): Boolean = isLibraryType(classFile.thisType)

    /**
     * Returns `true` if the given type belongs to the library part of the project.
     * This is generally the case if no class file was loaded for the given type.
     */
    def isLibraryType(objectType: ObjectType): Boolean = !projectTypes.contains(objectType)

    /**
     * Returns the source (for example, a `File` object or `URL` object) from which
     * the class file was loaded that defines the given object type, if any.
     *
     * @param objectType Some object type.
     */
    def source(objectType: ObjectType): Option[Source] = sources.get(objectType)
    def source(classFile: ClassFile): Option[Source] = source(classFile.thisType)

    /**
     * Returns the class file that defines the given `objectType`; if any.
     *
     * @param objectType Some object type.
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
     * @note This method is intended to be used by Java projects that want to interact with OPAL.
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
                projectClassFiles.foldLeft(0)(_ + _.methods.view.filter(_.body.isDefined).
                    foldLeft(0)(_ + _.body.get.instructions.count(_ != null))))
        )
    }

    /**
     * Returns the (number of) (non-synthetic) methods per method length
     * (size in length of the method's code array).
     */
    def projectMethodsLengthDistribution: Map[Int, Set[Method]] = {
        val nonSyntheticMethodsWithBody: Traversable[Method] = methodsWithBody.view.filterNot(_.isSynthetic)
        val data = SortedMap.empty[Int, Set[Method]]
        nonSyntheticMethodsWithBody.foldLeft(data) { (data, method) ⇒
            val methodLength = method.body.get.instructions.length
            val methods = data.getOrElse(methodLength, Set.empty[Method])
            data + ((methodLength, methods + method))
        }
    }

    /**
     * Returns the number of (non-synthetic) fields and methods per class file.
     * The number of class members of nested classes is also taken into consideration.
     * I.e., the map's key identifies the category and the value is a pair where the first value
     * is the count and the value is the names of the source elements.
     *
     * The count can be higher than the set of names of class members due to method overloading.
     */
    def projectClassMembersPerClassDistribution: Map[Int, (Int, Set[String])] = {
        val data = AnyRefMap.empty[String, Int]

        projectClassFiles foreach { classFile ⇒
            // we want to collect the size in relation to the source code;
            //i.e., across all nested classes
            val count =
                classFile.methods.iterator.filterNot(_.isSynthetic).size +
                    classFile.fields.iterator.filterNot(_.isSynthetic).size

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
        classFileFilter: ClassFile ⇒ Boolean
    ): Traversable[ClassFile] = {
        objectTypes.view.flatMap(classFile(_)) filter (classFileFilter)
    }

    override def toString: String = {
        val classDescriptions =
            sources map { (entry) ⇒
                val (ot, source) = entry
                ot.toJava+" « "+source.toString
            }

        classDescriptions.mkString(
            "Project("+
                "\n\tanalysisMode="+analysisMode+
                "\n\tlibraryClassFilesAreInterfacesOnly="+libraryClassFilesAreInterfacesOnly+
                "\n\t",
            "\n\t",
            "\n)"
        )
    }

    // --------------------------------------------------------------------------------------------
    //
    //    CODE TO MAKE IT POSSIBLE TO ATTACH SOME INFORMATION TO A PROJECT (ON DEMAND)
    //
    // --------------------------------------------------------------------------------------------

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
        val projectInformation = this.projectInformation
        for (i ← (0 until projectInformation.length())) {
            val pi = projectInformation.get(i)
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
     * @note    (Development Time)
     *          Every analysis using [[ProjectInformationKey]]s must list '''All
     *          requirements; failing to specify a requirement can end up in a deadlock.'''
     *
     * @see     [[ProjectInformationKey]] for further information.
     */
    def get[T <: AnyRef](pik: ProjectInformationKey[T]): T = {
        val pikUId = pik.uniqueId

        /* synchronization is done by the caller! */
        def derive(projectInformation: AtomicReferenceArray[AnyRef]): T = {
            var className = pik.getClass().getSimpleName()
            if (className.endsWith("Key"))
                className = className.substring(0, className.length - 3)
            else if (className.endsWith("Key$"))
                className = className.substring(0, className.length - 4)

            for (requiredProjectInformationKey ← pik.getRequirements) {
                get(requiredProjectInformationKey)
            }
            val pi = time {
                pik.doCompute(this)
            } { t ⇒ OPALLogger.info("project", s"initialization of $className took ${t.toSeconds}") }
            projectInformation.set(pikUId, pi)
            pi
        }

        val projectInformation = this.projectInformation
        if (pikUId < projectInformation.length()) {
            val pi = projectInformation.get(pikUId)
            if (pi ne null) {
                pi.asInstanceOf[T]
            } else {
                this.synchronized {
                    // It may be the case that the underlying array was replaced!
                    val projectInformation = this.projectInformation
                    // double-checked locking (works with Java >=6)
                    val pi = projectInformation.get(pikUId)
                    if (pi ne null) {
                        pi.asInstanceOf[T]
                    } else {
                        derive(projectInformation)
                    }
                }
            }
        } else {
            // We have to synchronize w.r.t. "this" object on write accesses
            // to make sure that we do not loose a concurrent update or
            // derive an information more than once.
            this.synchronized {
                val projectInformation = this.projectInformation
                if (pikUId < projectInformation.length()) {
                    get(pik)
                } else {
                    val newLength = Math.max(projectInformation.length * 2, pikUId * 2)
                    val newProjectInformation = new AtomicReferenceArray[AnyRef](newLength)
                    for (i ← 0 until projectInformation.length()) {
                        newProjectInformation.set(i, projectInformation.get(i))
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
        if (logContext != GlobalLogContext) { OPALLogger.unregister(logContext) }

        super.finalize()
    }
}

/**
 * Definition of factory methods to create [[Project]]s.
 *
 * @author Michael Eichberg
 */
object Project {

    lazy val GlobalConfig = ConfigFactory.load()

    lazy val JavaLibraryClassFileReader = Java9LibraryFramework

    private[this] def cache = new BytecodeInstructionsCache

    def JavaClassFileReader(
        theLogContext: LogContext = GlobalLogContext,
        theConfig:     Config     = GlobalConfig
    ) = {
        // The following makes use of Early Initializers
        class ConfiguredFramework extends {
            override implicit val logContext: LogContext = theLogContext
            override implicit val config: Config = theConfig
        } with Java9FrameworkWithLambdaExpressionsSupportAndCaching(cache)
        new ConfiguredFramework
    }

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
        apply(JavaClassFileReader().ClassFiles(file), projectLogger = projectLogger)
    }

    def apply(file: File, logContext: LogContext, config: Config): Project[URL] = {
        this(
            projectClassFilesWithSources = JavaClassFileReader(logContext, config).ClassFiles(file),
            libraryClassFilesWithSources = Traversable.empty,
            libraryClassFilesAreInterfacesOnly = true,
            virtualClassFiles = Traversable.empty,
            handleInconsistentProject = defaultHandlerForInconsistentProjects,
            config = config,
            logContext
        )
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
            libraryClassFilesAreInterfacesOnly = false /*it actually doesn't matter*/ ,
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
            JavaClassFileReader().ClassFiles(projectFile),
            JavaLibraryClassFileReader.ClassFiles(libraryFile),
            libraryClassFilesAreInterfacesOnly = true,
            virtualClassFiles = Traversable.empty
        )
    }

    def apply(
        projectFiles: Array[File],
        libraryFiles: Array[File]
    ): Project[URL] = {
        apply(
            JavaClassFileReader().AllClassFiles(projectFiles),
            JavaLibraryClassFileReader.AllClassFiles(libraryFiles),
            libraryClassFilesAreInterfacesOnly = true,
            virtualClassFiles = Traversable.empty
        )
    }

    def extend(project: Project[URL], file: File): Project[URL] = {
        project.extend(JavaClassFileReader().ClassFiles(file))
    }

    /**
     * Creates a new `Project` that consists of the class files of the previous
     * project and the newly given class files.
     */
    def extend[Source](
        project:                      Project[Source],
        projectClassFilesWithSources: Iterable[(ClassFile, Source)]
    ): Project[Source] = {

        apply(
            project.projectClassFilesWithSources ++ projectClassFilesWithSources,
            // We cannot ensure that the newly provided class files are loaded in the same way
            // therefore, we do not support extending the set of library class files.
            project.libraryClassFilesWithSources,
            project.libraryClassFilesAreInterfacesOnly,
            virtualClassFiles = Traversable.empty
        )(
                config = project.config,
                projectLogger = OPALLogger.logger(project.logContext.successor)
            )
    }

    /**
     * Creates a new `Project` that consists of the class files of the previous
     * project and the newly given class files.
     */
    private def extend[Source](
        project:                      Project[Source],
        projectClassFilesWithSources: Iterable[(ClassFile, Source)],
        libraryClassFilesWithSources: Iterable[(ClassFile, Source)]
    ): Project[Source] = {

        apply(
            project.projectClassFilesWithSources ++ projectClassFilesWithSources,
            project.libraryClassFilesWithSources ++ libraryClassFilesWithSources,
            project.libraryClassFilesAreInterfacesOnly,
            virtualClassFiles = Traversable.empty
        )(
                project.config,
                OPALLogger.logger(project.logContext.successor)
            )
    }

    def apply[Source](
        projectClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesAreInterfacesOnly: Boolean
    ): Project[Source] = {
        Project.apply[Source](
            projectClassFilesWithSources,
            libraryClassFilesWithSources,
            libraryClassFilesAreInterfacesOnly,
            virtualClassFiles = Traversable.empty
        )
    }

    /**
     * Creates a new `Project` that consists of the source files of the previous
     * project and uses the (new) configuration. The old project
     * configuration is by default used as a fallback, so not all values have to be updated.
     */
    def recreate[Source](
        project:                Project[Source],
        config:                 Config          = ConfigFactory.empty(),
        useOldConfigAsFallback: Boolean         = true
    ) = {
        apply(
            project.projectClassFilesWithSources,
            project.libraryClassFilesWithSources,
            project.libraryClassFilesAreInterfacesOnly,
            virtualClassFiles = Traversable.empty
        )(
            if (useOldConfigAsFallback) config.withFallback(project.config) else config,
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
        OPALLogger.log(ex.severity("project configuration", ex.message))(logContext)
    }

    /**
     * Creates a new Project.
     *
     * @param projectClassFilesWithSources The list of class files of this project that are considered
     *      to belong to the application/library that will be analyzed.
     *      [Thread Safety] The underlying data structure has to support concurrent access.
     *
     * @param libraryClassFilesWithSources The list of class files of this project that make up
     *      the libraries used by the project that will be analyzed.
     *      [Thread Safety] The underlying data structure has to support concurrent access.
     *
     * @param libraryClassFilesAreInterfacesOnly If `true` then only the public interface
     *      and no private methods or method implementations are available. Otherwise,
     *      the libraries are completely loaded.
     *
     * @param virtualClassFiles A list of virtual class files that have no direct
     *      representation in the project.
     *      Such declarations are created, e.g., to handle `invokedynamic`
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
        projectClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesAreInterfacesOnly: Boolean,
        virtualClassFiles:                  Traversable[ClassFile]           = Traversable.empty,
        handleInconsistentProject:          HandleInconsistenProject         = defaultHandlerForInconsistentProjects
    )(
        implicit
        config:        Config     = ConfigFactory.load(),
        projectLogger: OPALLogger = OPALLogger.globalLogger()
    ): Project[Source] = {
        implicit val logContext = new DefaultLogContext()
        OPALLogger.register(logContext, projectLogger)
        this(
            projectClassFilesWithSources,
            libraryClassFilesWithSources,
            libraryClassFilesAreInterfacesOnly,
            virtualClassFiles,
            handleInconsistentProject,
            config,
            logContext
        )
    }

    def apply[Source](
        projectClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesWithSources:       Traversable[(ClassFile, Source)],
        libraryClassFilesAreInterfacesOnly: Boolean,
        virtualClassFiles:                  Traversable[ClassFile],
        handleInconsistentProject:          HandleInconsistenProject,
        config:                             Config,
        logContext:                         LogContext
    ): Project[Source] = time {
        implicit val projectConfig = config
        implicit val projectLogContext = logContext

        try {
            import scala.collection.mutable.Set
            import scala.concurrent.{Future, Await, ExecutionContext}
            import scala.concurrent.duration.Duration
            import ExecutionContext.Implicits.global

            val classHierarchyFuture: Future[ClassHierarchy] = Future {
                val typeHierarchyDefinitions =
                    if (projectClassFilesWithSources.exists(_._1.thisType == ObjectType.Object) ||
                        libraryClassFilesWithSources.exists(_._1.thisType == ObjectType.Object)) {
                        OPALLogger.info("project configuration", "the JDK is part of the analysis")
                        ClassHierarchy.noDefaultTypeHierarchyDefinitions
                    } else {
                        val alternative = "(using the preconfigured type hierarchy (based on Java 7) for classes belonging java.lang)"
                        OPALLogger.info("project configuration", "JDK classes not found"+alternative)
                        ClassHierarchy.defaultTypeHierarchyDefinitions
                    }

                ClassHierarchy(
                    projectClassFilesWithSources.view.map(_._1) ++
                        libraryClassFilesWithSources.view.map(_._1) ++
                        virtualClassFiles,
                    typeHierarchyDefinitions
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

            var codeSize: Long = 0L

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
                            s"${projectType.toJava} is defined by multiple class files:\n\t"+
                                sources.get(projectType).getOrElse("<VIRTUAL>")+" and\n\t"+
                                source.map(_.toString).getOrElse("<VIRTUAL>")+
                                "\n\tkeeping the first one."
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

            val methodsWithBodySortedBySizeWithContext =
                methodToClassFile.view.filter(_._1.body.isDefined).toList.sortWith { (v1, v2) ⇒
                    v1._1.body.get.instructions.length > v2._1.body.get.instructions.length
                }.map(e ⇒ MethodInfo(sources(e._2.thisType), e._2, e._1)).toArray

            val methodsWithBodySortedBySize: Array[Method] =
                methodsWithBodySortedBySizeWithContext.view.map(mi ⇒ mi.method).toArray

            val project = new Project(
                projectClassFiles.toArray,
                libraryClassFiles.toArray,
                methodsWithBodySortedBySize,
                projectTypes,
                fieldToClassFile,
                methodToClassFile,
                objectTypeToClassFile,
                sources,
                methodsWithBodySortedBySizeWithContext,
                projectClassFilesCount,
                projectMethodsCount,
                projectFieldsCount,
                libraryClassFilesCount,
                libraryMethodsCount,
                libraryFieldsCount,
                codeSize,
                Await.result(classHierarchyFuture, Duration.Inf),
                AnalysisModes.withName(config.as[String](AnalysisMode.ConfigKey)),
                libraryClassFilesAreInterfacesOnly
            )

            time {
                val issues = validate(project)
                issues foreach { handleInconsistentProject(logContext, _) }
                OPALLogger.info(
                    "project configuration",
                    s"project validation revealed ${issues.size} significant issues"+
                        (if (issues.size > 0) "; validate the configured libraries for inconsistencies" else "")
                )
            } { t ⇒ OPALLogger.info("project setup", s"validating the project took ${t.toSeconds}") }

            project
        } catch {
            case t: Throwable ⇒
                OPALLogger.unregister(logContext)
                throw t
        }
    } { t ⇒
        OPALLogger.info("project setup", s"creating the project took ${t.toSeconds}")(logContext)
    }

    /**
     * Performs some fundamental validations to make sure that subsequent analyses don't have
     * to deal with completely broken projects!
     */
    private[this] def validate(project: SomeProject): Seq[InconsistentProjectException] = {

        val disclaimer = "(this inconsistency may lead to useless/wrong results)"

        import project.classHierarchy.isInterface

        var exs = List.empty[InconsistentProjectException]
        val exsMutex = new Object
        def addException(ex: InconsistentProjectException): Unit = {
            exsMutex.synchronized { exs = ex :: exs }
        }

        project.parForeachMethodWithBody(() ⇒ Thread.interrupted()) { e ⇒
            val BasicMethodInfo(c: ClassFile, m: Method) = e
            m.body.get.iterate { (pc, instruction) ⇒
                (instruction.opcode: @scala.annotation.switch) match {

                    case NEW.opcode ⇒
                        val objectType = instruction.asInstanceOf[NEW].objectType
                        if (isInterface(objectType).isYes) {
                            val ex = InconsistentProjectException(
                                s"cannot create an instance of interface ${objectType.toJava} in "+
                                    m.toJava(c) + s"pc=$pc $disclaimer",
                                Error
                            )
                            addException(ex)
                        }

                    case INVOKESTATIC.opcode ⇒
                        val invokestatic = instruction.asInstanceOf[INVOKESTATIC]
                        project.lookupMethodDefinition(invokestatic) foreach { m ⇒
                            if (!m.isStatic) {
                                val ex = InconsistentProjectException(
                                    s"static method call $invokestatic of an instance method in "+
                                        m.toJava(c) + s"pc=$pc $disclaimer",
                                    Error
                                )
                                addException(ex)
                            }
                        }

                    case INVOKEVIRTUAL.opcode | INVOKESPECIAL.opcode | INVOKEINTERFACE.opcode ⇒
                        val invocation = instruction.asInstanceOf[MethodInvocationInstruction]
                        project.lookupMethodDefinition(invocation) foreach { m ⇒
                            if (m.isStatic) {
                                val method = invocation.methodDescriptor.toJava(invocation.name)
                                val ex = InconsistentProjectException(
                                    s"instance method call of the static method $method in "+
                                        m.toJava(c) + s"pc=$pc $disclaimer",
                                    Error
                                )
                                addException(ex)
                            }
                        }

                    case _ ⇒ // Nothing special is checked (so far)
                }
            }
        }

        exs
    }
}

case class MethodInfo[Source](
    source:    Source,
    classFile: ClassFile,
    method:    Method
)

object BasicMethodInfo {

    def unapply(methodInfo: MethodInfo[_]): Some[(ClassFile, Method)] = {
        Some((methodInfo.classFile, methodInfo.method))
    }
}
