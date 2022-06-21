/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.nio.file.Files
import java.nio.file.Path
import java.io.File
import java.io.IOException
import java.io.Closeable
import java.awt.Desktop
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

import scala.io.Source
import scala.xml.Node

/**
 * Various io-related helper methods and classes.
 *
 * @note The implementations of the methods rely on Java NIO(2).
 *
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 */
package object io {

    /**
     * Replaces characters in the given file name (segment) that are (potentially) problematic
     * on some file system and also shortens the filename
     *
     * @see For more information visit https://en.wikipedia.org/wiki/Filename
     *
     * @param fileName The filename or a suffix/prefix thereof which should be sanitized.
     *
     * @return The sanitized file name.
     *
     */
    def sanitizeFileName(fileName: String): String = {
        // take(128+64) ... to have some space for something else...
        fileName.filterNot(_ == ' ').replaceAll("[\\/:*?\"<>|\\[\\]=!@,]", "_").take(128 + 64)
    }

    /**
     * Writes the XML document to a temporary file and opens the file in the
     * OS's default application.
     *
     * @param filenamePrefix A string the identifies the content of the file. (E.g.,
     *      "ClassHierarchy" or "CHACallGraph")
     * @param filenameSuffix The suffix of the file that identifies the used file format.
     *      (E.g., ".xhtml")
     * @return The name of the file if it was possible to write the file and open
     *      the native application.
     */
    @throws[IOException]("if it is not possible to create a temporary file")
    @throws[OpeningFileFailedException]("if it is not possible to open the file")
    def writeAndOpen(
        node:           Node,
        filenamePrefix: String,
        filenameSuffix: String
    ): File = {
        val data = node.toString
        writeAndOpen(data, filenamePrefix, filenameSuffix)
    }

    /**
     * Writes the given string (`data`) to a temporary file using the given prefix and suffix.
     * Afterwards the system's native application that claims to be able to handle
     * files with the given suffix is opened. If this fails, the string is printed to
     * the console.
     *
     * The string is always written using UTF-8 as the encoding.
     *
     * @param filenamePrefix A string the identifies the content of the file. (E.g.,
     *      "ClassHierarchy" or "CHACallGraph")
     * @param filenameSuffix The suffix of the file that identifies the used file format.
     *      (E.g., ".txt")
     * @return The name of the file if it was possible to write the file and open
     *      the native application.
     * @example
     *      Exemplary usage:
     *      {{{
     *      try {
     *          util.writeAndOpen("The Message", "Result", ".txt")
     *      } catch {
     *          case OpeningFileFailedException(file, _) =>
     *              Console.err.println("Details can be found in: "+file.toString)
     *      }}}
     */
    @throws[IOException]("if it is not possible to create a temporary file")
    @throws[OpeningFileFailedException]("if it is not possible to open the file")
    def writeAndOpen(
        data:           String,
        filenamePrefix: String,
        filenameSuffix: String
    ): File = {
        val file = write(data, filenamePrefix, filenameSuffix).toFile
        open(file)
        file
    }

    def open(file: File): Unit = {
        try {
            Desktop.getDesktop.open(file)
        } catch {
            case t: Throwable => throw OpeningFileFailedException(file, t)
        }
    }

    def write(
        data:           String,
        filenamePrefix: String,
        filenameSuffix: String
    ): Path = {

        write(Seq(data), filenamePrefix, filenameSuffix)
    }

    def write(
        data:           IterableOnce[String],
        filenamePrefix: String,
        filenameSuffix: String
    ): Path = {

        val path = Files.createTempFile(
            sanitizeFileName(filenamePrefix),
            sanitizeFileName(filenameSuffix)
        )
        write(data.iterator.map(_.getBytes("UTF-8")), path)
        path
    }

    def writeGZip(
        data:           String,
        filenamePrefix: String,
        filenameSuffix: String
    ): Path = {

        writeGZip(Seq(data), filenamePrefix, filenameSuffix)
    }

    def writeGZip(
        data:           IterableOnce[String],
        filenamePrefix: String,
        filenameSuffix: String
    ): Path = {

        val path = Files.createTempFile(
            sanitizeFileName(filenamePrefix),
            sanitizeFileName(filenameSuffix)
        )
        writeGZip(data.iterator.map(_.getBytes("UTF-8")), path)
        path
    }

    /**
     * A simple wrapper for `java.nio.Files.write(Path,byte[])`.
     */
    def write(data: Array[Byte], path: Path): Unit = Files.write(path, data)

    def write(data: IterableOnce[Array[Byte]], path: Path): Unit = {
        val out = new FileOutputStream(path.toFile)
        try {
            data.iterator.foreach(out.write)
        } finally {
            out.close()
        }
    }

    def writeGZip(data: Array[Byte], path: Path): Unit = {
        writeGZip(Seq(data), path)
    }

    def writeGZip(data: IterableOnce[Array[Byte]], path: Path): Unit = {
        val out = new GZIPOutputStream(new FileOutputStream(path.toFile))
        try {
            data.iterator.foreach(out.write)
        } finally {
            out.close()
        }
    }

    /**
     * This function takes a `Closeable` resource and a function `r` that will
     * process the `Closeable` resource.
     * This function takes care of the correct handling of `Closeable` resources.
     * When `r` has finished processing the resource or throws an exception, the
     * resource is closed.
     *
     * @note If `closable` is `null`, `null` is passed to `r`.
     *
     * @param closable The `Closeable` resource.
     * @param r The function that processes the `resource`.
     */
    def process[C <: Closeable, T](closable: C)(r: C => T): T = {
        // Implementation Note
        // Creating the closeable (I) in the try block doesn't make sense, hence
        // we don't need a by-name parameter. (If creating the closable fails,
        // then there is nothing to close.)
        try {
            r(closable)
        } finally {
            if (closable != null) closable.close()
        }
    }

    /**
     * This function takes a `Source` object and a function `r` that will
     * process the source.
     * This function takes care of the correct handling of resources.
     * When `r` has finished processing the source or throws an exception,
     * the source is closed.
     *
     * @note If `source` is `null`, `null` is passed to `r`.
     */
    def processSource[C <: Source, T](source: C)(r: C => T): T = {
        try {
            r(source)
        } finally {
            if (source != null) source.close()
        }
    }

}
