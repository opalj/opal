/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.reader

import java.io.{ InputStream, DataInputStream, BufferedInputStream }
import java.util.zip.{ ZipFile, ZipEntry }

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 * Abstract trait that implements a template method to read in a Java class file.
 *
 * This library supports class files from version 45 (Java 1.1) up to
 * (including) version 50 (Java 6). Version 51 (Java 7) is currently only
 * partially supported.
 *
 * '''Notes for Implementors'''
 *
 * Reading of the class file's major structures: the Constant Pool, Field/Method
 * declarations, the set of implemented Interfaces, and the Attributes is
 * delegated to special readers. This enables a very high-level of adaptability.
 *
 * For details see the JVM Specification: The ClassFile Structure.
 *
 * Format
 * {{{
 * ClassFile {
 * 	u4 magic;
 * 	u2 minor_version;
 * 	u2 major_version;
 * 	u2 constant_pool_count;
 * 	cp_info constant_pool[constant_pool_count-1];
 * 	u2 access_flags;
 * 	u2 this_class;
 * 	u2 super_class;
 * 	u2 interfaces_count;
 * 	u2 interfaces[interfaces_count];
 * 	u2 fields_count;
 * 	field_info fields[fields_count];
 * 	u2 methods_count;
 * 	method_info methods[methods_count];
 * 	u2 attributes_count;
 * 	attribute_info attributes[attributes_count];
 * }
 * }}}
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
   * The given stream is positioned
   * at the very beginning of the constant pool. This method is called by the
   * template method that reads in a class file to delegate the reading of the
   * constant pool. Only information belonging to the constant pool are allowed
   * to be read. The stream must not be closed after reading the constant pool.
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
   * '''Implementation Notice'''
   *
   * The given stream is positioned
   * directly before a class file's "attributes_count" field. This method is called by the
   * template method that reads in a class file to delegate the reading of the
   * attributes.
   *
   * '''From the Specification'''
   *
   * The attributes [...] appearing in the attributes table of a ClassFile
   * structure are the InnerClasses, EnclosingMethod, Synthetic, Signature,
   * SourceFile, SourceDebugExtension, Deprecated, RuntimeVisibleAnnotations,
   * RuntimeInvisibleAnnotations, and BootstrapMethods attributes.
I  */
  protected def Attributes(in: DataInputStream, cp: Constant_Pool): Attributes

  /**
   * Factory method to create the object that represents the class file
   * as a whole.
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
    attributes: Attributes)(implicit cp: Constant_Pool): ClassFile

  //
  // IMPLEMENTATION
  //

  /**
   * Reads in a class file.
   *
   * @param create a function that is intended to create a new `InputStream` and
   *  which must not return `null`. If you already do have an open input stream
   *  which should not be closed after reading the class file use [[de.tud.cs.st.bat.reader.ClassFileReader.ClassFile(DataInputStream)]] instead.
   *  The (newly created) InputStream returned by calling `create` is closed by this method.
   *  If the created input stream is not a `DataInputStream` the stream returned by `InputStream`
   *  will be automatically be wrapped.
   */
  def ClassFile(create: () ⇒ InputStream): ClassFile = {
    var in = create();
    if (!in.isInstanceOf[DataInputStream]) {
      if (!in.isInstanceOf[BufferedInputStream]) {
        in = new BufferedInputStream(in)
      }
      in = new DataInputStream(in)
    }
    try {
      ClassFile(in.asInstanceOf[DataInputStream])
    }
    finally {
      in.close
    }
  }

  /**
   * Reads in a single class file from a ZIP/Jar file.
   *
   * @param zipFileName the name of an existing ZIP/JAR file that contains class files.
   * @param zipFileEntryName the name of a class file stored in the specified ZIP/JAR file.
   */
  def ClassFile(zipFileName: String, zipFileEntryName: String): ClassFile = {
    val zipfile = new ZipFile(zipFileName)
    try {
      val zipentry = zipfile.getEntry(zipFileEntryName)
      val in = new DataInputStream(zipfile.getInputStream(zipentry))
      try {
        ClassFile(in)
      }
      finally {
        in.close
      }
    }
    finally {
      zipfile.close
    }
  }

  /**
   * Template method to read in a Java class file from the given input stream.
   *
   * @param in the DataInputStream from which the class file will be read. The
   *  stream is never closed by this method.
   */
  def ClassFile(in: DataInputStream): ClassFile = {
    // magic
    require(ClassFileReader.CLASS_FILE_MAGIC == in.readInt, "No class file.")

    val minor_version = in.readUnsignedShort // minor_version
    val major_version = in.readUnsignedShort // major_version

    // let's make sure that we support this class file's version
    require(major_version >= 45 && // at least JDK 1.1.
      (major_version < 51 ||
        (major_version == 51 && minor_version == 0))) // Java 6 = 50.0; Java 7 == 51.0

    implicit val cp = Constant_Pool(in)
    val access_flags = in.readUnsignedShort
    val this_class = in.readUnsignedShort
    val super_class = in.readUnsignedShort
    val interfaces = Interfaces(in, cp)
    val fields = Fields(in, cp)
    val methods = Methods(in, cp)
    val attributes = Attributes(in, cp)

    ClassFile(
      minor_version, major_version,
      access_flags,
      this_class, super_class, interfaces,
      fields, methods,
      attributes
    )
  }
}

object ClassFileReader {

  /**
   * The magic code with which every Java class file starts.
   */
  val CLASS_FILE_MAGIC = 0xCAFEBABE

}
