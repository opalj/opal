/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.control.ControlThrowable

import org.apache.commons.text.similarity.LevenshteinDistance
import org.opalj.control.fillArrayOfInt
import org.opalj.io.process

import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.concurrent.BoundedExecutionContext
import org.opalj.concurrent.NumberOfThreadsForIOBoundTasks
import org.opalj.concurrent.parForeachSeqElement
import org.opalj.concurrent.Tasks
import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Implements the template method to read in a Java class file. Additionally,
 * several convenience methods are defined to read in class files from various
 * sources (Streams, Files, JAR archives).
 *
 * This library supports class files from version 45 (Java 1.1) up to version 54 (Java 10).
 *
 * ==Notes for Implementors==
 * Reading of the class file's major structures: the constant pool, fields, methods
 * and the attributes is delegated to corresponding readers.
 * This enables a very high-level of adaptability.
 *
 * For further details see the JVM Specification: The ClassFile Structure.
 */
trait ClassFileReader extends ClassFileReaderConfiguration with Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    /**
     * The type of the object that represents a Java class file.
     */
    type ClassFile

    /**
     * The inherited interfaces.
     */
    final type Interfaces = Array[Constant_Pool_Index]

    /**
     * The type of the object that represents the fields of a class.
     */
    type Fields

    /**
     * The type of the object that represents all methods of a class.
     */
    type Methods

    /**
     * The type of the object that represents a class declaration's
     * attributes (e.g., the source file attribute.)
     */
    type Attributes

    // METHODS DELEGATING TO OTHER READERS
    //

    /**
     * Reads the constant pool using the given stream.
     *
     * When this method is called the given stream has to be positioned at the very
     * beginning of the constant pool. This method is called by the template method that
     * reads in a class file to delegate the reading of the constant pool. Only
     * information belonging to the constant pool are allowed to be read.
     *
     * The stream must not be closed after reading the constant pool.
     */
    protected def Constant_Pool(in: DataInputStream): Constant_Pool

    /**
     * Reads all field declarations using the given stream and constant pool.
     *
     * The given stream is positioned directly before a class file's "fields_count" field.
     * This method is called by the template method that reads in a class file to
     * delegate the reading of the declared fields.
     */
    protected def Fields(cp: Constant_Pool, in: DataInputStream): Fields

    /**
     * Reads all method declarations using the given stream and constant pool.
     *
     * The given stream is positioned directly before a class file's "methods_count" field.
     * This method is called by the
     * template method that reads in a class file to delegate the reading of the
     * declared method.
     */
    protected def Methods(cp: Constant_Pool, in: DataInputStream): Methods

    /**
     * Reads all attributes using the given stream and constant pool.
     *
     * The given stream is positioned directly before a class file's "attributes_count"
     * field.
     * This method is called by the template method that reads in a class file to
     * delegate the reading of the attributes.
     *
     * '''From the Specification'''
     *
     * The attributes [...] appearing in the attributes table of a ClassFile
     * structure are the InnerClasses, EnclosingMethod, Synthetic, Signature,
     * SourceFile, SourceDebugExtension, Deprecated, RuntimeVisibleAnnotations,
     * RuntimeInvisibleAnnotations, BootstrapMethods, RuntimeVisibleTypeAnnotations,
     * and RuntimeInvisibleTypeAnnotations attributes.
     */
    protected def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attributes

    /**
     * Factory method to create the `ClassFile` object that represents the class
     * file as a whole, plus any `ClassFile`s that have been synthesized in the process
     * of parsing it.
     *
     * The result will always contain at least one `ClassFile` object, namely the one that
     * is created from this method's parameters. Regardless of how many `ClassFile`s the
     * result contains, the `ClassFile` created from this method's parameters will always
     * be the result's first element.
     */
    protected def ClassFile(
        cp:            Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags:  Int,
        this_class:    Constant_Pool_Index,
        super_class:   Constant_Pool_Index,
        interfaces:    Interfaces,
        fields:        Fields,
        methods:       Methods,
        attributes:    Attributes
    ): ClassFile

    //
    // IMPLEMENTATION
    //

    import org.opalj.bi.reader.ClassFileReader.ExceptionHandler

    final val defaultExceptionHandler: ExceptionHandler = (source, t) => {
        error("class file reader", s"processing $source failed", t)
    }

    private[this] var classFilePostProcessors = ArraySeq.empty[List[ClassFile] => List[ClassFile]]

    /**
     * Register a class file post processor. A class file post processor
     * can transform the completely read and reified class file. Post processors
     * can only be registered before the usage of a class file reader. '''Registering
     * new `ClassFilePostProcessors` while processing class files is not supported
     * and the behavior is undefined'''.
     *
     * @note `PostProcessors` will be executed in last-in-first-out order.
     */
    def registerClassFilePostProcessor(p: List[ClassFile] => List[ClassFile]): Unit = {
        classFilePostProcessors :+= p
    }

    /**
     * Template method that reads a Java class file from the given input stream.
     *
     * All other methods to read a class file use this method to eventually parse a
     * class file.
     *
     * ==Class File Structure==
     * Parses a class file according to the specification:
     * <pre>
     * ClassFile {
     *    u4 magic;
     *    u2 minor_version;
     *    u2 major_version;
     *    u2 constant_pool_count;
     *    cp_info constant_pool[constant_pool_count-1];
     *    u2 access_flags;
     *    u2 this_class;
     *    u2 super_class;
     *    u2 interfaces_count;
     *    u2 interfaces[interfaces_count];
     *    u2 fields_count;
     *    field_info fields[fields_count];
     *    u2 methods_count;
     *    method_info methods[methods_count];
     *    u2 attributes_count;
     *    attribute_info attributes[attributes_count];
     * }
     * </pre>
     *
     * @param in    The `DataInputStream` from which the class file will be read. The
     *              stream is not closed by this method.
     *              '''It is highly recommended that the stream is buffered; otherwise the
     *              performance will be terrible!'''
     */
    def ClassFile(in: DataInputStream): List[ClassFile] = {
        // magic
        val readMagic = in.readInt
        if (ClassFileMagic != readMagic)
            throw BytecodeProcessingFailedException("the file does not start with 0xCAFEBABE")

        val minor_version = in.readUnsignedShort
        val major_version = in.readUnsignedShort

        def unsupportedVersion =
            s"unsupported class file version: $major_version.$minor_version"+
                " (Supported: 45(Java 1.1) <= version <= "+
                s"$LatestSupportedJavaMajorVersion(${jdkVersion(LatestSupportedJavaMajorVersion)}))"

        // let's make sure that we support this class file's version
        if (major_version < 45) // at least JDK 1.1 or we back out
            throw BytecodeProcessingFailedException(unsupportedVersion)

        if (major_version > LatestSupportedJavaMajorVersion || (
            major_version == LatestSupportedJavaMajorVersion
            && minor_version > LatestSupportedJavaVersion.minor
        )) {
            // Just log an error message for newer version, we might still be able to handle the
            // class if it doesn't use any features introduced in an unsupported version
            error("class file reader", unsupportedVersion)
        }

        val cp = Constant_Pool(in)
        val access_flags = in.readUnsignedShort
        val this_class = in.readUnsignedShort
        val super_class = in.readUnsignedShort
        val interfaces = {
            val interfaces_count = in.readUnsignedShort
            fillArrayOfInt(interfaces_count) { in.readUnsignedShort }
        }
        val fields = Fields(cp, in)
        val methods = Methods(cp, in)
        val attributes = Attributes(cp, AttributesParent.ClassFile, this_class, -1, in)

        var classFile = ClassFile(
            cp,
            minor_version, major_version,
            access_flags,
            this_class, super_class, interfaces,
            fields, methods,
            attributes
        )

        // Perform transformations that are specific to this class file.
        // (Used, e.g., to finally resolve the invokedynamic instructions.)
        classFile = applyDeferredActions(cp, classFile)

        // Perform general transformations on class files.
        classFilePostProcessors.foldLeft(List(classFile)) { (classFiles, postProcessor) =>
            postProcessor(classFiles)
        }
    }

    //
    // CONVENIENCE METHODS TO LOAD CLASS FILES FROM VARIOUS SOURCES
    //

    /**
     * Reads in a class file.
     *
     * @param   create A function that creates a new `InputStream` and
     *          which must not return `null`. If you already do have an open input stream
     *          which should not be closed after reading the class file use
     *          `...ClassFileReader.ClassFile(java.io.DataInputStream) : ClassFile` instead.
     *          The (newly created) `InputStream` returned by calling `create` is closed by
     *          this method.
     *          The created input stream will automatically be wrapped by OPAL to enable
     *          efficient reading of the class file.
     */
    def ClassFile(create: () => InputStream): List[ClassFile] = {
        process(create()) {
            case null =>
                throw new IllegalArgumentException("the created stream is null")

            case dis: DataInputStream      => ClassFile(dis)
            case bis: BufferedInputStream  => ClassFile(new DataInputStream(bis))
            case bas: ByteArrayInputStream => ClassFile(new DataInputStream(bas))
            case is =>
                ClassFile(new DataInputStream(new BufferedInputStream(is)))
        }
    }

    def isClassFileRepository(filename: String, containerName: Option[String]): Boolean = {
        if (containerName.isDefined) {
            // We don't want to extract inner jars,... from jmods (the default jmods contain
            // jars which contain class files also found in the jmods.)
            val containerNameLength = containerName.get.length
            if (containerNameLength > 5 && containerName.get.endsWith(".jmod")) {
                return false;
            }
        }
        val filenameLength = filename.length
        filenameLength > 4 && {
            val ending = filename.substring(filenameLength - 4, filenameLength).toLowerCase
            (ending == "jmod" && filename.charAt(filenameLength - 5) == '.') ||
                ending == ".jar" || ending == ".zip" || ending == ".war" || ending == ".ear"
        }
    }

    protected[this] def ClassFile(jarFile: ZipFile, jarEntry: ZipEntry): List[ClassFile] = {
        process(jarFile.getInputStream(jarEntry)) { in =>
            ClassFile(new DataInputStream(new BufferedInputStream(in)))
        }
    }

    /**
     * Reads in a single class file from a Jar file.
     *
     * @param jarFile An existing ZIP/JAR file that contains class files.
     * @param jarFileEntryName The name of a class file stored in the specified ZIP/JAR file.
     */
    @throws[java.io.IOException]("if the file is empty or the entry cannot be found")
    def ClassFile(jarFile: File, jarFileEntryName: String): List[ClassFile] = {
        if (jarFile.length() == 0)
            throw new IOException(s"the file $jarFile is empty");

        val levenshteinDistance = new LevenshteinDistance()

        process(new ZipFile(jarFile)) { zf =>
            val jarEntry = zf.getEntry(jarFileEntryName)
            if (jarEntry == null) {
                var names: List[(Int, String)] = Nil
                val zfEntries = zf.entries()
                while (zfEntries.hasMoreElements) {
                    val zfEntry = zfEntries.nextElement()
                    val zfEntryName = zfEntry.getName
                    val distance = levenshteinDistance(zfEntryName, jarFileEntryName).intValue()
                    names = (distance, zfEntryName) :: names
                }
                val mostRelatedNames = names.sortWith((l, r) => l._1 < r._1).map(_._2).take(15)
                val ending = if (mostRelatedNames.length > 15) ", ...)" else ")"
                val messageHeader = s"the file $jarFile does not contain $jarFileEntryName"
                val message = mostRelatedNames.mkString(s"$messageHeader (similar: ", ", ", ending)
                throw new IOException(message)
            }
            ClassFile(zf, jarEntry)
        }
    }

    /**
     * Reads in a single class file from a Jar file.
     *
     * @param jarFileName the name of an existing ZIP/JAR file that contains class files.
     * @param jarFileEntryName the name of a class file stored in the specified ZIP/JAR file.
     */
    @throws[java.io.IOException]("if the file is empty or the entry cannot be found")
    def ClassFile(jarFileName: String, jarFileEntryName: String): List[ClassFile] = {
        ClassFile(new File(jarFileName), jarFileEntryName)
    }

    /**
     * Reads in parallel all class files stored in the given jar/zip file.
     *
     * @param jarFile Some valid (non-empty) jar File.
     * @return The loaded class files.
     */
    def ClassFiles(
        jarFile:          ZipFile,
        exceptionHandler: ExceptionHandler
    ): List[(ClassFile, URL)] = {
        val Lock = new Object
        var classFiles: List[(ClassFile, URL)] = Nil

        def addClassFile(cf: ClassFile, url: URL): Unit = {
            Lock.synchronized {
                classFiles ::= ((cf, url))
            }
        }

        ClassFiles(jarFile, addClassFile, exceptionHandler)
        classFiles
    }

    /**
     * Reads the class files from the given JarInputStream
     */
    // The following solution is inspired by Ben Hermann's solution found at:
    // https://github.com/delphi-hub/delphi-crawler/blob/feature/streamworkaround/src/main/scala/de/upb/cs/swt/delphi/crawler/tools/JarStreamReader.scala
    // and
    // https://github.com/delphi-hub/delphi-crawler/blob/develop/src/main/scala/de/upb/cs/swt/delphi/crawler/tools/ClassStreamReader.scala
    def ClassFiles(in: => JarInputStream): List[(ClassFile, String)] = process(in) { in =>
        var je: JarEntry = in.getNextJarEntry()

        var futures: List[Future[List[(ClassFile, String)]]] = Nil

        while (je != null) {
            val entryName = je.getName
            if (entryName.endsWith(".class") || entryName.endsWith(".jar")) {
                val entryBytes = {
                    val baos = new ByteArrayOutputStream()
                    val buffer = new Array[Byte](32 * 1024)

                    LazyList.continually(in.read(buffer)).takeWhile(_ > 0).foreach { bytesRead =>
                        baos.write(buffer, 0, bytesRead)
                        baos.flush()
                    }
                    baos.toByteArray
                }
                futures ::= Future[List[(ClassFile, String)]] {
                    if (entryName.endsWith(".class")) {
                        val cfs = ClassFile(new DataInputStream(new ByteArrayInputStream(entryBytes)))
                        cfs map { cf => (cf, entryName) }
                    } else { // ends with ".jar"
                        info("class file reader", s"reading inner jar $entryName")
                        ClassFiles(new JarInputStream(new ByteArrayInputStream(entryBytes)))
                    }
                }(
                    // we can't use the OPALExecutionContext here, because the number of
                    // threads is bounded and (depending on the nesting level, we may need
                    // more threads..)
                    ExecutionContext.global
                )

            }
            je = in.getNextJarEntry()
        }

        futures.flatMap(f => Await.result(f, Duration.Inf))
    }

    /**
     * Reads '''in parallel''' all class files stored in the given jar file. For each
     * successfully read class file the function `classFileHandler` is called.
     *
     * @param zipFile A valid zip file that contains `.class` files and other
     *     `.jar` files; other files are ignored. Inner jar files are also unzipped.
     * @param classFileHandler A function that is called for each class file in
     *      the given jar file.
     *      Given that the jarFile is read in parallel '''this function has to be
     *      thread safe'''.
     * @param exceptionHandler The exception handler that is called when the reading
     *      of a class file fails. '''This function has to be thread safe'''.
     */
    def ClassFiles(
        zipFile:          ZipFile,
        classFileHandler: (ClassFile, URL) => Unit,
        exceptionHandler: ExceptionHandler
    ): Unit = {
        val zipFileURL = new File(zipFile.getName).toURI.toURL.toExternalForm
        val jarFileName = s"jar:$zipFileURL!/"
        ClassFiles(jarFileName, zipFile, classFileHandler, exceptionHandler)
    }

    private def ClassFiles(
        jarFileURL:       String, // the complete path to the given jar file.
        jarFile:          ZipFile,
        classFileHandler: (ClassFile, URL) => Unit,
        exceptionHandler: ExceptionHandler
    ): Unit = {

        // First let's collect all inner Jar Entries, then do the processing.
        // Otherwise - if the OPALExecutionContextTaskSupport uses a fixed
        // sized thread pool - we may run out of threads... to process anything.
        val innerJarEntries = new ConcurrentLinkedQueue[ZipEntry]

        val jarEntries: Array[ZipEntry] = jarFile.entries().asScala.toArray
        val nextEntryIndex = new AtomicInteger(jarEntries.length - 1)
        val parallelismLevel = NumberOfThreadsForIOBoundTasks
        val futures: Array[Future[Unit]] = new Array(parallelismLevel)
        var futureIndex = 0
        while (futureIndex < parallelismLevel) {
            futures(futureIndex) = Future[Unit] {
                var index = -1
                while ({ index = nextEntryIndex.getAndDecrement; index } >= 0) {
                    val jarEntry = jarEntries(index)
                    if (!jarEntry.isDirectory && jarEntry.getSize > 0) {
                        val jarEntryName = jarEntry.getName
                        if (jarEntryName.endsWith(".class")) {
                            try {
                                val url = new URL(jarFileURL + jarEntry.getName)
                                val classFiles = ClassFile(jarFile, jarEntry)
                                classFiles foreach (classFile => classFileHandler(classFile, url))
                            } catch {
                                case ct: ControlThrowable => throw ct
                                case t: Throwable         => exceptionHandler(jarEntryName, t)
                            }
                        } else if (isClassFileRepository(jarEntryName, Some(jarFile.getName))) {
                            innerJarEntries.add(jarEntry)
                        }
                    }
                }
            }(org.opalj.concurrent.OPALHTBoundedExecutionContext)
            futureIndex += 1
        }
        while ({ futureIndex -= 1; futureIndex } >= 0) {
            Await.ready(futures(futureIndex), Duration.Inf)
        }

        innerJarEntries.iterator().forEachRemaining { jarEntry =>
            // TODO make this commons.vfs compatible...
            // To read the nested jars directly without savin them in temp
            // https://stackoverflow.com/questions/9661214/uri-for-nested-zip-files-in-apaches-common-vfs
            // jar:jar/...!/...!...
            val nextJarFileURL = s"${jarFileURL}jar:${jarEntry.getName}!/"
            try {
                val jarData = new Array[Byte](jarEntry.getSize.toInt)
                val din = new DataInputStream(jarFile.getInputStream(jarEntry))
                din.readFully(jarData)
                din.close()
                ClassFiles(nextJarFileURL, jarData, classFileHandler, exceptionHandler)
            } catch {
                case ct: ControlThrowable => throw ct
                case t: Throwable         => exceptionHandler(nextJarFileURL, t)
            }
        }
    }

    /**
     * Loads class files from an in-memory representation of a jar file given in form
     * of a byte array.
     * This is done by writing the jar file data to a temporary file and then loading
     * the class files from it as done with any other jar file.
     */
    private def ClassFiles(
        jarFileURL:       String,
        jarData:          Array[Byte],
        classFileHandler: (ClassFile, URL) => Unit,
        exceptionHandler: ExceptionHandler
    ): Unit = {
        val pathToEntry = jarFileURL.substring(0, jarFileURL.length - 2)
        val entry = pathToEntry.substring(pathToEntry.lastIndexOf('/') + 1 /* the '/' */ + 4 /* "jar:" */ )
        try {
            val jarFile = File.createTempFile(entry, ".zip")

            process { new java.io.FileOutputStream(jarFile) } { fout => fout.write(jarData) }
            ClassFiles(jarFileURL, new ZipFile(jarFile), classFileHandler, exceptionHandler)

            jarFile.delete()
        } catch {
            case ct: ControlThrowable => throw ct
            case t: Throwable         => exceptionHandler(pathToEntry, t)
        }
    }

    private[this] def processJar(
        file:             File,
        exceptionHandler: ExceptionHandler = defaultExceptionHandler
    ): List[(ClassFile, URL)] = {
        try {
            process(new ZipFile(file)) { zf => ClassFiles(zf, exceptionHandler) }
        } catch {
            case e: Exception => { exceptionHandler(file, e); Nil }
        }
    }

    private[this] def processClassFile(
        file:             File,
        exceptionHandler: ExceptionHandler = defaultExceptionHandler
    ): List[(ClassFile, URL)] = {
        try {
            process(
                new DataInputStream(new BufferedInputStream(new FileInputStream(file)))
            ) { in => ClassFile(in).map(classFile => (classFile, file.toURI.toURL)) }
        } catch {
            case e: Exception => { exceptionHandler(file, e); Nil }
        }
    }

    /**
     * Loads class files from the given file location.
     *  - If the file denotes a single ".class" file this class file is loaded.
     *  - If the file object denotes a ".jar|.war|.ear|.zip" file, all class files in the
     *    jar file will be loaded.
     *  - If the file object specifies a directory object, all ".class" files
     *    in the directory and in all subdirectories are loaded as well as all
     *    class files stored in ".jar" files in one of the directories. This class loads
     *    all class files in parallel. However, this does not effect analyses working on the
     *    resulting `List`.
     */
    def ClassFiles(
        file:             File,
        exceptionHandler: ExceptionHandler = defaultExceptionHandler
    ): List[(ClassFile, URL)] = {

        if (!file.exists()) {
            Nil
        } else if (file.isFile) {
            val filename = file.getName
            if (file.length() == 0) Nil
            else if (isClassFileRepository(filename, None)) processJar(file)
            else if (filename.endsWith(".class")) processClassFile(file)
            else Nil
        } else if (file.isDirectory) {
            val jarFiles = ArrayBuffer.empty[File]
            val classFiles = ArrayBuffer.empty[File]
            def collectFiles(files: Array[File]): Unit = {
                if (files eq null)
                    return ;

                files foreach { file =>
                    val filename = file.getName
                    if (file.isFile) {
                        if (file.length() == 0) Nil
                        else if (isClassFileRepository(filename, None)) jarFiles += file
                        else if (filename.endsWith(".class")) classFiles += file
                    } else if (file.isDirectory) {
                        collectFiles(file.listFiles())
                    } else {
                        info(
                            "class file reader",
                            s"ignored: $file it is neither a file nor a directory"
                        )
                    }
                }
            }
            // 1. get the list of all files in the directory as well as all subdirectories
            collectFiles(file.listFiles())

            // 2. get all class files
            var allClassFiles = List.empty[(ClassFile, URL)]

            // 2.1 load - in parallel - all ".class" files
            if (classFiles.nonEmpty) {
                val theClassFiles = new ConcurrentLinkedQueue[(ClassFile, URL)]
                parForeachSeqElement(classFiles.toIndexedSeq, NumberOfThreadsForIOBoundTasks) { classFile =>
                    theClassFiles.addAll(processClassFile(classFile, exceptionHandler).asJava)
                }
                allClassFiles ++= theClassFiles.asScala
            }

            // 2.2 load - one after the other - all ".jar" files (processing jar files
            //    is already parallelized.)
            jarFiles.foreach { jarFile => allClassFiles ++= processJar(jarFile) }

            // 3. return all loaded class files
            allClassFiles
        } else {
            throw new UnknownError(s"$file is neither a file nor a directory")
        }
    }

    def AllClassFiles(
        files:            Iterable[File],
        exceptionHandler: ExceptionHandler = defaultExceptionHandler
    ): Iterable[(ClassFile, URL)] = {
        files.flatMap(file => ClassFiles(file, exceptionHandler))
    }

    /** Returns the class files of the current Java Runtime Image grouped by module. */
    def JRTClassFiles: Iterable[(String, List[(ClassFile, URL)])] = {
        def traverseModule(module: Path): List[(ClassFile, URL)] = {
            var allClassFiles = List.empty[(ClassFile, URL)]

            def traversePath(p: Path): Unit = {
                if (Files.isDirectory(p)) {
                    try {
                        for (subPath <- Files.newDirectoryStream(p, "*").asScala) {
                            traversePath(subPath)
                        }
                    } catch {
                        case e: Exception => {
                            error(
                                "class file reader",
                                "failed processing Java 9+ Runtime Image (jrt:/)",
                                e
                            )
                        }
                    }
                } else if (p.getFileName.toString.endsWith(".class")) {
                    val cf = ClassFile(() => Files.newInputStream(p))
                    allClassFiles = cf.map(c => (c, p.toUri.toURL)) ++: allClassFiles
                }
            }
            traversePath(module)
            allClassFiles
        }

        val allModulesPath = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("/modules")
        for {
            modulePath <- Files.newDirectoryStream(allModulesPath, "*").asScala
            if Files.isDirectory(modulePath)
        } yield {
            (modulePath.getFileName.toString, traverseModule(modulePath))
        }
    }

    /**
     * Goes over all files in parallel and calls back the given function which has to be thread-safe!
     */
    def processClassFiles(
        files:              Iterable[File],
        progressReporter:   File => Unit,
        classFileProcessor: ((ClassFile, URL)) => Unit,
        exceptionHandler:   ExceptionHandler           = defaultExceptionHandler
    ): Unit = {
        val ts = Tasks[File] { (tasks: Tasks[File], file: File) =>
            if (file.isFile && file.length() > 0) {
                val filename = file.getName

                if (isClassFileRepository(filename, None)) {
                    if (!filename.endsWith("-javadoc.jar") &&
                        !filename.endsWith("-sources.jar")) {
                        progressReporter(file)
                        processJar(file, exceptionHandler).foreach(classFileProcessor)
                    }
                } else if (filename.endsWith(".class")) {
                    progressReporter(file)
                    processClassFile(file, exceptionHandler).foreach(classFileProcessor)
                }
            } else if (file.isDirectory) {
                progressReporter(file)
                file.listFiles().foreach(tasks.submit)
            }
        }(
            // We need a fresh/privately owned execution context with a fixed number of threads
            // to avoid that – if the processor also uses the fixed size pool –
            // we potentially run out of threads!
            BoundedExecutionContext(
                "ClassFileReader.processClassFiles",
                NumberOfThreadsForIOBoundTasks
            )
        )
        files.foreach(ts.submit)
        ts.join()
    }

    /**
     * Searches for the first class file that is accepted by the filter. If no class file
     * can be found that is accepted by the filter the set of all class names is returned.
     *
     * @param files Some file. If the file names a .jar file the .jar file is opened and
     *              searched for a corresponding class file. If the file identifies a "directory"
     *              then, all files in that directory are processed.
     */
    def findClassFile(
        files:            Iterable[File],
        progressReporter: File => Unit,
        classFileFilter:  ClassFile => Boolean,
        className:        ClassFile => String,
        exceptionHandler: ExceptionHandler     = defaultExceptionHandler
    ): Either[(ClassFile, URL), Set[String]] = {
        var classNames = Set.empty[String]
        files.filter(_.exists()) foreach { file =>
            if (file.isFile && file.length() > 0) {
                val filename = file.getName
                (
                    if (isClassFileRepository(filename, None)) {
                        if (!filename.endsWith("-javadoc.jar") &&
                            !filename.endsWith("-sources.jar")) {
                            progressReporter(file)
                            processJar(file, exceptionHandler)
                        } else {
                            Nil
                        }
                    } else if (filename.endsWith(".class")) {
                        progressReporter(file)
                        processClassFile(file, exceptionHandler)
                    } else {
                        Nil
                    }
                ) filter { cfSource =>
                        val (cf, _) = cfSource
                        classNames += className(cf)
                        classFileFilter(cf)
                    } foreach { e => return Left(e); }
            } else if (file.isDirectory) {
                file.listFiles { (dir: File, name: String) =>
                    dir.isDirectory || isClassFileRepository(file.toString, None)
                } foreach { f =>
                    findClassFile(
                        List(f), progressReporter, classFileFilter, className, exceptionHandler
                    ) match {
                            case Left(cf) =>
                                return Left(cf);
                            case Right(moreClassNames) =>
                                classNames ++= moreClassNames
                            /*nothing else to do... let's continue*/
                        }
                }
            }
        }
        Right(classNames)
    }
}

/**
 * Helper methods related to reading class files.
 */
object ClassFileReader {

    type ExceptionHandler = (AnyRef, Throwable) => Unit

    final val SuppressExceptionHandler: ExceptionHandler = (_, _) => {}

}
