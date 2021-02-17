/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.annotation.switch
import scala.annotation.tailrec

import java.io.File
import java.lang.ref.SoftReference
import java.net.URL
import java.util.Arrays.{sort ⇒ sortArray}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray

import scala.collection.Map
import scala.collection.Set
import scala.collection.SortedMap
import scala.collection.immutable
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ArrayStack
import scala.collection.mutable.Buffer
import scala.collection.mutable.OpenHashMap

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.log.Error
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.log.StandardLogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.ConstArray
import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.mutable.RefArrayBuffer
import org.opalj.concurrent.ConcurrentExceptions
import org.opalj.concurrent.SequentialTasks
import org.opalj.concurrent.Tasks
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.parForeachArrayElement
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithInvokedynamicSupportAndCaching
import org.opalj.br.reader.Java9LibraryFramework

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
 *     `PropertyStoreKey` which gives access to the property store.
 *
 * ==Thread Safety==
 * This class is thread-safe.
 *
 * ==Prototyping Analyses/Querying Projects==
 * Projects can easily be created and queried using the Scala `REPL`. For example,
 * to create a project, you can use:
 * {{{
 * val project = org.opalj.br.analyses.Project(org.opalj.bytecode.JRELibraryFolder)
 * }}}
 * Now, to determine the number of methods that have at least one parameter of type
 * `int`, you can use:
 * {{{
 * project.methods.filter(_.parameterTypes.exists(_.isIntegerType)).size
 * }}}
 *
 * @tparam Source The type of the source of the class file. E.g., a `URL`, a `File`,
 *         a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *         presenting users meaningful messages w.r.t. the location of issues.
 *         We abstract over the type of the resource to facilitate the embedding in existing
 *         tools such as IDEs. E.g., in Eclipse `IResource`'s are used to identify the
 *         location of a resource (e.g., a source or class file.)
 *
 * @param  logContext The logging context associated with this project. Using the logging
 *         context after the project is no longer referenced (garbage collected) is not
 *         possible.
 *
 * @param classFilesCount The number of classes (including inner and annoymous classes as
 *         well as interfaces, annotations, etc.) defined in libraries and in
 *         the analyzed project.
 *
 * @param methodsCount The number of methods defined in libraries and in the analyzed project.
 *
 * @param fieldsCount The number of fields defined in libraries and in the analyzed project.
 *
 * @param allMethods All methods defined by this project as well as the visible methods
 *                   defined by the libraries.
 * @param allFields All fields defined by this project as well as the reified fields defined
 *                  in libraries.
 * @param allSourceElements `Iterable` over all source elements of the project. The set of all
 *                         source elements consists of (in this order): all methods + all fields
 *                         + all class files.
 *
 * @param libraryClassFilesAreInterfacesOnly If `true` then only the public interfaces
 *         of the methods of the library's classes are available; if `false` all methods and
 *         method bodies are reified.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
