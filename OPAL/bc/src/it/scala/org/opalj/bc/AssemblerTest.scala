/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.DataInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.ByteArrayInputStream
import java.io.DataOutputStream
import java.util.zip.ZipFile
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters._
import org.opalj.io.FailAfterByteArrayOutputStream
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.da.ClassFileReader.{ClassFile => LoadClassFile}

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Tests the assembler by loading and writing a large number of class files and by
 * comparing the output with the original class file. In this case the output has to
 * identical at the byte level.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AssemberTest extends AnyFlatSpec with Matchers {

    behavior of "the Assembler"

    val jmodsFile =
        locateTestResources("classfiles/Java9-selected-jmod-module-info.classes.zip", "bi")
    for {
        file <- JRELibraryFolder.listFiles() ++ allBITestJARs() ++ List(jmodsFile)
        if file.isFile
        if file.canRead
        if file.length() > 0
        fileName = file.getName
        if fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".jmod")
    } {
        it should s"be able to recreate every class of $file" in {
            val entriesCount = new AtomicInteger(0)

            val Lock = new Object
            var exceptions: List[Throwable] = List.empty[Throwable]

            val zipFile = new ZipFile(file)
            zipFile.entries().asScala.filter(_.getName.endsWith(".class")).toList.par.foreach { ze =>
                val (classFile, raw) = {
                    val file = zipFile.getInputStream(ze)
                    val classFileSize = ze.getSize.toInt
                    val raw = new Array[Byte](classFileSize)
                    val bin = new BufferedInputStream(file, classFileSize)
                    val bytesRead = bin.read(raw, 0, classFileSize)
                    assert(bytesRead == classFileSize, "the class file was not successfully read")
                    (
                        LoadClassFile(new DataInputStream(new ByteArrayInputStream(raw))).head,
                        raw
                    )
                }

                try {
                    var segmentInformation: List[(String, Int)] = Nil
                    val reassembledClassFile: Array[Byte] =
                        try {
                            Assembler(classFile, (s, w) => segmentInformation ::= ((s, w)))
                        } catch {
                            case t: Throwable =>
                                t.printStackTrace()
                                throw t;
                        }
                    segmentInformation = segmentInformation.reverse

                    // let's check all bytes for similarity
                    reassembledClassFile.zip(raw).zipWithIndex foreach { e =>
                        val ((c, r), i) = e
                        if (c != r) {
                            val (succeeded, remaining) = segmentInformation.partition(_._2 < i)
                            val failedSegment = remaining.head._1

                            try {
                                val size = raw.length
                                val failAfterStream = new FailAfterByteArrayOutputStream(i)(size)
                                Assembler.serialize(classFile)(
                                    Assembler.RichClassFile,
                                    new DataOutputStream(failAfterStream),
                                    (s, i) => {}
                                )
                            } catch {
                                case ioe: IOException => ioe.printStackTrace()
                            }
                            val successfullyReadBytes =
                                s"(i.e., successfully read ${succeeded.last._2} bytes)"
                            val message =
                                s"the class files differ starting with index $i ($failedSegment): "+
                                    s"found $c but expected $r"+
                                    succeeded.map(_._1).mkString(
                                        "; successfully read segments: ",
                                        ",",
                                        successfullyReadBytes
                                    )
                            fail(message)
                        }
                    }
                    entriesCount.incrementAndGet()
                } catch {
                    case e: Exception =>
                        Lock.synchronized {
                            val details = e.getMessage + e.getClass.getSimpleName
                            val message = s"failed: $ze(${classFile.thisType}); message:$details"
                            val newException = new RuntimeException(message, e)
                            exceptions ::= newException
                        }
                }
            }

            if (exceptions.nonEmpty) {
                val succeededCount = entriesCount.get
                val message =
                    exceptions.mkString(
                        s"assembling the class file failed for :\n",
                        "\n",
                        s"\n${exceptions.size} class files (and succeeded for: $succeededCount)\n"
                    )
                fail(message)
            } else {
                info(s"successfully processed ${entriesCount.get} class files")
            }
        }
    }
}
