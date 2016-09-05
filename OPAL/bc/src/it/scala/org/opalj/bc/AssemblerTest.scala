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
package bc

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.BufferedInputStream
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._
import org.opalj.da.ClassFileReader.{ClassFile ⇒ LoadClassFile}
import java.io.ByteArrayInputStream

import org.opalj.io.FailAfterByteArrayOutputStream
import java.io.IOException
import java.io.DataOutputStream
import org.opalj.bi.TestSupport

/**
 * Tests the assembler by loading and writing a large number of class files and by
 * comparing the output with the original class file.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AssemberTest extends FlatSpec with Matchers {

    behavior of "the Assembler"

    val jreLibFolder = org.opalj.bytecode.JRELibraryFolder
    val biClassfilesFolder = TestSupport.locateTestResources("classfiles", "bi")

    for {
        file ← jreLibFolder.listFiles() ++ biClassfilesFolder.listFiles()
        if file.isFile && file.canRead && file.getName.endsWith(".jar") && file.length() > 0
    } {

        it should (s"be able to process every class of $file") in {

            val zipFile = new ZipFile(file)
            val entriesCount = new AtomicInteger(0)

            val Lock = new Object
            var exceptions: List[Throwable] = Nil

            zipFile.entries().asScala.filter(_.getName.endsWith(".class")).toList.par.foreach { ze ⇒

                val (classFile, raw) = {
                    val file = zipFile.getInputStream(ze)
                    val classFileSize = ze.getSize.toInt
                    val raw = new Array[Byte](classFileSize)
                    val bin = new BufferedInputStream(file, classFileSize)
                    val bytesRead = bin.read(raw, 0, classFileSize)
                    assert(bytesRead == classFileSize, "the class file was not successfully read")
                    (
                        LoadClassFile { new DataInputStream(new ByteArrayInputStream(raw)) }.head,
                        raw
                    )
                }

                try {
                    var segmentInformation: List[(String, Int)] = Nil

                    val reassembledClassFile =
                        try {
                            Assembler(classFile, (s, w) ⇒ segmentInformation ::= ((s, w)))
                        } catch {
                            case t: Throwable ⇒ t.printStackTrace(); throw t
                        }
                    segmentInformation = segmentInformation.reverse

                    // let's check all bytes for similarity
                    reassembledClassFile.zip(raw).zipWithIndex.foreach { e ⇒
                        val ((c, r), i) = e
                        if (c != r) {
                            val (succeededSegments, remainingSegments) = segmentInformation.partition(_._2 < i)
                            val failedSegment = remainingSegments.head._1

                            try {
                                Assembler.serialize(classFile)(
                                    Assembler.RichClassFile,
                                    new DataOutputStream(new FailAfterByteArrayOutputStream(i)(raw.length)),
                                    (s, i) ⇒ {}
                                )
                            } catch {
                                case ioe: IOException ⇒ ioe.printStackTrace()
                            }
                            val message =
                                s"the class files differ starting with index $i ($failedSegment): "+
                                    s"found $c but expected $r"+
                                    succeededSegments.map(_._1).mkString(
                                        "; successfully read segments: ",
                                        ",",
                                        s"(i.e., successfully read ${succeededSegments.last._2} bytes)"
                                    )
                            fail(message)
                        }
                    }
                    entriesCount.incrementAndGet()
                } catch {
                    case e: Exception ⇒
                        Lock.synchronized {
                            val message = s"failed: $ze(${classFile.fqn}); message:"+e.getMessage + e.getClass.getSimpleName
                            val newException = new RuntimeException(message, e)
                            exceptions = newException :: exceptions
                        }
                }
            }

            if (exceptions.nonEmpty) {
                val message =
                    exceptions.mkString(
                        s"assembling the class file failed for :\n",
                        "\n",
                        s"\n${exceptions.size} class files (and succeeded for: ${entriesCount.get})\n"
                    )
                fail(message)
            } else {
                info(s"sucessfully processed ${entriesCount.get} class files")
            }
        }
    }
}