class Project[Source] private (
        private[this] val projectModules:             Map[String, ModuleDefinition[Source]], // just contains "module-info" class files
        private[this] val projectClassFiles:          Array[ClassFile], // contains no "module-info" class files
        private[this] val libraryModules:             Map[String, ModuleDefinition[Source]], // just contains "module-info" class files
        private[this] val libraryClassFiles:          Array[ClassFile],
        final val libraryClassFilesAreInterfacesOnly: Boolean,
        private[this] val methodsWithBody:            Array[Method], // methods with bodies sorted by size
        private[this] val methodsWithBodyAndContext:  Array[MethodInfo[Source]], // the concrete methods, sorted by size in descending order
        private[this] val projectTypes:               Set[ObjectType], // the types defined by the class files belonging to the project's code
        private[this] val objectTypeToClassFile:      Map[ObjectType, ClassFile],
        private[this] val sources:                    Map[ObjectType, Source],
        final val projectClassFilesCount:             Int,
        final val projectMethodsCount:                Int,
        final val projectFieldsCount:                 Int,
        final val libraryClassFilesCount:             Int,
        final val libraryMethodsCount:                Int,
        final val libraryFieldsCount:                 Int,
        final val codeSize:                           Long,
        final val MethodHandleSubtypes:               Set[ObjectType],
        final val VarHandleSubtypes:                  Set[ObjectType],
        final val classFilesCount:                    Int,
        final val methodsCount:                       Int,
        final val fieldsCount:                        Int,
        final val allProjectClassFiles:               ConstArray[ClassFile],
        final val allLibraryClassFiles:               ConstArray[ClassFile],
        final val allClassFiles:                      Iterable[ClassFile],
        final val allMethods:                         Iterable[Method],
        final val allFields:                          Iterable[Field],
        final val allSourceElements:                  Iterable[SourceElement],
        final val virtualMethodsCount:                Int,
        final val classHierarchy:                     ClassHierarchy,
        final val instanceMethods:                    Map[ObjectType, ConstArray[MethodDeclarationContext]],
        final val overridingMethods:                  Map[Method, Set[Method]],
        // Note that the referenced array will never shrink!
        @volatile private[this] var projectInformation: AtomicReferenceArray[AnyRef] = new AtomicReferenceArray[AnyRef](32)
)(
        implicit
        final val logContext: LogContext,
        final val config:     Config
) extends ProjectLike {

    /**
     * Returns a shallow clone of this project with an updated log context and (optionally)
     * filtered ProjectInformation objects.
     *
     * @param filterProjectInformation Enables filtering of the ProjectInformation objects
     *          that should be kept when a new Project is created.
     */
    def recreate(
        filterProjectInformation: Int ⇒ Boolean = _ ⇒ false
    ): Project[Source] = this.synchronized {
        // the synchronization is necessary to get exclusive access to "project information".
        val max = projectInformation.length()
        val newProjectInformation = new AtomicReferenceArray[AnyRef](max)
        var i = 0
        while (i < max) {
            if (filterProjectInformation(i)) {
                val pi = projectInformation.get(i)
                if (pi != null) {
                    newProjectInformation.set(i, pi)
                }
            }
            i += 1
        }
        val newLogContext = logContext.successor
        val newClassHierarchy = classHierarchy.updatedLogContext(newLogContext)
        new Project(
            projectModules,
            projectClassFiles,
            libraryModules,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly,
            methodsWithBody,
            methodsWithBodyAndContext,
            projectTypes,
            objectTypeToClassFile,
            sources,
            projectClassFilesCount,
            projectMethodsCount,
            projectFieldsCount,
            libraryClassFilesCount,
            libraryMethodsCount,
            libraryFieldsCount,
            codeSize,
            MethodHandleSubtypes,
            VarHandleSubtypes,
            classFilesCount,
            methodsCount,
            fieldsCount,
            allProjectClassFiles,
            allLibraryClassFiles,
            allClassFiles,
            allMethods,
            allFields,
            allSourceElements,
            virtualMethodsCount,
            newClassHierarchy,
            instanceMethods,
            overridingMethods,
            newProjectInformation
        )(
            newLogContext,
            config
        )
    }

    /* ------------------------------------------------------------------------------------------ *\
    |                                                                                              |
    |                                                                                              |
    |                                     PROJECT STATE                                            |
    |                                                                                              |
    |                                                                                              |
    \* ------------------------------------------------------------------------------------------ */

    final val ObjectClassFile: Option[ClassFile] = classFile(ObjectType.Object)

    final val MethodHandleClassFile: Option[ClassFile] = classFile(ObjectType.MethodHandle)

    final val VarHandleClassFile: Option[ClassFile] = classFile(ObjectType.VarHandle)

    final val allMethodsWithBody: ConstArray[Method] = ConstArray._UNSAFE_from(this.methodsWithBody)

    final val allMethodsWithBodyWithContext: ConstArray[MethodInfo[Source]] = {
        ConstArray._UNSAFE_from(this.methodsWithBodyAndContext)
    }

    /**
     * The set of all classes defined in a specific package.
     */
    // TODO Consider extracting to a ProjectInformationKey
    // TODO Java 9+
    final val classesPerPackage: Map[String, immutable.Set[ClassFile]] = {
        var classesPerPackage = Map.empty[String, RefArrayBuffer[ClassFile]]
        allClassFiles foreach { cf ⇒
            val packageName = cf.thisType.packageName
            val buffer =
                classesPerPackage.getOrElse(packageName, {
                    val buffer = RefArrayBuffer.empty[ClassFile]
                    classesPerPackage = classesPerPackage.updated(packageName, buffer)
                    buffer
                })
            buffer += cf
        }
        classesPerPackage.mapValues(cfs ⇒ cfs.toSet)
    }

    /**
     * Computes the set of all definitive functional interfaces in a top-down fashion.
     *
     * @see Java 8 language specification for details!
     *
     * @return The functional interfaces.
     */
    // TODO Consider extracting to a ProjectInformationKey
    final lazy val functionalInterfaces: UIDSet[ObjectType] = time {

        // Core idea: a subtype is only processed after processing all supertypes;
        // in case of partial type hierarchies it may happen that all known
        // supertypes are processed, but no all...

        // the set of interfaces that are not functional interfaces themselve, but
        // which can be extended.
        var irrelevantInterfaces = UIDSet.empty[ObjectType]
        val functionalInterfaces = AnyRefMap.empty[ObjectType, MethodSignature]
        var otherInterfaces = UIDSet.empty[ObjectType]

        // our worklist/-set; it only contains those interface types for which
        // we have complete supertype information
        val typesToProcess = classHierarchy.rootInterfaceTypes(ArrayStack.empty[ObjectType])

        // the given interface type is either not a functional interface or an interface
        // for which we have not enough information
        def noSAMInterface(interfaceType: ObjectType): Unit = {
            // println("non-functional interface: "+interfaceType.toJava)
            // assert(!irrelevantInterfaces.contains(interfaceType))
            // assert(!functionalInterfaces.contains(interfaceType))

            otherInterfaces += interfaceType
            classHierarchy.foreachSubinterfaceType(interfaceType) { i ⇒
                if (otherInterfaces.contains(i))
                    false
                else {
                    otherInterfaces += i
                    true
                }
            }
        }

        def processSubinterfaces(interfaceType: ObjectType): Unit = {
            classHierarchy.directSubinterfacesOf(interfaceType) foreach { subIType ⇒
                // println("processing subtype: "+subIType.toJava)
                // let's check if the type is potentially relevant
                if (!otherInterfaces.contains(subIType)) {

                    // only add those types for which we have already derived information for all
                    // superinterface types and which are not already classified..
                    if (classHierarchy.superinterfaceTypes(subIType) match {
                        case Some(superinterfaceTypes) ⇒
                            superinterfaceTypes.forall { superSubIType ⇒
                                superSubIType == interfaceType || {
                                    irrelevantInterfaces.contains(superSubIType) ||
                                        functionalInterfaces.contains(superSubIType)
                                }
                            }
                        case None ⇒ throw new UnknownError()
                    }) {
                        // we have all information about all supertypes...
                        typesToProcess.push(subIType)
                    }
                }
            }
        }

        def classifyPotentiallyFunctionalInterface(classFile: ClassFile): Unit = {
            if (!classFile.isInterfaceDeclaration) {
                // This may happen for "broken" projects (which we find, e.g., in case of
                // the JDK/Qualitas Corpus).
                noSAMInterface(classFile.thisType)
                return ;
            }
            val interfaceType = classFile.thisType

            val selectAbstractNonObjectMethods = (m: Method) ⇒ {
                m.isAbstract && (
                    ObjectClassFile.isEmpty /* in case of doubt we keep it ... */ || {
                        // Does not (re)define a method declared by java.lang.Object;
                        // see java.util.Comparator for an example!
                        // From the spec.: ... The definition of functional interface
                        // excludes methods in an interface that are also public methods
                        // in Object.
                        val objectMethod = ObjectClassFile.get.findMethod(m.name, m.descriptor)
                        objectMethod.isEmpty || !objectMethod.get.isPublic
                    }
                )
            }

            val abstractMethods = classFile.methods.filter(selectAbstractNonObjectMethods)
            val abstractMethodsCount = abstractMethods.size
            val isPotentiallyIrrelevant: Boolean = abstractMethodsCount == 0
            val isPotentiallyFunctionalInterface: Boolean = abstractMethodsCount == 1

            if (!isPotentiallyIrrelevant && !isPotentiallyFunctionalInterface) {
                noSAMInterface(interfaceType)
            } else {
                var sharedFunctionalMethod: MethodSignature = null
                if (classFile.interfaceTypes.forall { i ⇒
                    //... forall is "only" used to short-cut the evaluation; in case of
                    // false all relevant state is already updated
                    if (!irrelevantInterfaces.contains(i)) {
                        functionalInterfaces.get(i) match {
                            case Some(potentialFunctionalMethod) ⇒
                                if (sharedFunctionalMethod == null) {
                                    sharedFunctionalMethod = potentialFunctionalMethod
                                    true
                                } else if (sharedFunctionalMethod == potentialFunctionalMethod) {
                                    true
                                } else {
                                    // the super interface types define different abstract methods
                                    noSAMInterface(interfaceType)
                                    false
                                }
                            case None ⇒
                                // we have a partial type hierarchy...
                                noSAMInterface(interfaceType)
                                false
                        }
                    } else {
                        // the supertype is irrelevant...
                        true
                    }
                }) {
                    // all super interfaces are either irrelevant or share the same
                    // functionalMethod
                    if (sharedFunctionalMethod == null) {
                        if (isPotentiallyIrrelevant)
                            irrelevantInterfaces += interfaceType
                        else
                            functionalInterfaces(interfaceType) = abstractMethods.head.signature
                        processSubinterfaces(interfaceType)
                    } else if (isPotentiallyIrrelevant ||
                        sharedFunctionalMethod == abstractMethods.head.signature) {
                        functionalInterfaces(interfaceType) = sharedFunctionalMethod
                        processSubinterfaces(interfaceType)
                    } else {
                        // different methods are defined...
                        noSAMInterface(interfaceType)
                    }
                }
            }
        }

        while (typesToProcess.nonEmpty) {
            val interfaceType = typesToProcess.pop

            if (!otherInterfaces.contains(interfaceType) &&
                !functionalInterfaces.contains(interfaceType) &&
                !irrelevantInterfaces.contains(interfaceType)) {

                classFile(interfaceType) match {
                    case Some(classFile) ⇒ classifyPotentiallyFunctionalInterface(classFile)
                    case None            ⇒ noSAMInterface(interfaceType)
                }
            }
        }

        UIDSet.empty[ObjectType] ++ functionalInterfaces.keys
    } { t ⇒
        info("project setup", s"computing functional interfaces took ${t.toSeconds}")
    }

    // --------------------------------------------------------------------------------------------
    //
    //    CODE TO MAKE IT POSSIBLE TO ATTACH SOME INFORMATION TO A PROJECT (ON DEMAND)
    //
    // --------------------------------------------------------------------------------------------

    /**
     * Here, the usage of the project information key does not lead to its initialization!
     */
    private[this] val projectInformationKeyInitializationData = {
        new ConcurrentHashMap[ProjectInformationKey[AnyRef, AnyRef], AnyRef]()
    }

    /**
     * Returns the project specific initialization information for the given project information
     * key.
     */
    def getProjectInformationKeyInitializationData[T <: AnyRef, I <: AnyRef](
        key: ProjectInformationKey[T, I]
    ): Option[I] = {
        Option(projectInformationKeyInitializationData.get(key).asInstanceOf[I])
    }

    /**
     * Gets the project information key specific initialization object. If an object is already
     * registered, that object will be used otherwise `info` will be evaluated and that value
     * will be added and also returned.
     *
     * @note    Initialization data is discarded once the key is used.
     */
    def getOrCreateProjectInformationKeyInitializationData[T <: AnyRef, I <: AnyRef](
        key:  ProjectInformationKey[T, I],
        info: ⇒ I
    ): I = {
        projectInformationKeyInitializationData.computeIfAbsent(
            key.asInstanceOf[ProjectInformationKey[AnyRef, AnyRef]],
            new java.util.function.Function[ProjectInformationKey[AnyRef, AnyRef], I] {
                def apply(key: ProjectInformationKey[AnyRef, AnyRef]): I = info
            }
        ).asInstanceOf[I]
    }

    /**
     * Updates project information key specific initialization object. If an object is already
     * registered, that object will be given to `info`.
     *
     * @note    Initialization data is discarded once the key is used.
     */
    def updateProjectInformationKeyInitializationData[T <: AnyRef, I <: AnyRef](
        key: ProjectInformationKey[T, I]
    )(
        info: Option[I] ⇒ I
    ): I = {
        projectInformationKeyInitializationData.compute(
            key.asInstanceOf[ProjectInformationKey[AnyRef, AnyRef]],
            (_, current: AnyRef) ⇒ {
                info(Option(current.asInstanceOf[I]))
            }: I
        ).asInstanceOf[I]
    }

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
     * If the information was not yet required, the information is computed and
     * returned. Subsequent calls will directly return the information.
     *
     * @note    (Development Time)
     *          Every analysis using [[ProjectInformationKey]]s must list '''All
     *          requirements; failing to specify a requirement can end up in a deadlock.'''
     *
     * @see     [[ProjectInformationKey]] for further information.
     */
    def get[T <: AnyRef](pik: ProjectInformationKey[T, _]): T = {
        val pikUId = pik.uniqueId

        /* Synchronization is done by the caller! */
        def derive(projectInformation: AtomicReferenceArray[AnyRef]): T = {
            var className = pik.getClass.getSimpleName
            if (className.endsWith("Key"))
                className = className.substring(0, className.length - 3)
            else if (className.endsWith("Key$"))
                className = className.substring(0, className.length - 4)

            for (requiredProjectInformationKey ← pik.requirements(this)) {
                get(requiredProjectInformationKey)
            }
            val pi = time {
                val pi = pik.compute(this)
                // we don't need the initialization data anymore
                projectInformationKeyInitializationData.remove(pik)
                pi
            } { t ⇒ info("project", s"initialization of $className took ${t.toSeconds}") }
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
                if (pikUId >= projectInformation.length()) {
                    val newLength = Math.max(projectInformation.length * 2, pikUId * 2)
                    val newProjectInformation = new AtomicReferenceArray[AnyRef](newLength)
                    org.opalj.control.iterateUntil(0, projectInformation.length()) { i ⇒
                        newProjectInformation.set(i, projectInformation.get(i))
                    }
                    this.projectInformation = newProjectInformation
                    return derive(newProjectInformation);
                }
            }
            // else (pikUId < projectInformation.length()) => the underlying array is "large enough"
            get(pik)
        }
    }

    /**
     * Tests if the information identified by the given [[ProjectInformationKey]]
     * is available. If the information is not (yet) available, the information
     * will not be computed; `None` will be returned.
     *
     * @see [[ProjectInformationKey]] for further information.
     */
    def has[T <: AnyRef](pik: ProjectInformationKey[T, _]): Option[T] = {
        val pikUId = pik.uniqueId

        if (pikUId < this.projectInformation.length())
            Option(this.projectInformation.get(pikUId).asInstanceOf[T])
        else
            None
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
    def sourceElementsCount: Int = fieldsCount + methodsCount + classFilesCount

    private[this] def doParForeachClassFile[T](
        classFiles: Array[ClassFile], isInterrupted: () ⇒ Boolean
    )(
        f: ClassFile ⇒ T
    ): Unit = {
        val classFilesCount = classFiles.length
        if (classFilesCount == 0)
            return ;

        parForeachArrayElement(classFiles, NumberOfThreadsForCPUBoundTasks, isInterrupted)(f)
    }

    def parForeachProjectClassFile[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: ClassFile ⇒ T
    ): Unit = {
        doParForeachClassFile(this.projectClassFiles, isInterrupted)(f)
    }

    def parForeachLibraryClassFile[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: ClassFile ⇒ T
    ): Unit = {
        doParForeachClassFile(this.libraryClassFiles, isInterrupted)(f)
    }

    def parForeachClassFile[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: ClassFile ⇒ T
    ): Unit = {
        parForeachProjectClassFile(isInterrupted)(f)
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

    /**
     * Iterates over all methods with a body in parallel starting with the largest methods first.
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
    ): Unit = {
        val methods = this.methodsWithBodyAndContext
        if (methods.length == 0)
            return ;

        parForeachArrayElement(methods, parallelizationLevel, isInterrupted)(f)
    }

    /**
     * Iterates over all methods in parallel; actually, the methods belonging to a specific class
     * are analyzed sequentially..
     */
    def parForeachMethod[T](
        isInterrupted: () ⇒ Boolean = defaultIsInterrupted
    )(
        f: Method ⇒ T
    ): Unit = {
        parForeachClassFile(isInterrupted) { cf ⇒ cf.methods.foreach(f) }
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
     * Returns `true` iff the given type belongs to the project and not to a library.
     */
    def isProjectType(objectType: ObjectType): Boolean = projectTypes.contains(objectType)

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

    def hasInstanceMethod(
        receiverType:     ObjectType,
        name:             String,
        descriptor:       MethodDescriptor,
        isPackagePrivate: Boolean
    ): Boolean = {
        val data = instanceMethods(receiverType)

        @tailrec @inline def binarySearch(low: Int, high: Int): Int = {
            if (high < low)
                return -1;

            val mid = (low + high) / 2 // <= will never overflow...(by constraint...)
            val e = data(mid)
            val eComparison = e.method.compare(name, descriptor)
            if (eComparison == 0) {
                mid
            } else if (eComparison < 0) {
                binarySearch(mid + 1, high)
            } else {
                binarySearch(low, mid - 1)
            }
        }

        val candidateIndex = binarySearch(0, data.length - 1)
        // In case we found a method, but it is not `method.isPackagePrivate` == `isPackagePrivate`,
        // it is possible that there is another method with the same name and descriptor next
        // to that one (i.e. left or right).
        // Therefore, we also check if there exists such a method, with indices lower/higher to
        // the found one.
        candidateIndex != -1 && {
            var index = candidateIndex
            var method: Method = null
            // check the methods with a smaller (or equal) index
            while (index >= 0
                && { method = data(index).method; method.compare(name, descriptor) == 0 }) {
                if (method.isPackagePrivate == isPackagePrivate)
                    return true;
                index -= 1
            }

            index = candidateIndex + 1 // reset the index

            // check the methods with a higher index
            while (index < data.length
                && { method = data(index).method; method.compare(name, descriptor) == 0 }) {
                if (method.isPackagePrivate == isPackagePrivate)
                    return true;

                index += 1
            }

            false
        }
    }

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
        val nonSyntheticMethodsWithBody = methodsWithBody.iterator.filterNot(_.isSynthetic)
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

    override def toString: String = {
        val classDescriptions =
            sources map { entry ⇒
                val (ot, source) = entry
                ot.toJava+" « "+source.toString
            }

        classDescriptions.mkString(
            "Project("+
                "\n\tlibraryClassFilesAreInterfacesOnly="+libraryClassFilesAreInterfacesOnly+
                "\n\t",
            "\n\t",
            "\n)"
        )
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

        // DEPRECATED: super.finalize()
        // The "correct" solution requires Java 9 (Cleaner) - we want to remain compatible
        // Java 8 for the time being; hence, we will keep it as it is for the time being.
    }
}

/**
 * Definition of factory methods to create [[Project]]s.
 *
 * @author Michael Eichberg
 */
object Project {

    lazy val JavaLibraryClassFileReader: Java9LibraryFramework.type = Java9LibraryFramework

    @volatile private[this] var theCache: SoftReference[BytecodeInstructionsCache] = {
        new SoftReference(new BytecodeInstructionsCache)
    }
    private[this] def cache: BytecodeInstructionsCache = {
        var cache = theCache.get
        if (cache == null) {
            this.synchronized {
                cache = theCache.get
                if (cache == null) {
                    cache = new BytecodeInstructionsCache
                    theCache = new SoftReference(cache)
                }
            }
        }
        cache
    }

    def JavaClassFileReader(
        implicit
        theLogContext: LogContext = GlobalLogContext,
        theConfig:     Config     = BaseConfig
    ): Java9FrameworkWithInvokedynamicSupportAndCaching = {
        // The following makes use of early initializers
        class ConfiguredFramework extends {
            override implicit val logContext: LogContext = theLogContext
            override implicit val config: Config = theConfig
        } with Java9FrameworkWithInvokedynamicSupportAndCaching(cache)
        new ConfiguredFramework
    }

    /**
     * Performs some fundamental validations to make sure that subsequent analyses don't have
     * to deal with completely broken projects/that the user is aware of the issues!
     */
    private[this] def validate(project: SomeProject): Seq[InconsistentProjectException] = {

        implicit val logContext = project.logContext

        val disclaimer = "(this inconsistency may lead to useless/wrong results)"

        import project.classHierarchy
        import classHierarchy.isInterface

        var exs = List.empty[InconsistentProjectException]
        val exsMutex = new Object
        def addException(ex: InconsistentProjectException): Unit = {
            exsMutex.synchronized { exs = ex :: exs }
        }

        try {
            project.parForeachMethodWithBody(() ⇒ Thread.interrupted()) { mi ⇒
                val m: Method = mi.method
                val cf = m.classFile

                def completeSupertypeInformation =
                    classHierarchy.isSupertypeInformationComplete(cf.thisType)

                def missingSupertypeClassFile =
                    classHierarchy.allSupertypes(cf.thisType, false).find { t ⇒
                        project.classFile(t).isEmpty
                    }.map { ot ⇒
                        (classHierarchy.isInterface(ot) match {
                            case Yes     ⇒ "interface "
                            case No      ⇒ "class "
                            case Unknown ⇒ "interface/class "
                        }) + ot.toJava
                    }.getOrElse("<None>")

                m.body.get iterate { (pc: Int, instruction: Instruction) ⇒

                    def validateReceiverTypeKind(
                        invoke: NonVirtualMethodInvocationInstruction
                    ): Boolean = {
                        val typeIsInterface = isInterface(invoke.declaringClass.asObjectType)
                        if (typeIsInterface.isYesOrNo && typeIsInterface.isYes != invoke.isInterfaceCall) {
                            val ex = InconsistentProjectException(
                                s"the type of the declaring class of the target method of the invokes call in "+
                                    m.toJava(s"pc=$pc; $invoke - $disclaimer")+
                                    " is inconsistent; it is expected to be "+
                                    (if (invoke.isInterfaceCall) "an interface" else "a class"),
                                Error
                            )
                            addException(ex)
                            false
                        } else {
                            true
                        }
                    }

                    try {
                        (instruction.opcode: @switch) match {

                            case NEW.opcode ⇒
                                val NEW(objectType) = instruction
                                if (isInterface(objectType).isYes) {
                                    val ex = InconsistentProjectException(
                                        s"cannot create an instance of interface ${objectType.toJava} in "+
                                            m.toJava(s"pc=$pc $disclaimer"),
                                        Error
                                    )
                                    addException(ex)
                                }

                            case INVOKESTATIC.opcode ⇒
                                val invokestatic = instruction.asInstanceOf[INVOKESTATIC]
                                if (validateReceiverTypeKind(invokestatic)) {
                                    project.staticCall(cf.thisType, invokestatic) match {
                                        case _: Success[_] ⇒ /*OK*/
                                        case Empty         ⇒ /*OK - partial project*/
                                        case Failure ⇒
                                            val ex = InconsistentProjectException(
                                                s"target method of invokestatic call in "+
                                                    m.toJava(s"pc=$pc; $invokestatic - $disclaimer")+
                                                    " cannot be resolved; supertype information is complete="+
                                                    completeSupertypeInformation+
                                                    "; missing supertype class file: "+missingSupertypeClassFile,
                                                Error
                                            )
                                            addException(ex)
                                    }
                                }

                            case INVOKESPECIAL.opcode ⇒
                                val invokespecial = instruction.asInstanceOf[INVOKESPECIAL]
                                if (validateReceiverTypeKind(invokespecial)) {
                                    project.specialCall(cf.thisType, invokespecial) match {
                                        case _: Success[_] ⇒ /*OK*/
                                        case Empty         ⇒ /*OK - partial project*/
                                        case Failure ⇒
                                            val ex = InconsistentProjectException(
                                                s"target method of invokespecial call in "+
                                                    m.toJava(s"pc=$pc; $invokespecial - $disclaimer")+
                                                    " cannot be resolved; supertype information is complete="+
                                                    completeSupertypeInformation+
                                                    "; missing supertype class file: "+missingSupertypeClassFile,
                                                Error
                                            )
                                            addException(ex)
                                    }
                                }
                            case _ ⇒ // Nothing special is checked (so far)
                        }
                    } catch {
                        case t: Throwable ⇒
                            OPALLogger.error(
                                "OPAL",
                                s"project validation of ${m.toJava(s"pc=$pc/$instruction")} failed unexpectedly",
                                t
                            )
                    }
                }
            }
        } catch {
            case ce: ConcurrentExceptions ⇒
                ce.getSuppressed foreach { e ⇒
                    error("internal - ignored", "project validation failed", e)
                }
        }

        exs
    }

    /**
     * The type of the function that is called if an inconsistent project is detected.
     */
    type HandleInconsistentProject = (LogContext, InconsistentProjectException) ⇒ Unit

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

    def instanceMethods(
        classHierarchy:        ClassHierarchy,
        objectTypeToClassFile: ObjectType ⇒ Option[ClassFile]
    )(
        implicit
        logContext: LogContext
    ): Map[ObjectType, ConstArray[MethodDeclarationContext]] = time {

        import org.opalj.br.analyses.ProjectLike.findMaximallySpecificSuperinterfaceMethods

        // IDEA
        // Process the type hierarchy starting with the root type(s) to ensure that all method
        // information about all super types is available (already stored in instanceMethods)
        // when we process the subtype. If not all information is already available, which
        // can happen in the following case if the processing of C would be scheduled before B:
        //      interface A; interface B extends A; interface C extends A, B,
        // we postpone the processing of C until the information is available.

        val methods: AnyRefMap[ObjectType, Chain[MethodDeclarationContext]] = {
            new AnyRefMap(ObjectType.objectTypesCount)
        }

        // Here, "overridden" is to be taken with a grain of salt, because we have a static
        // method with the same name and descriptor as an instance method defined by a super
        // class...
        var staticallyOverriddenInstanceMethods: List[(ObjectType, String, MethodDescriptor)] = Nil

        var missingClassTypes = Set.empty[ObjectType]

        // Returns `true` if the potentially available information is not yet available.
        @inline def notYetAvailable(superinterfaceType: ObjectType): Boolean = {
            methods.get(superinterfaceType).isEmpty &&
                // If the class file is not known, we will never have any details;
                // hence, the information will "NEVER" be available; or - in other
                // words - all potentially available information is available.
                objectTypeToClassFile(superinterfaceType).nonEmpty
        }

        def computeDefinedMethods(tasks: Tasks[ObjectType], objectType: ObjectType): Unit = {
            // Due to the fact that we may inherit from multiple interfaces,
            // the computation may have been scheduled multiple times; hence, if we are
            // already done, just return.
            if (methods.get(objectType).nonEmpty)
                return ;

            val superclassType = classHierarchy.superclassType(objectType)

            val inheritedClassMethods: Chain[MethodDeclarationContext] =
                if (superclassType.isDefined) {
                    val theSuperclassType = superclassType.get
                    if (notYetAvailable(theSuperclassType)) {
                        // let's postpone the processing of this object type
                        // because we will get some result in the future
                        tasks.submit(objectType)
                        return ;
                    }
                    val superclassTypeMethods = methods.get(theSuperclassType)
                    if (superclassTypeMethods.nonEmpty) {
                        val inheritedClassMethods = superclassTypeMethods.get
                        if (classHierarchy.isInterface(objectType).isYes) {
                            // an interface does not inherit non-public methods from java.lang.Object
                            inheritedClassMethods.filter(mdc ⇒ mdc.method.isPublic)
                        } else {
                            inheritedClassMethods
                        }
                    } else
                        Naught
                } else {
                    Naught
                }

            // We have to select the most maximally specific methods, recall that:
            //  -   methods defined by a class have precedence over concrete methods defined
            //      by interfaces (e.g., default methods).
            //  -   an abstract method defined by an interface "nullifies" a concrete
            //      package-visible or protected visible method defined by a superclass.
            //  -   we assume that the project is valid; i.e., there is
            //      always at most one maximally specific method and if not, then
            //      the subclass resolves the conflict by defining the method.
            var definedMethods: Chain[MethodDeclarationContext] = inheritedClassMethods

            val superinterfaceTypes = classHierarchy.allSuperinterfacetypes(objectType)

            // We have to filter (remove) those interfaces that are directly and indirectly
            // inherited. In this case the potentially(!) correct method is defined by the interface
            // which also implements the indirectly inherited interface!
            // Concrete case:
            // interface S { default void m(){;} }
            // interface SL extends S { abstract void m(); /* m is made abstract!!! */ }
            // interface SR extends S { }
            // The concrete method m defined by S does NOT belong to the interface of SB(!):
            // interface SB extends SL,SR { }
            //
            // Hence, when we have to find the correct method, we first have to determine
            // that - in case of SB - the only relevant super interfaces are SL and SR, but
            // not S.

            def processMaximallySpecificSuperinterfaceMethod(
                inheritedInterfaceMethod: Method
            ): Unit = {
                // The relevant interface methods are public, hence, the package
                // name is not relevant!
                definedMethods find { definedMethod ⇒
                    definedMethod.descriptor == inheritedInterfaceMethod.descriptor &&
                        definedMethod.name == inheritedInterfaceMethod.name
                } match {
                    case Some(mdc) ⇒
                        // If there is already a method and it is from an interface, then it is not
                        // maximally specific and must be replaced. If it is from a class however, we
                        // must keep it.

                        if (mdc.method.classFile.isInterfaceDeclaration) {
                            definedMethods = definedMethods filterNot { definedMethod ⇒
                                definedMethod.descriptor == inheritedInterfaceMethod.descriptor &&
                                    definedMethod.name == inheritedInterfaceMethod.name
                            }
                            if (!inheritedInterfaceMethod.isAbstract)
                                definedMethods :&:= MethodDeclarationContext(inheritedInterfaceMethod)
                        }
                    case None ⇒
                        if (!inheritedInterfaceMethod.isAbstract)
                            definedMethods :&:= MethodDeclarationContext(inheritedInterfaceMethod)
                }
            }

            var interfaceMethods: Set[MethodSignature] = Set.empty
            var uniqueInterfaceMethods: Set[Method] = Set.empty
            var uniqueInterfaceMethodSignatures: Set[MethodSignature] = Set.empty
            for {
                superinterfaceType ← superinterfaceTypes
                superinterfaceClassfile ← objectTypeToClassFile(superinterfaceType)
                superinterfaceTypeMethod ← superinterfaceClassfile.methods
                if superinterfaceTypeMethod.isPublic &&
                    !superinterfaceTypeMethod.isStatic &&
                    !superinterfaceTypeMethod.isInitializer
            } {
                val signature = superinterfaceTypeMethod.signature
                if (interfaceMethods.contains(signature)) {
                    uniqueInterfaceMethodSignatures -= signature
                    uniqueInterfaceMethods = uniqueInterfaceMethods.filterNot { m ⇒
                        m.signature == signature
                    }
                } else {
                    interfaceMethods += signature
                    uniqueInterfaceMethodSignatures += signature
                    uniqueInterfaceMethods += superinterfaceTypeMethod
                }
            }

            uniqueInterfaceMethods foreach { m ⇒ processMaximallySpecificSuperinterfaceMethod(m) }

            // let's keep the contexts related to the maximally specific methods.
            /* OLD
            interfaceMethods.iterator.filterNot { ms ⇒
                uniqueInterfaceMethodSignatures.contains(ms)
            } foreach { interfaceMethod ⇒
            */
            interfaceMethods foreach { interfaceMethod ⇒
                if (!uniqueInterfaceMethodSignatures.contains(interfaceMethod)) {
                    val (_, maximallySpecificSuperiniterfaceMethod) =
                        findMaximallySpecificSuperinterfaceMethods(
                            superinterfaceTypes,
                            interfaceMethod.name, interfaceMethod.descriptor,
                            UIDSet.empty[ObjectType]
                        )(objectTypeToClassFile, classHierarchy, logContext)
                    if (maximallySpecificSuperiniterfaceMethod.size == 1) {
                        // A maximally specific interface method can only be invoked if it is unique!
                        processMaximallySpecificSuperinterfaceMethod(
                            maximallySpecificSuperiniterfaceMethod.head
                        )
                    }
                }
            }

            objectTypeToClassFile(objectType) match {
                case Some(classFile) ⇒
                    for { declaredMethod ← classFile.methods } {
                        if (declaredMethod.isVirtualMethodDeclaration) {
                            val declaredMethodContext = MethodDeclarationContext(declaredMethod)
                            // We have to filter multiple methods when we inherit (w.r.t. the
                            // visibility) multiple conflicting methods!
                            definedMethods = definedMethods.filterNot { mdc ⇒
                                declaredMethodContext.directlyOverrides(mdc) ||
                                    mdc.method.isPrivate &&
                                    declaredMethodContext.method.compare(mdc.method) == 0
                            }

                            // Recall that it is possible to make a method "abstract" again...
                            if (declaredMethod.isNotAbstract) {
                                definedMethods :&:= declaredMethodContext
                            }
                        } else if (declaredMethod.isStatic) {
                            val declaredMethodName = declaredMethod.name
                            val declaredMethodDescriptor = declaredMethod.descriptor
                            if (definedMethods.exists { mdc ⇒
                                mdc.name == declaredMethodName &&
                                    mdc.descriptor == declaredMethodDescriptor
                            }) {
                                // In this case we may have an "overriding" of an instance method by
                                // a static method defined by the current interface or class type.
                                // If so – we have to remove the instance method from the set
                                // of defined methods for THIS SPECIFIC CLASS/INTERFACE ONlY; however,
                                // we can only remove it later on, because the instance method is
                                // visible again in subclasses/subinterfaces and we therefore
                                // first have to propagate it.
                                staticallyOverriddenInstanceMethods ::=
                                    ((objectType, declaredMethodName, declaredMethodDescriptor))
                            }
                        } else if (!declaredMethod.isInitializer) {
                            // Private methods can be invoked by invokevirtual instructions (and
                            // invokeinterface for Java 11+). If a call is resoved to a private
                            // method, it is performed non-virtually, thus private methods
                            // effectively shadow inherited methods (only possible in code evolution
                            // scenarios)
                            val declaredMethodContext = MethodDeclarationContext(declaredMethod)
                            definedMethods = definedMethods.filter { mdc ⇒
                                declaredMethodContext.method.compare(mdc.method) != 0
                            }

                            // Recall that it is possible to make a method "abstract" again...
                            if (declaredMethod.isNotAbstract) {
                                definedMethods :&:= declaredMethodContext
                            }
                        }
                    }

                case None ⇒
                    // ... reached only in case of rather incomplete projects...
                    missingClassTypes += objectType
            }
            methods(objectType) = definedMethods
            classHierarchy.foreachDirectSubtypeOf(objectType)(tasks.submit)
        }

        val tasks = new SequentialTasks[ObjectType](computeDefinedMethods, abortOnExceptions = true)
        classHierarchy.rootTypes.foreach(tasks.submit)
        try {
            tasks.join()
            if (missingClassTypes.nonEmpty) {
                OPALLogger.warn(
                    "project configuration - instance methods",
                    missingClassTypes
                        .map(_.toJava)
                        .toList.sorted
                        .take(10)
                        .mkString(
                            "no class files found for: {",
                            ", ",
                            if (missingClassTypes.size > 10) ", ...}" else "}"
                        )
                )
            }
        } catch {
            case ce: ConcurrentExceptions ⇒
                ce.getSuppressed foreach { e ⇒
                    error("project setup", "computing the defined methods failed", e)
                }
        }

        staticallyOverriddenInstanceMethods foreach { sodm ⇒
            val (declaringType, name, descriptor) = sodm
            methods(declaringType) =
                methods(declaringType) filter { mdc ⇒
                    mdc.descriptor != descriptor || mdc.name != name
                }
        }

        val result = methods.mapValuesNow { mdcs ⇒
            val sortedMethods = mdcs.toArray
            sortArray(sortedMethods, MethodDeclarationContextOrdering)
            ConstArray._UNSAFE_from(sortedMethods)
        }
        result.repack
        result
    } { t ⇒ info("project setup", s"computing defined methods took ${t.toSeconds}") }

    /**
     * Returns for a given virtual method the set of all non-abstract virtual methods which
     * override it.
     *
     * This method takes the visibility of the methods and the defining context into consideration.
     *
     * @see     [[Method]]`.isVirtualMethodDeclaration` for further details.
     * @note    The map only contains those methods which have at least one concrete
     *          implementation.
     */
    def overridingMethods(
        classHierarchy:        ClassHierarchy,
        virtualMethodsCount:   Int,
        objectTypeToClassFile: Map[ObjectType, ClassFile]
    )(
        implicit
        theLogContext: LogContext
    ): Map[Method, Set[Method]] = time {

        implicit val classFileRepository = new ClassFileRepository {
            override implicit def logContext: LogContext = theLogContext
            override def classFile(objectType: ObjectType): Option[ClassFile] = {
                objectTypeToClassFile.get(objectType)
            }
        }

        // IDEA
        // 0.   We start with the leaf nodes of the class hierarchy and store for each method
        //      the set of overriding methods (recall that the overrides relation is reflexive).
        //      Hence, initially the set of overriding methods for a method contains the method
        //      itself.
        //
        // 1.   After that the direct superclass is scheduled to be analyzed if all subclasses
        //      are analyzed. The superclass then tests for each overridable method if it is
        //      overridden in the subclasses and, if so, looks up the respective sets of overriding
        //      methods and joins them.
        //      A method is overridden by a subclass if the set of instance methods of the
        //      subclass does not contain the super class' method.
        //
        // 2.   Continue with 1.

        // Stores for each type the number of subtypes that still need to be processed.
        val subtypesToProcessCounts = new Array[Int](ObjectType.objectTypesCount)
        classHierarchy.foreachKnownType { objectType ⇒
            val oid = objectType.id
            subtypesToProcessCounts(oid) = classHierarchy.directSubtypesCount(oid)
        }

        val methods = new AnyRefMap[Method, Set[Method]](virtualMethodsCount)

        def computeOverridingMethods(tasks: Tasks[ObjectType], objectType: ObjectType): Unit = {
            val declaredMethodPackageName = objectType.packageName

            // If we don't know anything about the methods, we just do nothing;
            // instanceMethods will also just reuse the information derived from the superclasses.
            try {
                for {
                    cf ← objectTypeToClassFile.get(objectType)
                    declaredMethod ← cf.methods
                    if declaredMethod.isVirtualMethodDeclaration
                } {
                    if (declaredMethod.isFinal) { //... the method is necessarily not abstract...
                        methods += ((declaredMethod, Set(declaredMethod)))
                    } else {
                        var overridingMethods = Set.empty[Method]
                        // let's join the results of all subtypes
                        classHierarchy.foreachSubtypeCF(objectType) { subtypeClassFile ⇒
                            subtypeClassFile.findDirectlyOverridingMethod(
                                declaredMethodPackageName,
                                declaredMethod
                            ) match {
                                case _: NoResult ⇒ true
                                case Success(overridingMethod) ⇒
                                    val nextOverridingMethods = methods(overridingMethod)
                                    if (nextOverridingMethods.nonEmpty) {
                                        overridingMethods ++= nextOverridingMethods
                                    }
                                    false // we don't have to analyze subsequent subtypes.
                            }
                        }

                        if (declaredMethod.isNotAbstract) overridingMethods += declaredMethod

                        methods(declaredMethod) = overridingMethods
                    }
                }
            } finally {
                // The try-finally is a safety net to ensure that this method at least
                // terminates and that exceptions can be reported!
                classHierarchy.foreachDirectSupertype(objectType) { supertype ⇒
                    val sid = supertype.id
                    val newCount = subtypesToProcessCounts(sid) - 1
                    subtypesToProcessCounts(sid) = newCount
                    if (newCount == 0) {
                        tasks.submit(supertype)
                    }
                }
            }
        }

        val tasks = new SequentialTasks[ObjectType](computeOverridingMethods)
        classHierarchy.leafTypes foreach { t ⇒ tasks.submit(t) }
        try {
            tasks.join()
        } catch {
            case ce: ConcurrentExceptions ⇒
                error("project setup", "computing overriding methods failed, e")
                ce.getSuppressed foreach { e ⇒
                    error("project setup", "computing the overriding methods failed", e)
                }
        }
        methods.repack
        methods
    } { t ⇒
        info("project setup", s"computing overriding information took ${t.toSeconds}")
    }

    //
    //
    // FACTORY METHODS
    //
    //

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
        val reader = JavaClassFileReader(logContext, config)
        this(
            projectClassFilesWithSources = reader.ClassFiles(file),
            libraryClassFilesWithSources = Traversable.empty,
            libraryClassFilesAreInterfacesOnly = true,
            virtualClassFiles = Traversable.empty,
            handleInconsistentProject = defaultHandlerForInconsistentProjects,
            config = config,
            logContext
        )
    }

    def apply(
        projectFiles: Array[File],
        libraryFiles: Array[File],
        logContext:   LogContext,
        config:       Config
    ): Project[URL] = {
        this(
            JavaClassFileReader(logContext, config).AllClassFiles(projectFiles),
            JavaLibraryClassFileReader.AllClassFiles(libraryFiles),
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
        )(projectLogger = projectLogger)
    }

    def apply(
        projectFile: File,
        libraryFile: File
    ): Project[URL] = {
        implicit val logContext: LogContext = GlobalLogContext
        val libraries: Traversable[(ClassFile, URL)] =
            if (!libraryFile.exists) {
                OPALLogger.error("project configuration", s"$libraryFile does not exist")
                Traversable.empty
            } else {
                val libraries = JavaLibraryClassFileReader.ClassFiles(libraryFile)
                if (libraries.isEmpty)
                    OPALLogger.warn("project configuration", s"$libraryFile is empty")
                libraries
            }
        apply(
            JavaClassFileReader().ClassFiles(projectFile),
            libraries,
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
        )(config = project.config, projectLogger = OPALLogger.logger(project.logContext.successor))
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
        )(project.config, OPALLogger.logger(project.logContext.successor))
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
     * configuration is — by default – used as a fallback, so not all values have to be updated.
     *
     * If you just want to clear the derived data, using `Project.recreate` is more efficient.
     */
    def recreate[Source](
        project:                Project[Source],
        config:                 Config          = ConfigFactory.empty(),
        useOldConfigAsFallback: Boolean         = true
    ): Project[Source] = {
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
     * @param libraryClassFilesAreInterfacesOnly If `true` then only the non-private interface of
     *         of the classes belonging to the library was loaded. I.e., this setting just reflects
     *         the way how the class files were loaded; it does not change the classes!
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
        handleInconsistentProject:          HandleInconsistentProject        = defaultHandlerForInconsistentProjects
    )(
        implicit
        config:        Config     = BaseConfig,
        projectLogger: OPALLogger = OPALLogger.globalLogger()
    ): Project[Source] = {
        implicit val logContext = new StandardLogContext()
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
        handleInconsistentProject:          HandleInconsistentProject,
        config:                             Config,
        logContext:                         LogContext
    ): Project[Source] = time {
        implicit val projectConfig = config
        implicit val projectLogContext = logContext

        try {
            import scala.collection.mutable.Set
            import scala.concurrent.Await
            import scala.concurrent.Future
            import scala.concurrent.duration.Duration
            import scala.concurrent.ExecutionContext.Implicits.{global ⇒ ScalaExecutionContext}

            val classHierarchyFuture: Future[ClassHierarchy] = Future {
                time {
                    val OTObject = ObjectType.Object
                    val typeHierarchyDefinitions =
                        if (projectClassFilesWithSources.exists(_._1.thisType == OTObject) ||
                            libraryClassFilesWithSources.exists(_._1.thisType == OTObject)) {
                            info("project configuration", "the JDK is part of the analysis")
                            ClassHierarchy.noDefaultTypeHierarchyDefinitions
                        } else {
                            val alternative =
                                "(using the preconfigured type hierarchy (based on Java 7) "+
                                    "for classes belonging java.lang)"
                            info("project configuration", "JDK classes not found "+alternative)
                            ClassHierarchy.defaultTypeHierarchyDefinitions
                        }
                    ClassHierarchy(
                        projectClassFilesWithSources.view.map(_._1) ++
                            libraryClassFilesWithSources.view.map(_._1) ++
                            virtualClassFiles,
                        typeHierarchyDefinitions
                    )
                } { t ⇒
                    info("project setup", s"computing type hierarchy took ${t.toSeconds}")
                }
            }(ScalaExecutionContext)

            val projectModules = AnyRefMap.empty[String, ModuleDefinition[Source]]
            var projectClassFiles = List.empty[ClassFile]
            val projectTypes = Set.empty[ObjectType]
            var projectClassFilesCount: Int = 0
            var projectMethodsCount: Int = 0
            var projectFieldsCount: Int = 0

            val libraryModules = AnyRefMap.empty[String, ModuleDefinition[Source]]
            var libraryClassFiles = List.empty[ClassFile]
            var libraryClassFilesCount: Int = 0
            var libraryMethodsCount: Int = 0
            var libraryFieldsCount: Int = 0

            var codeSize: Long = 0L

            val objectTypeToClassFile = OpenHashMap.empty[ObjectType, ClassFile] // IMPROVE Use ArrayMap as soon as we have project-local object type ids
            val sources = OpenHashMap.empty[ObjectType, Source] // IMPROVE Use ArrayMap as soon as we have project-local object type ids

            def processModule(
                classFile:        ClassFile,
                source:           Option[Source],
                modulesContainer: AnyRefMap[String, ModuleDefinition[Source]]
            ): Unit = {
                val moduleName = classFile.module.get.name
                if (projectModules.contains(moduleName)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"the module $moduleName is defined as part of the project:\n\t"+
                                projectModules(moduleName).source.getOrElse("<VIRTUAL>")+" and\n\t"+
                                source.map(_.toString).getOrElse("<VIRTUAL>")+
                                "\n\tkeeping the first one."
                        )
                    )
                } else if (libraryModules.contains(moduleName)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"the module $moduleName is defined as part of the libraries:\n\t"+
                                libraryModules(moduleName).source.getOrElse("<VIRTUAL>")+" and\n\t"+
                                source.map(_.toString).getOrElse("<VIRTUAL>")+
                                "\n\tkeeping the first one."
                        )
                    )
                } else {
                    modulesContainer += ((moduleName, ModuleDefinition(classFile, source)))
                }
            }

            def processProjectClassFile(classFile: ClassFile, source: Option[Source]): Unit = {
                val projectType = classFile.thisType
                if (classFile.isModuleDeclaration) {
                    processModule(classFile, source, projectModules)
                } else if (projectTypes.contains(projectType)) {
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
                        method.body.foreach(codeSize += _.instructions.length)
                    }
                    projectFieldsCount += classFile.fields.size
                    objectTypeToClassFile(projectType) = classFile
                    source.foreach(sources(classFile.thisType) = _)
                }
            }

            for ((classFile, source) ← projectClassFilesWithSources) {
                processProjectClassFile(classFile, Some(source))
            }

            for (classFile ← virtualClassFiles) {
                processProjectClassFile(classFile, None)
            }

            // The set `libraryTypes` is only used to improve the identification of
            // inconsistent projects while loading libraries.
            val libraryTypes = Set.empty[ObjectType]
            for ((libClassFile, source) ← libraryClassFilesWithSources) {
                val libraryType = libClassFile.thisType

                if (libClassFile.isModuleDeclaration) {
                    processModule(libClassFile, Some(source), libraryModules)

                } else if (projectTypes.contains(libClassFile.thisType)) {
                    val libraryTypeQualifier =
                        if (libClassFile.isInterfaceDeclaration) "interface" else "class"
                    val projectTypeQualifier = {
                        val projectClassFile = projectClassFiles.find(_.thisType == libraryType).get
                        if (projectClassFile.isInterfaceDeclaration) "interface" else "class"
                    }

                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"${libraryType.toJava} is defined by the project and a library: "+
                                sources.getOrElse(libraryType, "<VIRTUAL>")+" and "+
                                source.toString+"; keeping the project class file."
                        )
                    )

                    if (libraryTypeQualifier != projectTypeQualifier) {
                        handleInconsistentProject(
                            logContext,
                            InconsistentProjectException(
                                s"the kind of the type ${libraryType.toJava} "+
                                    s"defined by the project ($projectTypeQualifier) "+
                                    s"and a library ($libraryTypeQualifier) differs"
                            )
                        )
                    }

                } else if (libraryTypes.contains(libraryType)) {
                    handleInconsistentProject(
                        logContext,
                        InconsistentProjectException(
                            s"${libraryType.toJava} is defined multiple times in the libraries: "+
                                sources.getOrElse(libraryType, "<VIRTUAL>")+" and "+
                                source.toString+"; keeping the first one."
                        )
                    )
                } else {
                    libraryClassFiles ::= libClassFile
                    libraryTypes += libraryType
                    libraryClassFilesCount += 1
                    for (method ← libClassFile.methods) {
                        libraryMethodsCount += 1
                        method.body.foreach(codeSize += _.instructions.length)
                    }
                    libraryFieldsCount += libClassFile.fields.size
                    objectTypeToClassFile(libraryType) = libClassFile
                    sources(libraryType) = source
                }
            }

            val classHierarchy = Await.result(classHierarchyFuture, Duration.Inf)

            val instanceMethodsFuture = Future {
                this.instanceMethods(classHierarchy, objectTypeToClassFile.get)
            }

            val projectClassFilesArray = projectClassFiles.toArray
            val libraryClassFilesArray = libraryClassFiles.toArray
            val allMethods: Iterable[Method] = {
                new Iterable[Method] {
                    def iterator: Iterator[Method] = {
                        projectClassFilesArray.toIterator.flatMap { cf ⇒ cf.methods } ++
                            libraryClassFilesArray.toIterator.flatMap { cf ⇒ cf.methods }
                    }
                }
            }
            val virtualMethodsCount: Int = allMethods.count(m ⇒ m.isVirtualMethodDeclaration)

            val overridingMethodsFuture = Future {
                this.overridingMethods(classHierarchy, virtualMethodsCount, objectTypeToClassFile)
            }

            val methodsWithBodySortedBySizeWithContext =
                (projectClassFiles.iterator.flatMap(_.methods) ++
                    libraryClassFiles.iterator.flatMap(_.methods)).
                    filter(m ⇒ m.body.isDefined).
                    map(m ⇒ MethodInfo(sources(m.classFile.thisType), m)).
                    toArray.
                    sortWith { (v1, v2) ⇒ v1.method.body.get.codeSize > v2.method.body.get.codeSize }

            val methodsWithBodySortedBySize: Array[Method] =
                methodsWithBodySortedBySizeWithContext.map(mi ⇒ mi.method)

            val MethodHandleSubtypes = {
                classHierarchy.allSubtypes(ObjectType.MethodHandle, reflexive = true)
            }

            val VarHandleSubtypes = {
                classHierarchy.allSubtypes(ObjectType.VarHandle, reflexive = true)
            }

            val classFilesCount: Int = projectClassFilesCount + libraryClassFilesCount

            val methodsCount: Int = projectMethodsCount + libraryMethodsCount

            val fieldsCount: Int = projectFieldsCount + libraryFieldsCount

            val allProjectClassFiles: ConstArray[ClassFile] = ConstArray._UNSAFE_from(projectClassFilesArray)

            val allLibraryClassFiles: ConstArray[ClassFile] = ConstArray._UNSAFE_from(libraryClassFilesArray)

            val allClassFiles: Iterable[ClassFile] = {
                new Iterable[ClassFile] {
                    def iterator: Iterator[ClassFile] = {
                        projectClassFilesArray.toIterator ++ libraryClassFilesArray.toIterator
                    }
                }
            }

            val allFields: Iterable[Field] = {
                new Iterable[Field] {
                    def iterator: Iterator[Field] = {
                        projectClassFilesArray.toIterator.flatMap { cf ⇒ cf.fields } ++
                            libraryClassFilesArray.toIterator.flatMap { cf ⇒ cf.fields }
                    }
                }
            }

            val allSourceElements: Iterable[SourceElement] = allMethods ++ allFields ++ allClassFiles

            val project = new Project(
                projectModules,
                projectClassFilesArray,
                libraryModules,
                libraryClassFilesArray,
                libraryClassFilesAreInterfacesOnly,
                methodsWithBodySortedBySize,
                methodsWithBodySortedBySizeWithContext,
                projectTypes,
                objectTypeToClassFile,
                sources,
                projectClassFilesCount,
                projectMethodsCount,
                projectFieldsCount,
                libraryClassFilesCount,
                libraryMethodsCount,
                libraryFieldsCount,
                codeSize,
                MethodHandleSubtypes,
                VarHandleSubtypes,
                classFilesCount,
                methodsCount,
                fieldsCount,
                allProjectClassFiles,
                allLibraryClassFiles,
                allClassFiles,
                allMethods,
                allFields,
                allSourceElements,
                virtualMethodsCount,
                classHierarchy,
                Await.result(instanceMethodsFuture, Duration.Inf),
                Await.result(overridingMethodsFuture, Duration.Inf)
            )

            time {
                val issues = validate(project)
                issues.foreach { handleInconsistentProject(logContext, _) }
                info(
                    "project configuration",
                    s"project validation revealed ${issues.size} significant issues"+
                        (
                            if (issues.nonEmpty)
                                "; validate the configured libraries for inconsistencies"
                            else
                                ""
                        )
                )
            } { t ⇒ info("project setup", s"validating the project took ${t.toSeconds}") }

            project
        } catch {
            case t: Throwable ⇒ OPALLogger.unregister(logContext); throw t
        }
    } { t ⇒
        // If an exception was thrown, the logContext is no longer available!
        val lc = if (OPALLogger.isUnregistered(logContext)) GlobalLogContext else logContext
        info("project setup", s"creating the project took ${t.toSeconds}")(lc)
    }

}

case class ModuleDefinition[Source](module: ClassFile, source: Option[Source])
