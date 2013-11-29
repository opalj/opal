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
package reader

import java.io.{ File, FileInputStream, InputStream, DataInputStream, BufferedInputStream }
import java.util.zip.{ ZipFile, ZipEntry }
import java.net.URL

/**
 * Implements the template method to read in a Java class file. Additionally,
 * several convenience methods are defined to read in class files from various
 * sources (Streams, Files, JAR archives).
 *
 * This library supports class files from version 45 (Java 1.1) up to
 * version 51 (Java 7).
 *
 * ==Notes for Implementors==
 * Reading of the class file's major structures: the constant pool, fields, methods
 * the set of implemented interfaces, and the attributes is
 * delegated to special readers. This enables a very high-level of adaptability.
 *
 * ==Class File Structure==
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
 * For further details see the JVM Specification: The ClassFile Structure.
 *
 * @author Michael Eichberg
 */
trait ClassFileReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    /**
     * The type of the object that represents a Java class file.
     */
    type ClassFile

    /**
     * The type of the object that represents a class's fields.
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

    /**
     * The type of the object that represents the interfaces implemented by
     * a class/interface.
     */
    type Interfaces

    // METHODS DELEGATING TO OTHER READERS
    //

    /**
     * Reads the constant pool using the given stream.
     *
     * When this method is called, the given stream has to be positioned at the very
     * beginning of the constant pool. This method is called by the template method that
     * reads in a class file to delegate the reading of the constant pool. Only
     * information belonging to the constant pool are allowed to be read.
     *
     * The stream must not be closed after reading the constant pool.
     */
    protected def Constant_Pool(in: DataInputStream): Constant_Pool

    /**
     * Reads the information which interfaces are implemented/extended.
     *
     * The given stream is positioned
     * directly before a class file's "interfaces_count" field. This method is called by the
     * template method that reads in a class file to delegate the reading of the
     * extended interfaces.
     */
    protected def Interfaces(in: DataInputStream, cp: Constant_Pool): Interfaces

    /**
     * Reads all field declarations using the given stream and constant pool.
     *
     * The given stream is positioned
     * directly before a class file's "fields_count" field. This method is called by the
     * template method that reads in a class file to delegate the reading of the
     * declared fields.
     */
    protected def Fields(in: DataInputStream, cp: Constant_Pool): Fields

    /**
     * Reads all method declarations using the given stream and constant pool.
     *
     * The given stream is positioned directly before a class file's "methods_count" field.
     * This method is called by the
     * template method that reads in a class file to delegate the reading of the
     * declared method.
     */
    protected def Methods(in: DataInputStream, cp: Constant_Pool): Methods

    /**
     * Reads all attributes using the given stream and constant pool.
     *
     * The given stream is positioned directly before a class file's "attributes_count" field.
     * This method is called by the template method that reads in a class file to delegate the
     * reading of the attributes.
     *
     * '''From the Specification'''
     *
     * The attributes [...] appearing in the attributes table of a ClassFile
     * structure are the InnerClasses, EnclosingMethod, Synthetic, Signature,
     * SourceFile, SourceDebugExtension, Deprecated, RuntimeVisibleAnnotations,
     * RuntimeInvisibleAnnotations, and BootstrapMethods attributes.
     */
    protected def Attributes(
        ap: AttributeParent,
        cp: Constant_Pool,
        in: DataInputStream): Attributes

    /**
     * Factory method to create the object that represents the class file as a whole.
     */
    protected def ClassFile(
        minor_version: Int,
        major_version: Int,
        access_flags: Int,
        this_class: Constant_Pool_Index,
        super_class: Constant_Pool_Index,
        interfaces: Interfaces,
        fields: Fields,
        methods: Methods,
        attributes: Attributes)(
            implicit cp: Constant_Pool): ClassFile

    //
    // IMPLEMENTATION
    //
    import util.ControlAbstractions._

    private[this] var classFilePostProcessors: List[ClassFile ⇒ ClassFile] = Nil

    /**
     * Register a class file post processor. A class file post processor
     * can transform the completely read and reified class file. Post processors
     * can only be registered before the usage of a class file reader. '''Registering
     * new `ClassFilePostProcessors` while processing class files is not supported'''.  
     */
    def registerClassFilePostProcessor(p: ClassFile ⇒ ClassFile): Unit = {
        classFilePostProcessors = p :: classFilePostProcessors
    }

    /**
     * Template method to read in a Java class file from the given input stream.
     *
     * @param in The DataInputStream from which the class file will be read. The
     *    stream is not closed by this method.
     */
    def ClassFile(in: DataInputStream): ClassFile = {
        // magic
        require(CLASS_FILE_MAGIC == in.readInt, "No Java class file.")

        val minor_version = in.readUnsignedShort
        val major_version = in.readUnsignedShort

        // let's make sure that we support this class file's version
        require(
            major_version >= 45 && // at least JDK 1.1
                (major_version < 51 || // Java 6 = 50.0
                    (major_version == 51 && minor_version == 0)), // Java 7 == 51.0
            "Unsupported class file version: "+major_version+"."+minor_version)

        val cp = Constant_Pool(in)
        val access_flags = in.readUnsignedShort
        val this_class = in.readUnsignedShort
        val super_class = in.readUnsignedShort
        val interfaces = Interfaces(in, cp)
        val fields = Fields(in, cp)
        val methods = Methods(in, cp)
        val attributes = Attributes(AttributesParent.ClassFile, cp, in)

        var classFile = ClassFile(
            minor_version, major_version,
            access_flags,
            this_class, super_class, interfaces,
            fields, methods,
            attributes
        )(cp)

        // perform transformations that are specific to this class file
        classFile = applyDeferredActions(classFile, cp)

        // perform general transformations on class files
        classFilePostProcessors.foreach(p ⇒ { classFile = p(classFile) })

        classFile
    }

    //
    // CONVENIENCE METHODS TO LOAD CLASS FILES FROM VARIOUS SOURCES
    //

    /**
     * Reads in a class file from `InputStream`.
     *
     * @param create A function that creates a new `InputStream` and
     *  which must not return `null`. If you already do have an open input stream
     *  which should not be closed after reading the class file use
     *  `de.tud.cs.st.bat.reader.ClassFileReader.ClassFile(DataInputStream)` instead.
     *  The (newly created) InputStream returned by calling `create` is closed by this method.
     *  The created input stream will automatically be wrapped by BAT to enable efficient reading of the
     *  class file.
     */
    def ClassFile(create: () ⇒ InputStream): ClassFile = {
        process(create() match {
            case dis: DataInputStream ⇒ dis
            case is                   ⇒ new DataInputStream(new BufferedInputStream(is))
        }) {
            ClassFile(_)
        }
    }

    protected[this] def ClassFile(jarFile: ZipFile, jarEntry: ZipEntry): ClassFile = {
        ClassFile(() ⇒ jarFile.getInputStream(jarEntry))
    }

    /**
     * Reads in a single class file from a Jar file.
     *
     * @param jarFileName the name of an existing ZIP/JAR file that contains class files.
     * @param jarFileEntryName the name of a class file stored in the specified ZIP/JAR file.
     */
    def ClassFile(jarFileName: String, jarFileEntryName: String): ClassFile = {
        ClassFile(new File(jarFileName), jarFileEntryName)
    }

    /**
     * Reads in a single class file from a Jar file.
     *
     * @param jarFile an existing ZIP/JAR file that contains class files.
     * @param jarFileEntryName the name of a class file stored in the specified ZIP/JAR file.
     */
    def ClassFile(jarFile: File, jarFileEntryName: String): ClassFile = {
        process { new ZipFile(jarFile) } { zf ⇒
            val jarEntry = zf.getEntry(jarFileEntryName)
            ClassFile(zf, jarEntry)
        }
    }

    /**
     * Reads in parallel all class files stored in the given zip file.
     */
    def ClassFiles(jarFile: ZipFile): Seq[(ClassFile, URL)] = {
        val mutex = new Object
        var classFiles: List[(ClassFile, URL)] = Nil

        def addClassFile(jf: ZipFile, je: ZipEntry, cf: ClassFile) = {
            val jarFileURL = new File(jf.getName()).toURI().toURL().toExternalForm()
            val url = new URL("jar:"+jarFileURL+"!/"+je.getName())
            mutex.synchronized {
                classFiles = (cf, url) :: classFiles
            }
        }

        ClassFiles(jarFile, addClassFile)
        classFiles
    }

    /**
     * Reads '''in parallel''' all class files stored in the given jar file. For each
     * successfully read class file the function `f` is called.
     *
     * @param jarFile A valid jar file that contains `.class` files; other files
     *      are ignored.
     * @param f The function that is called for each class file in the given jar file.
     *      Given that the jarFile is read in parallel '''this function has to be
     *      thread safe'''.
     * @param exceptionHandler The exception handler that is called when the reading
     *      of a class file fails.
     */
    // TODO  [Improvement][ClassFileReader] Support reading of jar files within jar files.
    def ClassFiles(
        jarFile: ZipFile,
        f: (ZipFile, ZipEntry, ClassFile) ⇒ Unit,
        exceptionHandler: (Exception) ⇒ Unit = e ⇒ Console.err.println(e)) {

        import collection.JavaConversions._
        for (jarEntry ← (jarFile).entries.toIterable.par) {
            if (!jarEntry.isDirectory) {
                val jarEntryName = jarEntry.getName
                if (jarEntryName.endsWith(".class")) {
                    try {
                        val classFile = ClassFile(jarFile, jarEntry)
                        f(jarFile, jarEntry, classFile)
                    } catch {
                        case e: Exception ⇒ exceptionHandler(e)
                    }
                }
            }
        }
    }

    /**
     * Reads all class files from the given jar file.
     */
    def ClassFiles(jarFileName: String): Seq[(ClassFile, URL)] =
        process(new ZipFile(jarFileName)) { zf ⇒ ClassFiles(zf) }

    /**
     * Loads class files from the given file location. If the file denotes
     * a single ".class" file this class file is loaded. If the file
     * object denotes a ".jar" file, all class files in the jar file will be loaded.
     * If the file object specifies a directory object, all ".class" files
     * in the directory and in all subdirectories are loaded as well as all
     * class files stored in ".jar" files in one of the directories. This class loads
     * all class files in parallel. However, this does not effect analyses working on the
     * resulting `Seq`.
     */
    def ClassFiles(file: File): Seq[(ClassFile, URL)] = {
        if (file.isFile()) {
            if (file.getName.endsWith(".jar")) {
                println("Processing: "+file.toString)
                return ClassFiles(file.getAbsoluteFile.getPath)
            }

            if (file.getName.endsWith(".class"))
                return List(
                    (
                        ClassFile(() ⇒ new FileInputStream(file)),
                        file.toURI().toURL()
                    )
                )

            return Nil
        }

        // file.isDirectory
        (for (innerFile ← file.listFiles().par) yield ClassFiles(innerFile)).seq.flatten
    }

}

